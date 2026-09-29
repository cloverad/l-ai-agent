package com.ljr.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 联网搜索工具：优先 SearchAPI，未配置密钥时回退 DuckDuckGo HTML。
 */
@Component
public class WebSearchTool {

    private final String searchApiKey;

    public WebSearchTool(@Value("${app.search-api.api-key:}") String searchApiKey) {
        this.searchApiKey = searchApiKey;
    }

    @Tool(description = "联网搜索旅行相关信息（班次、票价、天气、景点、签证政策等），返回若干条标题和摘要。需要实时或官网信息时调用本工具，参数名用 query。")
    public String searchWeb(@ToolParam(description = "搜索关键词，例如：2026年9月30日 哈尔滨到北京 高铁") String query) {
        if (searchApiKey != null && !searchApiKey.isBlank()) {
            return searchBySearchApi(query);
        }
        return searchByDuckDuckGo(query);
    }

    private String searchBySearchApi(String query) {
        String body = HttpRequest.get("https://www.searchapi.io/api/v1/search")
                .form("engine", "google")
                .form("q", query)
                .form("api_key", searchApiKey)
                .timeout(15000)
                .execute()
                .body();
        JSONObject json = JSONUtil.parseObj(body);
        JSONArray organic = json.getJSONArray("organic_results");
        if (organic == null || organic.isEmpty()) {
            return "未搜索到结果: " + query;
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, organic.size());
        for (int i = 0; i < limit; i++) {
            JSONObject item = organic.getJSONObject(i);
            sb.append(i + 1).append(". ")
                    .append(item.getStr("title")).append("\n")
                    .append(item.getStr("snippet")).append("\n")
                    .append(item.getStr("link")).append("\n\n");
        }
        return sb.toString();
    }

    private String searchByDuckDuckGo(String query) {
        try {
            Document doc = Jsoup.connect("https://html.duckduckgo.com/html/")
                    .data("q", query)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .post();
            Elements results = doc.select(".result");
            if (results.isEmpty()) {
                return "未搜索到结果: " + query;
            }
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Element result : results) {
                if (i >= 5) {
                    break;
                }
                String title = result.select(".result__a").text();
                String snippet = result.select(".result__snippet").text();
                String link = result.select(".result__a").attr("href");
                if (title.isBlank()) {
                    continue;
                }
                sb.append(++i).append(". ").append(title).append("\n")
                        .append(snippet).append("\n")
                        .append(link).append("\n\n");
            }
            return sb.isEmpty() ? "未搜索到结果: " + query : sb.toString();
        } catch (Exception e) {
            return "搜索失败: " + e.getMessage();
        }
    }
}
