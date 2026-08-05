package com.nursery.slideshow.videojob.ffmpeg;

import com.nursery.slideshow.videojob.VideoGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FfmpegExecutor {

    private static final Pattern TIME_PATTERN = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)");

    private final FfmpegProperties properties;

    public FfmpegExecutor(FfmpegProperties properties) {
        this.properties = properties;
    }

    public String run(List<String> arguments) {
        return run(arguments, null);
    }

    /**
     * ffmpegを実行する。onEncodedSecondsを指定すると、標準エラー出力に流れてくる
     * "time=00:00:03.50"形式の進捗行を都度パースし、エンコード済み秒数を通知する
     * (動画作成中画面のプログレスバーをリアルタイムに近い形で進めるため)。
     */
    public String run(List<String> arguments, DoubleConsumer onEncodedSeconds) {
        List<String> command = new ArrayList<>();
        command.add(properties.getBinaryPath());
        command.addAll(arguments);

        log.info("Executing ffmpeg command: {}", String.join(" ", command));

        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new VideoGenerationException("FFmpegプロセスの起動に失敗しました", e);
        }

        // 出力の読み取りはプロセスの終了待機とは別スレッドで行う。
        // 同一スレッドで読み取ってからwaitForすると、プロセスがハングした際に
        // 出力読み取り自体が無制限にブロックし、タイムアウトが機能しなくなるため。
        OutputGobbler outputGobbler = new OutputGobbler(process.getInputStream(), onEncodedSeconds);
        Thread outputThread = new Thread(outputGobbler, "ffmpeg-output-reader");
        outputThread.setDaemon(true);
        outputThread.start();

        long timeoutSeconds = properties.getTimeoutSeconds();
        boolean finishedInTime;
        try {
            finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new VideoGenerationException("FFmpeg実行が中断されました", e);
        }

        if (!finishedInTime) {
            log.error("FFmpeg process timed out after {} seconds and will be forcibly terminated. command={}",
                    timeoutSeconds, String.join(" ", command));
            process.destroyForcibly();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new VideoGenerationException("FFmpegの実行がタイムアウトしました(" + timeoutSeconds + "秒)");
        }

        joinQuietly(outputThread);
        String output = outputGobbler.getOutput();

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("FFmpeg failed (exit={}): {}", exitCode, output);
            throw new VideoGenerationException("FFmpegの実行に失敗しました(exit=" + exitCode + ")");
        }

        return output;
    }

    private void joinQuietly(Thread thread) {
        try {
            thread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class OutputGobbler implements Runnable {
        private final InputStream inputStream;
        private final DoubleConsumer onEncodedSeconds;
        private volatile String output = "";

        OutputGobbler(InputStream inputStream, DoubleConsumer onEncodedSeconds) {
            this.inputStream = inputStream;
            this.onEncodedSeconds = onEncodedSeconds;
        }

        @Override
        public void run() {
            StringBuilder collected = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    collected.append(line).append('\n');
                    if (onEncodedSeconds != null) {
                        reportProgressIfPresent(line);
                    }
                }
            } catch (IOException e) {
                log.warn("FFmpeg出力の読み取り中にエラーが発生しました", e);
            } finally {
                output = collected.toString();
            }
        }

        private void reportProgressIfPresent(String line) {
            Matcher matcher = TIME_PATTERN.matcher(line);
            if (!matcher.find()) {
                return;
            }
            double hours = Double.parseDouble(matcher.group(1));
            double minutes = Double.parseDouble(matcher.group(2));
            double seconds = Double.parseDouble(matcher.group(3));
            onEncodedSeconds.accept(hours * 3600 + minutes * 60 + seconds);
        }

        String getOutput() {
            return output;
        }
    }
}
