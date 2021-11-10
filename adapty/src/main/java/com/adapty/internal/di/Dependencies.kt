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
import kotlin.LazyThreadSafetyMode.NONE

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object Dependencies {
    private lateinit var appContext: Context

    internal inline fun <reified T> inject(named: String? = null) = lazy(NONE) {
        injectInternal<T>(named)
    }

    private inline fun <reified T> injectInternal(named: String? = null) =
        (map[T::class.java]!![named] as DIObject<T>).provide()

    @get:JvmSynthetic
    internal val map = hashMapOf<Class<*>, Map<String?, DIObject<*>>>()

    private const val KINESIS = "kinesis"
    private const val BASE = "base"

    private fun <T> singleVariantDiObject(
        initializer: () -> T,
        initType: DIObject.InitType = DIObject.InitType.SINGLETON
    ): Map<String?, DIObject<T>> = mapOf(null to DIObject(initializer, initType))

    @JvmSynthetic
    internal fun init(context: Context) {
        appContext = context

        map.putAll(
            listOf(
                Gson::class.java to singleVariantDiObject({
                    GsonBuilder().registerTypeAdapter(
                        BigDecimal::class.java,
                        BigDecimalTypeAdapter()
                    ).create()
                }),

                PreferenceManager::class.java to singleVariantDiObject({
                    PreferenceManager(appContext, injectInternal())
                }),

                CloudRepository::class.java to singleVariantDiObject({
                    CloudRepository(
                        injectInternal(),
                        injectInternal()
                    )
                }),

                CacheRepository::class.java to singleVariantDiObject({
                    CacheRepository(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                HttpClient::class.java to mapOf(
                    null to DIObject({
                        DefaultHttpClient(
                            injectInternal(named = BASE),
                            injectInternal(),
                            injectInternal(),
                        )
                    }),
                    BASE to DIObject({
                        BaseHttpClient(
                            injectInternal(),
                            injectInternal(),
                            injectInternal(),
                        )
                    }),
                    KINESIS to DIObject({
                        BaseHttpClient(
                            injectInternal(named = KINESIS),
                            injectInternal(named = KINESIS),
                            injectInternal(named = KINESIS),
                        )
                    }),
                ),

                KinesisManager::class.java to singleVariantDiObject({
                    KinesisManager(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(named = KINESIS),
                        injectInternal()
                    )
                }),

                NetworkConnectionCreator::class.java to mapOf(
                    null to DIObject({
                        DefaultConnectionCreator(appContext, injectInternal())
                    }),
                    KINESIS to DIObject({
                        KinesisConnectionCreator(injectInternal())
                    }),
                ),

                HttpResponseManager::class.java to mapOf(
                    null to DIObject({
                        DefaultHttpResponseManager(
                            injectInternal(),
                            injectInternal(),
                            injectInternal(),
                        )
                    }),
                    KINESIS to DIObject({
                        DefaultHttpResponseManager(
                            injectInternal(named = KINESIS),
                            injectInternal(),
                            injectInternal(named = KINESIS),
                        )
                    }),
                ),

                ResponseBodyConverter::class.java to mapOf(
                    null to DIObject({
                        DefaultResponseBodyConverter(injectInternal())
                    }),
                    KINESIS to DIObject({
                        KinesisResponseBodyConverter(injectInternal())
                    }),
                ),

                NetworkLogger::class.java to mapOf(
                    null to DIObject({
                        DefaultNetworkLogger()
                    }),
                    KINESIS to DIObject({
                        KinesisNetworkLogger()
                    }),
                ),

                RequestFactory::class.java to singleVariantDiObject({
                    RequestFactory(
                        appContext,
                        injectInternal(),
                        injectInternal()
                    )
                }),

                PushTokenRetriever::class.java to singleVariantDiObject({ PushTokenRetriever() }),

                AttributionHelper::class.java to singleVariantDiObject({ AttributionHelper() }),

                CurrencyHelper::class.java to singleVariantDiObject({ CurrencyHelper() }),

                PaywallMapper::class.java to singleVariantDiObject({
                    PaywallMapper(
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                ProductMapper::class.java to singleVariantDiObject({ ProductMapper(appContext) }),

                PromoMapper::class.java to singleVariantDiObject({ PromoMapper() }),

                ProrationModeMapper::class.java to singleVariantDiObject({ ProrationModeMapper() }),

                PurchaserInfoMapper::class.java to singleVariantDiObject({ PurchaserInfoMapper() }),

                StoreManager::class.java to singleVariantDiObject({
                    StoreManager(
                        appContext,
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                AdaptyPeriodicRequestManager::class.java to singleVariantDiObject({
                    AdaptyPeriodicRequestManager(
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),

                AdaptyLifecycleManager::class.java to singleVariantDiObject({
                    AdaptyLifecycleManager(injectInternal())
                }),

                ProductsInteractor::class.java to singleVariantDiObject({
                    ProductsInteractor(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),

                PurchaserInteractor::class.java to singleVariantDiObject({
                    PurchaserInteractor(
                        appContext,
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),

                PurchasesInteractor::class.java to singleVariantDiObject({
                    PurchasesInteractor(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),

                AuthInteractor::class.java to singleVariantDiObject({
                    AuthInteractor(injectInternal(), injectInternal())
                }),

                VisualPaywallManager::class.java to singleVariantDiObject({
                    VisualPaywallManager(
                        injectInternal(),
                        injectInternal()
                    )
                }),

                AdaptyInternal::class.java to singleVariantDiObject({
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