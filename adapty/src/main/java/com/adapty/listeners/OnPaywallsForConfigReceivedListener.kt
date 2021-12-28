package com.adapty.listeners

import com.adapty.models.PaywallModel

interface OnPaywallsForConfigReceivedListener {
    fun onPaywallsForConfigReceived(paywalls: List<PaywallModel>)
}