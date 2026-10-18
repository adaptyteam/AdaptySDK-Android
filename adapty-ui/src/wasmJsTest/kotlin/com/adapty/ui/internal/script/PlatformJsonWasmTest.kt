package com.adapty.ui.internal.script

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Expected strings are what Gson 2.x `Gson().toJson(...)` / `fromJson(..., Any::class.java)` produce.
class PlatformJsonWasmTest {

    @Test
    fun stringifyEscapesHtmlCharsLikeGson() {
        val expected = "{\"s\":\"" + u("003c") + "a href" + u("003d") + u("0027") + "x" + u("0027") + u("003e") +
            u("0026") + u("003c") + "/a" + u("003e") + "\"}"
        assertEquals(expected, PlatformJson.stringify(mapOf("s" to "<a href='x'>&</a>")))
    }

    @Test
    fun stringifyEscapesQuotesBackslashAndControlChars() {
        assertEquals(""""q\"b\\s"""", PlatformJson.stringify("q\"b\\s"))
        assertEquals(""""\t\b\n\r\f"""", PlatformJson.stringify("\t\b\n\r" + Char(0x0C)))
        assertEquals(
            "\"" + u("0000") + u("0001") + u("001f") + u("000b") + "\"",
            PlatformJson.stringify("" + Char(0) + Char(1) + Char(0x1F) + Char(0x0B)),
        )
        assertEquals("\"" + u("2028") + u("2029") + "\"", PlatformJson.stringify("" + Char(0x2028) + Char(0x2029)))
        assertEquals("\"" + Char(0x7F) + "\"", PlatformJson.stringify("" + Char(0x7F)), "DEL is not escaped by Gson")
    }

    @Test
    fun stringifyKeepsNonAsciiRaw() {
        assertEquals("\"✓ ✕ 🌙 — ’\"", PlatformJson.stringify("✓ ✕ 🌙 — ’"))
    }

    @Test
    fun stringifyDropsNullMapValuesButKeepsNullListItems() {
        assertEquals("{}", PlatformJson.stringify(mapOf("k" to null)))
        assertEquals("""{"a":[null,1.0,2]}""", PlatformJson.stringify(mapOf("a" to listOf(null, 1.0, 2), "b" to null)))
    }

    @Test
    fun stringifyNumbersRoundTrip() {
        val values = listOf(0.0, 1.5, -2.0, 0.1, 1e10, 1.5e-7, 1e21, 123456789.0)
        assertEquals(values, PlatformJson.parse(PlatformJson.stringify(values)))
    }

    @Test
    fun parseNestedNumbersAndEscapes() {
        val parsed = PlatformJson.parse(
            """{"i":3,"n":-7,"e":1.5e3,"E":-2E-2,"z":-0.0,"u":"""" + u("2713") + " " + u("d83c") + u("df19") + """ \"q\" \\ \/ \n",""" +
                """"nested":{"l":[{"x":[-1,2.25]}],"t":true,"f":false,"nil":null}}"""
        )
        val expected = mapOf(
            "i" to 3.0,
            "n" to -7.0,
            "e" to 1500.0,
            "E" to -0.02,
            "z" to -0.0,
            "u" to "✓ 🌙 \"q\" \\ / \n",
            "nested" to mapOf(
                "l" to listOf(mapOf("x" to listOf(-1.0, 2.25))),
                "t" to true,
                "f" to false,
                "nil" to null,
            ),
        )
        assertEquals(expected, parsed)
        assertTrue((parsed as Map<*, *>)["i"] is Double)
    }

    @Test
    fun parseKeepsSourceKeyOrderLikeGson() {
        // JSON.parse/Object.keys would put integer-like keys first; Gson keeps insertion order
        val parsed = PlatformJson.parse("""{"b":1,"2":2,"a":3,"1":4}""") as Map<*, *>
        assertEquals(listOf("b", "2", "a", "1"), parsed.keys.toList())
    }

    @Test
    fun parseDuplicateKeyLastValueWinsAtFirstPosition() {
        val parsed = PlatformJson.parse("""{"a":1,"b":2,"a":3}""") as Map<*, *>
        assertEquals(listOf("a", "b"), parsed.keys.toList())
        assertEquals(3.0, parsed["a"])
    }

    @Test
    fun parseTopLevelScalars() {
        assertEquals("s", PlatformJson.parse("\"s\""))
        assertEquals(2.0, PlatformJson.parse(" 2 "))
        assertEquals(null, PlatformJson.parse("null"))
        assertEquals(listOf<Any?>(), PlatformJson.parse("[]"))
        assertEquals(mapOf<String, Any?>(), PlatformJson.parse("{ }"))
    }

    @Test
    fun parseRejectsInvalidJson() {
        assertFailsWith<IllegalArgumentException> { PlatformJson.parse("""{"a":}""") }
        assertFailsWith<IllegalArgumentException> { PlatformJson.parse("""[1,2""") }
        assertFailsWith<IllegalArgumentException> { PlatformJson.parse("""{"a":1} x""") }
    }
}

// Builds a JSON unicode escape (backslash, u, 4 hex digits).
private fun u(hex: String): String = "\\" + "u" + hex
