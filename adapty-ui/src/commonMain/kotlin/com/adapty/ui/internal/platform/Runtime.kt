package com.adapty.ui.internal.platform

// Runtime helpers used by the shared renderer (JVM/Android on Android, single-threaded JS on web).

internal expect fun currentTimeMillis(): Long

internal expect fun identityHashCode(value: Any?): Int

internal expect fun toRadians(degrees: Double): Double

internal expect val Throwable.platformMessage: String?

/** Runtime type name as printed by `javaClass` on Android (e.g. "class java.lang.Double"). */
internal expect val Any.platformTypeName: String

internal expect fun isMainThread(): Boolean

/** Thread-safe map (ConcurrentHashMap on Android). */
internal expect fun <K : Any, V : Any> concurrentMapOf(): MutableMap<K, V>

/** getOrPut that is atomic on a [concurrentMapOf] map (JVM `ConcurrentMap.getOrPut`). */
internal expect fun <K : Any, V : Any> MutableMap<K, V>.getOrPutAtomic(key: K, defaultValue: () -> V): V

internal expect fun <T : Any> concurrentSetOf(): MutableSet<T>

internal expect class ConcurrentQueue<E> {
    constructor()

    val size: Int
    fun offer(e: E): Boolean
    fun isEmpty(): Boolean
    fun drainTo(c: MutableCollection<in E>): Int
    fun clear()
}

internal expect class AtomicLongRef {
    constructor(initialValue: Long)

    fun incrementAndGet(): Long
}

internal expect class AtomicRef<V> {
    constructor()

    fun get(): V
    fun getAndSet(newValue: V): V
}

internal expect class WeakRef<T> {
    constructor(referent: T)

    fun get(): T?
}

/** JVM `@Synchronized` on Android; no-op elsewhere (single-threaded web). */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
internal expect annotation class PlatformSynchronized()
