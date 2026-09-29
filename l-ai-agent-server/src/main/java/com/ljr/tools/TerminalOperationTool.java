package com.ljr.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * 终端命令执行工具。
 */
@Component
public class TerminalOperationTool {

    @Tool(description = "在本地执行一条终端命令并返回输出，仅用于安全的查询类操作")
    public String executeTerminalCommand(@ToolParam(description = "要执行的命令，例如 echo hello") String command) {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder builder = isWindows
                    ? new ProcessBuilder("cmd.exe", "/c", command)
                    : new ProcessBuilder("bash", "-c", command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "命令执行超时";
            }
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            String result = output.toString().trim();
            return result.isEmpty() ? "命令执行完成，无输出" : result;
        } catch (Exception e) {
            return "命令执行失败: " + e.getMessage();
        }
    }
}
