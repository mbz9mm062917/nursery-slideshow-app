package com.nursery.slideshow.videojob.ffmpeg;

import com.nursery.slideshow.videojob.VideoGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

        String output = readOutput(process);

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoGenerationException("FFmpeg実行が中断されました", e);
        }

        if (exitCode != 0) {
            log.error("FFmpeg failed (exit={}): {}", exitCode, output);
            throw new VideoGenerationException("FFmpegの実行に失敗しました(exit=" + exitCode + ")");
        }

        return output;
    }

    private String readOutput(Process process) {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new VideoGenerationException("FFmpeg出力の読み取りに失敗しました", e);
        }
    }
}
