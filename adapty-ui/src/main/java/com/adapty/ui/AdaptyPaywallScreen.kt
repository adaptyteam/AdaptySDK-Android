package com.adapty.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.internal.ui.AdaptyPaywallInternal
import com.adapty.ui.internal.ui.PaywallViewModel
import com.adapty.ui.internal.ui.PaywallViewModelArgs
import com.adapty.ui.internal.ui.PaywallViewModelFactory
import com.adapty.ui.internal.ui.UserArgs
import com.adapty.ui.internal.utils.ProductLoadingFailureCallback
import com.adapty.ui.internal.utils.getCurrentLocale
import com.adapty.ui.listeners.AdaptyUiDefaultEventListener
import com.adapty.ui.listeners.AdaptyUiEventListener
import com.adapty.ui.listeners.AdaptyUiObserverModeHandler
import com.adapty.ui.listeners.AdaptyUiPersonalizedOfferResolver
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.ui.listeners.AdaptyUiTimerResolver
import java.util.UUID

/**
 * Paywall screen composable representation
 *
 * @param[viewConfiguration] An [AdaptyUI.LocalizedViewConfiguration] object containing information
 * about the visual part of the paywall. To load it, use the [AdaptyUI.getViewConfiguration] method.
 *
 * @param[products] Optional [AdaptyPaywallProduct] list. Pass this value in order to optimize
 * the display time of the products on the screen. If you pass `null`, `AdaptyUI` will
 * automatically fetch the required products.
 *
 * @param[eventListener] An object that implements the [AdaptyUiEventListener] interface.
 * Use it to respond to different events happening inside the purchase screen.
 * Also you can extend [AdaptyUiDefaultEventListener] so you don't need to override all the methods.
 *
 * @param[insets] You can override the default window inset handling by specifying the [AdaptyPaywallInsets].
 *
 * @param[personalizedOfferResolver] In case you want to indicate whether the price is personalized ([read more](https://developer.android.com/google/play/billing/integrate#personalized-price)),
 * you can implement [AdaptyUiPersonalizedOfferResolver] and pass your own logic
 * that maps [AdaptyPaywallProduct] to `true`, if the price of the product is personalized, otherwise `false`.
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
@Composable
public fun AdaptyPaywallScreen(
    viewConfiguration: AdaptyUI.LocalizedViewConfiguration,
    products: List<AdaptyPaywallProduct>?,
    eventListener: AdaptyUiEventListener,
    insets: AdaptyPaywallInsets = AdaptyPaywallInsets.UNSPECIFIED,
    personalizedOfferResolver: AdaptyUiPersonalizedOfferResolver = AdaptyUiPersonalizedOfferResolver.DEFAULT,
    customAssets: AdaptyCustomAssets = AdaptyCustomAssets.Empty,
    tagResolver: AdaptyUiTagResolver = AdaptyUiTagResolver.DEFAULT,
    timerResolver: AdaptyUiTimerResolver = AdaptyUiTimerResolver.DEFAULT,
    observerModeHandler: AdaptyUiObserverModeHandler? = null,
) {
    val context = LocalContext.current
    val vmArgs = remember {
        val userArgs = UserArgs.create(
            viewConfiguration,
            eventListener,
            insets,
            personalizedOfferResolver,
            customAssets,
            tagResolver,
            timerResolver,
            observerModeHandler,
            products,
            ProductLoadingFailureCallback { error -> eventListener.onLoadingProductsFailure(error, context) },
        )
        PaywallViewModelArgs.create(
            "${UUID.randomUUID().toString().hashCode()}",
            userArgs,
            context.getCurrentLocale(),
        )
    } ?: return

    val viewModel: PaywallViewModel = viewModel(
        key = viewConfiguration.id,
        factory = PaywallViewModelFactory(vmArgs)
    )
    AdaptyPaywallInternal(viewModel)
}