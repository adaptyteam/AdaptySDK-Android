package com.adapty.listeners

import com.adapty.models.AdaptyProfile

public fun interface OnProfileUpdatedListener {
    public fun onProfileReceived(profile: AdaptyProfile)
}