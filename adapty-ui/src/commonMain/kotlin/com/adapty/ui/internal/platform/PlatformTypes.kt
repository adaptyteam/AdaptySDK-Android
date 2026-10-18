package com.adapty.ui.internal.platform

// Android / JVM type names used by the shared renderer. Android: typealiases; web: minimal stand-ins.

public expect abstract class Context

public expect open class Activity

public expect class Bitmap

public expect abstract class Uri {
    abstract override fun toString(): String
}

public expect class PlatformLocale {
    public fun getLanguage(): String
    public fun getCountry(): String
}

internal val PlatformLocale.language: String get() = getLanguage()
internal val PlatformLocale.country: String get() = getCountry()

public expect open class PlatformDate {
    public open fun getTime(): Long
}

internal val PlatformDate.time: Long get() = getTime()

public expect open class BigDecimalLike : Comparable<BigDecimalLike> {
    public open fun toDouble(): Double
    override fun compareTo(other: BigDecimalLike): Int
}

public expect class PlatformUUID

internal expect open class PlatformTypeface

internal expect abstract class PlatformTimeZone

internal expect val COLOR_BLACK: Int
internal expect val COLOR_TRANSPARENT: Int

internal expect fun parseColorString(value: String): Int

internal expect fun defaultLocale(): PlatformLocale

internal expect fun localeForTag(tag: String): PlatformLocale

internal expect fun englishLocale(): PlatformLocale

internal expect fun platformDate(epochMillis: Long): PlatformDate

internal expect fun bigDecimalZero(): BigDecimalLike

internal expect fun randomUUID(): PlatformUUID
