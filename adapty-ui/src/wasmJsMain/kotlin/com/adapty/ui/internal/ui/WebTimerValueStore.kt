package com.adapty.ui.internal.ui

/** String key-value storage; implementations must not throw. */
internal interface WebKeyValueStorage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
}

/**
 * Mirrors Android `CacheRepository`: every value lives in memory for the page session, persisted values are
 * also written to [storage] (survive a page reload) and read back on a memory miss.
 */
internal class WebTimerValueStore(
    private val storage: WebKeyValueStorage = BrowserLocalStorage,
) : TimerValueStore {
    private val values = HashMap<String, Long>()

    override fun getLong(key: String, isPersisted: Boolean): Long? {
        values[key]?.let { return it }
        if (!isPersisted) return null
        return storage.getItem(STORAGE_PREFIX + key)?.toLongOrNull()?.also { values[key] = it }
    }

    override fun setLong(key: String, value: Long, isPersisted: Boolean) {
        values[key] = value
        if (isPersisted) storage.setItem(STORAGE_PREFIX + key, value.toString())
    }

    private companion object {
        const val STORAGE_PREFIX = "adapty_ui_timer_"
    }
}

// localStorage may be missing or throw (privacy mode, quota, sandboxed iframe): failures fall back to memory
private object BrowserLocalStorage : WebKeyValueStorage {
    override fun getItem(key: String): String? = localStorageGet(key)

    override fun setItem(key: String, value: String) = localStorageSet(key, value)
}

private fun localStorageGet(key: String): String? =
    js("{ try { return window.localStorage.getItem(key); } catch (e) { return null; } }")

private fun localStorageSet(key: String, value: String): Unit =
    js("{ try { window.localStorage.setItem(key, value); } catch (e) {} }")
