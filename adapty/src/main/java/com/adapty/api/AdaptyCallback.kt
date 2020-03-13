package com.adapty.api

import com.adapty.api.entity.purchaserInfo.AttributePurchaserInfoRes
import com.adapty.api.responses.RestoreReceiptResponse
import com.adapty.api.responses.ValidateReceiptResponse
import com.android.billingclient.api.Purchase

interface AdaptyCallback {

}

interface AdaptySystemCallback : AdaptyCallback {

    fun success(response: Any?, reqID: Int)

    fun fail(msg: String, reqID: Int)
}

interface AdaptyPurchaseCallback : AdaptyCallback {

    fun onResult(purchase: Purchase?, response: ValidateReceiptResponse?, error: String?)

}

interface AdaptyProfileCallback : AdaptyCallback {

    fun onResult(error: String?)

}

interface AdaptyRestoreCallback : AdaptyCallback {

    fun onResult(response: RestoreReceiptResponse?, error: String?)

}

interface AdaptyValidateCallback : AdaptyCallback {

    fun onResult(response: ValidateReceiptResponse?, error: String?)

}

interface AdaptyPurchaserInfoCallback : AdaptyCallback {

    fun onResult(response: AttributePurchaserInfoRes?, error: String?)

}