package com.storyweaver.ai.application.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class StructuredJsonSupport {

    private static final Set<String> SPLIT_SYMBOLS = Set.of("，", ",", "、", ";", "；", "/", "|", "\n");

    private final ObjectMapper objectMapper;

    public StructuredJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode readRoot(String rawResponse, String emptyMessage, String invalidMessage) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new IllegalStateException(emptyMessage);
        }

        String normalized = stripJsonMarkdown(rawResponse);
        try {
            return objectMapper.readTree(normalized);
        } catch (Exception exception) {
            String jsonCandidate = extractJsonCandidate(normalized);
            if (StringUtils.hasText(jsonCandidate)) {
                try {
                    return objectMapper.readTree(jsonCandidate);
                } catch (Exception ignored) {
                    throw new IllegalStateException(invalidMessage);
                }
            }
            throw new IllegalStateException(invalidMessage);
        }
    }

    public String stripJsonMarkdown(String rawResponse) {
        return rawResponse
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
    }

    public String extractJsonCandidate(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return "";
        }

        String objectCandidate = findBalancedJson(rawResponse, '{', '}');
        String arrayCandidate = findBalancedJson(rawResponse, '[', ']');
        if (!StringUtils.hasText(objectCandidate)) {
            return arrayCandidate;
        }
        if (!StringUtils.hasText(arrayCandidate)) {
            return objectCandidate;
        }

        int objectIndex = rawResponse.indexOf(objectCandidate);
        int arrayIndex = rawResponse.indexOf(arrayCandidate);
        if (objectIndex < 0) {
            return arrayCandidate;
        }
        if (arrayIndex < 0) {
            return objectCandidate;
        }
        return objectIndex <= arrayIndex ? objectCandidate : arrayCandidate;
    }

    public String readText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (!node.isMissingNode() && !node.isNull()) {
                String value = node.asText("").trim();
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    public List<String> readList(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (node.isArray()) {
                LinkedHashSet<String> values = new LinkedHashSet<>();
                for (JsonNode item : node) {
                    String value = item.asText("").trim();
                    if (StringUtils.hasText(value)) {
                        values.add(value);
                    }
                }
                return new ArrayList<>(values);
            }
            if (!node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText(""))) {
                return splitValues(node.asText(""));
            }
        }
        return new ArrayList<>();
    }

    public boolean readBoolean(JsonNode root, boolean defaultValue, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (node.isBoolean()) {
                return node.asBoolean();
            }
            if (!node.isMissingNode() && !node.isNull()) {
                String value = node.asText("").trim().toLowerCase();
                if ("true".equals(value) || "1".equals(value) || "yes".equals(value) || "是".equals(value)) {
                    return true;
                }
                if ("false".equals(value) || "0".equals(value) || "no".equals(value) || "否".equals(value)) {
                    return false;
                }
            }
        }
        return defaultValue;
    }

    public int readInt(JsonNode root, int defaultValue, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (node.isInt() || node.isLong()) {
                return node.asInt();
            }
            if (!node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText(""))) {
                try {
                    return Integer.parseInt(node.asText("").trim());
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    public String writeJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    public String normalizeJsonObject(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (node.isObject() || node.isArray()) {
                return writeJson(node);
            }
            if (!node.isMissingNode() && !node.isNull() && StringUtils.hasText(node.asText())) {
                return writeJson(java.util.Map.of("text", node.asText().trim()));
            }
        }
        return "{}";
    }

    private String findBalancedJson(String text, char openChar, char closeChar) {
        for (int start = 0; start < text.length(); start++) {
            if (text.charAt(start) != openChar) {
                continue;
            }

            String candidate = readBalancedJson(text, start, openChar, closeChar);
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private String readBalancedJson(String text, int start, char openChar, char closeChar) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (current == openChar) {
                depth++;
                continue;
            }
            if (current == closeChar) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1).trim();
                }
                if (depth < 0) {
                    return "";
                }
            }
        }
        return "";
    }

    private List<String> splitValues(String value) {
        String normalized = value;
        for (String symbol : SPLIT_SYMBOLS) {
            normalized = normalized.replace(symbol, "\n");
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : normalized.split("\n")) {
            String candidate = item.trim();
            if (StringUtils.hasText(candidate)) {
                values.add(candidate);
            }
        }
        return new ArrayList<>(values);
    }
}
