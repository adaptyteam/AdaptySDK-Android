package com.adapty.listeners

import com.adapty.models.PaywallModel

public interface OnPaywallsForConfigReceivedListener {
    public fun onPaywallsForConfigReceived(paywalls: List<PaywallModel>)
}