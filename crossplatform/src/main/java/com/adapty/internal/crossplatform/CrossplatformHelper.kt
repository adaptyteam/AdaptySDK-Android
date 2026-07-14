package com.adapty.internal.crossplatform

import android.app.Application
import android.content.Context
import com.adapty.internal.crossplatform.ui.Dependencies
import com.adapty.internal.crossplatform.ui.Dependencies.inject
import com.adapty.models.AdaptyWebPresentation
import com.adapty.utils.FileLocation
import java.util.concurrent.atomic.AtomicBoolean

class CrossplatformHelper internal constructor(
    private val callHandler: AdaptyCallHandler,
) {

    companion object {
        private val isInitialized = AtomicBoolean(false)

        @JvmStatic
        @JvmOverloads
        fun init(
            context: Context,
            onNewEvent: EventCallback<String>,
            transformFileLocation: FileLocationTransformer =
                FileLocationTransformer { value -> FileLocation.fromAsset(value) },
            isPresentationEmbedded: Boolean = false,
        ): Boolean =
            initInternal(null, context, onNewEvent, transformFileLocation, isPresentationEmbedded)

        @JvmStatic
        @JvmOverloads
        fun init(
            frameworkPluginInstance: Any,
            context: Context,
            onNewEvent: EventCallback<String>,
            transformFileLocation: FileLocationTransformer =
                FileLocationTransformer { value -> FileLocation.fromAsset(value) },
            isPresentationEmbedded: Boolean = false,
        ): Boolean =
            initInternal(frameworkPluginInstance, context, onNewEvent, transformFileLocation, isPresentationEmbedded)

        private fun initInternal(
            frameworkPluginInstance: Any?,
            context: Context,
            onNewEvent: EventCallback<String>,
            transformFileLocation: FileLocationTransformer,
            isPresentationEmbedded: Boolean,
        ): Boolean {
            if (isInitialized.compareAndSet(false, true)) {
                Dependencies.init(
                    context as? Application ?: context.applicationContext,
                    mutableMapOf(frameworkPluginInstance?.toString() to onNewEvent),
                    transformFileLocation,
                    isPresentationEmbedded,
                )
                return true
            } else {
                shared.callHandler.onNewEvent[frameworkPluginInstance?.toString()] = onNewEvent
            }
            return false
        }

        @JvmStatic
        val shared: CrossplatformHelper by inject()

        internal var meta: MetaInfo? = null
    }

    fun setActivity(activity: ActivityProvider?) {
        callHandler.activity = activity ?: ActivityProvider.Empty
    }

    @JvmOverloads
    fun onMethodCall(
        argument: Any?,
        methodName: String? = null,
        onResult: ResultCallback<String>,
    ) {
        callHandler.onMethodCall(argument, methodName, onResult)
    }

    fun release() {
        callHandler.onNewEvent.clear()
    }

    fun release(frameworkPluginInstance: Any) {
        callHandler.onNewEvent.remove(frameworkPluginInstance.toString())
    }

    fun openUrl(url: String, presentation: AdaptyWebPresentation) {
        callHandler.handleOpenUrl(url, presentation)
    }
}
