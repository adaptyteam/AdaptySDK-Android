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

const val PROFILE_ID = "PROFILE_ID"
const val CUSTOMER_USER_ID = "CUSTOMER_USER_ID"
const val INSTALLATION_META_ID = "CUSTOMER_USER_ID"
const val IAM_ACCESS_KEY_ID = "IAM_ACCESS_KEY_ID"
const val IAM_SECRET_KEY = "IAM_SECRET_KEY"
const val IAM_SESSION_TOKEN = "IAM_SESSION_TOKEN"
const val PURCHASER_INFO = "PURCHASER_INFO"
const val CONTAINERS = "CONTAINERS"
const val PRODUCTS = "PRODUCTS"
const val SYNCED_PURCHASES = "SYNCED_PURCHASES"
const val KINESIS_RECORDS = "KINESIS_RECORDS"
const val APP_KEY = "APP_KEY"

class PreferenceManager(context: Context) {

    private val privateMode = 0
    private val pref = context.getSharedPreferences(PREF_NAME, privateMode)
    private val editor = pref.edit()

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
                return Gson().fromJson(it, PurchaserInfoModel::class.java)
            }
            return null
        }
        set(info) {
            editor.putString(PURCHASER_INFO, Gson().toJson(info))
            editor.commit()
        }

    var containers: ArrayList<DataContainer>?
        get() {
            val json = pref.getString(CONTAINERS, null)
            if (TextUtils.isEmpty(json))
                return null
            else {
                return Gson().fromJson(json, object : TypeToken<ArrayList<DataContainer>>() {}.type)
            }
        }
        set(set) {
            editor.putString(CONTAINERS, Gson().toJson(set))
            editor.commit()
        }

    var products: ArrayList<Product>
        get() {
            val json = pref.getString(PRODUCTS, null)
            if (TextUtils.isEmpty(json))
                return arrayListOf()
            else {
                return Gson().fromJson(json, object : TypeToken<ArrayList<Product>>() {}.type)
            }
        }
        set(set) {
            editor.putString(PRODUCTS, Gson().toJson(set))
            editor.commit()
        }

    var syncedPurchases: ArrayList<RestoreItem>
        get() {
            val json = pref.getString(SYNCED_PURCHASES, null)
            if (TextUtils.isEmpty(json))
                return arrayListOf()
            else {
                return Gson().fromJson(json, object : TypeToken<ArrayList<RestoreItem>>() {}.type)
            }
        }
        set(set) {
            editor.putString(SYNCED_PURCHASES, Gson().toJson(set))
            editor.commit()
        }

    var kinesisRecords: ArrayList<AwsRecordModel>
        get() {
            val json = pref.getString(KINESIS_RECORDS, null)
            if (TextUtils.isEmpty(json))
                return arrayListOf()
            else {
                return Gson().fromJson(
                    json,
                    object : TypeToken<ArrayList<AwsRecordModel>>() {}.type
                )
            }
        }
        set(set) {
            editor.putString(KINESIS_RECORDS, Gson().toJson(set))
            editor.commit()
        }

    companion object {
        const val PREF_NAME = "AdaptySDKPrefs"
    }

}