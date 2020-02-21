package com.adapty

import android.app.Activity
import android.content.Context
import com.adapty.api.ApiClientRepository
import com.adapty.api.AdaptyCallback
import com.adapty.api.AdaptyPurchaseCallback
import com.adapty.utils.PreferenceManager
import com.adapty.api.responses.CreateProfileResponse
import com.adapty.purchase.InAppPurchases
import java.util.*

class Adapty {

    companion object {
        lateinit var applicationContext: Context
        lateinit var preferenceManager: PreferenceManager

        fun activate(applicationContext: Context, appKey: String) {
            this.applicationContext = applicationContext
            this.preferenceManager = PreferenceManager(applicationContext)
            this.preferenceManager.appKey = appKey

            ApiClientRepository.getInstance(preferenceManager)
                .createProfile(object : AdaptyCallback {
                    override fun success(response: Any?, reqID: Int) {
                        if (response is CreateProfileResponse) {
                            response.data?.attributes?.profileId?.let {
                                preferenceManager.profileID = it
                            }
                            response.data?.attributes?.customerUserId?.let {
                                preferenceManager.customerUserID = it
                            }
                        }

                        sendSyncMetaInstallRequest()
                    }

                    override fun fail(msg: String, reqID: Int) {

                    }

                })
        }


        fun sendSyncMetaInstallRequest() {
            ApiClientRepository.getInstance(preferenceManager)
                .syncMetaInstall(object : AdaptyCallback {
                    override fun success(response: Any?, reqID: Int) {
                    }

                    override fun fail(msg: String, reqID: Int) {

                    }

                })
        }

        fun updateProfile(
            customerUserId: String?,
            email: String?,
            phoneNumber: String?,
            facebookUserId: String?,
            mixpanelUserId: String?,
            amplitudeUserId: String?,
            firstName: String?,
            lastName: String?,
            gender: String?,
            birthday: String?, adaptyCallback: AdaptyCallback
        ) {
            ApiClientRepository.getInstance(preferenceManager).updateProfile(
                customerUserId,
                email,
                phoneNumber,
                facebookUserId,
                mixpanelUserId,
                amplitudeUserId,
                firstName,
                lastName,
                gender,
                birthday,
                adaptyCallback
            )

        }

        fun makePurchase(
            activity: Activity,
            type: String,
            productId: String,
            adaptyCallback: AdaptyPurchaseCallback
        ) {
            val inAppPurchases = InAppPurchases(activity, type, productId, adaptyCallback)
            inAppPurchases.setupBilling(productId, false)
        }

        fun restore(
            activity: Activity,
            type: String,
            productId: String,
            adaptyCallback: AdaptyPurchaseCallback
        ) {
            val inAppPurchases = InAppPurchases(activity, type, productId, adaptyCallback)
            inAppPurchases.setupBilling(productId, true)
        }

        fun validateReceipt(purchaseToken: String, adaptyCallback: AdaptyCallback) {
            ApiClientRepository.getInstance(preferenceManager)
                .validatePurchase(purchaseToken, adaptyCallback)
        }
    }
}