package com.apex.core.json;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization and specification unit tests for {@link MiniJson}.
 */
public class MiniJsonTest {

    @Test
    public void testEscapeSpecialCharacters() {
        assertEquals("", MiniJson.escape(null));
        assertEquals("hello", MiniJson.escape("hello"));
        assertEquals("hello\\\"world\\\"", MiniJson.escape("hello\"world\""));
        assertEquals("line1\\nline2", MiniJson.escape("line1\nline2"));
        assertEquals("tab\\tseparated", MiniJson.escape("tab\tseparated"));
        assertEquals("path\\\\to\\\\file", MiniJson.escape("path\\to\\file"));
    }

    @Test
    public void testToJsonPrimitivesAndMaps() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "Apex");
        map.put("version", 1);
        map.put("active", true);

        String json = MiniJson.toJson(map);
        assertEquals("{\"name\":\"Apex\",\"version\":1,\"active\":true}", json);
    }

    @Test
    public void testToJsonNestedCollections() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("key", "val");

        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("nested", inner);
        outer.put("items", Arrays.asList("a", "b"));

        String json = MiniJson.toJson(outer);
        assertEquals("{\"nested\":{\"key\":\"val\"},\"items\":[\"a\",\"b\"]}", json);
    }

    @Test
    public void testToJsonNullHandling() {
        assertEquals("{}", MiniJson.toJson(null));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nullable", null);
        String json = MiniJson.toJson(map);
        assertEquals("{\"nullable\":null}", json);
    }

    @Test
    public void testExtractFieldBasic() {
        String json = "{\"model\":\"gpt-4o\",\"content\":\"Hello, world!\",\"status\":\"ok\"}";
        assertEquals("gpt-4o", MiniJson.extractField(json, "model"));
        assertEquals("Hello, world!", MiniJson.extractField(json, "content"));
        assertEquals("ok", MiniJson.extractField(json, "status"));
    }

    @Test
    public void testExtractFieldEscapedQuotesAndNewlines() {
        String json = "{\"message\":\"Line 1\\nLine 2 with \\\"quotes\\\"\"}";
        String extracted = MiniJson.extractField(json, "message");
        assertEquals("Line 1\nLine 2 with \"quotes\"", extracted);
    }

    @Test
    public void testExtractFieldMissingOrNull() {
        assertEquals("", MiniJson.extractField(null, "field"));
        assertEquals("", MiniJson.extractField("{}", "nonexistent"));
        assertEquals("", MiniJson.extractField("{\"other\":123}", "missing"));
    }

    @Test
    public void testEncodeList() {
        MiniJson codec = new MiniJson();
        assertEquals("[]", codec.encodeList(null));
        assertEquals("[]", codec.encodeList(Collections.emptyList()));

        List<Object> list = new ArrayList<>();
        list.add("first");
        list.add(42);
        list.add(false);
        assertEquals("[\"first\",42,false]", codec.encodeList(list));
    }

    @Test
    public void testInstanceMethods() {
        MiniJson codec = MiniJson.INSTANCE;
        assertNotNull(codec);

        Map<String, Object> map = Collections.singletonMap("k", "v");
        assertEquals("{\"k\":\"v\"}", codec.encodeMap(map));
        assertNotNull(codec.decodeMap("{}"));
        assertNotNull(codec.decodeList("[]"));
    }
}
