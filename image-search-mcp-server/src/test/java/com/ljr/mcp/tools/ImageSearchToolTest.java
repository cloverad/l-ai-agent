package com.ljr.mcp.tools;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageSearchToolTest {

    @Test
    void searchImages_withoutKey_shouldReturnHint() {
        ImageSearchTool tool = new ImageSearchTool();
        ReflectionTestUtils.setField(tool, "apiKey", "");
        String result = tool.searchImages("Kyoto temple", 3);
        assertTrue(result.contains("PEXELS_API_KEY"), result);
    }
}
