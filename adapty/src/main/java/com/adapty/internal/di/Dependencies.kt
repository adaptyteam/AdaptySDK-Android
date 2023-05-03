package com.adapty.internal.di

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.AdaptyInternal
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.PreferenceManager
import com.adapty.internal.data.cache.ResponseCacheKeyProvider
import com.adapty.internal.data.cloud.*
import com.adapty.internal.data.models.*
import com.adapty.internal.domain.AuthInteractor
import com.adapty.internal.domain.ProductsInteractor
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.utils.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.Format
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object Dependencies {
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
    internal fun init(appContext: Context, apiKey: String, observerMode: Boolean) {
        map.putAll(
            listOf(
                Gson::class.java to singleVariantDiObject({
                    val dataKey = "data"
                    val attributesKey = "attributes"
                    val metaKey = "meta"
                    val paywallsKey = "paywalls"
                    val productsKey = "products"
                    val versionKey = "version"

                    val attributesObjectExtractor = ResponseDataExtractor { jsonElement ->
                        ((jsonElement as? JsonObject)?.get(dataKey) as? JsonObject)
                            ?.get(attributesKey) as? JsonObject
                    }
                    val dataArrayExtractor = ResponseDataExtractor { jsonElement ->
                        (jsonElement as? JsonObject)?.get(dataKey) as? JsonArray
                    }
                    val dataObjectExtractor = ResponseDataExtractor { jsonElement ->
                        (jsonElement as? JsonObject)?.get(dataKey) as? JsonObject
                    }
                    val fallbackPaywallsExtractor = ResponseDataExtractor { jsonElement ->
                        val paywalls = JsonArray()

                        ((jsonElement as? JsonObject)?.get(dataKey) as? JsonArray)
                            ?.forEach { element ->
                                ((element as? JsonObject)?.get(attributesKey) as? JsonObject)
                                    ?.let(paywalls::add)
                            }

                        val meta = (jsonElement as? JsonObject)?.get(metaKey) as? JsonObject

                        val products = (meta?.get(productsKey) as? JsonArray) ?: JsonArray()

                        val version = (meta?.get(versionKey) as? JsonPrimitive) ?: JsonPrimitive(0)

                        JsonObject().apply {
                            add(paywallsKey, paywalls)
                            add(productsKey, products)
                            add(versionKey, version)
                        }
                    }

                    GsonBuilder()
                        .registerTypeAdapterFactory(
                            AdaptyResponseTypeAdapterFactory(
                                TypeToken.get(PaywallDto::class.java),
                                attributesObjectExtractor,
                            )
                        )
                        .registerTypeAdapterFactory(
                            AdaptyResponseTypeAdapterFactory(
                                TypeToken.get(ViewConfigurationDto::class.java),
                                attributesObjectExtractor,
                            )
                        )
                        .registerTypeAdapterFactory(
                            AdaptyResponseTypeAdapterFactory(
                                TypeToken.get(ProfileDto::class.java),
                                attributesObjectExtractor,
                            )
                        )
                        .registerTypeAdapterFactory(
                            AdaptyResponseTypeAdapterFactory(
                                object : TypeToken<ArrayList<ProductDto>>() {},
                                dataArrayExtractor,
                            )
                        )
                        .registerTypeAdapterFactory(
                            AdaptyResponseTypeAdapterFactory(
                                object : TypeToken<ArrayList<String>>() {},
                                dataArrayExtractor,
                            )
                        )
                        .registerTypeAdapterFactory(
                            AdaptyResponseTypeAdapterFactory(
                                TypeToken.get(AnalyticsCreds::class.java),
                                dataObjectExtractor,
                            )
                        )
                        .registerTypeAdapterFactory(
                            AdaptyResponseTypeAdapterFactory(
                                TypeToken.get(FallbackPaywalls::class.java),
                                fallbackPaywallsExtractor,
                            )
                        )
                        .registerTypeAdapter(
                            BigDecimal::class.java,
                            BigDecimalDeserializer()
                        )
                        .create()
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
                        injectInternal(),
                    )
                }),

                HttpClient::class.java to mapOf(
                    BASE to DIObject({
                        BaseHttpClient(
                            injectInternal(),
                            injectInternal(),
                        )
                    }),
                    KINESIS to DIObject({
                        BaseHttpClient(
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
                        DefaultConnectionCreator(
                            injectInternal(),
                            injectInternal(),
                            apiKey,
                            observerMode,
                        )
                    }),
                    KINESIS to DIObject({
                        KinesisConnectionCreator(injectInternal(), injectInternal())
                    }),
                ),

                HttpResponseManager::class.java to mapOf(
                    null to DIObject({
                        DefaultHttpResponseManager(
                            injectInternal(),
                            injectInternal(),
                        )
                    }),
                    KINESIS to DIObject({
                        DefaultHttpResponseManager(
                            injectInternal(named = KINESIS),
                            injectInternal(),
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

                ResponseCacheKeyProvider::class.java to singleVariantDiObject({
                    ResponseCacheKeyProvider()
                }),

                RequestFactory::class.java to singleVariantDiObject({
                    RequestFactory(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                InstallationMetaCreator::class.java to singleVariantDiObject({
                    InstallationMetaCreator(injectInternal())
                }),

                MetaInfoRetriever::class.java to singleVariantDiObject({
                    MetaInfoRetriever(appContext, injectInternal(), injectInternal())
                }),

                CrossplatformMetaRetriever::class.java to singleVariantDiObject({
                    CrossplatformMetaRetriever()
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

                HashingHelper::class.java to singleVariantDiObject({ HashingHelper() }),

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

                ViewConfigurationMapper::class.java to singleVariantDiObject({
                    ViewConfigurationMapper()
                }),

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
                    AuthInteractor(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
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
                        observerMode,
                    )
                }),
            )
        )
    }
}