package com.ljr.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 网络资源下载工具。
 */
@Component
public class ResourceDownloadTool {

    private final String baseDir;

    public ResourceDownloadTool(@Value("${app.file-tool.dir}") String baseDir) {
        this.baseDir = baseDir;
        FileUtil.mkdir(baseDir);
    }

    @Tool(description = "仅当用户明确要求把文件保存到本地时，才从直链 URL 下载。展示图片请用 searchImages + Markdown，不要用本工具下图片。")
    public String downloadResource(
            @ToolParam(description = "资源直链 URL") String url,
            @ToolParam(description = "保存文件名，例如 note.pdf") String fileName) {
        try {
            String safe = fileName.replace("..", "").replace("/", "").replace("\\", "");
            File target = new File(baseDir, safe);
            HttpRequest.get(url)
                    .header("User-Agent", "Mozilla/5.0 (compatible; TravelAgent/1.0)")
                    .timeout(20000)
                    .execute()
                    .writeBody(target);
            if (!target.exists() || target.length() == 0) {
                return "下载失败: 文件为空或未写入";
            }
            return "下载成功: " + target.getAbsolutePath() + " (" + target.length() + " bytes)";
        } catch (Exception e) {
            return "下载失败: " + e.getMessage() + "。请改用 searchImages 返回的 Pexels 图片直链再下载。";
        }
    }
}
