package com.adapty.internal.di

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.AdaptyInternal
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.PreferenceManager
import com.adapty.internal.data.cloud.*
import com.adapty.internal.domain.AuthInteractor
import com.adapty.internal.domain.ProductsInteractor
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.utils.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.Format
import java.util.*
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
                    GsonBuilder()
                        .registerTypeAdapter(
                            BigDecimal::class.java,
                            BigDecimalDeserializer()
                        ).create()
                }),

                Format::class.java to singleVariantDiObject({
                    DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
                }),

                PreferenceManager::class.java to singleVariantDiObject({
                    PreferenceManager(appContext, injectInternal())
                }),

                CloudRepository::class.java to singleVariantDiObject({
                    CloudRepository(
                        injectInternal(named = BASE),
                        injectInternal()
                    )
                }),

                CacheRepository::class.java to singleVariantDiObject({
                    CacheRepository(
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                HttpClient::class.java to mapOf(
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
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                NetworkConnectionCreator::class.java to mapOf(
                    null to DIObject({
                        DefaultConnectionCreator(injectInternal())
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
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                InstallationMetaCreator::class.java to singleVariantDiObject({
                    InstallationMetaCreator(appContext, injectInternal())
                }),

                AdIdRetriever::class.java to singleVariantDiObject({
                    AdIdRetriever(appContext, injectInternal())
                }),

                CustomAttributeValidator::class.java to singleVariantDiObject({
                    CustomAttributeValidator()
                }),

                PaywallPicker::class.java to singleVariantDiObject({ PaywallPicker() }),

                ProductPicker::class.java to singleVariantDiObject({ ProductPicker() }),

                AttributionHelper::class.java to singleVariantDiObject({ AttributionHelper() }),

                CurrencyHelper::class.java to singleVariantDiObject({ CurrencyHelper() }),

                PaywallMapper::class.java to singleVariantDiObject({
                    PaywallMapper(injectInternal())
                }),

                ProductMapper::class.java to singleVariantDiObject({
                    ProductMapper(
                        appContext,
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                ProrationModeMapper::class.java to singleVariantDiObject({ ProrationModeMapper() }),

                ProfileMapper::class.java to singleVariantDiObject({ ProfileMapper() }),

                StoreManager::class.java to singleVariantDiObject({
                    StoreManager(
                        appContext,
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                LifecycleAwareRequestRunner::class.java to singleVariantDiObject({
                    LifecycleAwareRequestRunner(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                LifecycleManager::class.java to singleVariantDiObject({
                    LifecycleManager(injectInternal())
                }),

                ProductsInteractor::class.java to singleVariantDiObject({
                    ProductsInteractor(
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

                ProfileInteractor::class.java to singleVariantDiObject({
                    ProfileInteractor(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                PurchasesInteractor::class.java to singleVariantDiObject({
                    PurchasesInteractor(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),

                AuthInteractor::class.java to singleVariantDiObject({
                    AuthInteractor(injectInternal(), injectInternal(), injectInternal(), injectInternal())
                }),

                AdaptyInternal::class.java to singleVariantDiObject({
                    AdaptyInternal(
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