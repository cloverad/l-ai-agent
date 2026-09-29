package com.ljr.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 解决 IDE/旧终端未继承新设 User 环境变量导致 PEXELS_API_KEY 为空的问题。
 */
public class LocalEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "localEnvBootstrap";

    private static final List<String> KEYS = List.of(
            "PEXELS_API_KEY",
            "DASHSCOP_API_KEY",
            "SEARCH_API_KEY",
            "POSTGRES_USER",
            "POSTGRES_PASSWORD",
            "APP_MCP_ENABLED"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = new LinkedHashMap<>();

        for (Path envFile : candidateEnvFiles()) {
            loadDotEnvFile(envFile, props);
        }

        for (String key : KEYS) {
            if (hasText(props.get(key)) || hasText(environment.getProperty(key)) || hasText(System.getenv(key))) {
                continue;
            }
            String fromUser = readWindowsUserEnv(key);
            if (hasText(fromUser)) {
                props.put(key, fromUser);
            }
        }

        // 直接写入绑定键，避免仅靠占位符解析失败
        Object pexels = props.get("PEXELS_API_KEY");
        if (!hasText(pexels)) {
            pexels = firstNonBlank(
                    environment.getProperty("PEXELS_API_KEY"),
                    System.getenv("PEXELS_API_KEY"),
                    System.getProperty("PEXELS_API_KEY")
            );
            if (hasText(pexels)) {
                props.put("PEXELS_API_KEY", pexels);
            }
        }
        if (hasText(pexels) && !hasText(environment.getProperty("pexels.api-key"))) {
            props.put("pexels.api-key", pexels);
        }

        props.forEach((k, v) -> {
            if (v != null && System.getProperty(k) == null) {
                System.setProperty(k, String.valueOf(v));
            }
        });

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
        }

        String resolved = firstNonBlank(
                asString(props.get("PEXELS_API_KEY")),
                environment.getProperty("pexels.api-key"),
                environment.getProperty("PEXELS_API_KEY"),
                System.getenv("PEXELS_API_KEY"),
                System.getProperty("PEXELS_API_KEY")
        );
        if (hasText(resolved)) {
            System.err.println("[local-env] PEXELS_API_KEY configured (len=" + resolved.length() + ")");
        } else {
            System.err.println("[local-env] PEXELS_API_KEY MISSING — put it in project .env or User env, then restart");
        }
    }

    private static List<Path> candidateEnvFiles() {
        List<Path> files = new ArrayList<>();
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            files.add(dir.resolve(".env"));
            dir = dir.getParent();
        }
        return files;
    }

    private static void loadDotEnvFile(Path file, Map<String, Object> props) {
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, Charset.defaultCharset())) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("\uFEFF#")) {
                    continue;
                }
                if (trimmed.startsWith("\uFEFF")) {
                    trimmed = trimmed.substring(1).trim();
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!hasText(props.get(key)) && hasText(value)) {
                    props.put(key, value);
                }
            }
            System.err.println("[local-env] loaded .env: " + file.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[local-env] skip .env " + file + ": " + e.getMessage());
        }
    }

    private static String readWindowsUserEnv(String key) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return null;
        }
        try {
            Process process = new ProcessBuilder(
                    "reg", "query", "HKCU\\Environment", "/v", key)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                output = sb.toString();
            }
            int code = process.waitFor();
            if (code != 0 || output == null) {
                return null;
            }
            for (String line : output.split("\\R")) {
                String t = line.trim();
                if (!t.regionMatches(true, 0, key, 0, key.length())) {
                    continue;
                }
                String[] parts = t.split("\\s+", 3);
                if (parts.length >= 3) {
                    return parts[2].trim();
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (hasText(v)) {
                return v;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
