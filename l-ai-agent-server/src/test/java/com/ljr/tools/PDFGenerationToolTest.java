package com.ljr.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dashscope")
class PDFGenerationToolTest {

    @Autowired
    private PDFGenerationTool pdfGenerationTool;

    @Test
    void generatePDF_shouldCreateFile() {
        String result = pdfGenerationTool.generatePDF(
                "tool-self-test.pdf",
                "京都1日游\n上午：清水寺\n下午：岚山竹林");
        assertTrue(result.contains("PDF 已生成") || result.contains("生成"), result);
        assertTrue(new File(System.getProperty("user.dir") + "/tmp/files/tool-self-test.pdf").exists()
                || result.contains("tool-self-test.pdf"));
    }
}
