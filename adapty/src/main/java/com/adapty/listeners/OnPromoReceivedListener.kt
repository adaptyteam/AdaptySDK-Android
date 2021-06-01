package com.adapty.listeners

import com.adapty.models.PromoModel

interface OnPromoReceivedListener {
    fun onPromoReceived(promo: PromoModel)
}