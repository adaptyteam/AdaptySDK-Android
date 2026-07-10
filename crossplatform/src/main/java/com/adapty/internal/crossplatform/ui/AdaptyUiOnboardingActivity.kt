package com.adapty.internal.crossplatform.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import com.adapty.internal.crossplatform.R
import com.adapty.internal.crossplatform.ui.Dependencies.safeInject
import com.adapty.ui.onboardings.AdaptyOnboardingView
import kotlin.LazyThreadSafetyMode.NONE

class AdaptyUiOnboardingActivity : FragmentActivity() {

    internal companion object {
        internal const val VIEW_ID = "VIEW_ID"
    }

    private val onboardingView: AdaptyOnboardingView by lazy(NONE) {
        AdaptyOnboardingView(this)
    }

    private val onboardingUiManager: OnboardingUiManager? by safeInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val viewId = intent?.getStringExtra(VIEW_ID) ?: kotlin.run {
                    performBackPress()
                    return
                }

                if (onboardingUiManager?.handleSystemBack(viewId) != true) {
                    isEnabled = false
                    performBackPress()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val viewId = intent?.getStringExtra(VIEW_ID) ?: kotlin.run {
            performBackPress()
            return
        }

        with(onboardingView) {
            setContentView(this)
            val currentData = onboardingUiManager?.getData(viewId)
                ?: kotlin.run {
                    onboardingUiManager?.removeData(viewId)
                    performBackPress()
                    return@with
                }
            onboardingUiManager?.setCurrentView(this)
            val eventListener = onboardingUiManager?.newOnboardingEventListener(currentData.view) ?: kotlin.run {
                performBackPress()
                return
            }
            show(currentData.config, eventListener)
        }
    }

    fun close() {
        performBackPress()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.adapty_ui_no_anim,
                R.anim.adapty_ui_slide_down,
            )
        } else {
            overridePendingTransition(R.anim.adapty_ui_no_anim, R.anim.adapty_ui_slide_down)
        }
    }

    override fun onBackPressed() { }

    private fun performBackPress() {
        onboardingUiManager?.clearCurrentView()
        super.onBackPressed()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            onboardingUiManager?.isShown = false
        }
        super.onDestroy()
    }
}