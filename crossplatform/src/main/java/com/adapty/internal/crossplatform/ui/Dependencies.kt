@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform.ui

import android.content.Context
import com.adapty.internal.crossplatform.AdaptyCallHandler
import com.adapty.internal.crossplatform.CrossplatformHelper
import com.adapty.internal.crossplatform.EventCallback
import com.adapty.internal.crossplatform.FileLocationTransformer
import com.adapty.internal.crossplatform.SerializationHelper
import com.adapty.internal.crossplatform.UrlHandler
import com.adapty.internal.crossplatform.retryLazy
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.log
import com.adapty.utils.AdaptyLogLevel
import kotlin.LazyThreadSafetyMode.NONE

internal object Dependencies {
    internal inline fun <reified T> inject(named: String? = null) = lazy(NONE) {
        injectInternal<T>(named)
    }

    internal inline fun <reified T : Any> safeInject(named: String? = null) =
        retryLazy {
            runCatching { injectInternal<T>(named) }
                .onFailure { e ->
                    log(AdaptyLogLevel.ERROR, { "could not find instance of ${T::class.java.simpleName}($named)" })
                }
                .getOrNull()
        }

    private inline fun <reified T> injectInternal(named: String? = null) =
        (map[T::class.java]!![named] as DIObject<T>).provide()

    @get:JvmSynthetic
    internal val map = hashMapOf<Class<*>, Map<String?, DIObject<*>>>()

    private fun <T> singleVariantDiObject(
        initializer: () -> T,
        initType: DIObject.InitType = DIObject.InitType.SINGLETON
    ): Map<String?, DIObject<T>> = mapOf(null to DIObject(initializer, initType))

    @JvmSynthetic
    internal fun init(
        appContext: Context,
        onNewEvent: MutableMap<String?, EventCallback<String>>,
        transformFileLocation: FileLocationTransformer,
        isPresentationEmbedded: Boolean,
    ) {
        map.putAll(
            listOf(
                FlowUiManager::class.java to singleVariantDiObject({
                    FlowUiManager(injectInternal())
                }),

                OnboardingUiManager::class.java to singleVariantDiObject({
                    OnboardingUiManager(injectInternal())
                }),

                CrossplatformHelper::class.java to singleVariantDiObject({
                    CrossplatformHelper(
                        injectInternal(),
                    )
                }),

                SerializationHelper::class.java to singleVariantDiObject({
                    SerializationHelper(transformFileLocation)
                }),

                AdaptyCallHandler::class.java to singleVariantDiObject({
                    AdaptyCallHandler(
                        appContext,
                        injectInternal(),
                        injectInternal(),
                        injectInternal(),
                        onNewEvent,
                        transformFileLocation,
                    )
                }),

                UrlHandler::class.java to singleVariantDiObject({
                    UrlHandler()
                }),

                CrossplatformUiHelper::class.java to singleVariantDiObject({
                    CrossplatformUiHelper(
                        injectInternal(),
                        injectInternal(),
                        isPresentationEmbedded,
                    )
                }),
            )
        )
    }
}