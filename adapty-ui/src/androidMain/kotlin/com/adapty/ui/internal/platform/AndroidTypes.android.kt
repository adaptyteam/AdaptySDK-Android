package com.adapty.ui.internal.platform

// Android / JVM type names used by shared renderer code: actuals are the platform types.

public actual typealias Context = android.content.Context
public actual typealias Activity = android.app.Activity
public actual typealias Bitmap = android.graphics.Bitmap
public actual typealias Uri = android.net.Uri
public actual typealias PlatformLocale = java.util.Locale
public actual typealias PlatformDate = java.util.Date
public actual typealias BigDecimalLike = java.math.BigDecimal
public typealias RoundingMode = java.math.RoundingMode
public actual typealias PlatformUUID = java.util.UUID

internal actual const val COLOR_BLACK: Int = android.graphics.Color.BLACK
internal actual const val COLOR_TRANSPARENT: Int = android.graphics.Color.TRANSPARENT

internal actual fun parseColorString(value: String): Int = android.graphics.Color.parseColor(value)

internal actual fun defaultLocale(): PlatformLocale = java.util.Locale.getDefault()

internal actual fun localeForTag(tag: String): PlatformLocale = java.util.Locale.forLanguageTag(tag)

internal actual fun englishLocale(): PlatformLocale = java.util.Locale.ENGLISH

internal actual fun platformDate(epochMillis: Long): PlatformDate = java.util.Date(epochMillis)

internal actual fun bigDecimalZero(): BigDecimalLike = java.math.BigDecimal.ZERO

internal fun bigDecimalOne(): BigDecimalLike = java.math.BigDecimal.ONE

internal fun bigDecimalTen(): BigDecimalLike = java.math.BigDecimal.TEN

internal fun bigDecimalOf(value: Int): BigDecimalLike = java.math.BigDecimal(value)

internal actual fun randomUUID(): PlatformUUID = java.util.UUID.randomUUID()

internal actual typealias PlatformTypeface = android.graphics.Typeface

internal actual typealias PlatformTimeZone = java.util.TimeZone
