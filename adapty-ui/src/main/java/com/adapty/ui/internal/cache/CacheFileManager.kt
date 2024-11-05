@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.cache

import android.content.Context
import android.system.Os
import androidx.annotation.RestrictTo
import com.adapty.internal.utils.HashingHelper
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.utils.TimeInterval
import java.io.File

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CacheFileManager(
    private val context: Context,
    private val hashingHelper: HashingHelper,
) {

    fun getFile(url: String, isTemp: Boolean = false): File {
        val fileName = hashingHelper.md5(url)
        val file = File(context.cacheDir, "/$CACHE_FOLDER/${if (isTemp) "." else ""}$fileName")
        return file.also { it.parentFile?.mkdirs() }
    }

    fun getDir(): File {
        return File(context.cacheDir, "/$CACHE_FOLDER").also { it.mkdir() }
    }

    fun isTemp(file: File) = file.name.startsWith(".")

    fun getSize(file: File) = runCatching { Os.lstat(file.absolutePath).st_size }.getOrDefault(0L)

    fun isOlderThan(age: TimeInterval, file: File): Boolean {
        val currentMillis = System.currentTimeMillis()
        val lastModifiedAt = getLastModifiedAt(file)

        return (currentMillis - lastModifiedAt) > age.duration.inWholeMilliseconds
    }

    private fun getLastModifiedAt(file: File) =
        runCatching { Os.lstat(file.absolutePath).st_mtime }.getOrDefault(0L)

    private companion object {
        const val CACHE_FOLDER = "AdaptyUI"
    }
}