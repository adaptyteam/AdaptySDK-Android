package com.adapty.ui.internal.cache

import androidx.annotation.RestrictTo
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.hours
import java.util.concurrent.Executors

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CacheCleanupService(
    private val cacheFileManager: CacheFileManager,
    private val cacheConfigManager: MediaCacheConfigManager,
) {

    private val executor = Executors.newSingleThreadExecutor()

    fun clearExpired() {
        val currentCacheConfig =
            cacheConfigManager.currentCacheConfig.takeIf { config ->
                config.diskStorageSizeLimit > 0L
            } ?: return
        executor.execute {
            try {
                val cacheDir = cacheFileManager.getDir()
                if (cacheFileManager.getSize(cacheDir) <= currentCacheConfig.diskStorageSizeLimit)
                    return@execute
                cacheDir.listFiles()?.forEach { file ->
                    val exists = file?.exists() ?: return@forEach
                    val expired =
                        if (!cacheFileManager.isTemp(file))
                            cacheFileManager.isOlderThan(currentCacheConfig.discCacheValidityTime, file)
                        else
                            cacheFileManager.isOlderThan(TEMP_FILE_VALIDITY_TIME, file)

                    if (exists && expired)
                        file.delete()
                }
            } catch (e: Throwable) {
                log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# couldn't clear cache. ${e.localizedMessage}" }
            }
        }
    }

    fun clearAll() {
        executor.execute {
            try {
                cacheFileManager.getDir().listFiles()
                    ?.forEach { file ->
                        if (file?.exists() == true && (!cacheFileManager.isTemp(file) || cacheFileManager.isOlderThan(TEMP_FILE_VALIDITY_TIME, file)))
                            file.delete()
                    }
            } catch (e: Throwable) {
                log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# couldn't clear cache. ${e.localizedMessage}" }
            }
        }
    }

    private companion object {
        val TEMP_FILE_VALIDITY_TIME = 1.hours
    }
}