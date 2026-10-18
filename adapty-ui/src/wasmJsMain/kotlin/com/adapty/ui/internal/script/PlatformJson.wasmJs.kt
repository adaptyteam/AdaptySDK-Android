package com.adapty.ui.internal.script

// Gson-shaped JSON: objects → LinkedHashMap (source key order, like Gson's LinkedTreeMap), arrays → ArrayList,
// all numbers → Double. stringify follows Gson defaults (HTML-safe escaping, null map values dropped).
internal actual object PlatformJson {
    actual fun parse(json: String): Any? = JsonParser(json).parseDocument()

    actual fun stringify(value: Any?): String = StringBuilder().also { writeValue(it, value) }.toString()

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
                '<', '>', '&', '=', '\'', LINE_SEPARATOR, PARAGRAPH_SEPARATOR -> appendUnicodeEscape(out, c)
                else -> if (c < ' ') appendUnicodeEscape(out, c) else out.append(c)
            }
        }
        out.append('"')
    }

    private fun appendUnicodeEscape(out: StringBuilder, c: Char) {
        out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
    }

    private val LINE_SEPARATOR = Char(0x2028)
    private val PARAGRAPH_SEPARATOR = Char(0x2029)
}


// Strict RFC 8259 reader. Kotlin-side so object keys keep source order (Object.keys puts integer-like keys first).
private class JsonParser(private val src: String) {
    private var pos = 0

    fun parseDocument(): Any? {
        val value = readValue()
        skipWhitespace()
        if (pos != src.length) fail("trailing data")
        return value
    }

    private fun readValue(): Any? {
        skipWhitespace()
        if (pos >= src.length) fail("unexpected end")
        return when (val c = src[pos]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> readString()
            't' -> readLiteral("true", true)
            'f' -> readLiteral("false", false)
            'n' -> readLiteral("null", null)
            else -> if (c == '-' || c in '0'..'9') readNumber() else fail("unexpected '$c'")
        }
    }

    private fun readObject(): Map<String, Any?> {
        pos++
        val map = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (peek() == '}') { pos++; return map }
        while (true) {
            skipWhitespace()
            if (peek() != '"') fail("expected a key")
            val key = readString()
            skipWhitespace()
            expect(':')
            map[key] = readValue()
            skipWhitespace()
            when (next()) {
                ',' -> continue
                '}' -> return map
                else -> fail("expected ',' or '}'")
            }
        }
    }

    private fun readArray(): List<Any?> {
        pos++
        val list = ArrayList<Any?>()
        skipWhitespace()
        if (peek() == ']') { pos++; return list }
        while (true) {
            list.add(readValue())
            skipWhitespace()
            when (next()) {
                ',' -> continue
                ']' -> return list
                else -> fail("expected ',' or ']'")
            }
        }
    }

    private fun readString(): String {
        pos++
        val out = StringBuilder()
        while (true) {
            if (pos >= src.length) fail("unterminated string")
            val c = src[pos++]
            when {
                c == '"' -> return out.toString()
                c == '\\' -> out.append(readEscape())
                c < ' ' -> fail("control character in string")
                else -> out.append(c)
            }
        }
    }

    private fun readEscape(): Char = when (val e = next()) {
        '"' -> '"'
        '\\' -> '\\'
        '/' -> '/'
        'b' -> '\b'
        'f' -> Char(0x0C)
        'n' -> '\n'
        'r' -> '\r'
        't' -> '\t'
        'u' -> {
            if (pos + 4 > src.length) fail("bad unicode escape")
            val hex = src.substring(pos, pos + 4)
            if (!hex.all { it.digitToIntOrNull(16) != null }) fail("bad unicode escape")
            val code = hex.toInt(16)
            pos += 4
            Char(code)
        }
        else -> fail("bad escape '$e'")
    }

    private fun readNumber(): Double {
        val start = pos
        if (peek() == '-') pos++
        when {
            peek() == '0' -> pos++
            peek() in '1'..'9' -> skipDigits()
            else -> fail("bad number")
        }
        if (peek() == '.') {
            pos++
            if (peek() !in '0'..'9') fail("bad number")
            skipDigits()
        }
        if (peek() == 'e' || peek() == 'E') {
            pos++
            if (peek() == '+' || peek() == '-') pos++
            if (peek() !in '0'..'9') fail("bad number")
            skipDigits()
        }
        // JS Number() gives correctly rounded doubles, same as Java's Double.parseDouble
        return jsNumber(src.substring(start, pos))
    }

    private fun readLiteral(word: String, value: Any?): Any? {
        if (!src.startsWith(word, pos)) fail("unexpected token")
        pos += word.length
        return value
    }

    private fun skipDigits() {
        while (peek() in '0'..'9') pos++
    }

    private fun skipWhitespace() {
        while (pos < src.length && (src[pos] == ' ' || src[pos] == '\n' || src[pos] == '\r' || src[pos] == '\t')) pos++
    }

    private fun peek(): Char = if (pos < src.length) src[pos] else Char(0)

    private fun next(): Char = if (pos < src.length) src[pos++] else fail("unexpected end")

    private fun expect(c: Char) {
        if (next() != c) fail("expected '$c'")
    }

    private fun fail(reason: String): Nothing =
        throw IllegalArgumentException("Malformed JSON at $pos: $reason")
}

@JsFun("(s) => Number(s)")
private external fun jsNumber(s: String): Double
