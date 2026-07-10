@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.internal.crossplatform.ui

import android.app.Activity.OVERRIDE_TRANSITION_OPEN
import android.content.Intent
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.adapty.Adapty
import com.adapty.errors.AdaptyError
import com.adapty.internal.crossplatform.ActivityProvider
import com.adapty.internal.crossplatform.R
import com.adapty.internal.utils.DEFAULT_PLACEMENT_TIMEOUT
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.AdaptyUI
import com.adapty.ui.onboardings.AdaptyOnboardingConfiguration
import com.adapty.utils.AdaptyResult

class CrossplatformUiHelper internal constructor(
    private val flowUiManager: FlowUiManager,
    private val onboardingUiManager: OnboardingUiManager,
    private val isPresentationEmbedded: Boolean,
) {

    fun interface Callback<T> {
        operator fun invoke(result: T)
    }

    var activity = ActivityProvider.Empty

    var isObserverMode: Boolean
        get() = flowUiManager.isObserverMode
        set(value) {
            flowUiManager.isObserverMode = value
        }

    var uiEventsObserver: ((event: AdaptyUiEvent) -> Unit)? = null
        set(value) {
            field = value
            flowUiManager.uiEventsObserver = field
            onboardingUiManager.uiEventsObserver = field
        }

    fun handlePresentView(
        id: String,
        onSuccess: Callback<Unit>,
        onError: Callback<AdaptyUiBridgeError>,
    ) {
        if (!flowUiManager.hasData(id)) {
            onError(AdaptyUiBridgeError.ViewNotFound(id))
            return
        }

        if (!flowUiManager.isShown) {
            activity()?.let { activity ->
                flowUiManager.isShown = true

                activity.runOnUiThread {
                    if (isPresentationEmbedded) {
                        (activity as? FragmentActivity)?.let { fragmentActivity ->
                            AdaptyUiFlowDialogFragment.newInstance(id).show(
                                fragmentActivity.supportFragmentManager,
                                id
                            )
                            onSuccess(Unit)
                        } ?: kotlin.run {
                            onError(AdaptyUiBridgeError.ViewPresentationError(id))
                        }
                    } else {
                        activity.startActivity(
                            Intent(activity, AdaptyUiActivity::class.java)
                                .putExtra(AdaptyUiActivity.VIEW_ID, id)
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            activity.overrideActivityTransition(
                                OVERRIDE_TRANSITION_OPEN,
                                R.anim.adapty_ui_slide_up,
                                R.anim.adapty_ui_no_anim,
                            )
                        } else {
                            activity.overridePendingTransition(
                                R.anim.adapty_ui_slide_up,
                                R.anim.adapty_ui_no_anim,
                            )
                        }
                        onSuccess(Unit)
                    }
                }
            } ?: kotlin.run {
                onError(AdaptyUiBridgeError.ViewPresentationError(id))
            }
        } else {
            onError(AdaptyUiBridgeError.ViewAlreadyPresented(id))
        }
    }

    fun handleDismissView(
        id: String,
        onSuccess: Callback<Unit>,
        onError: Callback<AdaptyUiBridgeError>,
    ) {
        flowUiManager.addPendingDismissCallback(id, onSuccess)
        clearFlowUiDataCache(id)
        if (isPresentationEmbedded) {
            (activity() as? FragmentActivity)?.let { activity ->
                activity.runOnUiThread {
                    (activity.supportFragmentManager.findFragmentByTag(id) as? AdaptyUiFlowDialogFragment)?.close()
                    flowUiManager.isShown = false
                }
            } ?: kotlin.run {
                flowUiManager.removePendingDismissCallback(id, onSuccess)
                onError(AdaptyUiBridgeError.ViewNotFound(id))
                return
            }
        } else {
            (flowUiManager.getCurrentView()?.context as? AdaptyUiActivity)?.let { activity ->
                activity.runOnUiThread {
                    activity.close()
                    flowUiManager.isShown = false
                }
            } ?: kotlin.run {
                flowUiManager.removePendingDismissCallback(id, onSuccess)
                onError(AdaptyUiBridgeError.ViewNotFound(id))
                return
            }
        }
    }

    fun answerPermission(eventId: String, granted: Boolean, detail: String?) {
        flowUiManager.resolvePermission(eventId, granted, detail)
    }

    fun observerPurchaseDidStart(eventId: String) {
        flowUiManager.resolveObserverPurchaseStart(eventId)
    }

    fun observerPurchaseDidFinish(eventId: String) {
        flowUiManager.resolveObserverPurchaseFinish(eventId)
    }

    fun observerRestoreDidStart(eventId: String) {
        flowUiManager.resolveObserverRestoreStart(eventId)
    }

    fun observerRestoreDidFinish(eventId: String) {
        flowUiManager.resolveObserverRestoreFinish(eventId)
    }

    fun handleShowDialog(
        id: String,
        config: AdaptyUiDialogConfig,
        onSuccess: AdaptyUiDialog.Listener,
        onError: Callback<AdaptyUiBridgeError>,
    ) {
        (flowUiManager.getCurrentView()?.context as? FragmentActivity)?.let { activity ->
            activity.runOnUiThread {
                flowUiManager.onDialogActionListener = onSuccess
                AdaptyUiDialog.newInstance(config).show(activity.supportFragmentManager, AdaptyUiDialog.TAG)
            }
        } ?: kotlin.run {
            onError(AdaptyUiBridgeError.ViewNotFound(id))
            return
        }
    }

    fun handleCreateFlowView(
        args: CreateFlowViewArgs,
        onSuccess: Callback<AdaptyUiFlowView>,
        onError: Callback<AdaptyError>,
    ) {
        AdaptyUI.getFlowConfiguration(
            args.flow,
            loadTimeout = args.loadTimeout ?: DEFAULT_PLACEMENT_TIMEOUT,
        ) { viewConfigResult ->
            when (viewConfigResult) {
                is AdaptyResult.Success -> {
                    val viewConfig = viewConfigResult.value

                    if (args.preloadProducts) {
                        Adapty.getPaywallProducts(args.flow) { productsResult ->
                            when (productsResult) {
                                is AdaptyResult.Success -> {
                                    handleCreateViewResult(
                                        viewConfig,
                                        args,
                                        productsResult.value,
                                        onSuccess,
                                    )
                                }

                                is AdaptyResult.Error -> {
                                    handleCreateViewResult(
                                        viewConfig,
                                        args,
                                        null,
                                        onSuccess,
                                    )
                                }
                            }
                        }
                    } else {
                        handleCreateViewResult(
                            viewConfig,
                            args,
                            null,
                            onSuccess,
                        )
                    }
                }

                is AdaptyResult.Error -> {
                    onError(viewConfigResult.error)
                }
            }
        }
    }

    private fun handleCreateViewResult(
        viewConfig: AdaptyUI.FlowConfiguration,
        args: CreateFlowViewArgs,
        products: List<AdaptyPaywallProduct>? = null,
        onSuccess: Callback<AdaptyUiFlowView>,
    ) {
        val view = AdaptyUiFlowView(args.flow)

        cacheFlowUiData(
            FlowUiData(viewConfig, products, args, view)
        )

        onSuccess(view)
    }

    private fun cacheFlowUiData(flowUiData: FlowUiData) {
        flowUiManager.putData(flowUiData.view.id, flowUiData)
    }

    private fun clearFlowUiDataCache(viewId: String) {
        flowUiManager.removeData(viewId)
    }

    fun handleCreateOnboardingView(
        args: CreateOnboardingViewArgs,
        onSuccess: Callback<AdaptyUiOnboardingView>,
        onError: Callback<AdaptyError>,
    ) {
        val onboardingConfig = AdaptyUI.getOnboardingConfiguration(
            args.onboarding,
            args.externalUrlsPresentation ?: AdaptyWebPresentation.InAppBrowser,
        )

        handleCreateOnboardingViewResult(
            onboardingConfig,
            args,
            onSuccess,
        )
    }

    private fun handleCreateOnboardingViewResult(
        onboardingConfig: AdaptyOnboardingConfiguration,
        args: CreateOnboardingViewArgs,
        onSuccess: Callback<AdaptyUiOnboardingView>,
    ) {
        val onboarding = args.onboarding
        val view = AdaptyUiOnboardingView(onboarding)

        cacheOnboardingUiData(
            OnboardingUiData(onboardingConfig, view)
        )

        onSuccess(view)
    }

    private fun cacheOnboardingUiData(onboardingUiData: OnboardingUiData) {
        onboardingUiManager.putData(onboardingUiData.view.id, onboardingUiData)
    }

    fun handlePresentOnboardingView(
        id: String,
        onSuccess: Callback<Unit>,
        onError: Callback<AdaptyUiBridgeError>,
    ) {
        if (!onboardingUiManager.hasData(id)) {
            onError(AdaptyUiBridgeError.ViewNotFound(id))
            return
        }

        if (!onboardingUiManager.isShown) {
            activity()?.let { activity ->
                onboardingUiManager.isShown = true

                activity.runOnUiThread {
                    if (isPresentationEmbedded) {
                        (activity as? FragmentActivity)?.let { fragmentActivity ->
                            AdaptyUiOnboardingDialogFragment.newInstance(id).show(
                                fragmentActivity.supportFragmentManager,
                                id
                            )
                            onSuccess(Unit)
                        } ?: kotlin.run {
                            onError(AdaptyUiBridgeError.ViewPresentationError(id))
                        }
                    } else {
                        activity.startActivity(
                            Intent(activity, AdaptyUiOnboardingActivity::class.java)
                                .putExtra(AdaptyUiOnboardingActivity.VIEW_ID, id)
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            activity.overrideActivityTransition(
                                OVERRIDE_TRANSITION_OPEN,
                                R.anim.adapty_ui_slide_up,
                                R.anim.adapty_ui_no_anim,
                            )
                        } else {
                            activity.overridePendingTransition(
                                R.anim.adapty_ui_slide_up,
                                R.anim.adapty_ui_no_anim,
                            )
                        }
                        onSuccess(Unit)
                    }
                }
            } ?: kotlin.run {
                onError(AdaptyUiBridgeError.ViewPresentationError(id))
            }
        } else {
            onError(AdaptyUiBridgeError.ViewAlreadyPresented(id))
        }
    }

    fun handleDismissOnboardingView(
        id: String,
        onSuccess: Callback<Unit>,
        onError: Callback<AdaptyUiBridgeError>,
    ) {
        clearOnboardingUiDataCache(id)
        if (isPresentationEmbedded) {
            (activity() as? FragmentActivity)?.let { activity ->
                activity.runOnUiThread {
                    (activity.supportFragmentManager.findFragmentByTag(id) as? AdaptyUiOnboardingDialogFragment)?.close()
                    onboardingUiManager.isShown = false
                }
            } ?: kotlin.run {
                onError(AdaptyUiBridgeError.ViewNotFound(id))
                return
            }
        } else {
            (onboardingUiManager.getCurrentView()?.context as? AdaptyUiOnboardingActivity)?.let { activity ->
                activity.runOnUiThread {
                    activity.close()
                    onboardingUiManager.isShown = false
                }
            } ?: kotlin.run {
                onError(AdaptyUiBridgeError.ViewNotFound(id))
                return
            }
        }
        onSuccess(Unit)
    }

    private fun clearOnboardingUiDataCache(viewId: String) {
        onboardingUiManager.removeData(viewId)
    }
}