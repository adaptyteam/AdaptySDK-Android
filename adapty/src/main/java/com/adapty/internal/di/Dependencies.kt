package com.adapty.internal.di

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.AdaptyInternal
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.PreferenceManager
import com.adapty.internal.data.cache.PushTokenRetriever
import com.adapty.internal.data.cloud.*
import com.adapty.internal.domain.AuthInteractor
import com.adapty.internal.domain.ProductsInteractor
import com.adapty.internal.domain.PurchaserInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.utils.*
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

                CloudRepository::class.java to DIObject({
                    CloudRepository(
                        injectInternal(),
                        injectInternal()
                    )
                }),

                CacheRepository::class.java to DIObject({
                    CacheRepository(
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                HttpClient::class.java to DIObject({
                    HttpClient(
                        DefaultConnectionCreator(appContext, injectInternal()),
                        DefaultHttpResponseManager(
                            DefaultResponseBodyConverter(injectInternal()),
                            injectInternal(),
                        )
                    )
                }),

                KinesisManager::class.java to DIObject({
                    KinesisManager(
                        injectInternal(),
                        injectInternal(),
                        HttpClient(
                            KinesisConnectionCreator(injectInternal()),
                            DefaultHttpResponseManager(
                                KinesisResponseBodyConverter(injectInternal()),
                                injectInternal(),
                            )
                        ),
                        injectInternal()
                    )
                }),

                RequestFactory::class.java to DIObject({
                    RequestFactory(
                        appContext,
                        injectInternal(),
                        injectInternal()
                    )
                }),

                PushTokenRetriever::class.java to DIObject({ PushTokenRetriever() }),

                AttributionHelper::class.java to DIObject({ AttributionHelper() }),

                StoreManager::class.java to DIObject({ StoreManager(appContext) }),

                AdaptyPeriodicRequestManager::class.java to DIObject({
                    AdaptyPeriodicRequestManager(
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),

                AdaptyLifecycleManager::class.java to DIObject({
                    AdaptyLifecycleManager(injectInternal())
                }),

                ProductsInteractor::class.java to DIObject({
                    ProductsInteractor(injectInternal(), injectInternal(), injectInternal())
                }),

                PurchaserInteractor::class.java to DIObject({
                    PurchaserInteractor(
                        appContext,
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),

                PurchasesInteractor::class.java to DIObject({
                    PurchasesInteractor(injectInternal(), injectInternal(), injectInternal())
                }),

                AuthInteractor::class.java to DIObject({
                    AuthInteractor(injectInternal(), injectInternal())
                }),

                VisualPaywallManager::class.java to DIObject({
                    VisualPaywallManager(
                        injectInternal(),
                        injectInternal()
                    )
                }),

                AdaptyInternal::class.java to DIObject({
                    AdaptyInternal(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),
            )
        )
    }
}