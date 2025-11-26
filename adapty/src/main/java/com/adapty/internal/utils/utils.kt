@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cloud.NetConfigManager
import com.adapty.internal.data.cloud.Response
import com.adapty.internal.data.models.NetConfig
import com.adapty.internal.data.models.Onboarding
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.data.models.Variation
import com.adapty.internal.di.Dependencies
import com.adapty.models.AdaptyPaywall
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.adapty.utils.TimeInterval
import com.adapty.utils.seconds
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
import kotlinx.coroutines.sync.Semaphore
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.regex.Pattern
import kotlin.math.pow
import kotlin.random.Random

@JvmSynthetic
internal fun generateUuid() = UUID.randomUUID().toString()

/**
 * @suppress
 */
@InternalAdaptyApi
public fun getClassForNameOrNull(className: String): Class<*>? =
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

private val uiHandler = Handler(Looper.getMainLooper())
internal fun runOnMain(action: java.lang.Runnable) {
    uiHandler.post(action)
}

@JvmSynthetic
internal fun <T> Flow<T>.onSingleResult(action: (AdaptyResult<T>) -> Unit): Flow<AdaptyResult<T>> {
    var consumed = false
    return this
        .map<T, AdaptyResult<T>> { AdaptyResult.Success(it) }
        .catch { error ->
            emit(AdaptyResult.Error(error.asAdaptyError()))
        }
        .onEach { result ->
            if (!consumed) {
                consumed = true
                runOnMain { action(result) }
            }
        }
}

@JvmSynthetic
internal fun AdaptyResult<*>.errorOrNull() = (this as? AdaptyResult.Error)?.error

@JvmSynthetic
internal fun Semaphore.releaseQuietly() {
    try {
        release()
    } catch (t: Throwable) {
    }
}

/**
 * @suppress
 */
@InternalAdaptyApi
public fun Lock.unlockQuietly() {
    try {
        unlock()
    } catch (t: Throwable) {
    }
}

internal inline fun <T> Lock.withLockSafe(action: () -> T) =
    try {
        this.lock()
        action()
    } finally {
        this.unlockQuietly()
    }

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
internal const val DEFAULT_PLACEMENT_LOCALE = "en"

internal const val VERSION_NAME = "3.14.0"

/**
 * @suppress
 */
@InternalAdaptyApi
public val DEFAULT_PAYWALL_TIMEOUT: TimeInterval = 5.seconds

@JvmSynthetic
internal val MIN_PAYWALL_TIMEOUT = 1.seconds

@JvmSynthetic
internal const val PAYWALL_TIMEOUT_MILLIS_SHIFT = 500

@JvmSynthetic
internal const val INF_PAYWALL_TIMEOUT_MILLIS = Int.MAX_VALUE

@get:JvmSynthetic
internal val noLetterRegex by lazy { Pattern.compile("[^\\p{L}]") }

internal inline fun <reified T> Map<*, *>.getAs(key: String) = this[key] as? T

internal fun String.isValidUUID() = runCatching { UUID.fromString(this); true }.getOrDefault(false)

@JvmSynthetic
internal fun Variation.getLanguageCode() =
    (when (this) {
        is PaywallDto -> listOfNotNull(remoteConfig?.lang, getLocaleFromViewConfig(paywallBuilder))
        is Onboarding -> listOfNotNull(remoteConfig?.lang)
    })
        .firstNotNullOfOrNull { locale ->
            extractLanguageCode(locale)?.takeIf { it != DEFAULT_PLACEMENT_LOCALE }
        } ?: DEFAULT_PLACEMENT_LOCALE

@JvmSynthetic
internal fun getLocaleFromViewConfig(viewConfig: Map<String, Any>?) =
    viewConfig?.get("lang") as? String

@JvmSynthetic
internal fun AdaptyPaywall.getLocale() =
    setOfNotNull(remoteConfig?.locale, getLocaleFromViewConfig(viewConfig))
        .firstOrNull { it != DEFAULT_PLACEMENT_LOCALE }
        ?: DEFAULT_PLACEMENT_LOCALE

@JvmSynthetic
internal fun extractLanguageCode(locale: String) =
    locale.split(noLetterRegex).firstOrNull()?.lowercase(Locale.ENGLISH)

@JvmSynthetic
internal fun Long?.orDefault(default: Long = 0L) = this ?: default

@JvmSynthetic
internal fun TimeInterval.toMillis() =
    if (this == TimeInterval.INFINITE)
        INF_PAYWALL_TIMEOUT_MILLIS
    else
        duration.inWholeMilliseconds.coerceAtMost(INF_PAYWALL_TIMEOUT_MILLIS.toLong()).toInt()

@JvmSynthetic
internal fun <T> timeout(flow: Flow<T>, timeout: Int) =
    merge(
        flow,
        getTimeoutFlow<T>(timeout)
    )
        .take(1)

internal class TimeoutException: Exception()

private fun <T> getTimeoutFlow(timeout: Int) =
    flow<T> {
        delay(timeout.toLong())
        throw TimeoutException()
    }

internal fun <T> Flow<T>.recoverOnReachabilityError(nextValue: (error: Throwable) -> T) =
    catch { error ->
        if (error is TimeoutException || error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
            emit(nextValue(error))
        } else {
            throw error
        }
    }

@JvmSynthetic
internal fun getServerErrorDelay(attempt: Long) =
    run {
        val max = (2f.pow(attempt.toInt()).coerceAtMost(30f) * 1000L).toLong()
        Random.nextLong(max + 1).coerceAtLeast(500L)
    }

@JvmSynthetic
internal fun IOException.isServerUnreachableError(): Boolean {
    return this is ConnectException ||
            this is NoRouteToHostException ||
            this is SocketTimeoutException
}

@JvmSynthetic
internal fun <T> Flow<T>.retryIfNecessary(maxAttemptCount: Long, getDelay: (attempt: Long) -> Long = { getServerErrorDelay(it) }): Flow<T> =
    this.retryWhen { error, attempt ->
        if (error !is AdaptyError || (maxAttemptCount in 0..attempt)) {
            return@retryWhen false
        }

        when {
            error is Response.Error && error.backendError?.responseCode in NetConfig.SWITCHING_STATUSES -> {
                Dependencies.injectInternal<NetConfigManager>()
                    .switch(error.request.baseUrl)
                true
            }
            error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR -> {
                if (maxAttemptCount == INFINITE_RETRY) {
                    delay(getDelay(attempt))
                } else {
                    delay(NETWORK_ERROR_DELAY_MILLIS)
                }
                true
            }
            error.originalError is IOException -> {
                val connectivityHelper = runCatching { Dependencies.injectInternal<ConnectivityHelper>() }.getOrNull()
                val isServerUnreachable = error.originalError.isServerUnreachableError()
                val isUnknownHostException = error.originalError is UnknownHostException
                val hasInternet = connectivityHelper?.hasInternetConnectivity() == true

                delay(NETWORK_ERROR_DELAY_MILLIS)
                if (isServerUnreachable || (isUnknownHostException && hasInternet)) {
                    if (error is Response.Error)
                        Dependencies.injectInternal<NetConfigManager>()
                            .switch(error.request.baseUrl)
                } else if (maxAttemptCount == INFINITE_RETRY) {
                    connectivityHelper?.waitForInternetConnectivity()
                }
                true
            }
            else -> false
        }
    }

@JvmSynthetic
internal fun getCurrentProcessName(): String? {
    if (Build.VERSION.SDK_INT >= 28)
        return Application.getProcessName()
    return runCatching {
        Class.forName("android.app.ActivityThread")
            .getDeclaredMethod("currentProcessName")
            .invoke(null) as? String
    }.getOrElse { e ->
        Logger.log(WARN) { "Couldn't retrieve current process name: ${e.localizedMessage}" }
        null
    }
}

@JvmSynthetic
internal fun Context.getMainProcessName(): String? {
    return applicationInfo.processName ?: run {
        Logger.log(WARN) { "Couldn't retrieve main process name" }
        null
    }
}


internal fun combinedProductId(vendorProductId: String, basePlanId: String?) =
    basePlanId?.let { basePlanId -> "$vendorProductId:$basePlanId" } ?: vendorProductId

internal fun kotlin.time.Duration.Companion.fromProductType(productType: String?): kotlin.time.Duration = when (productType) {
    "weekly" -> 7.days
    "monthly" -> 30.days
    "two_months" -> 60.days
    "trimonthly" -> 90.days
    "semiannual" -> 180.days
    "annual" -> 365.days
    "lifetime" -> kotlin.time.Duration.INFINITE
    "consumable", "nonsubscriptions", "uncategorised" -> kotlin.time.Duration.ZERO
    else -> kotlin.time.Duration.ZERO
}
