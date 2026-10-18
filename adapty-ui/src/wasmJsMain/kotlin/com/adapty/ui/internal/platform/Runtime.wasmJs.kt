package com.adapty.ui.internal.platform

import kotlin.math.PI

// Single-threaded browser runtime: plain containers and holders.

private fun dateNow(): Double = js("Date.now()")

internal actual fun currentTimeMillis(): Long = dateNow().toLong()

/** No identity hash on wasm; default (identity-based) hashCode for non-overriding classes. */
internal actual fun identityHashCode(value: Any?): Int = value?.hashCode() ?: 0

internal actual fun toRadians(degrees: Double): Double = degrees / 180.0 * PI

internal actual val Throwable.platformMessage: String? get() = message

internal actual val Any.platformTypeName: String get() = "class ${this::class.simpleName}"

internal actual fun isMainThread(): Boolean = true

internal actual fun <K : Any, V : Any> concurrentMapOf(): MutableMap<K, V> = LinkedHashMap()

internal actual fun <K : Any, V : Any> MutableMap<K, V>.getOrPutAtomic(key: K, defaultValue: () -> V): V =
    getOrPut(key, defaultValue)

internal actual fun <T : Any> concurrentSetOf(): MutableSet<T> = LinkedHashSet()

internal actual class ConcurrentQueue<E> actual constructor() {
    private val items = ArrayDeque<E>()

    actual val size: Int get() = items.size

    actual fun offer(e: E): Boolean = items.add(e)

    actual fun isEmpty(): Boolean = items.isEmpty()

    actual fun drainTo(c: MutableCollection<in E>): Int {
        val n = items.size
        c.addAll(items)
        items.clear()
        return n
    }

    actual fun clear() {
        items.clear()
    }
}

internal actual class AtomicLongRef actual constructor(initialValue: Long) {
    private var value = initialValue

    actual fun incrementAndGet(): Long = ++value
}

internal actual class AtomicRef<V> {
    private var value: V? = null

    actual constructor()

    @Suppress("UNCHECKED_CAST")
    actual fun get(): V = value as V

    actual fun getAndSet(newValue: V): V {
        val old = get()
        value = newValue
        return old
    }
}

/** Strong holder: web previews are short-lived and single-threaded. */
internal actual class WeakRef<T> actual constructor(referent: T) {
    private val value: T? = referent

    actual fun get(): T? = value
}
