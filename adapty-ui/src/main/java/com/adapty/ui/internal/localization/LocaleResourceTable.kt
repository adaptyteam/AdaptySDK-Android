package com.adapty.ui.internal.localization

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

internal object LocaleResourceTable {

    private const val TYPE_ID = 0x7e
    const val RES_ID: Int = 0x7f000000 or (TYPE_ID shl 16)
    const val NO_MATCH: String = "-"

    private const val CONFIG_SIZE = 64
    private const val CONFIG_LANGUAGE = 8
    private const val CONFIG_REGION = 10
    private const val CONFIG_SCRIPT = 36
    private const val CONFIG_VARIANT = 40
    private const val CONFIG_LOCALE_END = 48

    private val LANGUAGE = Regex("[a-z]{2,3}")
    private val REGION = Regex("[A-Z]{2}|[0-9]{3}")
    private val SCRIPT = Regex("[A-Z][a-z]{3}")

    fun build(packageName: String, locales: List<Locale?>): ByteArray {
        val entries = mutableListOf(Entry(defaultConfig(), NO_MATCH))
        locales.forEachIndexed { index, locale ->
            val config = locale?.let(::config) ?: return@forEachIndexed
            if (entries.none { it.config.contentEquals(config) }) entries += Entry(config, index.toString())
        }
        entries.sortWith { a, b -> compareConfigs(a.config, b.config) }

        val valuePool = stringPoolUtf8(entries.map(Entry::value))
        val typePool = stringPoolUtf16((1 until TYPE_ID).map { "?$it" } + "string")
        val keyPool = stringPoolUtf8(listOf("v"))

        val spec = le(20)
            .putShort(0x0202).putShort(16).putInt(20)
            .put(TYPE_ID.toByte()).put(0).putShort(entries.size.toShort()).putInt(1)
            .putInt(0x0004)
            .array()

        val types = ByteArrayOutputStream()
        entries.forEachIndexed { index, entry ->
            val headerSize = 20 + CONFIG_SIZE
            val entriesStart = headerSize + 4
            val size = entriesStart + 16
            val type = le(size)
                .putShort(0x0201).putShort(headerSize.toShort()).putInt(size)
                .put(TYPE_ID.toByte()).put(0).putShort(0).putInt(1).putInt(entriesStart)
                .put(entry.config)
                .putInt(0)
                .putShort(8).putShort(0).putInt(0)
                .putShort(8).put(0).put(0x03).putInt(index)
                .array()
            types.write(type)
        }

        val packageHeader = 288
        val packageSize = packageHeader + typePool.size + keyPool.size + spec.size + types.size()
        val pkg = le(packageSize)
            .putShort(0x0200).putShort(packageHeader.toShort()).putInt(packageSize)
            .putInt(0x7f)
        val name = CharArray(128)
        packageName.toCharArray(name, 0, 0, minOf(127, packageName.length))
        name.forEach { pkg.putChar(it) }
        pkg.putInt(packageHeader).putInt(0).putInt(packageHeader + typePool.size).putInt(0).putInt(0)
            .put(typePool).put(keyPool).put(spec).put(types.toByteArray())

        val total = 12 + valuePool.size + packageSize
        return le(total)
            .putShort(0x0002).putShort(12).putInt(total).putInt(1)
            .put(valuePool).put(pkg.array())
            .array()
    }

    fun config(locale: Locale): ByteArray? {
        val language = when (val language = locale.language) {
            "he" -> "iw"
            "id" -> "in"
            "yi" -> "ji"
            else -> language
        }
        val region = locale.country
        val script = locale.script
        val variant = locale.variant
        if (!LANGUAGE.matches(language)) return null
        if (region.isNotEmpty() && !REGION.matches(region)) return null
        if (script.isNotEmpty() && !SCRIPT.matches(script)) return null
        if (variant.length > 8) return null
        val config = defaultConfig()
        pack(language, 'a').copyInto(config, CONFIG_LANGUAGE)
        if (region.isNotEmpty()) pack(region, '0').copyInto(config, CONFIG_REGION)
        script.toByteArray(Charsets.US_ASCII).copyInto(config, CONFIG_SCRIPT)
        variant.toByteArray(Charsets.US_ASCII).copyInto(config, CONFIG_VARIANT)
        return config
    }

    fun compareConfigs(a: ByteArray, b: ByteArray): Int {
        val languageRegion = uint32(a).compareTo(uint32(b))
        if (languageRegion != 0) return languageRegion
        for (i in CONFIG_SCRIPT until CONFIG_LOCALE_END) {
            val d = (a[i].toInt() and 0xff) - (b[i].toInt() and 0xff)
            if (d != 0) return d
        }
        return 0
    }

    private class Entry(val config: ByteArray, val value: String)

    private fun defaultConfig(): ByteArray = le(CONFIG_SIZE).putInt(0, CONFIG_SIZE).array()

    private fun uint32(config: ByteArray): Long =
        ByteBuffer.wrap(config).order(ByteOrder.LITTLE_ENDIAN).getInt(CONFIG_LANGUAGE).toLong() and 0xffffffffL

    private fun pack(code: String, base: Char): ByteArray {
        if (code.length == 2) return byteArrayOf(code[0].code.toByte(), code[1].code.toByte())
        val first = code[0] - base
        val second = code[1] - base
        val third = code[2] - base
        return byteArrayOf(
            (0x80 or (third shl 2) or (second shr 3)).toByte(),
            ((second shl 5) or first).toByte(),
        )
    }

    private fun le(size: Int): ByteBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

    private fun stringPoolUtf8(strings: List<String>): ByteArray {
        val data = ByteArrayOutputStream()
        val offsets = strings.map { string ->
            val offset = data.size()
            val bytes = string.toByteArray(Charsets.UTF_8)
            writeLength8(data, string.length)
            writeLength8(data, bytes.size)
            data.write(bytes)
            data.write(0)
            offset
        }
        while (data.size() % 4 != 0) data.write(0)
        return pool(offsets, data.toByteArray(), flags = 1 shl 8)
    }

    private fun stringPoolUtf16(strings: List<String>): ByteArray {
        val data = ByteArrayOutputStream()
        val offsets = strings.map { string ->
            val offset = data.size()
            data.write(string.length and 0xff)
            data.write((string.length shr 8) and 0xff)
            string.forEach { ch ->
                data.write(ch.code and 0xff)
                data.write((ch.code shr 8) and 0xff)
            }
            data.write(0)
            data.write(0)
            offset
        }
        while (data.size() % 4 != 0) data.write(0)
        return pool(offsets, data.toByteArray(), flags = 0)
    }

    private fun pool(offsets: List<Int>, data: ByteArray, flags: Int): ByteArray {
        val header = 28
        val stringsStart = header + 4 * offsets.size
        val pool = le(stringsStart + data.size)
            .putShort(0x0001).putShort(header.toShort()).putInt(stringsStart + data.size)
            .putInt(offsets.size).putInt(0).putInt(flags).putInt(stringsStart).putInt(0)
        offsets.forEach { pool.putInt(it) }
        return pool.put(data).array()
    }

    private fun writeLength8(out: ByteArrayOutputStream, length: Int) {
        if (length > 0x7f) out.write(0x80 or ((length shr 8) and 0x7f))
        out.write(length and 0xff)
    }
}
