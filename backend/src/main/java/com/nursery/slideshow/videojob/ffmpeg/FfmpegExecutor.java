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
import java.util.stream.Collectors;

@Slf4j
@Service
public class FfmpegExecutor {

    private final FfmpegProperties properties;

    public FfmpegExecutor(FfmpegProperties properties) {
        this.properties = properties;
    }

    public String run(List<String> arguments) {
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
        OutputGobbler outputGobbler = new OutputGobbler(process.getInputStream());
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
        private volatile String output = "";

        OutputGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                log.warn("FFmpeg出力の読み取り中にエラーが発生しました", e);
            }
        }

        String getOutput() {
            return output;
        }
    }
}
