package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.KinesisManager
import com.adapty.listeners.VisualPaywallListener
import com.adapty.models.PaywallModel
import com.adapty.visual.VisualPaywallActivity
import java.lang.ref.WeakReference

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class VisualPaywallManager(
    private val cacheRepository: CacheRepository,
    private val kinesisManager: KinesisManager
) {

    private var cachedPaywalls = hashMapOf<String, PaywallModel>()

    @JvmSynthetic
    @JvmField
    var listener: VisualPaywallListener? = null

    @JvmSynthetic
    @JvmField
    var currentVisualPaywallActivity: WeakReference<VisualPaywallActivity>? = null

    @JvmSynthetic
    fun fetchPaywall(paywallId: String) = cachedPaywalls[paywallId] ?: cacheRepository.getPaywalls()
        ?.firstOrNull { it.variationId == paywallId }
        ?.also { cachedPaywalls[paywallId] = it }

    @JvmSynthetic
    fun logEvent(paywallId: String?, eventName: String, vendorProductId: String? = null) {
        paywallId?.let(::fetchPaywall)?.let { paywall ->
            val subMap = hashMapOf(
                "is_promo" to "${paywall.isPromo}",
                "variation_id" to paywall.variationId
            )
            vendorProductId?.let { vendorProductId ->
                subMap["vendor_product_id"] = vendorProductId
            }
            kinesisManager.trackEvent(eventName, subMap)
        }
    }

    @JvmSynthetic
    fun closePaywall() {
        currentVisualPaywallActivity?.get()?.close()
    }

    @JvmSynthetic
    fun onCancel(paywallId: String?, modalActivity: VisualPaywallActivity?) {
        listener?.onCancel(modalActivity)
        logEvent(paywallId, "paywall_closed")
    }

}