package com.example.ui.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Coalesces stream elements into time-windowed batches, preserving order and every element. */
fun <T> Flow<T>.batchByTime(windowMs: Long): Flow<List<T>> = flow {
    val buf = mutableListOf<T>()
    var lastEmitTime = System.currentTimeMillis()

    collect { item ->
        buf.add(item)
        val now = System.currentTimeMillis()
        if (now - lastEmitTime >= windowMs) {
            emit(buf.toList())
            buf.clear()
            lastEmitTime = now
        }
    }
    if (buf.isNotEmpty()) {
        emit(buf.toList())
        buf.clear()
    }
}
