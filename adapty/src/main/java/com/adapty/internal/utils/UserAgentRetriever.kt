package com.adapty.internal.utils

import android.content.Context
import android.webkit.WebSettings
import androidx.annotation.RestrictTo
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.thread

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class UserAgentRetriever(
    private val appContext: Context,
) {

    private val lock = ReentrantReadWriteLock()

    @Volatile
    var userAgent: String? = null
        get() = try {
            lock.readLock().lock()
            field
        } finally {
            lock.readLock().unlock()
        }
        private set

    init {
        retrieveUserAgent()
    }

    private fun retrieveUserAgent() {
        thread {
            try {
                lock.writeLock().lock()
                userAgent = WebSettings.getDefaultUserAgent(appContext)
            } catch (e: Exception) {
            } finally {
                lock.writeLock().unlock()
            }
        }
    }
}