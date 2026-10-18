@file:OptIn(InternalAdaptyUiApi::class, com.adapty.internal.utils.InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui

import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.di.DIObject
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.OBSERVER_MODE
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.internal.utils.PriceFormatter
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.platform.Context
import com.adapty.ui.internal.platform.ERROR
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.platformMessage
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.text.PriceConverter
import com.adapty.ui.internal.text.TagResolver
import com.adapty.ui.internal.text.TextResolver
import com.adapty.ui.internal.utils.getCurrentLocale
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.ui.listeners.AdaptyUiTagResolver

// Live-SDK view-model args built from the core DI graph; null (logged) when Adapty isn't activated.
internal fun FlowViewModelArgs.Companion.create(
    flowKey: String,
    locale: PlatformLocale,
    context: Context,
) =
    runCatching {
        val mediaFetchService = Dependencies.injectInternal<MediaFetchService>()
        val cacheRepository = Dependencies.injectInternal<CacheRepository>()
        val metaInfoRetriever = Dependencies.injectInternal<MetaInfoRetriever>()
        val isObserverMode = Dependencies.injectInternal<Boolean>(OBSERVER_MODE)
        val priceFormatter = Dependencies.injectInternal<PriceFormatter>(locale.toString()) {
            DIObject({ PriceFormatter(locale) })
        }
        val priceConverter = PriceConverter()
        val tagResolver = TagResolver(
            CorePriceFormatting(priceFormatter),
            priceConverter,
            AdaptyUiTagResolver.Default,
            locale,
        )
        val textResolver = TextResolver(tagResolver)
        val stateHandler = Dependencies.injectInternal<StateHandler>()
        FlowViewModelArgs(
            flowKey,
            isObserverMode,
            MediaFetchServiceSource(mediaFetchService),
            CacheRepositoryTimerStore(cacheRepository),
            MetaInfoRetrieverSdkMetaInfo(metaInfoRetriever, context),
            textResolver,
            stateHandler,
        )
    }.getOrElse { e ->
        log(ERROR) {
            "$LOG_PREFIX_ERROR $flowKey rendering error: ${e.platformMessage}"
        }
        null
    }

// Shared-code entry: the locale is taken from the platform context.
internal fun FlowViewModelArgs.Companion.create(
    flowKey: String,
    ctx: PlatformContextHandle,
): FlowViewModelArgs? = create(flowKey, ctx.context.getCurrentLocale(), ctx.context)
