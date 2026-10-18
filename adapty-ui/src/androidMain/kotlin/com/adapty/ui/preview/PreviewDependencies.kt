@file:OptIn(InternalAdaptyUiApi::class, com.adapty.internal.utils.InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.preview

import android.content.Context
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.PreferenceManager
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.AdaptyUiAccessor
import com.adapty.internal.utils.CrossplatformMetaRetriever
import com.adapty.internal.utils.HashingHelper
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.internal.utils.PriceFormatter
import com.adapty.ui.internal.cache.CacheCleanupService
import com.adapty.ui.internal.cache.CacheFileManager
import com.adapty.ui.internal.cache.MediaCacheConfigManager
import com.adapty.ui.internal.cache.MediaDownloader
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.cache.MediaSaver
import com.adapty.ui.internal.cache.SingleMediaHandlerFactory
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.script.JSActionBridge
import com.adapty.ui.internal.script.JSStateHandler
import com.adapty.ui.internal.script.JSStateMachine
import com.adapty.ui.internal.text.MissingTagKind
import com.adapty.ui.internal.text.PriceConverter
import com.adapty.ui.internal.text.TagResolver
import com.adapty.ui.internal.text.TextResolver
import com.adapty.ui.internal.ui.CacheRepositoryTimerStore
import com.adapty.ui.internal.ui.CorePriceFormatting
import com.adapty.ui.internal.ui.FlowViewModelArgs
import com.adapty.ui.internal.ui.MediaFetchServiceSource
import com.adapty.ui.internal.ui.MetaInfoRetrieverSdkMetaInfo
import com.adapty.ui.internal.ui.SdkMetaInfo
import com.adapty.ui.internal.ui.TimerValueStore
import com.adapty.ui.internal.utils.AdaptyUiVideoAccessor
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.google.gson.Gson
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Preview runs without Adapty.activate(): core services are built here instead of injected from DI.

private var videoPlayerDepsInitialized = false

private val previewEngineLock = Any()

@Volatile
private var sharedPreviewStateHandler: JSStateHandler? = null

private val previewCacheRepositoryLock = Any()

@Volatile
private var previewCacheRepositoryInstance: CacheRepository? = null

private val previewMetaInfoRetrieverLock = Any()

@Volatile
private var previewMetaInfoRetrieverInstance: MetaInfoRetriever? = null

private val previewAdaptersLock = Any()

@Volatile
private var previewTimerStoreInstance: TimerValueStore? = null

@Volatile
private var previewSdkMetaInfoInstance: SdkMetaInfo? = null

internal fun createPreviewViewModelArgs(
    context: Context,
    previewEventCallback: PreviewEventCallback?,
    onMissingTag: ((String, MissingTagKind) -> String)?,
): FlowViewModelArgs {
    initVideoPlayerDepsIfNeeded(context)
    val stateHandler = getOrCreatePreviewStateHandler(context)
    val tagResolver = TagResolver(
        CorePriceFormatting(PriceFormatter(Locale.US)),
        PriceConverter(),
        AdaptyUiTagResolver.Default,
        Locale.US,
        onMissingTag,
    )
    return FlowViewModelArgs(
        "preview_${UUID.randomUUID().toString().hashCode()}",
        false,
        MediaFetchServiceSource(createPreviewMediaFetchService(context)),
        providePreviewTimerStore(context),
        providePreviewSdkMetaInfo(context),
        TextResolver(tagResolver),
        stateHandler,
    ) { scope ->
        listOf(PreviewPurchaseEffectHandler(scope, previewEventCallback)::handle)
    }
}

private fun initVideoPlayerDepsIfNeeded(context: Context) {
    if (videoPlayerDepsInitialized) return
    videoPlayerDepsInitialized = true
    runCatching {
        AdaptyUiVideoAccessor().provideDeps(context)?.let { deps -> Dependencies.contribute(deps) }
    }
}

// One JS engine shared by all previews: keeps engine startup off every bundle switch
private fun getOrCreatePreviewStateHandler(context: Context): JSStateHandler =
    sharedPreviewStateHandler ?: synchronized(previewEngineLock) {
        sharedPreviewStateHandler ?: run {
            val gson = Gson()
            JSStateHandler(JSStateMachine(context.applicationContext, JSActionBridge(gson), gson), gson)
                .also { sharedPreviewStateHandler = it }
        }
    }

private fun createPreviewMediaFetchService(context: Context): MediaFetchService {
    val cacheFileManager = CacheFileManager(context, HashingHelper())
    val cacheCleanupService = CacheCleanupService(cacheFileManager, MediaCacheConfigManager())
    val factory = SingleMediaHandlerFactory(
        MediaDownloader(),
        MediaSaver(cacheFileManager),
        cacheFileManager,
        cacheCleanupService,
    )
    return MediaFetchService(factory)
}

private fun providePreviewTimerStore(context: Context): TimerValueStore =
    previewTimerStoreInstance ?: synchronized(previewAdaptersLock) {
        previewTimerStoreInstance
            ?: CacheRepositoryTimerStore(providePreviewCacheRepository(context)).also { previewTimerStoreInstance = it }
    }

private fun providePreviewSdkMetaInfo(context: Context): SdkMetaInfo =
    previewSdkMetaInfoInstance ?: synchronized(previewAdaptersLock) {
        previewSdkMetaInfoInstance
            ?: MetaInfoRetrieverSdkMetaInfo(providePreviewMetaInfoRetriever(context), context.applicationContext)
                .also { previewSdkMetaInfoInstance = it }
    }

private fun providePreviewCacheRepository(context: Context): CacheRepository {
    // Plain null checks: generic inference over core-internal types breaks codegen (error/NonExistentClass)
    val existing: CacheRepository? = previewCacheRepositoryInstance
    if (existing != null) return existing
    synchronized(previewCacheRepositoryLock) {
        val current: CacheRepository? = previewCacheRepositoryInstance
        if (current != null) return current
        val instance: CacheRepository = createPreviewCacheRepository(context.applicationContext)
        previewCacheRepositoryInstance = instance
        return instance
    }
}

// CacheRepository needs the whole core graph; only `cache` and `preferenceManager` are used by the UI
private fun createPreviewCacheRepository(appContext: Context): CacheRepository {
    val instance = allocateInstance(CacheRepository::class.java)
    try {
        CacheRepository::class.java.getDeclaredField("cache").apply { isAccessible = true }
            .set(instance, ConcurrentHashMap<String, Any>(32))
    } catch (e: Exception) {
        e.printStackTrace()
    }
    try {
        CacheRepository::class.java.getDeclaredField("preferenceManager").apply { isAccessible = true }
            .set(instance, PreferenceManager(appContext, Gson()))
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return instance
}

private fun providePreviewMetaInfoRetriever(context: Context): MetaInfoRetriever {
    // Plain null checks: generic inference over core-internal types breaks codegen (error/NonExistentClass)
    val existing: MetaInfoRetriever? = previewMetaInfoRetrieverInstance
    if (existing != null) return existing
    synchronized(previewMetaInfoRetrieverLock) {
        val current: MetaInfoRetriever? = previewMetaInfoRetrieverInstance
        if (current != null) return current
        val instance: MetaInfoRetriever = createPreviewMetaInfoRetriever(context.applicationContext)
        previewMetaInfoRetrieverInstance = instance
        return instance
    }
}

// Real package name / OS / device values; collaborators the preview never calls are left unconstructed
private fun createPreviewMetaInfoRetriever(appContext: Context): MetaInfoRetriever =
    MetaInfoRetriever(
        appContext,
        allocateInstance(CrossplatformMetaRetriever::class.java),
        allocateInstance(AdaptyUiAccessor::class.java),
        providePreviewCacheRepository(appContext),
        allocateInstance(PreferenceManager::class.java),
    )

private val unsafeInstance: Any by lazy {
    requireNotNull(
        Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
    )
}

@Suppress("UNCHECKED_CAST")
private fun <T> allocateInstance(clazz: Class<T>): T {
    val allocate = Class.forName("sun.misc.Unsafe").getDeclaredMethod("allocateInstance", Class::class.java)
    return allocate.invoke(unsafeInstance, clazz) as T
}
