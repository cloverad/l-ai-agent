package com.ljr.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 提供 tmp/files 下生成文件的浏览器下载（触发另存为对话框）。
 */
@RestController
@RequestMapping("/files")
@Tag(name = "文件下载")
public class FileDownloadController {

    private final Path baseDir;

    public FileDownloadController(@Value("${app.file-tool.dir}") String baseDir) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
    }

    @GetMapping("/download")
    @Operation(summary = "下载工具生成的文件（如 PDF）")
    public ResponseEntity<Resource> download(@RequestParam("name") String name) {
        String safe = name.replace("..", "").replace("/", "").replace("\\", "").trim();
        if (safe.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Path resolved = baseDir.resolve(safe).normalize();
        if (!resolved.startsWith(baseDir) || !Files.isRegularFile(resolved)) {
            return ResponseEntity.notFound().build();
        }
        File file = resolved.toFile();
        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = safe.toLowerCase().endsWith(".pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(mediaType)
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }
}
