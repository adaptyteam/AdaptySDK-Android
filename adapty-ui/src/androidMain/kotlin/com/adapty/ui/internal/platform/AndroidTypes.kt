package com.adapty.ui.internal.platform

// Android / JVM type names used by shared renderer code (become expect/actual in Task 6c).

public typealias Context = android.content.Context
public typealias Activity = android.app.Activity
public typealias Bitmap = android.graphics.Bitmap
public typealias Uri = android.net.Uri
public typealias PlatformLocale = java.util.Locale
public typealias PlatformDate = java.util.Date
public typealias BigDecimalLike = java.math.BigDecimal
public typealias RoundingMode = java.math.RoundingMode
public typealias PlatformUUID = java.util.UUID

internal const val COLOR_BLACK: Int = android.graphics.Color.BLACK
internal const val COLOR_TRANSPARENT: Int = android.graphics.Color.TRANSPARENT

internal fun parseColorString(value: String): Int = android.graphics.Color.parseColor(value)

internal fun defaultLocale(): PlatformLocale = java.util.Locale.getDefault()

internal fun localeForTag(tag: String): PlatformLocale = java.util.Locale.forLanguageTag(tag)

internal fun englishLocale(): PlatformLocale = java.util.Locale.ENGLISH

internal fun platformDate(epochMillis: Long): PlatformDate = java.util.Date(epochMillis)

internal fun bigDecimalZero(): BigDecimalLike = java.math.BigDecimal.ZERO

internal fun bigDecimalOne(): BigDecimalLike = java.math.BigDecimal.ONE

internal fun bigDecimalTen(): BigDecimalLike = java.math.BigDecimal.TEN

internal fun bigDecimalOf(value: Int): BigDecimalLike = java.math.BigDecimal(value)

internal fun randomUUID(): PlatformUUID = java.util.UUID.randomUUID()

internal typealias PlatformTypeface = android.graphics.Typeface
