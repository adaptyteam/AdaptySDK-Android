package com.adapty.ui.internal.platform

import android.os.Build
import android.os.Looper
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

// JVM / Android runtime helpers used by shared renderer code (become expect/actual in Task 6c).

internal fun currentTimeMillis(): Long = System.currentTimeMillis()

internal fun identityHashCode(value: Any?): Int = System.identityHashCode(value)

internal fun toRadians(degrees: Double): Double = Math.toRadians(degrees)

internal val Throwable.platformMessage: String? get() = localizedMessage

/** Runtime type name as printed by `javaClass` (e.g. "class java.lang.Double"). */
internal val Any.platformTypeName: String get() = javaClass.toString()

internal fun isMainThread(): Boolean = Looper.getMainLooper().thread == Thread.currentThread()

internal typealias ConcurrentMutableMap<K, V> = ConcurrentHashMap<K, V>

internal fun <K : Any, V : Any> concurrentMapOf(): ConcurrentMutableMap<K, V> = ConcurrentHashMap()

internal fun <T : Any> concurrentSetOf(): MutableSet<T> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        ConcurrentHashMap.newKeySet()
    } else {
        Collections.newSetFromMap(ConcurrentHashMap())
    }

internal typealias ConcurrentQueue<T> = java.util.concurrent.LinkedBlockingQueue<T>

internal typealias AtomicBool = java.util.concurrent.atomic.AtomicBoolean
internal typealias AtomicLongRef = java.util.concurrent.atomic.AtomicLong
internal typealias AtomicRef<T> = java.util.concurrent.atomic.AtomicReference<T>
internal typealias WeakRef<T> = java.lang.ref.WeakReference<T>

internal class PlatformLock {
    internal inline fun <T> withLock(block: () -> T): T = synchronized(this, block)
}

internal fun platformLock(): PlatformLock = PlatformLock()
