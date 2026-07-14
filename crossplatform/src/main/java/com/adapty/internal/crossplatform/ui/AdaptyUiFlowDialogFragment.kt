
package com.adapty.internal.crossplatform.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.adapty.internal.crossplatform.R
import com.adapty.internal.crossplatform.ui.Dependencies.safeInject
import com.adapty.ui.AdaptyFlowView
import kotlin.LazyThreadSafetyMode.NONE

class AdaptyUiFlowDialogFragment : DialogFragment() {

    internal companion object {
        internal const val VIEW_ID = "VIEW_ID"

        fun newInstance(viewId: String): AdaptyUiFlowDialogFragment {
            return AdaptyUiFlowDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(VIEW_ID, viewId)
                }
            }
        }
    }

    private val flowView: AdaptyFlowView by lazy(NONE) {
        AdaptyFlowView(requireContext())
    }

    private val flowUiManager: FlowUiManager? by safeInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AdaptyVisualPaywallTheme)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.AdaptyUiScreenTransition)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return androidx.activity.ComponentDialog(requireContext(), theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return flowView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewId = arguments?.getString(VIEW_ID) ?: kotlin.run {
            dismissAllowingStateLoss()
            return
        }

        with(flowView) {
            val currentData = flowUiManager?.getData(viewId)
                ?: kotlin.run {
                    flowUiManager?.removeData(viewId)
                    dismissAllowingStateLoss()
                    return@with
                }
            flowUiManager?.setCurrentView(this)
            val eventListener = flowUiManager?.newFlowEventListener(currentData) ?: kotlin.run {
                dismissAllowingStateLoss()
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

    private fun closeView() {
        flowUiManager?.clearCurrentView()
        dismiss()
    }

    fun close() {
        closeView()
    }

    override fun onDestroy() {
        val isChangingConfigurations = activity?.isChangingConfigurations == true
        if (!isChangingConfigurations) {
            flowUiManager?.isShown = false
        }
        super.onDestroy()
    }
}
