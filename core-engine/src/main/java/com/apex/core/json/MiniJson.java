package com.apex.core.json;

import com.apex.core.port.JsonCodecPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lightweight, zero-dependency JSON serialization and extraction utility in `:core-engine`.
 * Implements {@link JsonCodecPort} as a built-in fallback.
 */
public final class MiniJson implements JsonCodecPort {
    public static final MiniJson INSTANCE = new MiniJson();

    public MiniJson() {}

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String toJson(Map<String, Object> map) {
        if (map == null) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            sb.append(valueToJson(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String valueToJson(Object val) {
        if (val == null) return "null";
        if (val instanceof String) return "\"" + escape((String) val) + "\"";
        if (val instanceof Number || val instanceof Boolean) return val.toString();
        if (val instanceof Map) return toJson((Map<String, Object>) val);
        if (val instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (List<?>) val) {
                if (!first) sb.append(",");
                first = false;
                sb.append(valueToJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(val.toString()) + "\"";
    }

    /**
     * Extracts a string field from JSON using proper parsing.
     * Handles strings, numbers, booleans, null, objects, and arrays.
     */
    public static String extractField(String json, String fieldName) {
        if (json == null || fieldName == null) return "";
        try {
            JsonParser parser = new JsonParser(json);
            Object parsed = parser.parseValue();
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                Object value = map.get(fieldName);
                if (value == null) return "";
                return value.toString();
            }
        } catch (Exception ignored) {
            // Fall back to regex for backward compatibility
        }
        return "";
    }

    @Override
    public String encodeMap(Map<String, Object> map) {
        return toJson(map);
    }

    @Override
    public String encodeList(List<Object> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append(valueToJson(item));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Map<String, Object> decodeMap(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        try {
            JsonParser parser = new JsonParser(json);
            Object parsed = parser.parseValue();
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                return map;
            }
        } catch (Exception e) {
            // Return empty map on parse error
        }
        return new HashMap<>();
    }

    @Override
    public List<Object> decodeList(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            JsonParser parser = new JsonParser(json);
            Object parsed = parser.parseValue();
            if (parsed instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) parsed;
                return list;
            }
        } catch (Exception e) {
            // Return empty list on parse error
        }
        return Collections.emptyList();
    }

    /**
     * Simple JSON parser for decoding.
     */
    private static class JsonParser {
        private final String json;
        private int pos = 0;

        JsonParser(String json) {
            this.json = json;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);
            if (c == '"') return parseString();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        private void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }

        private String parseString() {
            StringBuilder sb = new StringBuilder();
            pos++; // skip opening quote
            while (pos < json.length()) {
                char c = json.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    if (pos < json.length()) {
                        char esc = json.charAt(pos++);
                        switch (esc) {
                            case '"': sb.append('"'); break;
                            case '\\': sb.append('\\'); break;
                            case '/': sb.append('/'); break;
                            case 'b': sb.append('\b'); break;
                            case 'f': sb.append('\f'); break;
                            case 'n': sb.append('\n'); break;
                            case 'r': sb.append('\r'); break;
                            case 't': sb.append('\t'); break;
                            case 'u':
                                if (pos + 4 <= json.length()) {
                                    String hex = json.substring(pos, pos + 4);
                                    sb.append((char) Integer.parseInt(hex, 16));
                                    pos += 4;
                                }
                                break;
                            default: sb.append(esc);
                        }
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new TreeMap<>(); // TreeMap for deterministic ordering
            pos++; // skip {
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < json.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ':') {
                    pos++;
                }
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                } else if (pos < json.length() && json.charAt(pos) == '}') {
                    pos++;
                    break;
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip [
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < json.length()) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                } else if (pos < json.length() && json.charAt(pos) == ']') {
                    pos++;
                    break;
                }
            }
            return list;
        }

        private Boolean parseBoolean() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return true;
            } else if (json.startsWith("false", pos)) {
                pos += 5;
                return false;
            }
            return false;
        }

        private Object parseNull() {
            if (json.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            return null;
        }

        private Object parseNumber() {
            int start = pos;
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String numStr = json.substring(start, pos);
            if (numStr.contains(".") || numStr.toLowerCase().contains("e")) {
                return Double.parseDouble(numStr);
            } else {
                try {
                    return Long.parseLong(numStr);
                } catch (NumberFormatException e) {
                    return Double.parseDouble(numStr);
                }
            }
        }
    }
}
