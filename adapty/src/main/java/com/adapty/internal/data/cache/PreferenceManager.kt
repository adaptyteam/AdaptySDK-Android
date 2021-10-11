package com.adapty.internal.data.cache

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.ProductDto
import com.adapty.internal.data.models.responses.PaywallsResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PreferenceManager(context: Context, private val gson: Gson) {

    private val privateMode = 0
    private val pref = context.getSharedPreferences(PREF_NAME, privateMode)
    private val editor = pref.edit()

    @JvmSynthetic
    fun saveContainersAndProducts(
        containers: ArrayList<PaywallsResponse.Data>?,
        products: ArrayList<ProductDto>?
    ) {
        editor
            .putString(CONTAINERS, gson.toJson(containers))
            .putString(PRODUCTS, gson.toJson(products))
            .commit()
    }

    @JvmSynthetic
    fun clearOnLogout() {
        editor
            .remove(CUSTOMER_USER_ID)
            .remove(INSTALLATION_META_ID)
            .remove(PROFILE_ID)
            .remove(CONTAINERS)
            .remove(PRODUCTS)
            .remove(SYNCED_PURCHASES)
            .remove(PURCHASER_INFO)
            .remove(IAM_ACCESS_KEY_ID)
            .remove(IAM_SECRET_KEY)
            .remove(IAM_SESSION_TOKEN)
            .commit()
    }

    @JvmSynthetic
    fun clearCachedRequestData() {
        editor
            .remove(UPDATE_PROFILE_REQUEST_KEY)
            .remove(UPDATE_ADJUST_REQUEST_KEY)
            .remove(UPDATE_APPSFLYER_REQUEST_KEY)
            .remove(UPDATE_BRANCH_REQUEST_KEY)
            .remove(UPDATE_CUSTOM_ATTRIBUTION_REQUEST_KEY)
            .remove(SYNC_META_REQUEST_KEY)
            .commit()
    }

    @JvmSynthetic
    fun getBoolean(key: String, defaultValue: Boolean) =
        pref.getBoolean(key, defaultValue)

    @JvmSynthetic
    fun saveBoolean(key: String, value: Boolean) =
        editor
            .putBoolean(key, value)
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

    private fun isNotEmpty(str: String) = str.length > 4

    private companion object {
        private const val PREF_NAME = "AdaptySDKPrefs"
    }

}