package com.adapty

import android.app.Activity
import android.content.Context
import com.adapty.api.*
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
                .createProfile(object : AdaptySystemCallback {
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
                .syncMetaInstall(object : AdaptySystemCallback {
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
            birthday: String?, adaptyCallback: AdaptyProfileCallback
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
            InAppPurchases(activity, false, type, productId, adaptyCallback)
        }

        fun restore(
            activity: Activity,
            type: String,
            productId: String,
            adaptyCallback: AdaptyRestoreCallback
        ) {
            InAppPurchases(activity, true, type, productId, adaptyCallback)
        }

        fun validateReceipt(purchaseToken: String, adaptyCallback: AdaptyValidateCallback) {
            ApiClientRepository.getInstance(preferenceManager)
                .validatePurchase(purchaseToken, adaptyCallback)
        }
    }
}