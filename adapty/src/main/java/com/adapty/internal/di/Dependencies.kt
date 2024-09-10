package com.adapty.internal.di

import android.app.Application
import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.AdaptyInternal
import com.adapty.internal.data.cache.CacheEntityTypeAdapterFactory
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cache.PreferenceManager
import com.adapty.internal.data.cache.ResponseCacheKeyProvider
import com.adapty.internal.data.cloud.*
import com.adapty.internal.data.models.*
import com.adapty.internal.data.models.requests.SendEventRequest
import com.adapty.internal.domain.AuthInteractor
import com.adapty.internal.domain.ProductsInteractor
import com.adapty.internal.domain.ProfileInteractor
import com.adapty.internal.domain.PurchasesInteractor
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyConfig
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Semaphore
import java.math.BigDecimal
import java.util.*
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KClass

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalAdaptyApi
public object Dependencies {

    public inline fun <reified T: Any> inject(named: String? = null, noinline putIfAbsent: (() -> DIObject<T>)? = null): Lazy<T> = lazy(NONE) {
        injectInternal<T>(named, putIfAbsent)
    }

    public inline fun <reified T: Any> injectInternal(named: String? = null, noinline putIfAbsent: (() -> DIObject<T>)? = null): T =
        resolve(named, T::class, putIfAbsent)

    public fun <T: Any> resolve(named: String? = null, classOfT: KClass<T>, putIfAbsent: (() -> DIObject<T>)?): T {
        if (putIfAbsent == null)
            return (map[classOfT]!![named] as DIObject<T>).provide()
        val desiredObjectBucket = map[classOfT] ?: kotlin.run {
            val newObject = putIfAbsent()
            contribute(classOfT to mapOf(named to newObject))
            return newObject.provide()
        }
        val desiredObject = desiredObjectBucket[named] as? DIObject<T> ?: kotlin.run {
            val newObject = putIfAbsent()
            contribute(classOfT to desiredObjectBucket.toMutableMap().apply { put(named, newObject) })
            return newObject.provide()
        }
        return desiredObject.provide()
    }

    @get:JvmSynthetic
    internal val map = hashMapOf<KClass<*>, Map<String?, DIObject<*>>>()

    private const val BASE = "base"
    private const val ANALYTICS = "analytics"
    private const val RECORD_ONLY = "record_only"
    private const val LOCAL = "local"
    private const val REMOTE = "remote"
    public const val OBSERVER_MODE: String = "observer_mode"

    public fun <T> singleVariantDiObject(
        initializer: () -> T,
        initType: DIObject.InitType = DIObject.InitType.SINGLETON
    ): Map<String?, DIObject<T>> = mapOf(null to DIObject(initializer, initType))

    public fun contribute(deps: Iterable<Pair<KClass<*>, Map<String?, DIObject<*>>>>) {
        map.putAll(deps)
    }

    public fun contribute(dep: Pair<KClass<*>, Map<String?, DIObject<*>>>) {
        map[dep.first] = dep.second
    }

    public var onInitialDepsCreated: (() -> Unit)? = null

    @JvmSynthetic
    internal fun init(appContext: Context, config: AdaptyConfig) {
        map.putAll(
            listOf(
                Context::class to singleVariantDiObject({ appContext }),
                Gson::class to mapOf(
                    BASE to DIObject({
                        val dataKey = "data"
                        val attributesKey = "attributes"
                        val metaKey = "meta"
                        val placementIdKey = "placement_id"
                        val versionKey = "version"
                        val profileKey = "profile"
                        val errorsKey = "errors"
                        val responseCreatedAtKey = "response_created_at"
                        val snapshotAtKey = "snapshot_at"

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
                        val variationsExtractor = ResponseDataExtractor { jsonElement ->
                            val variations = JsonArray()

                            ((jsonElement as? JsonObject)?.get(dataKey) as? JsonArray)
                                ?.forEach { element ->
                                    ((element as? JsonObject)?.get(attributesKey) as? JsonObject)
                                        ?.let(variations::add)
                                }

                            val meta = (jsonElement as? JsonObject)?.get(metaKey) as? JsonObject

                            val snapshotAt = (meta?.get(responseCreatedAtKey) as? JsonPrimitive) ?: JsonPrimitive(0)

                            val version = (meta?.get(versionKey) as? JsonPrimitive) ?: JsonPrimitive(0)

                            JsonObject().apply {
                                add(dataKey, variations)
                                add(snapshotAtKey, snapshotAt)
                                add(versionKey, version)
                            }
                        }
                        val fallbackVariationsExtractor = ResponseDataExtractor { jsonElement ->
                            val jsonObject = jsonElement.asJsonObject
                            jsonObject.remove(metaKey)

                            val variations = JsonArray()

                            jsonObject.getAsJsonObject(dataKey).entrySet()
                                .first { (key, value) ->
                                    val desiredArray = (value as? JsonArray)?.isEmpty == false
                                    desiredArray.also {
                                        if (desiredArray) jsonObject.addProperty(placementIdKey, key)
                                    }
                                }
                                .value.asJsonArray
                                .forEach { element ->
                                    ((element as? JsonObject)?.get(attributesKey) as? JsonObject)
                                        ?.let(variations::add)
                                }

                            jsonObject.add(dataKey, variations)
                            jsonObject
                        }
                        val validationResultExtractor = ResponseDataExtractor { jsonElement ->
                            (((jsonElement as? JsonObject)?.get(dataKey) as? JsonObject)
                                ?.get(attributesKey) as? JsonObject)?.let { result ->

                                val errors = (result.remove(errorsKey) as? JsonArray) ?: JsonArray()

                                JsonObject().apply {
                                    add(profileKey, result)
                                    add(errorsKey, errors)
                                }
                            }
                        }

                        GsonBuilder()
                            .registerTypeAdapterFactory(
                                AdaptyResponseTypeAdapterFactory(
                                    TypeToken.get(Variations::class.java),
                                    variationsExtractor,
                                )
                            )
                            .registerTypeAdapterFactory(
                                AdaptyResponseTypeAdapterFactory(
                                    TypeToken.get(AnalyticsConfig::class.java),
                                    dataObjectExtractor,
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
                                    TypeToken.get(FallbackVariations::class.java),
                                    fallbackVariationsExtractor,
                                )
                            )
                            .registerTypeAdapterFactory(
                                AdaptyResponseTypeAdapterFactory(
                                    TypeToken.get(ValidationResult::class.java),
                                    validationResultExtractor,
                                )
                            )
                            .registerTypeAdapterFactory(
                                CacheEntityTypeAdapterFactory()
                            )
                            .registerTypeAdapterFactory(
                                CreateOrUpdateProfileRequestTypeAdapterFactory()
                            )
                            .registerTypeAdapter(
                                object : TypeToken<Set<BackendError.InternalError>>() {}.type,
                                BackendInternalErrorDeserializer()
                            )
                            .registerTypeAdapter(
                                SendEventRequest::class.java,
                                SendEventRequestSerializer()
                            )
                            .registerTypeAdapter(
                                AnalyticsEvent::class.java,
                                AnalyticsEventTypeAdapter()
                            )
                            .registerTypeAdapter(
                                AnalyticsData::class.java,
                                AnalyticsDataTypeAdapter()
                            )
                            .registerTypeAdapter(
                                BigDecimal::class.java,
                                BigDecimalDeserializer()
                            )
                            .create()
                    }),
                    ANALYTICS to DIObject({
                        GsonBuilder()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create()
                    })
                ),

                Boolean::class to mapOf(
                    OBSERVER_MODE to DIObject({ config.observerMode }),
                ),

                PreferenceManager::class to singleVariantDiObject({
                    PreferenceManager(appContext, injectInternal(named = BASE))
                }),

                CloudRepository::class to singleVariantDiObject({
                    CloudRepository(
                        injectInternal(named = BASE),
                        injectInternal()
                    )
                }),

                CacheRepository::class to singleVariantDiObject({
                    CacheRepository(
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                HttpClient::class to mapOf(
                    BASE to DIObject({
                        BaseHttpClient(
                            injectInternal(),
                            injectInternal(named = BASE),
                            injectInternal(named = BASE),
                        )
                    }),
                    ANALYTICS to DIObject({
                        BaseHttpClient(
                            injectInternal(),
                            injectInternal(named = ANALYTICS),
                            injectInternal(named = RECORD_ONLY),
                        )
                    }),
                ),

                Semaphore::class to mapOf(
                    LOCAL to DIObject({
                        Semaphore(1)
                    }),
                    REMOTE to DIObject({
                        Semaphore(1)
                    }),
                ),

                AnalyticsEventQueueDispatcher::class to singleVariantDiObject({
                    AnalyticsEventQueueDispatcher(
                        injectInternal(),
                        injectInternal(named = ANALYTICS),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(named = LOCAL),
                        injectInternal(named = REMOTE),
                    )
                }),

                AnalyticsTracker::class to mapOf(
                    BASE to DIObject({
                        AnalyticsManager(
                            injectInternal(named = RECORD_ONLY),
                            injectInternal(),
                        )
                    }),
                    RECORD_ONLY to DIObject({
                        AnalyticsEventRecorder(
                            injectInternal(),
                            injectInternal(named = ANALYTICS),
                            injectInternal(named = LOCAL),
                        )
                    }),
                ),

                NetworkConnectionCreator::class to singleVariantDiObject({
                    DefaultConnectionCreator()
                }),

                HttpResponseManager::class to mapOf(
                    BASE to DIObject({
                        DefaultHttpResponseManager(
                            injectInternal(),
                            injectInternal(),
                            injectInternal(named = BASE),
                        )
                    }),
                    ANALYTICS to DIObject({
                        DefaultHttpResponseManager(
                            injectInternal(),
                            injectInternal(),
                            injectInternal(named = RECORD_ONLY),
                        )
                    }),
                ),

                ResponseBodyConverter::class to singleVariantDiObject({
                    DefaultResponseBodyConverter(injectInternal(named = BASE))
                }),

                ResponseCacheKeyProvider::class to singleVariantDiObject({
                    ResponseCacheKeyProvider()
                }),

                PayloadProvider::class to singleVariantDiObject({
                    PayloadProvider(injectInternal(), injectInternal())
                }),

                RequestFactory::class to singleVariantDiObject({
                    RequestFactory(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(named = BASE),
                        config.apiKey,
                        config.observerMode,
                    )
                }),

                InstallationMetaCreator::class to singleVariantDiObject({
                    InstallationMetaCreator(injectInternal())
                }),

                MetaInfoRetriever::class to singleVariantDiObject({
                    MetaInfoRetriever(
                        appContext,
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                CrossplatformMetaRetriever::class to singleVariantDiObject({
                    CrossplatformMetaRetriever()
                }),

                AdaptyUiAccessor::class to singleVariantDiObject({
                    AdaptyUiAccessor()
                }),

                AdIdRetriever::class to singleVariantDiObject({
                    AdIdRetriever(appContext, injectInternal())
                }),

                AppSetIdRetriever::class to singleVariantDiObject({
                    AppSetIdRetriever(appContext)
                }),

                StoreCountryRetriever::class to singleVariantDiObject({
                    StoreCountryRetriever(injectInternal())
                }),

                UserAgentRetriever::class to singleVariantDiObject({
                    UserAgentRetriever(appContext)
                }),

                IPv4Retriever::class to singleVariantDiObject({
                    IPv4Retriever(config.ipAddressCollectionDisabled, injectInternal())
                }),

                FallbackPaywallRetriever::class to singleVariantDiObject({
                    FallbackPaywallRetriever(appContext, injectInternal(named = BASE))
                }),

                CustomAttributeValidator::class to singleVariantDiObject({
                    CustomAttributeValidator()
                }),

                VariationPicker::class to singleVariantDiObject({
                    VariationPicker(injectInternal())
                }),

                AttributionHelper::class to singleVariantDiObject({ AttributionHelper() }),

                PriceFormatter::class to singleVariantDiObject({
                    PriceFormatter(
                        injectInternal<MetaInfoRetriever>().currentLocale ?: Locale.getDefault()
                    )
                }),

                HashingHelper::class to singleVariantDiObject({ HashingHelper() }),

                PaywallMapper::class to singleVariantDiObject({
                    PaywallMapper(injectInternal(named = BASE))
                }),

                ProductMapper::class to singleVariantDiObject({
                    ProductMapper(
                        appContext,
                        injectInternal(),
                    )
                }),

                ReplacementModeMapper::class to singleVariantDiObject({ ReplacementModeMapper() }),

                ProfileMapper::class to singleVariantDiObject({ ProfileMapper() }),

                StoreManager::class to singleVariantDiObject({
                    StoreManager(
                        appContext,
                        injectInternal(),
                        injectInternal(named = BASE),
                    )
                }),

                LifecycleAwareRequestRunner::class to singleVariantDiObject({
                    LifecycleAwareRequestRunner(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(named = BASE),
                        injectInternal(),
                    )
                }),

                LifecycleManager::class to singleVariantDiObject({
                    LifecycleManager(appContext as Application, injectInternal())
                }),

                ProductsInteractor::class to singleVariantDiObject({
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
                        injectInternal(named = BASE),
                    )
                }),

                ProfileInteractor::class to singleVariantDiObject({
                    ProfileInteractor(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                    )
                }),

                PurchasesInteractor::class to singleVariantDiObject({
                    PurchasesInteractor(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal()
                    )
                }),

                AuthInteractor::class to singleVariantDiObject({
                    AuthInteractor(
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

                AdaptyInternal::class to singleVariantDiObject({
                    AdaptyInternal(
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(named = BASE),
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        config.observerMode,
                        config.ipAddressCollectionDisabled,
                    )
                }),
            )
        )
        onInitialDepsCreated?.invoke()
    }
}