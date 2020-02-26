package com.adapty

import android.app.Activity
import android.content.Context
import com.adapty.api.*
import com.adapty.utils.PreferenceManager
import com.adapty.api.responses.CreateProfileResponse
import com.adapty.api.responses.SyncMetaInstallResponse
import com.adapty.api.responses.ValidateReceiptResponse
import com.adapty.purchase.InAppPurchases
import com.android.billingclient.api.Purchase
import java.util.*

class Adapty {

    companion object {
        lateinit var applicationContext: Context
        lateinit var preferenceManager: PreferenceManager

        fun activate(applicationContext: Context, appKey: String) = activate(applicationContext, appKey, null)

        fun activate(applicationContext: Context, appKey: String, customerUserId: String?) {
            this.applicationContext = applicationContext
            this.preferenceManager = PreferenceManager(applicationContext)
            this.preferenceManager.appKey = appKey

            ApiClientRepository.getInstance(preferenceManager)
                .createProfile(customerUserId, object : AdaptySystemCallback {
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
                        if (response is SyncMetaInstallResponse) {
                            response.data?.id?.let {
                                preferenceManager.installationMetaID = it
                            }
                        }
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
            birthday: String?, adaptyCallback: (String?) -> Unit
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
                object : AdaptyProfileCallback {
                    override fun onResult(error: String?) {
                        adaptyCallback.invoke(error)
                    }

                }
            )

        }

        fun makePurchase(
            activity: Activity,
            type: String,
            productId: String,
            adaptyCallback: (Purchase?, String?) -> Unit
        ) {
            InAppPurchases(activity, false, type, productId, object : AdaptyPurchaseCallback {
                override fun onResult(response: Purchase?, error: String?) {
                    adaptyCallback.invoke(response, error)
                }
            })
        }

        fun restore(
            activity: Activity,
            type: String,
            adaptyCallback: (String?) -> Unit
        ) {
            InAppPurchases(activity, true, type, "", object : AdaptyRestoreCallback {
                override fun onResult(error: String?) {
                    adaptyCallback.invoke(error)
                }
            })
        }

        fun validateReceipt(productId: String, purchaseToken: String, adaptyCallback: (ValidateReceiptResponse?, error: String?) -> Unit) {
            ApiClientRepository.getInstance(preferenceManager)
                .validatePurchase(productId, purchaseToken, object : AdaptyValidateCallback {
                    override fun onResult(response: ValidateReceiptResponse?, error: String?) {
                        adaptyCallback.invoke(response, error)
                    }
                })
        }
    }
}