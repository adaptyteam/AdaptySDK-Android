@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui

import android.app.Activity
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adapty.internal.di.DIObject
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.OBSERVER_MODE
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.MetaInfoRetriever
import com.adapty.internal.utils.PriceFormatter
import com.adapty.internal.utils.runOnMain
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyCustomAssets
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.script.ActionHandler
import com.adapty.ui.internal.script.StateAccessor
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.store.AnalyticsEffectHandler
import com.adapty.ui.internal.store.ConfigEffectHandler
import com.adapty.ui.internal.store.DataLoadingEffectHandler
import com.adapty.ui.internal.store.Effect
import com.adapty.ui.internal.store.EffectHandler
import com.adapty.ui.internal.store.EventDispatcherEffectHandler
import com.adapty.ui.internal.store.FocusLossVerifier
import com.adapty.ui.internal.store.JsEffectHandler
import com.adapty.ui.internal.store.ListenerEffectHandler
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.store.ObserverModeEffectHandler
import com.adapty.ui.internal.store.FlowState
import com.adapty.ui.internal.store.NavigationState
import com.adapty.ui.internal.store.PurchaseEffectHandler
import com.adapty.ui.internal.store.TimerCallbackScheduler
import com.adapty.ui.internal.store.TimerEffectHandler
import com.adapty.ui.internal.store.buildInitialState
import com.adapty.ui.internal.store.reduce
import com.adapty.ui.internal.text.PriceConverter
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.TagResolver
import com.adapty.ui.internal.text.TextResolver
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.ui.event.EventDispatcher
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.listeners.ContextAwareEventListener
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

internal class FlowViewModel(
    val flowKey: String,
    private val isObserverMode: Boolean,
    private val textResolver: TextResolver,
    private val stateHandler: StateHandler,
) : ViewModel() {

    internal lateinit var effectHandlers: List<EffectHandler>
    
    init {
        stateHandler.onStateRefreshed = { dispatch(Message.FlushPendingNavigation) }
    }
    
    override fun onCleared() {
        super.onCleared()
    }

    val dataState = mutableStateOf<UserArgs?>(null)

    private val _state: MutableState<FlowState?> = mutableStateOf(null)
    val state: FlowState? get() = _state.value

    var configChangeHandoffPending = false

    var activityProvider: (() -> Activity)? = null

    internal var contextAwareListener: ContextAwareEventListener? = null
        private set

    fun setContextAwareListener(listener: ContextAwareEventListener?) {
        contextAwareListener = listener
    }

    val eventDispatcher: EventDispatcher = EventDispatcher()

    fun dispatch(message: Message) {
        if (Looper.getMainLooper().thread != Thread.currentThread()) {
            runOnMain { dispatch(message) }
            return
        }
        val currentState = _state.value ?: run {
            log(WARN) { "$LOG_PREFIX dispatch dropped (state not initialized): $message" }
            return
        }
        val (newState, effects) = reduce(currentState, message)
        _state.value = newState
        if (newState.navigation.entries.isNotEmpty() &&
            (newState.navigation.entries !== currentState.navigation.entries ||
                newState.ui.timerCommands !== currentState.ui.timerCommands)
        ) {
            newState.config.viewConfig.runtimeState.let { runtime ->
                runtime.navigation = NavigationState(entries = newState.navigation.entries)
                runtime.timerCommands = newState.ui.timerCommands
            }
        }
        effects.forEach { effect -> handleEffect(effect) }
    }

    fun setNewData(newData: UserArgs) {
        if (_state.value?.config?.viewConfig !== newData.viewConfig) {
            var initial = buildInitialState(newData, isObserverMode, null)
            val runtime = newData.viewConfig.runtimeState
            runtime.restorableNavigation?.let { restoredNav ->
                initial = initial.copy(
                    navigation = restoredNav,
                    ui = initial.ui.copy(timerCommands = runtime.timerCommands),
                )
            }
            _state.value = initial
        }
        dataState.value = newData
        dispatch(Message.DataUpdated(newData))
    }

    val isNotifyingClose: Boolean get() = closeNotificationDepth > 0

    private var closeNotificationDepth = 0

    private fun handleEffect(effect: Effect) {
        val isClose = effect is Effect.NotifyListener.ActionPerformed && effect.action is AdaptyUI.Action.Close
        if (isClose) closeNotificationDepth++
        try {
            effectHandlers.forEach { it.handle(effect, ::dispatch) }
        } finally {
            if (isClose) closeNotificationDepth--
        }
    }

    fun setActionHandler(handler: ActionHandler?) {
        stateHandler.setActionHandler(handler)
    }
    
    @Composable
    fun resolveState(): StateAccessor {
        return stateHandler.observeState()
    }

    @Composable
    fun resolveText(stringId: StringId, textAttrs: Attributes?): com.adapty.ui.internal.text.StringWrapper? {
        val s = state ?: return null
        return textResolver.resolve(stringId, textAttrs, s.texts.items, s.products.items, s.assets.items, s.config.viewConfig.locale)
    }

    @Composable
    internal fun resolveTextWith(
        stringId: StringId,
        textAttrs: Attributes?,
        texts: Map<String, AdaptyUI.FlowConfiguration.TextItem>,
        products: Map<String, AdaptyPaywallProduct>,
        assets: Map<String, AdaptyUI.FlowConfiguration.Asset>,
    ): com.adapty.ui.internal.text.StringWrapper? {
        val locale = state?.config?.viewConfig?.locale
            ?: dataState.value?.viewConfig?.locale
            ?: Locale.getDefault()
        return textResolver.resolve(stringId, textAttrs, texts, products, assets, locale)
    }

}

internal class FlowViewModelFactory(
    private val vmArgs: FlowViewModelArgs,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(FlowViewModel::class.java) -> {
                val vm = FlowViewModel(
                    vmArgs.flowKey,
                    vmArgs.isObserverMode,
                    vmArgs.textResolver,
                    vmArgs.stateHandler,
                )
                vm.effectHandlers = buildEffectHandlers(vm, vmArgs)
                @Suppress("UNCHECKED_CAST")
                vm as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private fun buildEffectHandlers(
    vm: FlowViewModel,
    args: FlowViewModelArgs,
): List<EffectHandler> {
    val scope = vm.viewModelScope
    return listOf(
        JsEffectHandler(scope, args.stateHandler),
        PurchaseEffectHandler(scope, args.flowKey, { vm.activityProvider?.invoke() }, { vm.contextAwareListener }),
        ObserverModeEffectHandler(scope, args.flowKey),
        DataLoadingEffectHandler(scope, args.flowKey, args.mediaFetchService),
        ListenerEffectHandler { vm.contextAwareListener },
        EventDispatcherEffectHandler(vm.eventDispatcher),
        ConfigEffectHandler(args.textResolver),
        AnalyticsEffectHandler(args.flowKey),
        TimerEffectHandler(args.cacheRepository),
        TimerCallbackScheduler(scope),
        FocusLossVerifier(scope),
    ) + (args.extraEffectHandlerFactory?.invoke(scope)?.map { fn ->
        EffectHandler { effect, dispatch -> fn(effect, dispatch) }
    } ?: emptyList())
}

internal class FlowViewModelArgs(
    val flowKey: String,
    val isObserverMode: Boolean,
    val mediaFetchService: MediaFetchService,
    val cacheRepository: CacheRepository,
    val metaInfoRetriever: MetaInfoRetriever,
    val textResolver: TextResolver,
    val stateHandler: StateHandler,
    val extraEffectHandlerFactory: ((CoroutineScope) -> List<(Effect, (Message) -> Unit) -> Unit>)? = null,
) {
    companion object {
        fun create(
            flowKey: String,
            locale: Locale,
        ) =
            runCatching {
                val mediaFetchService = Dependencies.injectInternal<MediaFetchService>()
                val cacheRepository = Dependencies.injectInternal<CacheRepository>()
                val metaInfoRetriever = Dependencies.injectInternal<MetaInfoRetriever>()
                val isObserverMode = Dependencies.injectInternal<Boolean>(OBSERVER_MODE)
                val priceFormatter = Dependencies.injectInternal<PriceFormatter>(locale.toString()) {
                    DIObject({ PriceFormatter(locale) })
                }
                val priceConverter = PriceConverter()
                val tagResolver = TagResolver(
                    priceFormatter,
                    priceConverter,
                    AdaptyUiTagResolver.Default,
                    locale,
                )
                val textResolver = TextResolver(tagResolver)
                val stateHandler = Dependencies.injectInternal<StateHandler>()
                FlowViewModelArgs(
                    flowKey,
                    isObserverMode,
                    mediaFetchService,
                    cacheRepository,
                    metaInfoRetriever,
                    textResolver,
                    stateHandler,
                )
            }.getOrElse { e ->
                log(ERROR) {
                    "$LOG_PREFIX_ERROR $flowKey rendering error: ${e.localizedMessage}"
                }
                null
            }
    }
}

@InternalAdaptyApi
public class UserArgs internal constructor(
    internal val viewConfig: AdaptyUI.FlowConfiguration,
    internal val eventListener: AdaptyFlowEventListener,
    internal val userInsets: AdaptyFlowInsets,
    internal val customAssets: AdaptyCustomAssets,
    internal val tagResolver: AdaptyUiTagResolver,
    internal val timerResolver: AdaptyUiTimerResolver,
    internal val observerModeHandler: AdaptyUiObserverModeHandler?,
    internal val products: List<AdaptyPaywallProduct>,
    internal val productLoadingFailureCallback: ProductLoadingFailureCallback,
) {
    internal companion object {
        internal fun create(
            viewConfig: AdaptyUI.FlowConfiguration,
            eventListener: AdaptyFlowEventListener,
            userInsets: AdaptyFlowInsets,
            customAssets: AdaptyCustomAssets,
            tagResolver: AdaptyUiTagResolver,
            timerResolver: AdaptyUiTimerResolver,
            observerModeHandler: AdaptyUiObserverModeHandler?,
            products: List<AdaptyPaywallProduct>?,
            productLoadingFailureCallback: ProductLoadingFailureCallback,
        ): UserArgs =
            UserArgs(
                viewConfig,
                eventListener,
                userInsets,
                customAssets,
                tagResolver,
                timerResolver,
                observerModeHandler,
                products.orEmpty(),
                productLoadingFailureCallback,
            )
    }
}
