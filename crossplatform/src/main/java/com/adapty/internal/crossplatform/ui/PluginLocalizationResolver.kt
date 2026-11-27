package com.adapty.internal.crossplatform.ui

import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.DEFAULT_LOCALE
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.EVENT_ID
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.FLOW_VIEW_DID_ASK_LOCALIZATION
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.LOCALES
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.PREFERRED_LOCALES
import com.adapty.internal.crossplatform.ui.AdaptyUiEventListener.Companion.RESOLVER_ID
import com.adapty.ui.AdaptyUiCustomLocalizationResolver
import com.adapty.ui.AdaptyUiLocalizationResolver
import com.adapty.ui.AdaptyUiLocalizationSource
import com.adapty.ui.AdaptyUiLocalizationStrategy
import com.adapty.utils.Callback
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class LocalizationRequestRegistry(
    private val scheduler: ScheduledExecutorService = defaultScheduler,
) {
    private class Pending(val callback: Callback<String?>) {
        @Volatile
        var timeout: ScheduledFuture<*>? = null
    }

    private val pending = ConcurrentHashMap<String, Pending>()

    fun perform(timeoutMillis: Long, emit: (eventId: String) -> Unit, callback: Callback<String?>) {
        val eventId = UUID.randomUUID().toString()
        val request = Pending(callback)
        pending[eventId] = request
        request.timeout = scheduler.schedule({ resolve(eventId, null) }, timeoutMillis, TimeUnit.MILLISECONDS)
        emit(eventId)
    }

    fun resolve(eventId: String, result: String?) {
        val request = pending.remove(eventId) ?: return
        request.timeout?.cancel(false)
        request.callback.onResult(result)
    }

    fun flushCancelled() {
        pending.keys.toList().forEach { eventId -> resolve(eventId, null) }
    }

    private companion object {
        val defaultScheduler: ScheduledExecutorService by lazy {
            Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "AdaptyLocalizationRequestTimeout").apply { isDaemon = true }
            }
        }
    }
}

internal class PluginLocalizationResolver(
    private val id: String,
    private val registry: LocalizationRequestRegistry,
    private val eventObserver: () -> ((AdaptyUiEvent) -> Unit)?,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : AdaptyUiCustomLocalizationResolver {

    override fun resolve(
        preferredLocales: List<String>,
        source: AdaptyUiLocalizationSource,
        callback: Callback<String?>,
    ) {
        val observer = eventObserver() ?: return callback.onResult(null)

        registry.perform(timeoutMillis, { eventId ->
            val data = listOfNotNull(
                EVENT_ID to eventId,
                RESOLVER_ID to id,
                PREFERRED_LOCALES to preferredLocales,
                LOCALES to source.locales,
                source.defaultLocale?.let { DEFAULT_LOCALE to it },
            ).toTypedArray()
            observer(AdaptyUiEvent(FLOW_VIEW_DID_ASK_LOCALIZATION, *data))
        }, callback)
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 2_000L
    }
}

internal fun LocalizationStrategyArgs.toStrategy(
    customResolver: (customId: String) -> AdaptyUiCustomLocalizationResolver,
): AdaptyUiLocalizationStrategy {
    val customId = customId
    if (customId != null) {
        val fallback = when (resolver) {
            LocalizationStrategyArgs.SYSTEM -> AdaptyUiLocalizationResolver.System
            LocalizationStrategyArgs.LEGACY -> AdaptyUiLocalizationResolver.Legacy
            else -> AdaptyUiLocalizationResolver.Auto
        }
        return AdaptyUiLocalizationStrategy.Custom(customResolver(customId), fallback)
    }
    return when (resolver) {
        LocalizationStrategyArgs.SYSTEM -> AdaptyUiLocalizationStrategy.System
        LocalizationStrategyArgs.LEGACY -> AdaptyUiLocalizationStrategy.Legacy
        else -> AdaptyUiLocalizationStrategy.Auto
    }
}
