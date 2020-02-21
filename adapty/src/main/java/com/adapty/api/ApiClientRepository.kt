package com.adapty.api

import com.adapty.Adapty.Companion.applicationContext
import com.adapty.api.entity.profile.AttributeProfileReq
import com.adapty.api.entity.profile.DataProfileReq
import com.adapty.api.entity.receipt.AttributeValidateReceiptReq
import com.adapty.api.entity.receipt.DataValidateReceiptReq
import com.adapty.api.entity.syncmeta.DataSyncMetaReq
import com.adapty.api.requests.CreateProfileRequest
import com.adapty.api.requests.SyncMetaInstallRequest
import com.adapty.api.requests.UpdateProfileRequest
import com.adapty.api.requests.ValidateReceiptRequest
import com.adapty.utils.PreferenceManager
import com.adapty.utils.UUIDTimeBased
import java.util.*

class ApiClientRepository(var preferenceManager: PreferenceManager) {

    private var apiClient = ApiClient(applicationContext)

    fun createProfile(adaptyCallback: AdaptyCallback) {

        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = UUIDTimeBased.generateId().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }

        val profileRequest = CreateProfileRequest()
        profileRequest.data = DataProfileReq()
        profileRequest.data?.id = uuid
        profileRequest.data?.type = "adapty_analytics_profile"

        apiClient.createProfile(profileRequest, adaptyCallback)
    }

    fun updateProfile(customerUserId: String?,
                      email: String?,
                      phoneNumber: String?,
                      facebookUserId: String?,
                      mixpanelUserId: String?,
                      amplitudeUserId: String?,
                      firstName: String?,
                      lastName: String?,
                      gender: String?,
                      birthday: String?,
                      adaptyCallback: AdaptyCallback) {

        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = UUIDTimeBased.generateId().toString()
            preferenceManager.profileID = uuid
        }

        val profileRequest = UpdateProfileRequest()
        profileRequest.data = DataProfileReq()
        profileRequest.data?.id = uuid
        profileRequest.data?.type = "adapty_analytics_profile"
        profileRequest.data?.attributes = AttributeProfileReq()
        profileRequest.data?.attributes?.apply {
            this.customerUserId = customerUserId
            this.email = email
            this.phoneNumber = phoneNumber
            this.facebookUserId = facebookUserId
            this.mixpanelUserId = mixpanelUserId
            this.amplitudeUserId = amplitudeUserId
            this.firstName = firstName
            this.lastName = lastName
            this.gender = gender
            this.birthday = birthday
        }

        apiClient.updateProfile(profileRequest, adaptyCallback)
    }

    fun syncMetaInstall(adaptyCallback: AdaptyCallback? = null) {

        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = UUIDTimeBased.generateId().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }

        val syncMetaRequest = SyncMetaInstallRequest()
        syncMetaRequest.data = DataSyncMetaReq()
        syncMetaRequest.data?.id = uuid
        syncMetaRequest.data?.type = "adapty_analytics_profile_installation_meta"

        apiClient.syncMeta(syncMetaRequest, adaptyCallback)
    }

    fun validatePurchase(purchaseToken: String, adaptyCallback: AdaptyCallback? = null) {
        var uuid = preferenceManager.profileID
        if (uuid.isEmpty()) {
            uuid = UUIDTimeBased.generateId().toString()
            preferenceManager.profileID = uuid
            preferenceManager.installationMetaID = uuid
        }

        val validateReceiptRequest = ValidateReceiptRequest()
        validateReceiptRequest.data = DataValidateReceiptReq()
        validateReceiptRequest.data?.id = uuid
        validateReceiptRequest.data?.attributes = AttributeValidateReceiptReq()
        validateReceiptRequest.data?.attributes?.token = purchaseToken
        validateReceiptRequest.data?.attributes?.profileId = uuid

        apiClient.validatePurchase(validateReceiptRequest, adaptyCallback)
    }

    companion object Factory {

        private lateinit var instance: ApiClientRepository

        @Synchronized
        fun getInstance(preferenceManager: PreferenceManager): ApiClientRepository {
            if (!::instance.isInitialized)
                instance = ApiClientRepository(preferenceManager)

            return instance
        }
    }
}