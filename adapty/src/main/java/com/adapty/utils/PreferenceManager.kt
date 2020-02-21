package com.adapty.utils

import android.content.Context

const val PROFILE_ID = "PROFILE_ID"
const val CUSTOMER_USER_ID = "CUSTOMER_USER_ID"
const val INSTALLATION_META_ID = "CUSTOMER_USER_ID"
const val APP_KEY = "APP_KEY"

class PreferenceManager (context: Context) {

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

    companion object {
        const val PREF_NAME = "AdaptySDKPrefs"
    }

}