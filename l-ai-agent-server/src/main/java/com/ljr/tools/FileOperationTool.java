package com.ljr.tools;

import cn.hutool.core.io.FileUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 本地文件读写工具。
 */
@Component
public class FileOperationTool {

    private final String baseDir;

    public FileOperationTool(@Value("${app.file-tool.dir}") String baseDir) {
        this.baseDir = baseDir;
        FileUtil.mkdir(baseDir);
    }

    @Tool(description = "读取指定文件名的文本内容（相对于应用文件目录）")
    public String readFile(@ToolParam(description = "文件名，例如 plan.txt") String fileName) {
        String path = resolve(fileName);
        if (!FileUtil.exist(path)) {
            return "文件不存在: " + fileName;
        }
        return FileUtil.readString(path, StandardCharsets.UTF_8);
    }

    @Tool(description = "将文本内容写入指定文件（相对于应用文件目录），返回保存路径")
    public String writeFile(
            @ToolParam(description = "文件名，例如 kyoto-plan.txt") String fileName,
            @ToolParam(description = "要写入的文本内容") String content) {
        String path = resolve(fileName);
        FileUtil.writeString(content, path, StandardCharsets.UTF_8);
        return "已写入文件: " + path;
    }

    private String resolve(String fileName) {
        String safe = fileName.replace("..", "").replace("/", "").replace("\\", "");
        return baseDir + "/" + safe;
    }
}
