package com.adapty

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.adapty.api.*
import com.adapty.api.entity.DataState
import com.adapty.api.entity.paywalls.*
import com.adapty.api.entity.profile.update.ProfileParameterBuilder
import com.adapty.api.entity.purchaserInfo.*
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.api.entity.validate.GoogleValidationResult
import com.adapty.api.responses.*
import com.adapty.purchase.InAppPurchases
import com.adapty.purchase.InAppPurchasesInfo
import com.adapty.purchase.PurchaseType
import com.adapty.utils.*
import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

class Adapty {

    companion object {
        lateinit var context: Context
        private lateinit var preferenceManager: PreferenceManager
        private var onPurchaserInfoUpdatedListener: OnPurchaserInfoUpdatedListener? = null
        private var onPromoReceivedListener: OnPromoReceivedListener? = null
        private var requestQueue: ArrayList<() -> Unit> = arrayListOf()
        private var isActivated = false
        private val kinesisManager by lazy {
            KinesisManager(preferenceManager)
        }
        private val liveTracker by lazy {
            AdaptyLiveTracker(kinesisManager)
        }
        private val gson = Gson()
        private val apiClientRepository by lazy {
            ApiClientRepository(preferenceManager, gson)
        }

        @JvmStatic
        fun activate(
            context: Context,
            appKey: String
        ) =
            activate(context, appKey, null, null)

        @JvmStatic
        fun activate(
            context: Context,
            appKey: String,
            customerUserId: String?
        ) {
            activate(context, appKey, customerUserId, null)
        }

        private fun activate(
            context: Context,
            appKey: String,
            customerUserId: String?,
            adaptyCallback: ((String?) -> Unit)?
        ) {
            LogHelper.logVerbose("activate($appKey, ${customerUserId ?: ""})")
            if (isActivated)
                return

            require(!appKey.isBlank()) { "Public SDK key must not be empty." }
            require(context.applicationContext is Application) { "Application context must be provided." }
            require(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) { "INTERNET permission must be granted." }

            isActivated = true

            this.context = context.applicationContext
            this.preferenceManager = PreferenceManager(this.context)
            this.preferenceManager.appKey = appKey
            (this.context as Application).registerActivityLifecycleCallbacks(liveTracker)

            addToQueue {
                activateInQueue(customerUserId, adaptyCallback)
            }
        }

        private fun activateInQueue(
            customerUserId: String?,
            adaptyCallback: ((String?) -> Unit)?
        ) {
            if (preferenceManager.profileID.isEmpty()) {
                apiClientRepository.createProfile(customerUserId, object : AdaptySystemCallback {
                    override fun success(response: Any?, reqID: Int) {
                        if (response is CreateProfileResponse) {
                            response.data?.attributes?.apply {
                                profileId?.let {
                                    preferenceManager.profileID = it
                                }
                                this.customerUserId?.let {
                                    preferenceManager.customerUserID = it
                                }

                                checkChangesPurchaserInfo(this)
                            }
                        }

                        adaptyCallback?.invoke(null)

                        nextQueue()

                        getStartedPaywalls()

                        getPromoOnStart()

                        sendSyncMetaInstallRequest()

                        syncPurchasesBody(false, null)

                    }

                    override fun fail(msg: String, reqID: Int) {
                        adaptyCallback?.invoke(msg)
                        nextQueue()
                    }
                })
            } else {
                makeStartRequests(adaptyCallback)
            }
        }

        private fun nextQueue() {
            if (requestQueue.isNotEmpty())
                requestQueue.removeAt(0)

            if (requestQueue.isNotEmpty())
                requestQueue.first().invoke()
        }

        private fun addToQueue(action: () -> Unit) {
            requestQueue.add(action)

            if (requestQueue.size == 1)
                requestQueue[0].invoke()
        }

        private fun makeStartRequests(adaptyCallback: ((String?) -> Unit)?) {
            sendSyncMetaInstallRequest()

            getStartedPaywalls()

            getPromoOnStart()

            syncPurchasesBody(false) { _ ->
                var isCallbackSent = false
                getPurchaserInfo(false) { info, state, error ->
                    if (!isCallbackSent) {
                        isCallbackSent = true
                        adaptyCallback?.invoke(error)
                        nextQueue()
                        return@getPurchaserInfo
                    }

                    info?.takeIf(::isPurchaserInfoChanged)?.let {
                        onPurchaserInfoUpdatedListener?.onPurchaserInfoReceived(it)
                    }
                }
            }
        }

        private fun checkChangesPurchaserInfo(res: AttributePurchaserInfoRes)
                = checkChangesPurchaserInfo(generatePurchaserInfoModel(res))

        private fun checkChangesPurchaserInfo(purchaserInfo: PurchaserInfoModel) {
            if (isPurchaserInfoChanged(purchaserInfo)) {
                onPurchaserInfoUpdatedListener?.onPurchaserInfoReceived(purchaserInfo)
            }
            preferenceManager.purchaserInfo = purchaserInfo
        }

        private fun isPurchaserInfoChanged(info: PurchaserInfoModel)
                = preferenceManager.purchaserInfo != info

        @JvmStatic
        fun sendSyncMetaInstallRequest() {
            LogHelper.logVerbose("sendSyncMetaInstallRequest()")
            apiClientRepository.syncMetaInstall(object : AdaptySystemCallback {
                override fun success(response: Any?, reqID: Int) {
                    if (response is SyncMetaInstallResponse) {
                        response.data?.attributes?.let { attrs ->
                            attrs.iamAccessKeyId?.let {
                                preferenceManager.iamAccessKeyId = it
                            }
                            attrs.iamSecretKey?.let {
                                preferenceManager.iamSecretKey = it
                            }
                            attrs.iamSessionToken?.let {
                                preferenceManager.iamSessionToken = it
                            }
                            attrs.profileId?.let {
                                if (it != preferenceManager.profileID) {
                                    preferenceManager.profileID = it
                                }
                            }
                        }

                        liveTracker.start()
                    }
                }

                override fun fail(msg: String, reqID: Int) {
                }

            })
        }

        @JvmStatic
        fun identify(customerUserId: String, adaptyCallback: (error: String?) -> Unit) {
            LogHelper.logVerbose("identify()")
            addToQueue { identifyInQueue(customerUserId, adaptyCallback) }
        }

        private fun identifyInQueue(
            customerUserId: String,
            adaptyCallback: (String?) -> Unit
        ) {
            if (customerUserId.isBlank()) {
                LogHelper.logError("customerUserId should not be empty")
                adaptyCallback.invoke("customerUserId should not be empty")
                nextQueue()
                return
            } else if (customerUserId == preferenceManager.customerUserID) {
                adaptyCallback.invoke(null)
                nextQueue()
                return
            }

            apiClientRepository.createProfile(customerUserId, object : AdaptySystemCallback {
                override fun success(response: Any?, reqID: Int) {
                    var profileIdChanged = false
                    if (response is CreateProfileResponse) {
                        response.data?.attributes?.apply {
                            profileId?.let {
                                profileIdChanged = it != preferenceManager.profileID
                                preferenceManager.profileID = it
                            }
                            this.customerUserId?.let {
                                preferenceManager.customerUserID = it
                            }

                            checkChangesPurchaserInfo(this)
                        }
                    }

                    adaptyCallback.invoke(null)

                    nextQueue()

                    if (!profileIdChanged) return

                    preferenceManager.products = arrayListOf()
                    preferenceManager.containers = null

                    getStartedPaywalls()

                    sendSyncMetaInstallRequest()

                    syncPurchasesBody(false, null)
                }

                override fun fail(msg: String, reqID: Int) {
                    adaptyCallback.invoke(msg)

                    nextQueue()
                }

            })
        }

        @JvmStatic
        fun updateProfile(params: ProfileParameterBuilder, adaptyCallback: (String?) -> Unit) {
            addToQueue {
                apiClientRepository.updateProfile(
                    params.email,
                    params.phoneNumber,
                    params.facebookUserId,
                    params.mixpanelUserId,
                    params.amplitudeUserId,
                    params.amplitudeDeviceId,
                    params.appmetricaProfileId,
                    params.appmetricaDeviceId,
                    params.firstName,
                    params.lastName,
                    params.gender,
                    params.birthday,
                    params.customAttributes,
                    object : AdaptyProfileCallback {
                        override fun onResult(response: UpdateProfileResponse?, error: String?) {
                            response?.data?.attributes?.profileId?.let {
                                if (it != preferenceManager.profileID) {
                                    preferenceManager.profileID = it
                                }
                            }
                            adaptyCallback.invoke(error)
                            nextQueue()
                        }

                    }
                )
            }
        }

        private fun getPurchaserInfo(
            needQueue: Boolean,
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, state: DataState, error: String?) -> Unit
        ) {
            val info = preferenceManager.purchaserInfo
            info?.let {
                adaptyCallback.invoke(it, DataState.CACHED, null)
            }

            apiClientRepository.getProfile(
                object : AdaptyPurchaserInfoCallback {
                    override fun onResult(response: AttributePurchaserInfoRes?, error: String?) {
                        response?.let {
                            val purchaserInfo = generatePurchaserInfoModel(it)
                            adaptyCallback.invoke(purchaserInfo, DataState.SYNCED, error)
                            preferenceManager.purchaserInfo = purchaserInfo
                        } ?: kotlin.run {
                            adaptyCallback.invoke(null, DataState.SYNCED, error)
                        }

                        if (needQueue) {
                            nextQueue()
                        }
                    }
                }
            )
        }

        @JvmStatic
        fun getPurchaserInfo(
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, state: DataState, error: String?) -> Unit
        ) {
            addToQueue { getPurchaserInfo(true, adaptyCallback) }
        }

        private fun getStartedPaywalls() {
            getPaywallsInQueue(
                false
            ) { containers, products, state, error -> }
        }

        private fun getPromoOnStart() {
            getPromoInQueue(false) { promo, error ->
                if (!error.isNullOrEmpty() || promo == null)
                    return@getPromoInQueue
                onPromoReceivedListener?.onPromoReceived(promo)
            }
        }

        @JvmStatic
        fun getPaywalls(
            adaptyCallback: (paywalls: List<PaywallModel>, products: ArrayList<ProductModel>, state: DataState, error: String?) -> Unit
        ) {
            LogHelper.logVerbose("getPaywalls()")
            addToQueue {
                getPaywallsInQueue(true, adaptyCallback)
            }
        }

        private fun getPaywallsInQueue(
            needQueue: Boolean,
            adaptyCallback: (paywalls: List<PaywallModel>, products: ArrayList<ProductModel>, state: DataState, error: String?) -> Unit
        ) {
            val cntrs = preferenceManager.containers
            cntrs?.toPaywalls()?.let {
                adaptyCallback.invoke(it, preferenceManager.products, DataState.CACHED, null)
            }

            apiClientRepository.getPaywalls(
                object : AdaptyPaywallsCallback {
                    override fun onResult(
                        containers: ArrayList<DataContainer>,
                        products: ArrayList<ProductModel>,
                        error: String?
                    ) {

                        if (!error.isNullOrEmpty()) {
                            adaptyCallback.invoke(arrayListOf(), arrayListOf(), DataState.SYNCED, error)
                            if (needQueue)
                                nextQueue()
                            return
                        }

                        val data: ArrayList<Any> =
                            containers.filterTo(arrayListOf()) { !it.attributes?.products.isNullOrEmpty() }

                        if (data.isEmpty() && products.isEmpty()) {
                            preferenceManager.apply {
                                this.containers = containers
                                this.products = products
                            }
                            adaptyCallback.invoke(containers.toPaywalls(), products, DataState.SYNCED, error)
                            if (needQueue)
                                nextQueue()
                            return
                        }

                        if (products.isNotEmpty())
                            data.add(products)

                        InAppPurchasesInfo(
                            context,
                            data,
                            object : AdaptyPaywallsInfoCallback {
                                override fun onResult(data: ArrayList<Any>, error: String?) {
                                    if (error != null) {
                                        adaptyCallback.invoke(containers.toPaywalls(), products, DataState.SYNCED, error)
                                        if (needQueue)
                                            nextQueue()
                                        return
                                    }

                                    val cArray = ArrayList<DataContainer>()
                                    val pArray = ArrayList<ProductModel>()

                                    for (d in data) {
                                        if (d is DataContainer)
                                            cArray.add(d)
                                        else if (d is ArrayList<*>)
                                            pArray.addAll(d as ArrayList<ProductModel>)
                                    }

                                    val ar =
                                        containers.filterTo(arrayListOf()) { c -> cArray.all { it.id != c.id } }

                                    ar.addAll(0, cArray)

                                    preferenceManager.apply {
                                        this.containers = ar
                                        this.products = pArray
                                    }

                                    adaptyCallback.invoke(ar.toPaywalls(), pArray, DataState.SYNCED, null)
                                    if (needQueue)
                                        nextQueue()
                                }
                            })
                    }

                }
            )
        }

        @JvmStatic
        fun getPromo(
            adaptyCallback: (promo: PromoModel?, error: String?) -> Unit
        ) {
            LogHelper.logVerbose("getPromos()")
            addToQueue {
                getPromoInQueue(true, adaptyCallback)
            }
        }

        private var currentPromo: PromoModel? = null

        private fun getPromoInQueue(
            needQueue: Boolean,
            adaptyCallback: (promo: PromoModel?, error: String?) -> Unit
        ) {
            apiClientRepository.getPromo(
                object : AdaptyPromosCallback {
                    override fun onResult(
                        promo: PromoModel?,
                        error: String?
                    ) {

                        if (!error.isNullOrEmpty() || promo == null) {
                            adaptyCallback.invoke(null, error)
                            if (needQueue)
                                nextQueue()
                            return
                        }

                        fun finishSettingPaywallToPromo(it: PaywallModel) {
                            promo.paywall = it
                            adaptyCallback.invoke(promo, error)
                            if (currentPromo != promo) {
                                currentPromo = promo
                                onPromoReceivedListener?.onPromoReceived(promo)
                            }
                            if (needQueue)
                                nextQueue()
                        }

                        preferenceManager.containers
                            ?.toPaywalls()
                            ?.firstOrNull { it.variationId == promo.variationId }
                            ?.let {
                                finishSettingPaywallToPromo(it)
                            } ?: kotlin.run {
                            getPaywallsInQueue(needQueue) { paywalls, products, state, error ->
                                if (state == DataState.SYNCED) {
                                    if (error.isNullOrEmpty()) {
                                        paywalls
                                            .firstOrNull { it.variationId == promo.variationId }
                                            ?.let {
                                                finishSettingPaywallToPromo(it)
                                            } ?: adaptyCallback.invoke(null, "Paywall not found")
                                    } else {
                                        adaptyCallback.invoke(null, error)
                                    }
                                }
                            }
                        }
                    }

                }
            )
        }

        @JvmStatic
        fun makePurchase(
            activity: Activity,
            product: ProductModel,
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, purchaseToken: String?, googleValidationResult: GoogleValidationResult?, product: ProductModel, error: String?) -> Unit
        ) {
            LogHelper.logVerbose("makePurchase()")
            addToQueue {
                InAppPurchases(
                    context,
                    WeakReference(activity),
                    false,
                    preferenceManager,
                    product,
                    null,
                    apiClientRepository,
                    object : AdaptyPurchaseCallback {
                        override fun onResult(
                            purchase: Purchase?,
                            response: ValidateReceiptResponse?,
                            error: String?
                        ) {
                            val purchaserInfo = response?.data?.attributes
                                ?.let(::generatePurchaserInfoModel)
                                ?.also(::checkChangesPurchaserInfo)
                            val validationResult = response?.data?.attributes?.googleValidationResult

                            adaptyCallback.invoke(purchaserInfo, purchase?.purchaseToken, validationResult, product, error)
                            nextQueue()
                        }
                    })
            }
        }

        @JvmStatic
        @JvmOverloads
        fun syncPurchases(adaptyCallback: ((error: String?) -> Unit)? = null) {
            addToQueue {
                syncPurchasesBody(true, adaptyCallback)
            }
        }

        private fun syncPurchasesBody(
            needQueue: Boolean,
            adaptyCallback: ((String?) -> Unit)?
        ) {
            if (!::preferenceManager.isInitialized)
                preferenceManager = PreferenceManager(context)

            InAppPurchases(context,
                null,
                true,
                preferenceManager,
                ProductModel(),
                null,
                apiClientRepository,
                object : AdaptyRestoreCallback {
                    override fun onResult(response: RestoreReceiptResponse?, error: String?) {
                        if (adaptyCallback != null) {
                            if (error.isNullOrEmpty())
                                adaptyCallback.invoke(null)
                            else
                                adaptyCallback.invoke(error)
                            if (needQueue) {
                                nextQueue()
                            }
                        }
                    }
                })
        }

        @JvmStatic
        fun restorePurchases(
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, googleValidationResultList: List<GoogleValidationResult>?, error: String?) -> Unit
        ) {
            addToQueue {
                if (!::preferenceManager.isInitialized)
                    preferenceManager = PreferenceManager(context)

                InAppPurchases(
                    context,
                    null,
                    true,
                    preferenceManager,
                    ProductModel(),
                    null,
                    apiClientRepository,
                    object : AdaptyRestoreCallback {
                        override fun onResult(response: RestoreReceiptResponse?, error: String?) {
                            val purchaserInfo = response?.data?.attributes
                                ?.let(::generatePurchaserInfoModel)
                                ?.also(::checkChangesPurchaserInfo)
                            val validationResultList = response?.data?.attributes?.googleValidationResult

                            adaptyCallback.invoke(purchaserInfo, validationResultList, error)

                            nextQueue()
                        }
                    })
            }
        }

        @JvmStatic
        @JvmOverloads
        fun validatePurchase(
            purchaseType: PurchaseType,
            productId: String,
            purchaseToken: String,
            purchaseOrderId: String? = null,
            product: ProductModel? = null,
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, purchaseToken: String, googleValidationResult: GoogleValidationResult?, error: String?) -> Unit
        ) {
            if (purchaseOrderId == null && product == null) {
                addToQueue {
                    validate(purchaseType, productId, purchaseToken, purchaseOrderId, product, adaptyCallback)
                }
            } else {
                validate(purchaseType, productId, purchaseToken, purchaseOrderId, product, adaptyCallback)
            }
        }

        private fun validate(
            purchaseType: PurchaseType,
            productId: String,
            purchaseToken: String,
            purchaseOrderId: String? = null,
            product: ProductModel? = null,
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, purchaseToken: String, googleValidationResult: GoogleValidationResult?, error: String?) -> Unit
        ) {
            apiClientRepository.validatePurchase(
                purchaseType.toString(),
                productId,
                purchaseToken,
                purchaseOrderId,
                product,
                object : AdaptyValidateCallback {
                    override fun onResult(
                        response: ValidateReceiptResponse?,
                        error: String?
                    ) {
                        val purchaserInfo = response?.data?.attributes
                            ?.let(::generatePurchaserInfoModel)
                            ?.also(::checkChangesPurchaserInfo)
                        val validationResult = response?.data?.attributes?.googleValidationResult

                        adaptyCallback.invoke(purchaserInfo, purchaseToken, validationResult, error)
                        nextQueue()
                    }
                })
        }

        @JvmStatic
        @JvmOverloads
        fun updateAttribution(
            attribution: Any,
            source: AttributionType,
            networkUserId: String? = null,
            adaptyCallback: (error: String?) -> Unit
        ) {
            LogHelper.logVerbose("updateAttribution()")
            addToQueue {
                apiClientRepository.updateAttribution(
                    attribution,
                    source,
                    networkUserId,
                    object : AdaptySystemCallback {
                        override fun success(response: Any?, reqID: Int) {
                            adaptyCallback.invoke(null)
                            nextQueue()
                        }

                        override fun fail(msg: String, reqID: Int) {
                            adaptyCallback.invoke(msg)
                            nextQueue()
                        }
                    })
            }
        }

        @JvmStatic
        fun logout(adaptyCallback: (error: String?) -> Unit) {
            addToQueue { logoutInQueue(adaptyCallback) }
        }

        private fun logoutInQueue(adaptyCallback: (String?) -> Unit) {
            if (!::context.isInitialized) {
                adaptyCallback.invoke("Adapty was not initialized")
                nextQueue()
                return
            }

            if (!::preferenceManager.isInitialized) {
                preferenceManager = PreferenceManager(context)
            }

            preferenceManager.clearOnLogout()

            activateInQueue(null, adaptyCallback)
        }

        @JvmStatic
        fun refreshPushToken(newToken: String) {
            apiClientRepository.pushToken = newToken
            if (isActivated && preferenceManager.profileID.isNotEmpty()) {
                sendSyncMetaInstallRequest()
            }
        }

        @JvmStatic
        fun handlePromoIntent(
            intent: Intent?,
            adaptyCallback: (promo: PromoModel?, error: String?) -> Unit
        ): Boolean {
            if (intent?.getStringExtra("source") != "adapty") {
                return false
            }
            kinesisManager.trackEvent(
                "promo_push_opened",
                mapOf("promo_delivery_id" to intent.getStringExtra("promo_delivery_id"))
            )
            getPromoInQueue(false, adaptyCallback)
            return true
        }

        @JvmStatic
        fun setOnPurchaserInfoUpdatedListener(onPurchaserInfoUpdatedListener: OnPurchaserInfoUpdatedListener?) {
            this.onPurchaserInfoUpdatedListener = onPurchaserInfoUpdatedListener
        }

        @JvmStatic
        fun setOnPromoReceivedListener(onPromoReceivedListener: OnPromoReceivedListener?) {
            this.onPromoReceivedListener = onPromoReceivedListener
        }

        @JvmStatic
        fun setLogLevel(logLevel: AdaptyLogLevel) {
            LogHelper.setLogLevel(logLevel)
        }
    }
}