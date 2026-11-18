package com.adapty.ui.internal.script

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

internal class IsolateRegistry<K : Any, H : Any>(
    private val maxLive: Int,
    private val slots: Slots<K, H>,
    private val create: () -> H,
    private val close: (H) -> Unit,
) {

    interface Slots<K, H> {
        fun get(owner: K): H?
        fun set(owner: K, handler: H?)

        fun onEvicted(owner: K)
    }

    private class OwnerRef<K : Any, H : Any>(
        owner: K,
        queue: ReferenceQueue<K>,
        val handler: H,
    ) : WeakReference<K>(owner, queue)

    private val collected = ReferenceQueue<K>()

    private val live = LinkedHashMap<H, OwnerRef<K, H>>(16, 0.75f, true)

    val liveCount: Int get() = synchronized(this) { live.size }

    fun existing(owner: K): H? = closingAfter { closing ->
        drainCollected(closing)
        slots.get(owner)?.takeIf { live[it] != null }
    }

    fun getOrCreate(owner: K): H = closingAfter { closing ->
        drainCollected(closing)
        slots.get(owner)?.takeIf { live[it] != null }?.let { return@closingAfter it }
        while (live.size >= maxLive) evictEldest(closing)
        val handler = create()
        slots.set(owner, handler)
        live[handler] = OwnerRef(owner, collected, handler)
        handler
    }

    fun release(owner: K): Unit = closingAfter { closing ->
        drainCollected(closing)
        val handler = slots.get(owner) ?: return@closingAfter
        slots.set(owner, null)
        if (live.remove(handler) != null) closing += handler
    }

    fun move(from: K, to: K): Unit = closingAfter { closing ->
        drainCollected(closing)
        val handler = slots.get(from)?.takeIf { live[it] != null } ?: return@closingAfter
        if (from === to) return@closingAfter
        slots.set(from, null)
        slots.get(to)?.let { stale ->
            if (live.remove(stale) != null) closing += stale
        }
        slots.set(to, handler)
        live[handler] = OwnerRef(to, collected, handler)
    }

    private inline fun <T> closingAfter(block: (MutableList<H>) -> T): T {
        val closing = mutableListOf<H>()
        try {
            return synchronized(this) { block(closing) }
        } finally {
            closing.forEach(close)
        }
    }

    private fun drainCollected(closing: MutableList<H>) {
        while (true) {
            @Suppress("UNCHECKED_CAST")
            val ref = collected.poll() as? OwnerRef<K, H> ?: return
            if (live[ref.handler] === ref) {
                live.remove(ref.handler)
                closing += ref.handler
            }
        }
    }

    private fun evictEldest(closing: MutableList<H>) {
        val (handler, ref) = live.entries.first()
        live.remove(handler)
        closing += handler
        ref.get()?.let { owner ->
            slots.set(owner, null)
            slots.onEvicted(owner)
        }
    }
}
