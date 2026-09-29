package com.ljr.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 网页抓取工具。
 */
@Component
public class WebScrapingTool {

    @Tool(description = "抓取指定 URL 网页的纯文本内容，适合提取景点介绍、攻略正文")
    public String scrapeWebPage(@ToolParam(description = "完整网页 URL，例如 https://example.com") String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();
            String text = doc.body() == null ? doc.text() : doc.body().text();
            if (text.length() > 4000) {
                text = text.substring(0, 4000) + "...(已截断)";
            }
            return text.isBlank() ? "页面无可用文本: " + url : text;
        } catch (Exception e) {
            return "网页抓取失败: " + e.getMessage();
        }
    }
}
