package com.ljr.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 获取服务器当前日期时间，供模型推算「明天」「下周」等相对时间。
 */
@Component
public class GetCurrentDateTimeTool {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Tool(description = """
            获取当前日期与时间（Asia/Shanghai）。
            仅在系统未提供时间基准、或需要再次核实时通过 tool calling 调用；不要把调用写成 JSON 文本。
            """)
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDate today = now.toLocalDate();
        String weekday = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA);
        return """
                时区: Asia/Shanghai
                当前时间: %s
                今天: %s（%s）
                明天: %s
                后天: %s
                """.formatted(
                now.format(DATE_TIME),
                today,
                weekday,
                today.plusDays(1),
                today.plusDays(2)
        ).trim();
    }
}
