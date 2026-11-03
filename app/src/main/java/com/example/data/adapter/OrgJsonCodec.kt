package com.example.data.adapter

import com.apex.core.port.JsonCodecPort
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-performance, zero-dependency JSON codec implementing [JsonCodecPort] using Android's
 * native [org.json] framework.
 *
 * This allows `:app` and `:security-interface` to parse and format complex model and tool payloads
 * losslessly without exposing third-party JSON dependencies to `:core-engine`.
 */
class OrgJsonCodec : JsonCodecPort {

    override fun encodeMap(map: MutableMap<String, Any?>?): String {
        if (map == null) return "{}"
        return JSONObject(map).toString()
    }

    override fun encodeList(list: MutableList<Any?>?): String {
        if (list == null) return "[]"
        return JSONArray(list).toString()
    }

    override fun decodeMap(json: String?): MutableMap<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        if (json.isNullOrBlank()) return result
        try {
            val jsonObject = JSONObject(json)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                result[key] = convertJsonValue(value)
            }
        } catch (_: Exception) {}
        return result
    }

    override fun decodeList(json: String?): MutableList<Any?> {
        val result = mutableListOf<Any?>()
        if (json.isNullOrBlank()) return result
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                result.add(convertJsonValue(jsonArray.get(i)))
            }
        } catch (_: Exception) {}
        return result
    }

    private fun convertJsonValue(value: Any): Any? {
        return when (value) {
            JSONObject.NULL -> null
            is JSONObject -> {
                val nestedMap = mutableMapOf<String, Any?>()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    nestedMap[k] = convertJsonValue(value.get(k))
                }
                nestedMap
            }
            is JSONArray -> {
                val nestedList = mutableListOf<Any?>()
                for (i in 0 until value.length()) {
                    nestedList.add(convertJsonValue(value.get(i)))
                }
                nestedList
            }
            else -> value
        }
    }
}
