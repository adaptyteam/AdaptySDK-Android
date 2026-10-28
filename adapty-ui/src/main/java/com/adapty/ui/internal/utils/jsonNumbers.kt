package com.adapty.ui.internal.utils

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs

internal fun normalizeJsonNumbers(value: Any): Any = when (value) {
    is Double -> value.takeIf {
        it % 1.0 == 0.0 && it >= Long.MIN_VALUE.toDouble() && it < Long.MAX_VALUE.toDouble()
    }?.toLong() ?: value
    is Map<*, *> -> value.mapValues { (_, v) -> v?.let(::normalizeJsonNumbers) }
    is List<*> -> value.map { v -> v?.let(::normalizeJsonNumbers) }
    else -> value
}

internal fun Map<String, Any?>.normalizeJsonNumbers(): Map<String, Any?> =
    mapValues { (_, v) -> v?.let(::normalizeJsonNumbers) }

internal fun Double.toJsString(): String {
    if (isNaN()) return "NaN"
    if (isInfinite()) return if (this > 0) "Infinity" else "-Infinity"
    if (this == 0.0) return "0"
    val exact = BigDecimal(this)
    val decimal = (1..17).asSequence()
        .map { exact.round(MathContext(it, RoundingMode.HALF_EVEN)) }
        .first { it.toDouble() == this }
        .stripTrailingZeros()
    val magnitude = abs(this)
    if (magnitude >= 1e-6 && magnitude < 1e21) return decimal.toPlainString()
    val digits = decimal.unscaledValue().abs().toString()
    val exponent = digits.length - 1 - decimal.scale()
    val mantissa = if (digits.length == 1) digits else "${digits[0]}.${digits.substring(1)}"
    val sign = if (this < 0) "-" else ""
    return "$sign${mantissa}e${if (exponent >= 0) "+" else "-"}${abs(exponent)}"
}
