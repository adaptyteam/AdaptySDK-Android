package com.adapty.ui.preview

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener
import com.adapty.ui.listeners.AdaptyFlowEventListener

/** View-system host for [AdaptyFlowPreview]. */
public class AdaptyFlowPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var viewConfiguration by mutableStateOf<AdaptyUI.FlowConfiguration?>(null)
    private var products: List<AdaptyPaywallProduct>? = null
    private var eventListener: AdaptyFlowEventListener = AdaptyFlowDefaultEventListener()
    private var previewEventCallback: PreviewEventCallback? = null

    public fun show(
        viewConfiguration: AdaptyUI.FlowConfiguration,
        products: List<AdaptyPaywallProduct>? = null,
        eventListener: AdaptyFlowEventListener = AdaptyFlowDefaultEventListener(),
        previewEventCallback: PreviewEventCallback? = null,
    ) {
        this.products = products
        this.eventListener = eventListener
        this.previewEventCallback = previewEventCallback
        this.viewConfiguration = viewConfiguration
    }

    @Composable
    override fun Content() {
        val config = viewConfiguration ?: return
        AdaptyFlowPreview(config, products, eventListener, previewEventCallback)
    }
}
