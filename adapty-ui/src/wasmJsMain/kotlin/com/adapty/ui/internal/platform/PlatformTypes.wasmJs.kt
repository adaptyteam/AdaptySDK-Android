package com.adapty.ui.internal.platform

import kotlin.random.Random

// Web stand-ins for Android / JVM types.

public actual abstract class Context

/** The single web host context. */
internal object WebContext : Context()

public actual open class Activity

public actual class Bitmap

public actual abstract class Uri {
    actual abstract override fun toString(): String
}

internal class WebUri(private val value: String) : Uri() {
    override fun toString(): String = value
    override fun equals(other: Any?): Boolean = other is WebUri && other.value == value
    override fun hashCode(): Int = value.hashCode()
}

public actual class PlatformLocale(private val language: String, private val country: String) {
    public actual fun getLanguage(): String = language
    public actual fun getCountry(): String = country

    override fun toString(): String = if (country.isEmpty()) language else "${language}_$country"
    override fun equals(other: Any?): Boolean =
        other is PlatformLocale && other.language == language && other.country == country
    override fun hashCode(): Int = 31 * language.hashCode() + country.hashCode()
}

public actual open class PlatformDate(private val epochMillis: Long) {
    public actual open fun getTime(): Long = epochMillis
}

/** Double-backed decimal; precise arithmetic is not needed for web previews. */
public actual open class BigDecimalLike(internal val value: Double) : Comparable<BigDecimalLike> {
    public actual open fun toDouble(): Double = value
    actual override fun compareTo(other: BigDecimalLike): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
    override fun equals(other: Any?): Boolean = other is BigDecimalLike && other.value == value
    override fun hashCode(): Int = value.hashCode()
}

public actual class PlatformUUID internal constructor(private val value: String) {
    override fun toString(): String = value
}

/** Skia typeface handle (a system-font variation instance); null when no web fonts are registered. */
internal actual open class PlatformTypeface internal constructor(internal val skia: org.jetbrains.skia.Typeface?)

internal actual abstract class PlatformTimeZone

internal actual val COLOR_BLACK: Int = 0xFF000000.toInt()
internal actual val COLOR_TRANSPARENT: Int = 0

private val namedColors = mapOf(
    "black" to 0xFF000000, "darkgray" to 0xFF444444, "gray" to 0xFF888888, "lightgray" to 0xFFCCCCCC,
    "white" to 0xFFFFFFFF, "red" to 0xFFFF0000, "green" to 0xFF00FF00, "blue" to 0xFF0000FF,
    "yellow" to 0xFFFFFF00, "cyan" to 0xFF00FFFF, "magenta" to 0xFFFF00FF, "aqua" to 0xFF00FFFF,
    "fuchsia" to 0xFFFF00FF, "darkgrey" to 0xFF444444, "grey" to 0xFF888888, "lightgrey" to 0xFFCCCCCC,
    "lime" to 0xFF00FF00, "maroon" to 0xFF800000, "navy" to 0xFF000080, "olive" to 0xFF808000,
    "purple" to 0xFF800080, "silver" to 0xFFC0C0C0, "teal" to 0xFF008080,
)

/** Same contract as android.graphics.Color.parseColor: #RRGGBB, #AARRGGBB or a color name. */
internal actual fun parseColorString(value: String): Int {
    if (value.startsWith("#")) {
        val hex = value.substring(1)
        val parsed = hex.toLongOrNull(16) ?: throw IllegalArgumentException("Unknown color")
        return when (hex.length) {
            6 -> (parsed or 0xFF000000).toInt()
            8 -> parsed.toInt()
            else -> throw IllegalArgumentException("Unknown color")
        }
    }
    return namedColors[value.lowercase()]?.toInt() ?: throw IllegalArgumentException("Unknown color")
}

private fun navigatorLanguage(): String = js("navigator.language || 'en-US'")

internal actual fun defaultLocale(): PlatformLocale = localeForTag(navigatorLanguage())

internal actual fun localeForTag(tag: String): PlatformLocale {
    val parts = tag.split('-', '_')
    val language = parts.getOrNull(0)?.lowercase().orEmpty()
    val country = parts.drop(1).firstOrNull { it.length == 2 || it.length == 3 && it.all(Char::isDigit) }?.uppercase().orEmpty()
    return PlatformLocale(language, country)
}

internal actual fun englishLocale(): PlatformLocale = PlatformLocale("en", "")

internal actual fun platformDate(epochMillis: Long): PlatformDate = PlatformDate(epochMillis)

internal actual fun bigDecimalZero(): BigDecimalLike = BigDecimalLike(0.0)

internal actual fun randomUUID(): PlatformUUID {
    val hex = "0123456789abcdef"
    val chars = CharArray(36) { i ->
        when (i) {
            8, 13, 18, 23 -> '-'
            14 -> '4'
            else -> hex[Random.nextInt(16)]
        }
    }
    return PlatformUUID(chars.concatToString())
}
