package com.adapty.api

import com.adapty.api.entity.paywalls.DataContainer
import com.adapty.api.entity.paywalls.ProductModel
import com.adapty.api.entity.paywalls.PromoModel
import com.adapty.api.entity.purchaserInfo.AttributePurchaserInfoRes
import com.adapty.api.responses.RestoreReceiptResponse
import com.adapty.api.responses.UpdateProfileResponse
import com.adapty.api.responses.ValidateReceiptResponse
import com.android.billingclient.api.Purchase

interface AdaptyCallback {

}

interface AdaptySystemCallback : AdaptyCallback {

    fun success(response: Any?, reqID: Int)

    fun fail(error: AdaptyError, reqID: Int)
}

interface AdaptyPurchaseCallback : AdaptyCallback {

    fun onResult(purchase: Purchase?, response: ValidateReceiptResponse?, error: AdaptyError?)

}

interface AdaptyProfileCallback : AdaptyCallback {

    fun onResult(response: UpdateProfileResponse?, error: AdaptyError?)

}

interface AdaptyRestoreCallback : AdaptyCallback {

    fun onResult(response: RestoreReceiptResponse?, error: AdaptyError?)

}

interface AdaptyValidateCallback : AdaptyCallback {

    fun onResult(response: ValidateReceiptResponse?, error: AdaptyError?)

}

interface AdaptyPurchaserInfoCallback : AdaptyCallback {

    fun onResult(response: AttributePurchaserInfoRes?, error: AdaptyError?)

}

interface AdaptyPaywallsCallback : AdaptyCallback {

    fun onResult(containers: ArrayList<DataContainer>, products: ArrayList<ProductModel>, error: AdaptyError?)

}

interface AdaptyPaywallsInfoCallback : AdaptyCallback {

    fun onResult(data: ArrayList<Any>, error: AdaptyError?)

}

interface AdaptyPromosCallback : AdaptyCallback {

    fun onResult(promo: PromoModel?, error: AdaptyError?)

}