@file:JvmName("VideoUtils")
@file:androidx.annotation.OptIn(UnstableApi::class)
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.adapty.internal.di.DIObject
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import java.io.File
import kotlin.reflect.KClass

@SuppressLint("RestrictedApi")
@androidx.annotation.OptIn(UnstableApi::class)
internal fun createPlayer(context: Context): ExoPlayer? {
    val cache = runCatching { Dependencies.injectInternal<Cache>() }.getOrElse { e ->
        log(ERROR) { "$LOG_PREFIX_ERROR couldn't retrieve player cache: (${e.localizedMessage})" }
        return null
    }
    val upstreamFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
    val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(upstreamFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .build()
}

private fun createPlayerCache(context: Context): Cache {
    val cacheSize = 50L * 1024 * 1024
    val cacheDir = File(context.cacheDir, "AdaptyUI/video")
    return SimpleCache(
        cacheDir,
        LeastRecentlyUsedCacheEvictor(cacheSize),
        StandaloneDatabaseProvider(context),
    )
}

internal fun Uri.asMediaItem() = MediaItem.fromUri(this)

@SuppressLint("RestrictedApi")
internal fun providePlayerDeps(context: Context): Iterable<Pair<KClass<*>, Map<String?, DIObject<*>>>> {
    return listOf(
        Cache::class to Dependencies.singleVariantDiObject({
            createPlayerCache(context)
        }),
    )
}

internal const val VERSION_NAME = "3.4.0"
internal const val LOG_PREFIX = "UI (video) v${VERSION_NAME}:"
internal const val LOG_PREFIX_ERROR = "UI (video) v${VERSION_NAME} error:"