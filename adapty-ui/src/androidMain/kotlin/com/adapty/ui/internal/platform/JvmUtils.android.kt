package com.adapty.ui.internal.platform

import android.os.Build
import android.os.Looper
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

// JVM / Android runtime helpers used by shared renderer code.

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()

internal actual fun identityHashCode(value: Any?): Int = System.identityHashCode(value)

internal actual fun toRadians(degrees: Double): Double = Math.toRadians(degrees)

internal actual val Throwable.platformMessage: String? get() = localizedMessage

/** Runtime type name as printed by `javaClass` (e.g. "class java.lang.Double"). */
internal actual val Any.platformTypeName: String get() = javaClass.toString()

internal actual fun isMainThread(): Boolean = Looper.getMainLooper().thread == Thread.currentThread()

internal typealias ConcurrentMutableMap<K, V> = ConcurrentHashMap<K, V>

internal actual fun <K : Any, V : Any> concurrentMapOf(): MutableMap<K, V> = ConcurrentHashMap()

internal actual fun <K : Any, V : Any> MutableMap<K, V>.getOrPutAtomic(key: K, defaultValue: () -> V): V =
    (this as ConcurrentMap<K, V>).getOrPut(key, defaultValue)

internal actual fun <T : Any> concurrentSetOf(): MutableSet<T> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        ConcurrentHashMap.newKeySet()
    } else {
        Collections.newSetFromMap(ConcurrentHashMap())
    }

internal actual typealias ConcurrentQueue<E> = java.util.concurrent.LinkedBlockingQueue<E>

internal typealias AtomicBool = java.util.concurrent.atomic.AtomicBoolean
internal actual typealias AtomicLongRef = java.util.concurrent.atomic.AtomicLong
internal actual typealias AtomicRef<V> = java.util.concurrent.atomic.AtomicReference<V>
internal actual typealias WeakRef<T> = java.lang.ref.WeakReference<T>

internal actual typealias PlatformSynchronized = kotlin.jvm.Synchronized

internal class PlatformLock {
    internal inline fun <T> withLock(block: () -> T): T = synchronized(this, block)
}

internal fun platformLock(): PlatformLock = PlatformLock()
