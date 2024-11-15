@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.cache

import androidx.annotation.RestrictTo
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MediaSaver(
    private val cacheFileManager: CacheFileManager,
) {

    fun save(url: String, connection: HttpURLConnection): Result<File> {
        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# saving media \"...${url.takeLast(10)}\"" }
        try {
            val tempFile = cacheFileManager.getFile(url, isTemp = true)
            copyStreamToFile(connection.inputStream, tempFile)
            val file = cacheFileManager.getFile(url)
            if (tempFile.exists())
                tempFile.renameTo(file)
            log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# saved media \"...${url.takeLast(10)}\"" }
            return Result.success(file)
        } catch (e: Throwable) {
            log(ERROR) { "$LOG_PREFIX #AdaptyMediaCache# saving media \"...${url.takeLast(10)}\" failed: ${e.localizedMessage}" }
            return Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    private fun copyStreamToFile(inputStream: InputStream, outputFile: File) {
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024)
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }
}