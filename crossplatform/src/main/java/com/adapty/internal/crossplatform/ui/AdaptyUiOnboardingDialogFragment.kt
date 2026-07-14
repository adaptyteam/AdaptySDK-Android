
package com.adapty.internal.crossplatform.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import com.adapty.internal.crossplatform.R
import com.adapty.internal.crossplatform.ui.Dependencies.safeInject
import com.adapty.ui.onboardings.AdaptyOnboardingView
import kotlin.LazyThreadSafetyMode.NONE

class AdaptyUiOnboardingDialogFragment : DialogFragment() {

    internal companion object {
        internal const val VIEW_ID = "VIEW_ID"

        fun newInstance(viewId: String): AdaptyUiOnboardingDialogFragment {
            return AdaptyUiOnboardingDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(VIEW_ID, viewId)
                }
            }
        }
    }

    private val onboardingView: AdaptyOnboardingView by lazy(NONE) {
        AdaptyOnboardingView(requireContext())
    }

    private val onboardingUiManager: OnboardingUiManager? by safeInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AdaptyVisualPaywallTheme)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.AdaptyUiScreenTransition)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = androidx.activity.ComponentDialog(requireContext(), theme)
        dialog.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return onboardingView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewId = arguments?.getString(VIEW_ID) ?: kotlin.run {
            dismissAllowingStateLoss()
            return
        }

        with(onboardingView) {
            val currentData = onboardingUiManager?.getData(viewId)
                ?: kotlin.run {
                    onboardingUiManager?.removeData(viewId)
                    dismissAllowingStateLoss()
                    return@with
                }
            onboardingUiManager?.setCurrentView(this)
            val eventListener = onboardingUiManager?.newOnboardingEventListener(currentData.view) ?: kotlin.run {
                dismissAllowingStateLoss()
                return
            }
            show(currentData.config, eventListener)
        }
    }

    private fun handleBackPress() {
        val viewId = arguments?.getString(VIEW_ID) ?: kotlin.run {
            closeView()
            return
        }

        if (onboardingUiManager?.handleSystemBack(viewId) != true) {
            closeView()
        }
    }

    private fun closeView() {
        onboardingUiManager?.clearCurrentView()
        dismiss()
    }

    fun close() {
        closeView()
    }

    override fun onDestroy() {
        val isChangingConfigurations = activity?.isChangingConfigurations == true
        if (!isChangingConfigurations) {
            onboardingUiManager?.isShown = false
        }
        super.onDestroy()
    }
}
