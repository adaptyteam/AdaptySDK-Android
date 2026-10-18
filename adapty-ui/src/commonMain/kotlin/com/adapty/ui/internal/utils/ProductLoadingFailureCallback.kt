package com.adapty.ui.internal.utils

import com.adapty.ui.internal.platform.AdaptyError

internal fun interface ProductLoadingFailureCallback {
    fun onLoadingProductsFailure(error: AdaptyError): Boolean
}