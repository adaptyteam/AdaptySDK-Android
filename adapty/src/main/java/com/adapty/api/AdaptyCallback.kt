package com.adapty.api

import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.Product
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

interface AdaptyPaywallsCallback : AdaptyCallback {

    fun onResult(containers: ArrayList<DataContainer>, products: ArrayList<Product>, error: String?)

}

interface AdaptyPaywallsInfoCallback : AdaptyCallback {

    fun onResult(data: ArrayList<Any>, error: String?)

}