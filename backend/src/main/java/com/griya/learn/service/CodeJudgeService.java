package com.griya.learn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CodeJudgeService {

    private static final Logger log = LoggerFactory.getLogger(CodeJudgeService.class);
    private static final int COMPILE_TIMEOUT_SEC = 10;
    private static final int RUN_TIMEOUT_SEC = 5;
    private static final Path WORK_DIR = Path.of("downloadData", "judge");

    public Map<String, Object> judge(Integer questionId, String problem, String userCode,
                                      String inputExample, String outputExample) {
        Map<String, Object> result = new HashMap<>();

        if (userCode == null || userCode.isBlank()) {
            result.put("correct", false);
            result.put("error", "代码不能为空");
            return result;
        }

        try {
            Files.createDirectories(WORK_DIR);
        } catch (IOException e) {
            result.put("correct", false);
            result.put("error", "无法创建判题工作目录");
            return result;
        }

        String sessionId = "j_" + System.currentTimeMillis();
        Path srcFile = WORK_DIR.resolve(sessionId + ".c");
        Path exeFile = WORK_DIR.resolve(sessionId + ".exe");

        try {
            Files.writeString(srcFile, userCode, StandardCharsets.UTF_8);
        } catch (IOException e) {
            result.put("correct", false);
            result.put("error", "写入源代码失败");
            return result;
        }

        // Step 1: compile
        Map<String, Object> compileResult = compile(srcFile, exeFile);
        if (compileResult != null) {
            cleanup(srcFile, exeFile);
            result.putAll(compileResult);
            return result;
        }

        // Step 2: run with input
        Map<String, Object> runResult = run(exeFile, unescapeNewlines(inputExample));
        cleanup(srcFile, exeFile);

        if (runResult.containsKey("error")) {
            result.putAll(runResult);
            return result;
        }

        // Step 3: compare
        String actualOutput = normalizeOutput((String) runResult.get("stdout"));
        String expectedOutput = normalizeOutput(unescapeNewlines(outputExample != null ? outputExample : ""));

        boolean correct = actualOutput.equals(expectedOutput);
        result.put("correct", correct);
        result.put("expected", expectedOutput);
        result.put("actual", actualOutput);
        result.put("stdout", runResult.get("stdout"));
        result.put("stderr", runResult.get("stderr"));

        log.info("[JUDGE] id={}, correct={}", questionId, correct);
        return result;
    }

    private Map<String, Object> compile(Path srcFile, Path exeFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "gcc", srcFile.toString(), "-o", exeFile.toString(),
                "-std=c11", "-Wall", "-O2"
            );
            pb.redirectErrorStream(true);

            Process proc = pb.start();
            boolean finished = proc.waitFor(COMPILE_TIMEOUT_SEC, TimeUnit.SECONDS);

            if (!finished) {
                proc.destroyForcibly();
                return Map.of("correct", false, "compileError", "编译超时（" + COMPILE_TIMEOUT_SEC + "秒）");
            }

            int exitCode = proc.exitValue();
            if (exitCode != 0) {
                String errOutput = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                // sanitize temp path from error messages
                errOutput = errOutput.replace(srcFile.toString(), "main.c");
                errOutput = errOutput.lines()
                    .filter(l -> !l.contains("warning:"))
                    .reduce("", (a, b) -> a + "\n" + b)
                    .trim();
                if (errOutput.length() > 500) errOutput = errOutput.substring(0, 500);
                return Map.of("correct", false, "compileError", errOutput);
            }

            return null; // success
        } catch (IOException e) {
            return Map.of("correct", false, "compileError",
                "编译失败: gcc 未安装或不可用。请确保系统已安装 gcc 并添加到 PATH 环境变量。");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("correct", false, "compileError", "编译被中断");
        }
    }

    private Map<String, Object> run(Path exeFile, String input) {
        try {
            ProcessBuilder pb = new ProcessBuilder(exeFile.toString());
            pb.directory(WORK_DIR.toFile());
            pb.redirectErrorStream(false);

            Process proc = pb.start();

            // feed input
            if (input != null && !input.isEmpty()) {
                try (OutputStream os = proc.getOutputStream()) {
                    os.write(input.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }

            // close stdin
            try { proc.getOutputStream().close(); } catch (Exception ignored) {}

            boolean finished = proc.waitFor(RUN_TIMEOUT_SEC, TimeUnit.SECONDS);

            if (!finished) {
                proc.destroyForcibly();
                return Map.of("correct", false, "error", "运行超时（" + RUN_TIMEOUT_SEC + "秒）");
            }

            String stdout = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = proc.exitValue();

            if (exitCode != 0 && !stderr.isEmpty()) {
                return Map.of("correct", false, "error",
                    "运行时错误 (exit=" + exitCode + "): " + stderr.substring(0, Math.min(200, stderr.length())));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("stdout", stdout);
            result.put("stderr", stderr);
            result.put("exitCode", exitCode);
            return result;

        } catch (IOException e) {
            return Map.of("correct", false, "error", "运行失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("correct", false, "error", "运行被中断");
        }
    }

    private String normalizeOutput(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : raw.replace("\r\n", "\n").split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            sb.append(trimmed).append('\n');
        }
        return sb.toString().trim();
    }

    private String unescapeNewlines(String text) {
        if (text == null) return "";
        return text.replace("\\n", "\n");
    }

    private void cleanup(Path src, Path exe) {
        try { Files.deleteIfExists(src); } catch (Exception ignored) {}
        try { Files.deleteIfExists(exe); } catch (Exception ignored) {}
    }
}
