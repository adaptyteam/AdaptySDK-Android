package com.adapty.internal.utils

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyError.RetryType
import com.adapty.errors.AdaptyErrorCode
import com.adapty.utils.AdaptyResult
import com.adapty.utils.ImmutableList
import com.adapty.utils.ImmutableMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.take
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min
import kotlin.math.pow

@JvmSynthetic
internal fun generateUuid() = UUID.randomUUID().toString()

@JvmSynthetic
internal fun getClassForNameOrNull(className: String): Class<*>? =
    try {
        Class.forName(className)
    } catch (e: ClassNotFoundException) {
        null
    }

@JvmSynthetic
internal fun Throwable.asAdaptyError(): AdaptyError {
    return (this as? AdaptyError) ?: AdaptyError(
        originalError = this,
        adaptyErrorCode = AdaptyErrorCode.UNKNOWN
    )
}

@JvmSynthetic
internal fun <T> List<T>.immutableWithInterop(): ImmutableList<T> {
    return ImmutableList(this)
}

@JvmSynthetic
internal fun <K, V> Map<K, V>.immutableWithInterop(): ImmutableMap<K, V> {
    return ImmutableMap(this)
}

@JvmSynthetic
internal fun <T> Flow<T>.flowOnIO(): Flow<T> =
    this.flowOn(Dispatchers.IO)

@JvmSynthetic
internal fun <T> Flow<T>.flowOnMain(): Flow<T> =
    this.flowOn(Dispatchers.Main)

@JvmSynthetic
internal fun <T> Flow<T>.onSingleResult(action: suspend (AdaptyResult<T>) -> Unit): Flow<AdaptyResult<T>> {
    var consumed = false
    return this
        .map<T, AdaptyResult<T>> { AdaptyResult.Success(it) }
        .catch { error ->
            emit(AdaptyResult.Error(error.asAdaptyError()))
        }
        .onEach { result ->
            if (!consumed) {
                consumed = true
                action(result)
            }
        }
}

@JvmSynthetic
internal fun AdaptyResult<*>.errorOrNull() = (this as? AdaptyResult.Error)?.error

@JvmSynthetic
internal fun execute(block: suspend CoroutineScope.() -> Unit) =
    adaptyScope.launch(context = Dispatchers.IO, block = block)

@JvmSynthetic
@JvmField
internal val adaptyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@JvmSynthetic
internal const val NETWORK_ERROR_DELAY_MILLIS = 2000L

@JvmSynthetic
internal const val INFINITE_RETRY = -1L

@JvmSynthetic
internal const val DEFAULT_RETRY_COUNT = 3L

@JvmSynthetic
internal const val DEFAULT_PAYWALL_LOCALE = "en"

@InternalAdaptyApi
public const val DEFAULT_PAYWALL_TIMEOUT_MILLIS: Int = 5000

@JvmSynthetic
internal const val MIN_PAYWALL_TIMEOUT_MILLIS = 1000

@JvmSynthetic
internal const val PAYWALL_TIMEOUT_MILLIS_SHIFT = 500

@JvmSynthetic
internal const val INF_PAYWALL_TIMEOUT_MILLIS = Int.MAX_VALUE

@get:JvmSynthetic
internal val noLetterRegex by lazy { Pattern.compile("[^\\p{L}]") }

@JvmSynthetic
internal fun extractLanguageCode(locale: String) =
    locale.split(noLetterRegex, 1).firstOrNull().orEmpty()

@JvmSynthetic
internal fun <T> timeout(flow: Flow<T>, timeout: Int) =
    merge(
        flow.catch<T?> { e ->
            if ((e as? AdaptyError)?.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR) emit(null) else throw e
        },
        getTimeoutFlow<T>(timeout)
    )
        .take(1)

private fun <T> getTimeoutFlow(timeout: Int) =
    flow<T?> {
        delay(timeout.toLong())
        emit(null)
    }

@JvmSynthetic
internal fun getServerErrorDelay(attempt: Long) =
    min((2f.pow(attempt.coerceAtMost(7).toInt()) + 1), 90f).toLong() * 1000L

@JvmSynthetic
internal fun <T> Flow<T>.retryIfNecessary(maxAttemptCount: Long = INFINITE_RETRY): Flow<T> =
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