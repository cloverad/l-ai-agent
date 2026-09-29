package com.ljr.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dashscope")
class ImageSearchToolIT {

    @Autowired
    private ImageSearchTool imageSearchTool;

    @Test
    void searchImages_shouldReturnUrls_whenKeyLoadedFromEnvFileOrUserEnv() {
        String result = imageSearchTool.searchImages("Kyoto temple", 2);
        System.out.println("ImageSearchTool 结果预览长度: " + (result == null ? 0 : result.length()));
        assertFalse(result == null || result.isBlank(), "结果不应为空");
        assertFalse(result.contains("未配置 PEXELS_API_KEY") && !result.contains("media_gallery"),
                "密钥未注入到 Spring，请确认 .env 或用户环境变量。实际返回: " + result);
        assertFalse(result.contains("图片搜索失败"), result);
        assertTrue(
                result.contains("media_gallery") || result.contains("http"),
                "应返回 media_gallery 或图片 URL: " + result);
    }
}
