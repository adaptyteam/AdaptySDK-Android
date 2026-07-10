@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.adapty.internal.crossplatform.ui

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.adapty.internal.crossplatform.R
import com.adapty.internal.crossplatform.ui.Dependencies.safeInject
import com.adapty.ui.AdaptyFlowView
import kotlin.LazyThreadSafetyMode.NONE

class AdaptyUiActivity : FragmentActivity() {

    internal companion object {
        internal const val VIEW_ID = "VIEW_ID"
    }

    private val flowView: AdaptyFlowView by lazy(NONE) {
        AdaptyFlowView(this)
    }

    private val flowUiManager: FlowUiManager? by safeInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewId = intent?.getStringExtra(VIEW_ID) ?: kotlin.run {
            closeView()
            return
        }

        with(flowView) {
            setContentView(this)
            val currentData = flowUiManager?.getData(viewId)
                ?: kotlin.run {
                    flowUiManager?.removeData(viewId)
                    closeView()
                    return@with
                }
            flowUiManager?.setCurrentView(this)
            val eventListener = flowUiManager?.newFlowEventListener(currentData) ?: kotlin.run {
                    closeView()
                    return
                }

            val (viewConfig, products, customTags, customTimers, customAssets) = currentData
            show(
                viewConfig = viewConfig,
                products = products,
                eventListener = eventListener,
                customAssets = customAssets,
                customTags = customTags,
                customTimers = customTimers,
                enableSafeAreaPaddings = currentData.enableSafeAreaPaddings,
                observerModeHandler = flowUiManager?.observerModeHandlerOrNull(currentData, eventListener),
            )
        }
    }

    fun close() {
        closeView()
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

    private fun closeView() {
        flowUiManager?.clearCurrentView()
        finish()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            flowUiManager?.isShown = false
        }
        super.onDestroy()
    }
}