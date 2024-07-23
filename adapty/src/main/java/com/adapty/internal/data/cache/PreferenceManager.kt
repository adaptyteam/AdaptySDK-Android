package com.adapty.internal.data.cache

import android.content.Context
import androidx.annotation.RestrictTo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PreferenceManager(context: Context, private val gson: Gson) {

    private val privateMode = 0
    private val pref = context.getSharedPreferences(PREF_NAME, privateMode)
    private val editor = pref.edit()

    @JvmSynthetic
    fun clearData(keys: Set<String>) {
        editor
            .apply { keys.forEach(::remove) }
            .commit()
    }

    @JvmSynthetic
    fun getKeysToRemove(containsKeys: Set<String>, startsWithKeys: Set<String>): Set<String> =
        pref.all.keys.filterTo(mutableSetOf()) { key ->
            key != null && (key in containsKeys || startsWithKeys.firstOrNull { key.startsWith(it) } != null)
        }

    @JvmSynthetic
    fun getBoolean(key: String, defaultValue: Boolean?) =
        if (pref.contains(key)) {
            pref.getBoolean(key, defaultValue ?: false)
        } else {
            defaultValue
        }

    @JvmSynthetic
    fun saveBoolean(key: String, value: Boolean) =
        editor
            .putBoolean(key, value)
            .commit()

    @JvmSynthetic
    fun getLong(key: String, defaultValue: Long?) =
        if (pref.contains(key)) {
            pref.getLong(key, defaultValue ?: 0L)
        } else {
            defaultValue
        }

    @JvmSynthetic
    fun saveLong(key: String, value: Long) =
        editor
            .putLong(key, value)
            .commit()

    @JvmSynthetic
    fun getString(key: String) = pref.getString(key, null)

    @JvmSynthetic
    fun saveString(key: String, value: String) =
        editor
            .putString(key, value)
            .commit()

    @JvmSynthetic
    fun saveStrings(map: Map<String, String>) =
        editor
            .apply {
                map.forEach { (key, value) -> putString(key, value) }
            }
            .commit()

    @JvmSynthetic
    inline fun <reified T> getData(key: String, classOfT: Class<T>? = null): T? {
        return pref.getString(key, null)?.takeIf(::isNotEmpty)?.let {
            try {
                classOfT?.let { classOfT ->
                    gson.fromJson(it, classOfT)
                } ?: gson.fromJson<T>(it, object : TypeToken<T>() {}.type)
            } catch (e: Exception) {
                null
            }
        }
    }

    @JvmSynthetic
    fun saveData(key: String, data: Any?) {
        editor.putString(key, gson.toJson(data)).commit()
    }

    @JvmSynthetic
    fun contains(key: String) = pref.contains(key)

    private fun isNotEmpty(str: String) = str.length > 4

    private companion object {
        private const val PREF_NAME = "AdaptySDKPrefs"
    }

}