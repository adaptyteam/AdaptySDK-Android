package com.adapty.internal.utils

import android.content.Context
import android.os.Build
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyError.RetryType
import com.adapty.errors.AdaptyErrorCode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.min
import kotlin.math.pow

@JvmSynthetic
internal fun generateUuid() = UUID.randomUUID().toString()

@JvmSynthetic
internal fun getCurrentLocale(context: Context) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales.get(0)
    } else {
        context.resources.configuration.locale
    }

private val currencyLocaleMap by lazy {
    hashMapOf<Currency, Locale>().apply {
        for (locale in Locale.getAvailableLocales()) {
            try {
                val currency = Currency.getInstance(locale)
                this[currency] = locale
            } catch (e: Exception) {
            }
        }
    }
}

private val currencyMap by lazy {
    hashMapOf<String, Currency>()
}

@JvmSynthetic
internal fun getCurrencySymbol(currencyCode: String) =
    currencyMap.getOrPut(currencyCode, { Currency.getInstance(currencyCode) }).getOnlySymbol() ?: currencyCode

private fun Currency.getOnlySymbol(): String? {
    if (!currencyLocaleMap.containsKey(this)) return null

    val rawSign = getSymbol(currencyLocaleMap[this])
    return rawSign.firstOrNull { char -> char !in CharRange('A', 'Z') }?.toString() ?: rawSign
}

private val priceFormatter by lazy {
    DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
}

@JvmSynthetic
internal fun formatPrice(priceAmountMicros: Long): String {
    return priceFormatter.format(
        BigDecimal.valueOf(priceAmountMicros)
            .divide(BigDecimal.valueOf(1_000_000L))
    )
}

@JvmSynthetic
internal fun Throwable.asAdaptyError(): AdaptyError {
    return (this as? AdaptyError) ?: AdaptyError(
        originalError = this,
        adaptyErrorCode = AdaptyErrorCode.UNKNOWN
    )
}

@JvmSynthetic
internal fun <T> Flow<T>.flowOnIO(): Flow<T> =
    this.flowOn(Dispatchers.IO)

@JvmSynthetic
internal fun <T> Flow<T>.flowOnMain(): Flow<T> =
    this.flowOn(Dispatchers.Main)

@JvmSynthetic
internal fun execute(block: suspend CoroutineScope.() -> Unit) =
    adaptyScope.launch(context = Dispatchers.IO, block = block)

@JvmSynthetic
@JvmField
internal val adaptyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private const val NETWORK_ERROR_DELAY_MILLIS = 2000L

private fun getServerErrorDelay(attempt: Long) =
    min((2f.pow(attempt.coerceAtMost(7).toInt()) + 1), 90f).toLong() * 1000L

@JvmSynthetic
internal fun <T> Flow<T>.retryIfNecessary(maxAttemptCount: Long = -1): Flow<T> =
    this.retryWhen { error, attempt ->
        if (error !is AdaptyError || (maxAttemptCount in 0..attempt)) {
            return@retryWhen false
        }

        when (error.getRetryType(maxAttemptCount < 0)) {
            RetryType.PROGRESSIVE -> {
                delay(getServerErrorDelay(attempt))
                return@retryWhen true
            }
            RetryType.SIMPLE -> {
                delay(NETWORK_ERROR_DELAY_MILLIS)
                return@retryWhen true
            }
            else -> {
                return@retryWhen false
            }
        }
    }