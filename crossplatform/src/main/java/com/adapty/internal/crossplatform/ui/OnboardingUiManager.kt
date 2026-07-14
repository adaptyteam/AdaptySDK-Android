@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform.ui

import android.content.Context
import androidx.core.view.doOnAttach
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.adapty.internal.crossplatform.SerializationHelper
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.log
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.AdaptyUI
import com.adapty.ui.onboardings.AdaptyOnboardingView
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCloseAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCustomAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingLoadedAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingOpenPaywallAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingStateUpdatedAction
import com.adapty.ui.onboardings.errors.AdaptyOnboardingError
import com.adapty.ui.onboardings.events.AdaptyOnboardingAnalyticsEvent
import com.adapty.ui.onboardings.listeners.AdaptyOnboardingEventListener
import com.adapty.utils.AdaptyLogLevel
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

internal class OnboardingUiManager(
    private val serializationHelper: SerializationHelper,
) {
    private val cachedOnboardingUiData = ConcurrentHashMap<String, OnboardingUiData>()

    fun getData(key: String): OnboardingUiData? = cachedOnboardingUiData[key]

    fun putData(key: String, data: OnboardingUiData) {
        cachedOnboardingUiData[key] = data
    }

    fun removeData(key: String) {
        cachedOnboardingUiData.remove(key)
    }

    fun hasData(key: String) = cachedOnboardingUiData[key] != null

    var isShown = false

    var uiEventsObserver: ((event: AdaptyUiEvent) -> Unit)? = null

    fun handleSystemBack(key: String): Boolean {
        getData(key) ?: return false

        // do nothing

        return true
    }

    private var onboardingView: WeakReference<AdaptyOnboardingView>? = null

    fun getCurrentView(): AdaptyOnboardingView? = onboardingView?.get()

    fun setCurrentView(view: AdaptyOnboardingView) {
        onboardingView = WeakReference(view)
    }

    fun clearCurrentView() {
        onboardingView?.clear()
    }

    fun setupOnboardingView(
        onboardingView: AdaptyOnboardingView,
        viewModelStoreOwner: ViewModelStoreOwner?,
        args: Any?,
        id: String,
        platformViewOnEvent: ((viewId: String, eventId: String, eventData: String) -> Unit)? = null,
    ) {
        onboardingView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        onboardingView.doOnAttach {
            val onboardingArgs = (args as? String)?.let { serializationHelper.parseJsonArgument<CreateOnboardingViewArgs>(it) } ?: kotlin.run {
                log(AdaptyLogLevel.ERROR, { "could not parse args as Onboarding ($args)" })
                return@doOnAttach
            }

            val onboardingConfig = AdaptyUI.getOnboardingConfiguration(
                onboardingArgs.onboarding,
                onboardingArgs.externalUrlsPresentation ?: AdaptyWebPresentation.InAppBrowser,
            )

            val eventListener = newOnboardingEventListener(
                AdaptyUiOnboardingView(onboardingArgs.onboarding, id),
                platformViewOnEvent?.let { id to it },
            )

            onboardingView.show(onboardingConfig, eventListener)
        }
    }

    fun clearOnboardingView(onboardingView: AdaptyOnboardingView) {
        onboardingView.setViewTreeViewModelStoreOwner(null)
    }

    fun newOnboardingEventListener(
        view: AdaptyUiOnboardingView,
        platformViewOnEvent: Pair<String, (viewId: String, eventId: String, eventData: String) -> Unit>? = null,
    ) =
        object : AdaptyOnboardingEventListener {

            private fun sendEvent(event: AdaptyUiEvent) {
                if (platformViewOnEvent != null) {
                    val (viewId, onEvent) = platformViewOnEvent
                    onEvent(viewId, event.id, serializationHelper.toJson(event.data))
                } else {
                    uiEventsObserver?.invoke(event)
                }
            }

            override fun onAnalyticsEvent(
                event: AdaptyOnboardingAnalyticsEvent,
                context: Context
            ) {
                sendEvent(AdaptyUiEvent.fromOnboardingEvent(event, view))
            }

            override fun onCloseAction(
                action: AdaptyOnboardingCloseAction,
                context: Context
            ) {
                sendEvent(AdaptyUiEvent.fromOnboardingEvent(action, view))
            }

            override fun onCustomAction(
                action: AdaptyOnboardingCustomAction,
                context: Context
            ) {
                sendEvent(AdaptyUiEvent.fromOnboardingEvent(action, view))
            }

            override fun onError(error: AdaptyOnboardingError, context: Context) {
                sendEvent(AdaptyUiEvent.fromOnboardingEvent(error, view))
            }

            override fun onFinishLoading(action: AdaptyOnboardingLoadedAction, context: Context) {
                sendEvent(AdaptyUiEvent.fromOnboardingEvent(action, view))
            }

            override fun onOpenPaywallAction(
                action: AdaptyOnboardingOpenPaywallAction,
                context: Context
            ) {
                sendEvent(AdaptyUiEvent.fromOnboardingEvent(action, view))
            }

            override fun onStateUpdatedAction(
                action: AdaptyOnboardingStateUpdatedAction,
                context: Context
            ) {
                sendEvent(AdaptyUiEvent.fromOnboardingEvent(action, view))
            }
        }

}