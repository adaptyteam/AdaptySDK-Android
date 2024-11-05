package com.adapty.ui.internal.utils

import com.adapty.errors.AdaptyError

internal fun interface ProductLoadingFailureCallback {
    fun onLoadingProductsFailure(error: AdaptyError): Boolean
}