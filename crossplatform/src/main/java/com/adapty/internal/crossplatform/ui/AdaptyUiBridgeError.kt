package com.adapty.internal.crossplatform.ui

import com.adapty.errors.AdaptyErrorCode

sealed class AdaptyUiBridgeError(
    val errorCode: AdaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
    val message: String,
) {
    class ViewNotFound internal constructor(viewId: String) :
        AdaptyUiBridgeError(message = "AdaptyUIError.viewNotFound($viewId)")

    class ViewAlreadyPresented internal constructor(viewId: String) :
        AdaptyUiBridgeError(message = "AdaptyUIError.viewAlreadyPresented($viewId)")

    class ViewPresentationError internal constructor(viewId: String) :
        AdaptyUiBridgeError(message = "AdaptyUIError.viewPresentationError($viewId)")

    val rawCode = when (errorCode) {
        AdaptyErrorCode.WRONG_PARAMETER -> 3001
        else -> 0
    }
}
