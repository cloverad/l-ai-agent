package com.ljr.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 行程 PDF 生成工具。
 */
@Component
public class PDFGenerationTool {

    private final String baseDir;

    public PDFGenerationTool(@Value("${app.file-tool.dir}") String baseDir) {
        this.baseDir = baseDir;
        FileUtil.mkdir(baseDir);
    }

    @Tool(description = "将旅行行程文本生成为 PDF 文件并保存到本地，返回文件路径")
    public String generatePDF(
            @ToolParam(description = "PDF 文件名，例如 tokyo-3days.pdf") String fileName,
            @ToolParam(description = "PDF 正文内容，支持多行行程说明") String content) {
        String safe = fileName.replace("..", "").replace("/", "").replace("\\", "");
        if (!safe.toLowerCase().endsWith(".pdf")) {
            safe = safe + ".pdf";
        }
        File target = new File(baseDir, safe);
        try (PdfWriter writer = new PdfWriter(target);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            // 优先使用系统中文字体，避免中文乱码
            PdfFont font = createChineseFont();
            document.setFont(font);
            for (String line : content.split("\\R")) {
                document.add(new Paragraph(line.isBlank() ? " " : line));
            }
            return "PDF 已生成: " + safe + "\n<<<PDF:" + safe + ">>>";
        } catch (Exception e) {
            return "PDF 生成失败: " + e.getMessage();
        }
    }

    private PdfFont createChineseFont() throws Exception {
        String[] candidates = {
                "C:/Windows/Fonts/msyh.ttc,0",
                "C:/Windows/Fonts/simsun.ttc,0",
                "C:/Windows/Fonts/simhei.ttf"
        };
        for (String path : candidates) {
            File fontFile = new File(path.contains(",") ? path.substring(0, path.indexOf(',')) : path);
            if (fontFile.exists()) {
                return PdfFontFactory.createFont(path, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
        return PdfFontFactory.createFont();
    }
}
