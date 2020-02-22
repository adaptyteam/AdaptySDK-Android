package com.adapty.api

import com.adapty.api.responses.ValidateReceiptResponse
import com.android.billingclient.api.Purchase

interface AdaptyCallback {

}

interface AdaptySystemCallback : AdaptyCallback {

    fun success(response: Any?, reqID: Int)

    fun fail(msg: String, reqID: Int)
}

interface AdaptyPurchaseCallback : AdaptyCallback {

    fun onResult(response: Purchase?, error: String?)

}

interface AdaptyProfileCallback : AdaptyCallback {

    fun onResult(error: String?)

}

interface AdaptyRestoreCallback : AdaptyCallback {

    fun onResult(error: String?)

}

interface AdaptyValidateCallback : AdaptyCallback {

    fun onResult(response: ValidateReceiptResponse?, error: String?)

}