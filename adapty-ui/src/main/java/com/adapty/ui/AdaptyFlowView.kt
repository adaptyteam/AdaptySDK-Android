@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.UiThread
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.internal.script.SDKGlobals
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.ui.AdaptyFlowInternal
import com.adapty.ui.internal.ui.FlowViewModel
import com.adapty.ui.internal.ui.FlowViewModelArgs
import com.adapty.ui.internal.ui.FlowViewModelFactory
import com.adapty.ui.internal.ui.UserArgs
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.FlowMode
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.adoptFlowStateForConfigChange
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.internal.utils.getCurrentLocale
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.initializeFlowStateAsync
import com.adapty.ui.internal.utils.withAdaptyUIActivated
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import java.util.UUID

public class AdaptyFlowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var instanceId: String? = null

    private fun resolveViewModelKey(): String? {
        instanceId?.let { return it }
        if (id == View.NO_ID) {
            log(WARN) {
                "$LOG_PREFIX AdaptyFlowView (${hashCode()}) has no stable id; falling back to a shared ViewModel. " +
                        "Set a stable id to give each paywall its own ViewModel that survives configuration changes."
            }
            return null
        }
        return UUID.randomUUID().toString().also { instanceId = it }
    }

    private val viewModelArgs: FlowViewModelArgs? by lazy {
        FlowViewModelArgs.create(
            "${UUID.randomUUID().toString().hashCode()}",
            context.getCurrentLocale(),
        )
    }

    private val viewModelLazy = lazy<FlowViewModel?> {
        val viewModelStoreOwner = findViewTreeViewModelStoreOwner()
            ?: run {
                log(ERROR) { "$LOG_PREFIX AdaptyFlowView (${hashCode()}) rendering error: No ViewModelStoreOwner found" }
                return@lazy null
            }
        viewModelArgs?.let { args ->
            val factory = FlowViewModelFactory(args)
            val provider = ViewModelProvider(viewModelStoreOwner, factory)
            when (val key = resolveViewModelKey()) {
                null -> provider[FlowViewModel::class.java]
                else -> provider[key, FlowViewModel::class.java]
            }

        }
    }
    private val viewModel: FlowViewModel? by viewModelLazy

    /**
     * Should be called only on UI thread
     *
     * If the [AdaptyFlowView] has been created by calling [AdaptyUI.getFlowView],
     * calling this method is unnecessary.
     *
     * @param[flowConfiguration] An [AdaptyUI.FlowConfiguration] object containing information
     * about the visual part of the flow. To load it, use the [AdaptyUI.getFlowConfiguration] method.
     *
     * @param[products] Optional [AdaptyPaywallProduct] list. Pass this value in order to optimize
     * the display time of the products on the screen. If you pass `null`, `AdaptyUI` will
     * automatically fetch the required products.
     *
     * @param[eventListener] An object that implements the [AdaptyFlowEventListener] interface.
     * Use it to respond to different events happening inside the purchase screen.
     * Also you can extend [AdaptyFlowDefaultEventListener] so you don't need to override all the methods.
     *
     * @param[insets] You can override the default window inset handling by specifying the [AdaptyFlowInsets].
     *
     * @param[customAssets] If you are going to use custom assets functionality, pass [AdaptyCustomAssets] here.
     *
     * @param[tagResolver] If you are going to use custom tags functionality, pass the resolver function here.
     *
     * @param[timerResolver] If you are going to use custom timer functionality, pass the resolver function here.
     *
     * @param[observerModeHandler] If you use Adapty in [Observer mode](https://adapty.io/docs/observer-vs-full-mode),
     * pass the [AdaptyUiObserverModeHandler] implementation to handle purchases on your own.
     */
    @UiThread
    public fun showFlow(
        flowConfiguration: AdaptyUI.FlowConfiguration,
        products: List<AdaptyPaywallProduct>?,
        eventListener: AdaptyFlowEventListener,
        insets: AdaptyFlowInsets = AdaptyFlowInsets.Unspecified,
        customAssets: AdaptyCustomAssets = AdaptyCustomAssets.Empty,
        tagResolver: AdaptyUiTagResolver = AdaptyUiTagResolver.Default,
        timerResolver: AdaptyUiTimerResolver = AdaptyUiTimerResolver.Default,
        observerModeHandler: AdaptyUiObserverModeHandler? = null,
    ) {
        val args = viewModelArgs ?: return

        runOnceWhenAttached {
            val vm = viewModel ?: return@runOnceWhenAttached

            fun pushData() {
                runOnceWhenAttached {
                    vm.setNewData(
                        UserArgs.create(
                            flowConfiguration,
                            eventListener,
                            insets,
                            customAssets,
                            tagResolver,
                            timerResolver,
                            observerModeHandler,
                            products,
                            ProductLoadingFailureCallback { error -> eventListener.onLoadingProductsFailure(error, context) },
                        )
                    )
                }
            }

            val previousConfig = vm.dataState.value?.viewConfig
            val configChangeHandoff = vm.configChangeHandoffPending
            vm.configChangeHandoffPending = false
            if (previousConfig === flowConfiguration && vm.state != null) {
                pushData()
                return@runOnceWhenAttached
            }

            if (configChangeHandoff && previousConfig != null &&
                previousConfig.id == flowConfiguration.id && vm.state != null
            ) {
                stateHandler.adoptFlowStateForConfigChange(previousConfig, flowConfiguration)
            }

            if (stateHandler.stateOwner !== flowConfiguration) {
                vm.dataState.value = null
            }
            val sdkEnvJson = SDKGlobals.buildSDKEnvJson(args.metaInfoRetriever, context, flowConfiguration.mode, flowConfiguration.locale, flowConfiguration.localizationId, flowConfiguration.isRtl)
            val mode = flowConfiguration.mode
            val sdkProductsJson = if (mode is FlowMode.Live)
                SDKGlobals.buildStaticSDKProductsJson(mode.flow)
            else
                SDKGlobals.buildSDKProductsJson(emptyMap())
            stateHandler.initializeFlowStateAsync(flowConfiguration, sdkEnvJson, sdkProductsJson) {
                pushData()
            }
        }
    }

    @Composable
    override fun Content() {
        val viewModel = viewModel ?: return
        if (viewModel.dataState.value != null) {
            AdaptyFlowInternal(viewModel)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val id = if (id != View.NO_ID) instanceId else null
        return SavedState(superState, id)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            instanceId = state.instanceId
            super.onRestoreInstanceState(state.superState)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        log(VERBOSE) { "$LOG_PREFIX AdaptyFlowView (${hashCode()}) onAttachedToWindow" }
    }

    override fun onDetachedFromWindow() {
        log(VERBOSE) { "$LOG_PREFIX AdaptyFlowView (${hashCode()}) onDetachedFromWindow" }
        if (viewModelLazy.isInitialized() &&
            context.getActivityOrNull()?.isChangingConfigurations == true
        ) {
            viewModelLazy.value?.configChangeHandoffPending = true
        }
        super.onDetachedFromWindow()
    }

    private fun runOnceWhenAttached(action: () -> Unit) {
        if (isAttachedToWindow) {
            action()
        } else {
            addOnAttachStateChangeListener(object: OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View?) {
                    action()
                    removeOnAttachStateChangeListener(this)
                }

                override fun onViewDetachedFromWindow(v: View?) {
                    removeOnAttachStateChangeListener(this)
                }
            })
        }
    }

    private val stateHandler: StateHandler by lazy {
        withAdaptyUIActivated {
            Dependencies.injectInternal<StateHandler>()
        }
    }

    private class SavedState : View.BaseSavedState {
        val instanceId: String?

        constructor(superState: Parcelable?, instanceId: String?) : super(superState) {
            this.instanceId = instanceId
        }

        constructor(source: Parcel) : super(source) {
            instanceId = source.readString()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(instanceId)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel) = SavedState(source)
                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }
    }
}