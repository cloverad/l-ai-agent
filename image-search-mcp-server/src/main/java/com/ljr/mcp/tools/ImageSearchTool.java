package com.ljr.mcp.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Pexels 目的地图片搜索工具
 */
@Service
public class ImageSearchTool {

    private static final String PEXELS_SEARCH_URL = "https://api.pexels.com/v1/search";

    @Value("${pexels.api-key:}")
    private String apiKey;

    @Tool(description = "根据关键词搜索旅行目的地风景图片，返回图片 URL 列表")
    public String searchImages(
            @ToolParam(description = "搜索关键词，建议使用英文，例如 Kyoto temple / Tokyo night") String query,
            @ToolParam(description = "返回图片数量，默认 5，最大 15") Integer count) {
        if (apiKey == null || apiKey.isBlank()) {
            return "未配置 PEXELS_API_KEY，无法搜索图片";
        }
        int limit = (count == null || count <= 0) ? 5 : Math.min(count, 15);
        String body = HttpRequest.get(PEXELS_SEARCH_URL)
                .header("Authorization", apiKey)
                .form("query", query)
                .form("per_page", limit)
                .timeout(10000)
                .execute()
                .body();

        JSONObject json = JSONUtil.parseObj(body);
        JSONArray photos = json.getJSONArray("photos");
        if (photos == null || photos.isEmpty()) {
            return "未找到与「" + query + "」相关的图片";
        }

        List<String> urls = new ArrayList<>();
        for (int i = 0; i < photos.size(); i++) {
            JSONObject photo = photos.getJSONObject(i);
            String url = photo.getByPath("src.large", String.class);
            if (url != null) {
                urls.add(url);
            }
        }
        return String.join("\n", urls);
    }
}
