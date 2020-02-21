package com.adapty.api

import com.android.billingclient.api.Purchase

interface AdaptyCallback {

    fun success(response: Any?, reqID: Int)

    fun fail(msg: String, reqID: Int)
}

interface AdaptyPurchaseCallback {

    fun success(response: Purchase?, reqID: Int)

    fun fail(msg: String, reqID: Int)
}