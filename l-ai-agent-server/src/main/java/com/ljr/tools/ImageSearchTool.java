package com.ljr.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图片搜索：优先 Pexels；失败时降级 WebSearch。
 * 输出带固定标记的 media_gallery，便于前端原样提取，避免模型截断 JSON。
 */
@Component
@Slf4j
public class ImageSearchTool {

    public static final String GALLERY_START = "<<<MEDIA_GALLERY>>>";
    public static final String GALLERY_END = "<<<END_MEDIA_GALLERY>>>";

    private static final String PEXELS_SEARCH_URL = "https://api.pexels.com/v1/search";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private final String apiKey;
    private final WebSearchTool webSearchTool;

    public ImageSearchTool(
            @Value("${pexels.api-key:}") String apiKey,
            @Lazy WebSearchTool webSearchTool) {
        this.apiKey = apiKey;
        this.webSearchTool = webSearchTool;
    }

    @Tool(description = """
            搜索旅行目的地图片。返回内容夹在 <<<MEDIA_GALLERY>>> 与 <<<END_MEDIA_GALLERY>>> 之间。
            最终回复必须原样粘贴整段标记（含起止标记），不要增删改 JSON；前端会渲染成图片卡片。不要下载到本地。
            """)
    public String searchImages(
            @ToolParam(description = "搜索关键词，例如 Kyoto temple / Forbidden City Beijing") String query,
            @ToolParam(description = "返回图片数量，默认 3，最大 6") Integer count) {
        int limit = (count == null || count <= 0) ? 3 : Math.min(count, 6);

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String gallery = searchPexels(query, limit);
                if (gallery != null) {
                    return gallery;
                }
            } catch (Exception e) {
                log.warn("Pexels 搜索失败，降级 WebSearch: {}", e.getMessage());
            }
        }

        return searchWebFallback(query, limit);
    }

    private String searchPexels(String query, int limit) {
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
            return null;
        }
        JSONArray items = new JSONArray();
        for (int i = 0; i < photos.size(); i++) {
            JSONObject photo = photos.getJSONObject(i);
            String large = photo.getByPath("src.large", String.class);
            String thumb = photo.getByPath("src.medium", String.class);
            if (large == null) {
                continue;
            }
            String photographer = photo.getStr("photographer", "Pexels");
            JSONObject item = new JSONObject();
            item.set("thumb", thumb != null ? thumb : large);
            item.set("url", large);
            item.set("credit", "Pexels · " + photographer);
            items.add(item);
        }
        if (items.isEmpty()) {
            return null;
        }
        return wrapGallery(buildGalleryObject(items, null));
    }

    private String searchWebFallback(String query, int limit) {
        String searchResult = webSearchTool.searchWeb(query + " travel photo");
        JSONArray items = new JSONArray();
        Set<String> seen = new LinkedHashSet<>();
        Matcher matcher = URL_PATTERN.matcher(searchResult == null ? "" : searchResult);
        while (matcher.find() && items.size() < Math.max(limit, 5)) {
            String url = matcher.group().replaceAll("[),.;]+$", "");
            if (!seen.add(url)) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.set("thumb", url);
            item.set("url", url);
            item.set("credit", "Web");
            items.add(item);
        }
        String note = items.isEmpty()
                ? "Pexels/联网均未拿到图源，可改关键词再试"
                : "Pexels 不可用，已降级为联网可点击链接";
        return wrapGallery(buildGalleryObject(items, note));
    }

    private static JSONObject buildGalleryObject(JSONArray items, String note) {
        JSONObject gallery = new JSONObject();
        gallery.set("type", "media_gallery");
        gallery.set("items", items);
        if (note != null && !note.isBlank()) {
            gallery.set("note", note);
        }
        return gallery;
    }

    private static String wrapGallery(JSONObject gallery) {
        return GALLERY_START + "\n" + gallery.toString() + "\n" + GALLERY_END;
    }
}
