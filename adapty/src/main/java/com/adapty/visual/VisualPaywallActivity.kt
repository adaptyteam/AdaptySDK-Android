package com.adapty.visual

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.adapty.R
import com.adapty.internal.di.Dependencies.inject
import com.adapty.internal.utils.VisualPaywallManager
import java.lang.ref.WeakReference

public class VisualPaywallActivity : AppCompatActivity() {

    internal companion object {
        internal const val PAYWALL_ID_EXTRA = "PAYWALL_ID_EXTRA"
    }

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
                insets.getInsets(Type.systemBars()).let {
                    paddingTop = it.top
                    paddingBottom = it.bottom
                }

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
        val paywall =
            intent?.getStringExtra(PAYWALL_ID_EXTRA)?.takeIf(CharSequence::isNotEmpty)
                ?.let(visualPaywallManager::fetchPaywall)
                ?: kotlin.run {
                    close()
                    return
                }

        visualPaywallView.showPaywall(paywall, paddingTop, paddingBottom)
    }

    public fun close() {
        visualPaywallManager.currentVisualPaywallActivity = null
        super.onBackPressed()
        overridePendingTransition(R.anim.adapty_paywall_no_anim, R.anim.adapty_paywall_slide_down)
    }

    @Deprecated(
        message = "To close the paywall, please call either '.close()', or 'Adapty.closeVisualPaywall()' instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith(
            expression = "close()",
            imports = arrayOf("com.adapty.Adapty")
        )
    )
    override fun onBackPressed() {
        visualPaywallView.onCancel()
    }
}