@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.cache

import androidx.annotation.RestrictTo
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.unlockQuietly
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.ReentrantLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SingleMediaHandler(
    private val mediaDownloader: MediaDownloader,
    private val mediaSaver: MediaSaver,
    private val cacheFileManager: CacheFileManager,
    private val cacheCleanupService: CacheCleanupService,
    private val executor: ExecutorService,
    private val url: String,
) {

    private val lock = ReentrantLock()

    fun loadMedia(onResult: ((Result<File>) -> Unit)? = null, onCachedSkipped: ((Boolean) -> Unit)? = null) {
        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# requesting media \"...${url.takeLast(10)}\"" }
        executor.execute {
            try {
                val cachedFile = cacheFileManager.getFile(url)
                if (cachedFile.exists() && cacheFileManager.getSize(cachedFile) > 0L) {
                    log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# media \"...${url.takeLast(10)}\" retrieved from cache" }
                    onCachedSkipped?.invoke(false)
                    onResult?.invoke(Result.success(cachedFile))
                    return@execute
                } else {
                    onCachedSkipped?.invoke(true)
                    lock.lock()
                    if (cachedFile.exists() && cacheFileManager.getSize(cachedFile) > 0L) {
                        lock.unlockQuietly()
                        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# media \"...${url.takeLast(10)}\" retrieved from cache" }
                        onCachedSkipped?.invoke(false)
                        onResult?.invoke(Result.success(cachedFile))
                        return@execute
                    }
                    mediaDownloader.download(url)
                        .mapCatching { connection ->
                            mediaSaver.save(url, connection)
                        }
                        .onSuccess { result ->
                            lock.unlockQuietly()
                            onResult?.invoke(result)
                            cacheCleanupService.clearExpired()
                        }
                        .onFailure { e ->
                            lock.unlockQuietly()
                            onResult?.invoke(Result.failure(e))
                        }
                }
            } catch (e: Throwable) {
                lock.unlockQuietly()
                onResult?.invoke(Result.failure(e))
            }
        }
    }
}