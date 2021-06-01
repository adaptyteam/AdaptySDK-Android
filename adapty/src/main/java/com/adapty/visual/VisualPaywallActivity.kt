package com.adapty.visual

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.adapty.R
import com.adapty.errors.AdaptyError
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.VisualPaywallManager
import com.adapty.listeners.VisualPaywallListener
import com.adapty.models.GoogleValidationResult
import com.adapty.models.ProductModel
import com.adapty.models.PurchaserInfoModel
import java.lang.ref.WeakReference

internal class VisualPaywallActivity : AppCompatActivity() {

    companion object {
        internal const val PAYWALL_ID_EXTRA = "PAYWALL_ID_EXTRA"
    }

    private lateinit var paywallId: String
    private var paddingTop = 0
    private var paddingBottom = 0

    private val visualPaywallView: VisualPaywallView by lazy {
        findViewById(R.id.visual_paywall_view)
    }

    private val visualPaywallManager: VisualPaywallManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.adapty_activity_visual_paywall)
        visualPaywallManager.currentVisualPaywallActivity = WeakReference(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.setOnApplyWindowInsetsListener(
                visualPaywallView
            ) { _, insets ->
                paddingTop = insets.systemWindowInsetTop
                paddingBottom = insets.systemWindowInsetBottom
                ViewCompat.setOnApplyWindowInsetsListener(visualPaywallView, null)
                setupVisualPaywallView(visualPaywallView, paddingTop, paddingBottom)
                insets
            }
        } else {
            setupVisualPaywallView(visualPaywallView, 0, 0)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        setupVisualPaywallView(visualPaywallView, paddingTop, paddingBottom)
    }

    private fun setupVisualPaywallView(
        visualPaywallView: VisualPaywallView,
        paddingTop: Int,
        paddingBottom: Int
    ) {
        paywallId = intent?.getStringExtra(PAYWALL_ID_EXTRA).orEmpty()

        visualPaywallView.loadPaywall(paywallId, paddingTop, paddingBottom)

        visualPaywallView.actionListener = object : VisualPaywallListener {
            override fun onPurchased(
                purchaserInfo: PurchaserInfoModel?,
                purchaseToken: String?,
                googleValidationResult: GoogleValidationResult?,
                product: ProductModel
            ) {
                visualPaywallManager.listener?.onPurchased(
                    purchaserInfo,
                    purchaseToken,
                    googleValidationResult,
                    product
                )
            }

            override fun onPurchaseFailure(product: ProductModel, error: AdaptyError) {
                visualPaywallManager.listener?.onPurchaseFailure(product, error)
            }

            override fun onRestorePurchases(
                purchaserInfo: PurchaserInfoModel?,
                googleValidationResultList: List<GoogleValidationResult>?,
                error: AdaptyError?
            ) {
                visualPaywallManager.listener?.onRestorePurchases(
                    purchaserInfo,
                    googleValidationResultList,
                    error
                )
            }

            override fun onClosed() {
                onBackPressed()
            }
        }
    }

    override fun onBackPressed() {
        visualPaywallManager.currentVisualPaywallActivity = null
        visualPaywallManager.listener?.onClosed()
        super.onBackPressed()
        overridePendingTransition(R.anim.adapty_paywall_no_anim, R.anim.adapty_paywall_slide_down)
    }
}