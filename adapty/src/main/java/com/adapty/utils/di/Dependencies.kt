package com.adapty.utils.di

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.api.ApiClient
import com.adapty.utils.BigDecimalTypeAdapter
import com.adapty.utils.KinesisManager
import com.adapty.utils.PreferenceManager
import com.adapty.utils.push.PushTokenRetriever
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.math.BigDecimal

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object Dependencies {
    private lateinit var appContext: Context

    internal inline fun <reified T> inject() = lazy(LazyThreadSafetyMode.NONE) {
        injectInternal<T>()
    }

    private inline fun <reified T> injectInternal() = (map[T::class.java] as DIObject<T>).provide()

    @get:JvmSynthetic
    internal val map = hashMapOf<Class<*>, DIObject<*>>()

    @JvmSynthetic
    internal fun init(context: Context) {
        appContext = context

        map.putAll(
            listOf(
                Gson::class.java to DIObject({
                    GsonBuilder().registerTypeAdapter(
                        BigDecimal::class.java,
                        BigDecimalTypeAdapter()
                    ).create()
                }),

                PreferenceManager::class.java to DIObject({
                    PreferenceManager(appContext, injectInternal())
                }),

                ApiClient::class.java to DIObject({
                    ApiClient(appContext, injectInternal(), injectInternal())
                }),

                KinesisManager::class.java to DIObject({
                    KinesisManager(
                        injectInternal(),
                        injectInternal()
                    )
                }),

                PushTokenRetriever::class.java to DIObject({ PushTokenRetriever() })
            )
        )
    }
}