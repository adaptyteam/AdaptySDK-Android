package com.adapty.visual

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewClientCompat
import com.adapty.Adapty
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.VisualPaywallManager
import com.adapty.models.PaywallModel
import com.adapty.models.ProductModel

@Deprecated("The functionality is deprecated and will be removed in future releases.")
@SuppressLint("SetJavaScriptEnabled")
public class VisualPaywallView : WebView {

    public constructor(context: Context) : super(context)

    public constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        webViewClient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            object : WebViewClientCompat() {

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    if (!request.hasGesture()) return false
                    processVisualElementClick(request.url)
                    return true
                }
            }
        } else {
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let(::processVisualElementClick)
                    return true
                }
            }
        }
        settings.javaScriptEnabled = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        setLayerType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                View.LAYER_TYPE_HARDWARE
            } else {
                View.LAYER_TYPE_SOFTWARE
            }, null
        )
    }

    private var paywallId: String? = null

    private val visualPaywallManager: VisualPaywallManager by inject()

    @JvmOverloads
    public fun showPaywall(paywall: PaywallModel, paddingTop: Int = 0, paddingBottom: Int = 0) {
        paywallId = paywall.variationId

        visualPaywallManager.logEvent(paywallId, "paywall_showed")

        var visualPaywall = paywall.visualPaywall
            ?.replace("%adapty_paywall_padding_top%", "$paddingTop")
            ?.replace("%adapty_paywall_padding_bottom%", "$paddingBottom")
        paywall.products.forEach {
            visualPaywall = visualPaywall
                ?.replace("%adapty_title_${it.vendorProductId}%", it.localizedTitle)
                ?.replace("%adapty_price_${it.vendorProductId}%", it.localizedPrice.orEmpty())
                ?.replace(
                    "%adapty_duration_${it.vendorProductId}%",
                    it.localizedSubscriptionPeriod.orEmpty()
                )
                ?.replace(
                    "%adapty_introductory_price_${it.vendorProductId}%",
                    "${it.introductoryDiscount?.localizedPrice}"
                )
                ?.replace(
                    "%adapty_introductory_duration_${it.vendorProductId}%",
                    it.introductoryDiscount?.localizedSubscriptionPeriod.orEmpty()
                )
                ?.replace(
                    "%adapty_trial_duration_${it.vendorProductId}%",
                    it.localizedFreeTrialPeriod.orEmpty()
                )
        }
        post {
            loadDataWithBaseURL(null, visualPaywall, "text/html", "utf-8", null)
        }
    }

    private fun processVisualElementClick(uri: Uri) {
        when {
            uri.scheme?.startsWith("http") == true -> {
                (context as? Activity)?.startActivity(
                    Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "")
                )
            }
            uri.scheme == "adapty" -> {
                when {
                    uri.lastPathSegment == "close_paywall" -> onCancel()
                    uri.lastPathSegment == "restore_purchases" -> restorePurchases()
                    uri.pathSegments.size >= 2 -> {
                        when (uri.pathSegments[uri.pathSegments.size - 2]) {
                            "subscribe" -> {
                                paywallId?.let(visualPaywallManager::fetchPaywall)
                                    ?.products?.firstOrNull { it.vendorProductId == uri.lastPathSegment }
                                    ?.let { product ->
                                        (context as? Activity)?.let { activity ->
                                            makePurchase(activity, product)
                                        }
                                    }
                            }
                            "in_app" -> {
                                visualPaywallManager.logEvent(
                                    paywallId,
                                    "in_app_clicked",
                                    uri.lastPathSegment
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processVisualElementClick(url: String) {
        processVisualElementClick(Uri.parse(url))
    }

    private fun makePurchase(activity: Activity, product: ProductModel) {
        visualPaywallManager.logEvent(paywallId, "purchase_started", product.vendorProductId)
        Adapty.makePurchase(
            activity,
            product
        ) { purchaserInfo, purchaseToken, googleValidationResult, product, error ->
            error?.let { error ->
                if (error.adaptyErrorCode == AdaptyErrorCode.USER_CANCELED) {
                    visualPaywallManager.logEvent(
                        paywallId,
                        "purchase_cancelled",
                        product.vendorProductId
                    )
                }
                visualPaywallManager.listener?.onPurchaseFailure(
                    product,
                    error,
                    context as? VisualPaywallActivity
                )
            }
                ?: visualPaywallManager.listener?.onPurchased(
                    purchaserInfo,
                    purchaseToken,
                    googleValidationResult,
                    product,
                    context as? VisualPaywallActivity
                )
        }
    }

    private fun restorePurchases() {
        visualPaywallManager.logEvent(paywallId, "purchase_restore")
        Adapty.restorePurchases { purchaserInfo, googleValidationResultList, error ->
            visualPaywallManager.listener?.onRestorePurchases(
                purchaserInfo,
                googleValidationResultList,
                error,
                context as? VisualPaywallActivity
            )
        }
    }

    @JvmSynthetic
    internal fun onCancel() {
        visualPaywallManager.onCancel(paywallId, context as? VisualPaywallActivity)
    }
}