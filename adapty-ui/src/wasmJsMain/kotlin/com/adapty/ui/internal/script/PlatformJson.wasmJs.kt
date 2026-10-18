package com.adapty.ui.internal.script

// Gson-shaped JSON on top of the browser's JSON.parse: objects → LinkedHashMap, arrays → ArrayList,
// all numbers → Double. stringify follows Gson defaults (HTML-safe escaping, null map values dropped).
internal actual object PlatformJson {
    actual fun parse(json: String): Any? = toKotlin(jsonParse(json))

    actual fun stringify(value: Any?): String = StringBuilder().also { writeValue(it, value) }.toString()

    private fun toKotlin(value: JsAny?): Any? = when (jsKind(value)) {
        KIND_BOOLEAN -> jsToBoolean(value!!)
        KIND_NUMBER -> jsToDouble(value!!)
        KIND_STRING -> jsToString(value!!)
        KIND_ARRAY -> {
            val array = value!!
            val size = jsArrayLength(array)
            ArrayList<Any?>(size).apply { for (i in 0 until size) add(toKotlin(jsArrayGet(array, i))) }
        }
        KIND_OBJECT -> {
            val obj = value!!
            val keys = jsObjectKeys(obj)
            val size = jsArrayLength(keys)
            LinkedHashMap<String, Any?>().apply {
                for (i in 0 until size) {
                    val key = jsToString(jsArrayGet(keys, i)!!)
                    put(key, toKotlin(jsObjectGet(obj, key)))
                }
            }
        }
        else -> null
    }

    private fun writeValue(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> writeString(out, value)
            is Char -> writeString(out, value.toString())
            is Boolean -> out.append(value.toString())
            is Number -> out.append(value.toString())
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((k, v) in value) {
                    if (v == null) continue
                    if (!first) out.append(',')
                    first = false
                    writeString(out, k.toString())
                    out.append(':')
                    writeValue(out, v)
                }
                out.append('}')
            }
            is Iterable<*> -> writeArray(out, value.iterator())
            is Array<*> -> writeArray(out, value.iterator())
            else -> writeString(out, value.toString())
        }
    }

    private fun writeArray(out: StringBuilder, items: Iterator<Any?>) {
        out.append('[')
        var first = true
        for (item in items) {
            if (!first) out.append(',')
            first = false
            writeValue(out, item)
        }
        out.append(']')
    }

    private fun writeString(out: StringBuilder, value: String) {
        out.append('"')
        for (c in value) {
            when (c) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\u000C' -> out.append("\\f")
                '<', '>', '&', '=', '\'', ' ', ' ' -> appendUnicodeEscape(out, c)
                else -> if (c < ' ') appendUnicodeEscape(out, c) else out.append(c)
            }
        }
        out.append('"')
    }

    private fun appendUnicodeEscape(out: StringBuilder, c: Char) {
        out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
    }

    private const val KIND_BOOLEAN = 1
    private const val KIND_NUMBER = 2
    private const val KIND_STRING = 3
    private const val KIND_ARRAY = 4
    private const val KIND_OBJECT = 5
}

@JsFun("(s) => JSON.parse(s)")
private external fun jsonParse(json: String): JsAny?

@JsFun(
    """(v) => v === null || v === undefined ? 0 : typeof v === 'boolean' ? 1 : typeof v === 'number' ? 2
        : typeof v === 'string' ? 3 : Array.isArray(v) ? 4 : 5"""
)
private external fun jsKind(value: JsAny?): Int

@JsFun("(v) => v")
private external fun jsToBoolean(value: JsAny): Boolean

@JsFun("(v) => v")
private external fun jsToDouble(value: JsAny): Double

@JsFun("(v) => v")
private external fun jsToString(value: JsAny): String

@JsFun("(a) => a.length")
private external fun jsArrayLength(array: JsAny): Int

@JsFun("(a, i) => a[i]")
private external fun jsArrayGet(array: JsAny, index: Int): JsAny?

@JsFun("(o) => Object.keys(o)")
private external fun jsObjectKeys(obj: JsAny): JsAny

@JsFun("(o, k) => o[k]")
private external fun jsObjectGet(obj: JsAny, key: String): JsAny?
