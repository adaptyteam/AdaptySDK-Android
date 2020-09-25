package com.adapty.utils

import android.content.Context
import android.text.TextUtils
import com.adapty.api.aws.AwsRecordModel
import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.Product
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.api.entity.restore.RestoreItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceManager(context: Context) {

    private val privateMode = 0
    private val pref = context.getSharedPreferences(PREF_NAME, privateMode)
    private val editor = pref.edit()
    private val gson = Gson()

    var profileID: String
        get() {
            return pref.getString(PROFILE_ID, "").toString()
        }
        set(profileID) {
            editor.putString(PROFILE_ID, profileID)
            editor.commit()
        }

    var customerUserID: String
        get() {
            return pref.getString(CUSTOMER_USER_ID, "").toString()
        }
        set(customerUserID) {
            editor.putString(CUSTOMER_USER_ID, customerUserID)
            editor.commit()
        }

    var iamAccessKeyId: String?
        get() {
            return pref.getString(IAM_ACCESS_KEY_ID, null)
        }
        set(iamAccessKeyId) {
            editor.putString(IAM_ACCESS_KEY_ID, iamAccessKeyId)
            editor.commit()
        }

    var iamSecretKey: String?
        get() {
            return pref.getString(IAM_SECRET_KEY, null)
        }
        set(iamSecretKey) {
            editor.putString(IAM_SECRET_KEY, iamSecretKey)
            editor.commit()
        }

    var iamSessionToken: String?
        get() {
            return pref.getString(IAM_SESSION_TOKEN, null)
        }
        set(iamSessionToken) {
            editor.putString(IAM_SESSION_TOKEN, iamSessionToken)
            editor.commit()
        }

    var installationMetaID: String
        get() {
            return pref.getString(INSTALLATION_META_ID, "").toString()
        }
        set(installationMetaID) {
            editor.putString(INSTALLATION_META_ID, installationMetaID)
            editor.commit()
        }

    var appKey: String
        get() {
            return pref.getString(APP_KEY, "").toString()
        }
        set(appKey) {
            editor.putString(APP_KEY, appKey)
            editor.commit()
        }

    var purchaserInfo: PurchaserInfoModel?
        get() {
            val str = pref.getString(PURCHASER_INFO, null)
            str?.let {
                return gson.fromJson(it, PurchaserInfoModel::class.java)
            }
            return null
        }
        set(info) {
            editor.putString(PURCHASER_INFO, gson.toJson(info))
            editor.commit()
        }

    var containers: ArrayList<DataContainer>?
        get() {
            val json = pref.getString(CONTAINERS, null)
            return if (TextUtils.isEmpty(json))
                null
            else {
                gson.fromJson(json, object : TypeToken<ArrayList<DataContainer>>() {}.type)
            }
        }
        set(value) {
            editor.putString(CONTAINERS, gson.toJson(value))
            editor.commit()
        }

    var products: ArrayList<Product>
        get() {
            val json = pref.getString(PRODUCTS, null)
            return if (TextUtils.isEmpty(json))
                arrayListOf()
            else {
                gson.fromJson(json, object : TypeToken<ArrayList<Product>>() {}.type)
            }
        }
        set(value) {
            editor.putString(PRODUCTS, gson.toJson(value))
            editor.commit()
        }

    var syncedPurchases: ArrayList<RestoreItem>
        get() {
            val json = pref.getString(SYNCED_PURCHASES, null)
            return if (TextUtils.isEmpty(json))
                arrayListOf()
            else {
                gson.fromJson(json, object : TypeToken<ArrayList<RestoreItem>>() {}.type)
            }
        }
        set(value) {
            editor.putString(SYNCED_PURCHASES, gson.toJson(value))
            editor.commit()
        }

    var kinesisRecords: ArrayList<AwsRecordModel>
        get() {
            val json = pref.getString(KINESIS_RECORDS, null)
            return if (TextUtils.isEmpty(json))
                arrayListOf()
            else {
                gson.fromJson(json, object : TypeToken<ArrayList<AwsRecordModel>>() {}.type)
            }
        }
        set(value) {
            editor.putString(KINESIS_RECORDS, gson.toJson(value))
            editor.commit()
        }

    private companion object {
        private const val PREF_NAME = "AdaptySDKPrefs"

        private const val PROFILE_ID = "PROFILE_ID"
        private const val CUSTOMER_USER_ID = "CUSTOMER_USER_ID"
        private const val INSTALLATION_META_ID = "INSTALLATION_META_ID"
        private const val IAM_ACCESS_KEY_ID = "IAM_ACCESS_KEY_ID"
        private const val IAM_SECRET_KEY = "IAM_SECRET_KEY"
        private const val IAM_SESSION_TOKEN = "IAM_SESSION_TOKEN"
        private const val PURCHASER_INFO = "PURCHASER_INFO"
        private const val CONTAINERS = "CONTAINERS"
        private const val PRODUCTS = "PRODUCTS"
        private const val SYNCED_PURCHASES = "SYNCED_PURCHASES"
        private const val KINESIS_RECORDS = "KINESIS_RECORDS"
        private const val APP_KEY = "APP_KEY"
    }

}