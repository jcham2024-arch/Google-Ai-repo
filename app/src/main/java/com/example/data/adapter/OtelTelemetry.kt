package com.example.data.adapter

import android.util.Log
import com.apex.core.port.TelemetryPort

/**
 * Android and SLF4J log-backed implementation of [TelemetryPort] in `:app`.
 */
class OtelTelemetry(
    private val tagPrefix: String = "ApexTelemetry"
) : TelemetryPort {

    override fun startSpan(name: String, attributes: MutableMap<String, String>?): TelemetryPort.Span {
        val startMs = System.currentTimeMillis()
        Log.d(tagPrefix, "[SPAN START] $name $attributes")
        return OtelSpan(name, startMs, attributes ?: mutableMapOf())
    }

    override fun counter(name: String, delta: Long, tags: MutableMap<String, String>?) {
        Log.i(tagPrefix, "[COUNTER] $name += $delta $tags")
    }

    override fun histogram(name: String, valueMs: Long, tags: MutableMap<String, String>?) {
        Log.i(tagPrefix, "[HISTOGRAM] $name = ${valueMs}ms $tags")
    }

    override fun log(level: TelemetryPort.Level?, event: String, fields: MutableMap<String, Any>?): Unit {
        val lvl = level ?: TelemetryPort.Level.INFO
        val fieldsMsg = fields ?: emptyMap<String, Any>()
        val msg = "$event $fieldsMsg"

        when (lvl) {
            TelemetryPort.Level.DEBUG -> Log.d(tagPrefix, msg)
            TelemetryPort.Level.INFO -> Log.i(tagPrefix, msg)
            TelemetryPort.Level.WARN -> Log.w(tagPrefix, msg)
            TelemetryPort.Level.ERROR -> Log.e(tagPrefix, msg)
        }
    }

    private inner class OtelSpan(
        private val spanName: String,
        private val startMs: Long,
        private val attributes: MutableMap<String, String>
    ) : TelemetryPort.Span {

        override fun setAttribute(key: String, value: String) {
            attributes[key] = value
        }

        override fun recordException(throwable: Throwable) {
            Log.e(tagPrefix, "[SPAN ERROR] $spanName", throwable)
            attributes["error.type"] = throwable.javaClass.simpleName
            attributes["error.message"] = throwable.message ?: ""
        }

        override fun close() {
            val duration = System.currentTimeMillis() - startMs
            Log.d(tagPrefix, "[SPAN END] $spanName (${duration}ms) $attributes")
        }
    }
}
