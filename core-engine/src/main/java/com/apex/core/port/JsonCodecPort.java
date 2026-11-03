package com.apex.core.port;

import java.util.List;
import java.util.Map;

/**
 * Minimal JSON codec port used by the core engine to serialize and deserialize
 * model requests, responses, and tool schemas.
 * <p>
 * Implementations must be lossless for UTF-8 encoding, escape sequences, nested maps, and arrays.
 */
public interface JsonCodecPort {
    /**
     * Serializes a key-value map into a JSON string.
     *
     * @param map map of string keys to object values.
     * @return valid JSON formatted string.
     */
    String encodeMap(Map<String, Object> map);

    /**
     * Serializes a list into a JSON array string.
     *
     * @param list list of object values.
     * @return valid JSON array formatted string.
     */
    String encodeList(List<Object> list);

    /**
     * Parses a JSON object string into a map.
     *
     * @param json JSON object string.
     * @return map of string keys to values.
     */
    Map<String, Object> decodeMap(String json);

    /**
     * Parses a JSON array string into a list.
     *
     * @param json JSON array string.
     * @return list of parsed elements.
     */
    List<Object> decodeList(String json);
}
