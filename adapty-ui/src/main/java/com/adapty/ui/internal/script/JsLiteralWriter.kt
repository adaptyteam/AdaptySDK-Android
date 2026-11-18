package com.adapty.ui.internal.script

import java.util.Collections
import java.util.IdentityHashMap

internal object JsLiteralWriter {

    private const val MAX_DEPTH = 128

    fun writeEvent(fields: Map<String, Any?>): String = buildString {
        append('{')
        fields.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) append(',')
            appendKey(key)
            append(':')
            appendValue(value, 0, newAncestors())
        }
        append('}')
    }

    fun write(value: Any?): String = buildString { appendValue(value, 0, newAncestors()) }

    private fun newAncestors(): MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())

    private fun StringBuilder.appendValue(value: Any?, depth: Int, ancestors: MutableSet<Any>) {
        if (depth > MAX_DEPTH) {
            append("null")
            return
        }
        when (value) {
            null -> append("null")
            is String -> appendString(value)
            is Boolean -> append(value)
            is Int, is Long, is Short, is Byte -> append(value)
            is Double -> appendDouble(value)
            is Number -> appendDouble(value.toDouble())
            is Map<*, *> -> appendCollection(value, depth, ancestors) {
                if (value.keys.any { it !is String }) {
                    append("null")
                } else {
                    append('{')
                    value.entries.forEachIndexed { index, (key, item) ->
                        if (index > 0) append(',')
                        appendKey(key as String)
                        append(':')
                        appendValue(item, depth + 1, ancestors)
                    }
                    append('}')
                }
            }
            is List<*> -> appendCollection(value, depth, ancestors) { appendArray(value, depth, ancestors) }
            is Array<*> -> appendCollection(value, depth, ancestors) { appendArray(value.asList(), depth, ancestors) }
            is IntArray -> appendArray(value.asList(), depth, ancestors)
            is LongArray -> appendArray(value.asList(), depth, ancestors)
            is ShortArray -> appendArray(value.asList(), depth, ancestors)
            is ByteArray -> appendArray(value.asList(), depth, ancestors)
            is DoubleArray -> appendArray(value.asList(), depth, ancestors)
            is FloatArray -> appendArray(value.asList(), depth, ancestors)
            is BooleanArray -> appendArray(value.asList(), depth, ancestors)
            else -> append("null")
        }
    }

    private inline fun StringBuilder.appendCollection(
        value: Any,
        depth: Int,
        ancestors: MutableSet<Any>,
        body: StringBuilder.() -> Unit,
    ) {
        if (!ancestors.add(value)) {
            append("null")
            return
        }
        body()
        ancestors.remove(value)
    }

    private fun StringBuilder.appendArray(items: List<*>, depth: Int, ancestors: MutableSet<Any>) {
        append('[')
        items.forEachIndexed { index, item ->
            if (index > 0) append(',')
            appendValue(item, depth + 1, ancestors)
        }
        append(']')
    }

    private fun StringBuilder.appendDouble(value: Double) {
        when {
            value.isNaN() -> append("NaN")
            value == Double.POSITIVE_INFINITY -> append("Infinity")
            value == Double.NEGATIVE_INFINITY -> append("-Infinity")
            value == 0.0 && 1.0 / value < 0 -> append("-0")
            else -> append(value.toString())
        }
    }

    private fun StringBuilder.appendKey(key: String) {
        if (key == "__proto__") {
            append("[\"__proto__\"]")
        } else {
            appendString(key)
        }
    }

    private fun StringBuilder.appendString(value: String) {
        append('"')
        var i = 0
        while (i < value.length) {
            val c = value[i]
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c < ' ' || c == ' ' || c == ' ' -> appendUnicodeEscape(c)
                Character.isHighSurrogate(c) && i + 1 < value.length && Character.isLowSurrogate(value[i + 1]) -> {
                    append(c).append(value[i + 1])
                    i++
                }
                Character.isSurrogate(c) -> appendUnicodeEscape(c)
                else -> append(c)
            }
            i++
        }
        append('"')
    }

    private fun StringBuilder.appendUnicodeEscape(c: Char) {
        append("\\u")
        append(Integer.toHexString(c.code).padStart(4, '0'))
    }
}
