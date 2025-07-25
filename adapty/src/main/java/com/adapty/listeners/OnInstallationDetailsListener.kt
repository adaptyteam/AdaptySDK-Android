package com.adapty.listeners

import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyInstallationDetails

public interface OnInstallationDetailsListener {
    public fun onInstallationDetailsSuccess(details: AdaptyInstallationDetails)
    public fun onInstallationDetailsFailure(error: AdaptyError)
}