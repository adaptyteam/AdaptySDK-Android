package com.adapty.ui.internal.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebTimerValueStoreTest {

    private class FakeStorage : WebKeyValueStorage {
        val items = HashMap<String, String>()
        override fun getItem(key: String): String? = items[key]
        override fun setItem(key: String, value: String) {
            items[key] = value
        }
    }

    private class FailingStorage : WebKeyValueStorage {
        override fun getItem(key: String): String? = null
        override fun setItem(key: String, value: String) {}
    }

    @Test
    fun persistedValueSurvivesNewStoreInstance() {
        val storage = FakeStorage()
        WebTimerValueStore(storage).setLong("p_timer_t_end", 123L, isPersisted = true)

        assertEquals(123L, WebTimerValueStore(storage).getLong("p_timer_t_end", isPersisted = true))
    }

    @Test
    fun nonPersistedValueStaysInMemory() {
        val storage = FakeStorage()
        val store = WebTimerValueStore(storage)
        store.setLong("k", 5L, isPersisted = false)

        assertEquals(5L, store.getLong("k", isPersisted = false))
        assertEquals(emptyMap(), storage.items)
        assertNull(WebTimerValueStore(storage).getLong("k", isPersisted = false))
    }

    @Test
    fun nonPersistedReadIgnoresStorage() {
        val storage = FakeStorage()
        WebTimerValueStore(storage).setLong("k", 7L, isPersisted = true)

        assertNull(WebTimerValueStore(storage).getLong("k", isPersisted = false))
    }

    @Test
    fun storageFailureFallsBackToMemory() {
        val store = WebTimerValueStore(FailingStorage())
        store.setLong("k", 9L, isPersisted = true)

        assertEquals(9L, store.getLong("k", isPersisted = true))
    }

    @Test
    fun corruptStoredValueIsIgnored() {
        val storage = FakeStorage()
        storage.items["adapty_ui_timer_k"] = "not a number"

        assertNull(WebTimerValueStore(storage).getLong("k", isPersisted = true))
    }

    @Test
    fun browserLocalStorageRoundTrip() {
        val key = "web_timer_store_test_${currentMillis()}"
        WebTimerValueStore().setLong(key, 42L, isPersisted = true)
        try {
            assertEquals(42L, WebTimerValueStore().getLong(key, isPersisted = true))
        } finally {
            removeLocalStorageItem("adapty_ui_timer_$key")
        }
    }
}

private fun currentMillis(): Double = js("Date.now()")

private fun removeLocalStorageItem(key: String): Unit = js("window.localStorage.removeItem(key)")
