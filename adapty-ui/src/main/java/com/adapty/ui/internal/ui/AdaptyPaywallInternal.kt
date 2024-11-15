@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adapty.errors.AdaptyError
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchasedInfo
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.ui.element.SectionElement
import com.adapty.ui.internal.ui.element.fillModifierWithScopedParams
import com.adapty.ui.internal.ui.element.render
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.InsetWrapper
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LocalCustomInsets
import com.adapty.ui.internal.utils.OPENED_ADDITIONAL_SCREEN_KEY
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.internal.utils.getInsets
import com.adapty.ui.internal.utils.getProductGroupKey
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.wrap
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdaptyPaywallInternal(viewModel: PaywallViewModel) {
    val userArgs = viewModel.dataState.value ?: return
    val viewConfig = userArgs.viewConfig
    CompositionLocalProvider(
        LocalLayoutDirection provides if (viewConfig.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalCustomInsets provides userArgs.userInsets.wrap(),
    ) {
        val insets = getInsets()
        BoxWithConstraints {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val screenHeightPxFromConfig: Int
            val maxHeightPxFromConstraints: Int
            with(density) {
                screenHeightPxFromConfig = configuration.screenHeightDp.dp.roundToPx()
                maxHeightPxFromConstraints = maxHeight.roundToPx()
            }
            var loggedNonSkipping by remember { mutableStateOf(false) }
            if (!insets.isCustom) {
                val insetTop = insets.getTop(density)
                val insetBottom = insets.getBottom(density)
                if ((insetTop == 0 && insetBottom == 0 && maxHeightPxFromConstraints - screenHeightPxFromConfig > 10)) {
                    log(VERBOSE) { "$LOG_PREFIX skipping ($screenHeightPxFromConfig; $maxHeightPxFromConstraints)" }
                    return@BoxWithConstraints
                } else {
                    if (!loggedNonSkipping) {
                        log(VERBOSE) { "$LOG_PREFIX non-skipping ($insetTop; $insetBottom; $screenHeightPxFromConfig; $maxHeightPxFromConstraints)" }
                        loggedNonSkipping = true
                    }
                }
            } else {
                if (!loggedNonSkipping) {
                    log(VERBOSE) { "$LOG_PREFIX non-skipping (custom insets: ${(insets as? InsetWrapper.Custom)?.insets}" }
                    loggedNonSkipping = true
                }
            }
            val context = LocalContext.current
            val resolveAssets = { viewModel.assets }
            val resolveText = @Composable { stringId: StringId -> viewModel.resolveText(stringId) }
            val resolveState = { viewModel.state }
            val sheetState = rememberBottomSheetState()
            val scope = rememberCoroutineScope()
            val eventCallback = createEventCallback(
                context,
                userArgs,
                viewModel,
                scope,
                sheetState,
            )
            renderDefaultScreen(
                viewConfig.screens,
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
            )

            val currentBottomSheet = (viewModel.state[OPENED_ADDITIONAL_SCREEN_KEY] as? String)?.let { screenId ->
                viewConfig.screens.bottomSheets[screenId]
            }
            if (currentBottomSheet != null) {
                BottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = {
                        viewModel.state.remove(OPENED_ADDITIONAL_SCREEN_KEY)
                    },
                ) {
                    currentBottomSheet.content.render(
                        resolveAssets,
                        resolveText,
                        resolveState,
                        eventCallback,
                        fillModifierWithScopedParams(
                            currentBottomSheet.content,
                            Modifier.fillWithBaseParams(currentBottomSheet.content, resolveAssets),
                        )
                    )
                }
            }

            if (viewModel.isLoading.value)
                Loading()

            LaunchedEffectSaveable(Unit) {
                viewModel.logShowPaywall(viewConfig)
            }
        }
    }
}

@Composable
internal fun LaunchedEffectSaveable(
    key: Any?,
    effect: suspend CoroutineScope.() -> Unit
) {
    val hasExecuted = rememberSaveable(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!hasExecuted.value) {
            hasExecuted.value = true
            effect()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun createEventCallback(
    localContext: Context,
    userArgs: UserArgs,
    viewModel: PaywallViewModel,
    scope: CoroutineScope,
    sheetState: SheetState,
): EventCallback {
    val viewConfig = userArgs.viewConfig
    val eventListener = userArgs.eventListener
    val timerResolver = userArgs.timerResolver
    val observerModeHandler = userArgs.observerModeHandler
    val personalizedOfferResolver = userArgs.personalizedOfferResolver
    return object: EventCallback {
        override fun onActions(actions: List<Action>) {
            actions.forEach { action ->
                when (action) {
                    is Action.SwitchSection -> {
                        val sectionKey = SectionElement.getKey(action.sectionId)
                        viewModel.state[sectionKey] = action.index
                    }
                    is Action.SelectProduct -> {
                        val productGroupKey = getProductGroupKey(action.groupId)
                        viewModel.state[productGroupKey] = action.productId
                        val product = viewModel.products[action.productId] ?: return
                        eventListener.onProductSelected(product, localContext)
                    }
                    is Action.UnselectProduct -> {
                        val productGroupKey = getProductGroupKey(action.groupId)
                        viewModel.state.remove(productGroupKey)
                    }
                    is Action.PurchaseProduct -> {
                        val activity = localContext.getActivityOrNull() ?: return
                        val product = viewModel.products[action.productId] ?: return

                        viewModel.onPurchaseInitiated(
                            activity,
                            viewConfig.paywall,
                            product,
                            this,
                            observerModeHandler,
                            personalizedOfferResolver,
                        )
                    }
                    is Action.PurchaseSelectedProduct -> {
                        val activity = localContext.getActivityOrNull() ?: return
                        val productGroupKey = getProductGroupKey(action.groupId)
                        val product = viewModel.state[productGroupKey]?.let { id ->
                            viewModel.products[id]
                        } ?: return

                        viewModel.onPurchaseInitiated(
                            activity,
                            viewConfig.paywall,
                            product,
                            this,
                            observerModeHandler,
                            personalizedOfferResolver,
                        )
                    }
                    is Action.ClosePaywall -> eventListener.onActionPerformed(AdaptyUI.Action.Close, localContext)
                    is Action.Custom -> eventListener.onActionPerformed(AdaptyUI.Action.Custom(action.customId), localContext)
                    is Action.OpenUrl -> eventListener.onActionPerformed(AdaptyUI.Action.OpenUrl(action.url), localContext)
                    is Action.RestorePurchases -> viewModel.onRestorePurchases(this)
                    is Action.OpenScreen -> viewModel.state[OPENED_ADDITIONAL_SCREEN_KEY] = action.screenId
                    is Action.CloseCurrentScreen -> {
                        scope.launch {
                            if (sheetState.isVisible)
                                sheetState.hide()
                            viewModel.state.remove(OPENED_ADDITIONAL_SCREEN_KEY)
                        }
                    }
                    else -> Unit
                }
            }
        }

        override fun getTimerStartTimestamp(timerId: String, isPersisted: Boolean): Long? {
            return viewModel.getTimerStartTimestamp(viewConfig.paywall.placementId, timerId, isPersisted)
        }

        override fun setTimerStartTimestamp(timerId: String, value: Long, isPersisted: Boolean) {
            viewModel.setTimerStartTimestamp(viewConfig.paywall.placementId, timerId, value, isPersisted)
        }

        override fun timerEndAtDate(timerId: String): Date {
            return timerResolver.timerEndAtDate(timerId)
        }

        override fun onAwaitingSubscriptionUpdateParams(product: AdaptyPaywallProduct): AdaptySubscriptionUpdateParameters? {
            return eventListener.onAwaitingSubscriptionUpdateParams(product, localContext)
        }

        override fun onPurchaseCanceled(product: AdaptyPaywallProduct) {
            eventListener.onPurchaseCanceled(product, localContext)
        }

        override fun onPurchaseFailure(error: AdaptyError, product: AdaptyPaywallProduct) {
            eventListener.onPurchaseFailure(error, product, localContext)
        }

        override fun onPurchaseStarted(product: AdaptyPaywallProduct) {
            eventListener.onPurchaseStarted(product, localContext)
        }

        override fun onPurchaseSuccess(
            purchasedInfo: AdaptyPurchasedInfo?,
            product: AdaptyPaywallProduct
        ) {
            eventListener.onPurchaseSuccess(purchasedInfo, product, localContext)
        }

        override fun onRestoreFailure(error: AdaptyError) {
            eventListener.onRestoreFailure(error, localContext)
        }

        override fun onRestoreStarted() {
            eventListener.onRestoreStarted(localContext)
        }

        override fun onRestoreSuccess(profile: AdaptyProfile) {
            eventListener.onRestoreSuccess(profile, localContext)
        }
    }
}