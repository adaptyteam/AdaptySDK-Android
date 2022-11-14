package com.adapty.utils

public open class ImmutableCollection<T, C : Collection<T>>(@get:JvmSynthetic internal val collection: C) : Iterable<T> {

    @get:JvmName("size")
    public val size: Int
        get() = collection.size

    public fun isEmpty(): Boolean = collection.isEmpty()

    public operator fun contains(element: @UnsafeVariance T): Boolean = collection.contains(element)

    public override operator fun iterator(): Iterator<T> = collection.iterator()

    public fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean =
        collection.containsAll(elements)

    public fun indexOf(element: @UnsafeVariance T): Int = collection.indexOf(element)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImmutableCollection<*, *>) return false

        if (collection != other.collection) return false

        return true
    }

    override fun hashCode(): Int {
        return collection.hashCode()
    }

    override fun toString(): String {
        return collection.toString()
    }
}


public class ImmutableList<T>(list: List<T>) : ImmutableCollection<T, List<T>>(list) {

    public operator fun get(index: Int): T = collection[index]
}


public class ImmutableMap<K, V>(@get:JvmSynthetic internal val map: Map<K, V>) {

    @get:JvmName("size")
    public val size: Int
        get() = map.size

    public fun isEmpty(): Boolean = map.isEmpty()

    public fun containsKey(key: K): Boolean = map.containsKey(key)

    public fun containsValue(value: @UnsafeVariance V): Boolean = map.containsValue(value)

    public operator fun get(key: K): V? = map[key]

    @get:JvmName("keySet")
    public val keys: ImmutableCollection<K, Set<K>> get() = ImmutableCollection(map.keys)

    @get:JvmName("values")
    public val values: ImmutableList<V> get() = ImmutableList(map.values.toList())

    @get:JvmName("entrySet")
    public val entries: ImmutableCollection<Entry<K, V>, Set<Entry<K, V>>> get() =
        ImmutableCollection(map.entries.mapTo(mutableSetOf(), Entry.Companion::from))

    public fun forEach(action: Callback<Entry<K, V>>) {
        map.entries.forEach { mapEntry -> action.onResult(Entry.from(mapEntry)) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImmutableMap<*, *>

        if (map != other.map) return false

        return true
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    override fun toString(): String {
        return map.toString()
    }

    public class Entry<K, V> private constructor(private val mapEntry: Map.Entry<K, V>) {

        public val key: K get() = mapEntry.key

        public val value: V get() = mapEntry.value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Entry<*, *>

            if (mapEntry != other.mapEntry) return false

            return true
        }

        override fun hashCode(): Int {
            return mapEntry.hashCode()
        }

        override fun toString(): String {
            return mapEntry.toString()
        }

        internal companion object {
            @JvmSynthetic
            fun <K, V> from(mapEntry: Map.Entry<K, V>) = Entry(mapEntry)
        }
    }
}