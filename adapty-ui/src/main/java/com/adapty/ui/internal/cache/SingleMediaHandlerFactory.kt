package com.adapty.ui.internal.cache

import androidx.annotation.RestrictTo
import java.util.concurrent.Executors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SingleMediaHandlerFactory(
    private val mediaDownloader: MediaDownloader,
    private val mediaSaver: MediaSaver,
    private val cacheFileManager: CacheFileManager,
    private val cacheCleanupService: CacheCleanupService,
) {

    private val executor = Executors.newCachedThreadPool()

    private val handlers = hashMapOf<String, SingleMediaHandler>()

    fun get(mediaKey: String): SingleMediaHandler =
        handlers.getOrPut(mediaKey) {
            SingleMediaHandler(
                mediaDownloader,
                mediaSaver,
                cacheFileManager,
                cacheCleanupService,
                executor,
                mediaKey,
            )
        }
}