package com.adapty.internal.crossplatform.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.adapty.internal.crossplatform.R
import com.adapty.internal.crossplatform.ui.Dependencies.safeInject

class AdaptyUiDialog : DialogFragment() {

    private val flowUiManager: FlowUiManager? by safeInject()

    private val listener: Listener? get() = flowUiManager?.onDialogActionListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (listener == null)
            dismissAllowingStateLoss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val config = runCatching { arguments?.getParcelable<AdaptyUiDialogConfig>(CONFIG) }.getOrNull() ?: run {
            dismissAllowingStateLoss()
            return super.onCreateDialog(savedInstanceState)
        }
        return AlertDialog.Builder(requireContext(), R.style.AdaptyAlertDialogTheme)
            .setTitle(config.title)
            .setMessage(config.content)
            .apply {
                config.defaultActionTitle?.let { actionTitle ->
                    setNegativeButton(actionTitle) { _, _ ->
                        listener?.onDialogAction(PRIMARY_ACTION)
                    }
                }
                config.secondaryActionTitle?.let { actionTitle ->
                    setPositiveButton(actionTitle) { _, _ ->
                        listener?.onDialogAction(SECONDARY_ACTION)
                    }
                }
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        listener?.onDialogAction(PRIMARY_ACTION)
        super.onCancel(dialog)
    }

    companion object {
        fun newInstance(config: AdaptyUiDialogConfig): AdaptyUiDialog {
            return AdaptyUiDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(CONFIG, config)
                }
            }
        }

        const val TAG = "AdaptyUIDialog"
        private const val CONFIG = "config"

        const val PRIMARY_ACTION = "primary"
        const val SECONDARY_ACTION = "secondary"
    }

    fun interface Listener {
        fun onDialogAction(action: String)
    }
}
