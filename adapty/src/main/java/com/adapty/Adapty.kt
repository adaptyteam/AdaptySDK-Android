package com.adapty

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.adapty.api.*
import com.adapty.api.entity.paywalls.*
import com.adapty.api.entity.profile.update.ProfileParameterBuilder
import com.adapty.api.entity.purchaserInfo.*
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.api.entity.validate.GoogleValidationResult
import com.adapty.api.responses.*
import com.adapty.purchase.InAppPurchases
import com.adapty.purchase.InAppPurchasesInfo
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
        private var activateRequested = false
        private var readyToActivate = false
        private var isRunning = false
        private val kinesisManager by lazy {
            KinesisManager(preferenceManager)
        }
        private val liveTracker by lazy {
            AdaptyLiveTracker(kinesisManager, apiClientRepository, ::checkChangesPurchaserInfo)
        }
        private val gson = Gson()
        private val apiClientRepository by lazy {
            ApiClientRepository(preferenceManager, gson)
        }

        init {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object: LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
                fun onCreate() {
                    readyToActivate = true
                    if (activateRequested && requestQueue.isNotEmpty()) {
                        isRunning = true
                        requestQueue[0].invoke()
                    }
                    activateRequested = false
                }
            })
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
            adaptyCallback: ((AdaptyError?) -> Unit)?
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

            if (!readyToActivate) activateRequested = true
            addToQueue(readyToActivate) {
                activateInQueue(customerUserId, adaptyCallback)
            }
        }

        private fun activateInQueue(
            customerUserId: String?,
            adaptyCallback: ((AdaptyError?) -> Unit)?
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
                            }
                        }

                        makeStartRequests(adaptyCallback)

                    }

                    override fun fail(error: AdaptyError, reqID: Int) {
                        isRunning = false
                        adaptyCallback?.invoke(error)
                    }
                })
            } else {
                makeStartRequests(adaptyCallback)
            }
        }

        private fun nextQueue() {
            if (requestQueue.isNotEmpty())
                requestQueue.removeAt(0)

            if (requestQueue.isNotEmpty()) {
                isRunning = true
                requestQueue.first().invoke()
            } else {
                isRunning = false
            }
        }

        private fun addToQueue(isReady: Boolean = true, action: () -> Unit) {
            requestQueue.add(action)

            if (isReady && shouldRunImmediately()) {
                isRunning = true
                requestQueue[0].invoke()
            }
        }

        private fun shouldRunImmediately() =
            if (isRunning) requestQueue.size == 1 else requestQueue.isNotEmpty()

        private fun makeStartRequests(adaptyCallback: ((error: AdaptyError?) -> Unit)?) {
            sendSyncMetaInstallRequest {
                getStartedPaywalls()
                getPromoOnStart()
                syncPurchasesBody(false) { _ ->
                    getPurchaserInfoInternal(true) { info, error ->
                        adaptyCallback?.invoke(error)
                    }
                }
                apiClientRepository.syncAttributions()
            }
        }

        private fun checkChangesPurchaserInfo(res: AttributePurchaserInfoRes)
                = checkChangesPurchaserInfo(generatePurchaserInfoModel(res))

        private fun checkChangesPurchaserInfo(purchaserInfo: PurchaserInfoModel) {
            if (isPurchaserInfoChanged(purchaserInfo)) {
                onPurchaserInfoUpdatedListener?.onPurchaserInfoReceived(purchaserInfo)
                preferenceManager.purchaserInfo = purchaserInfo
            }
        }

        private fun isPurchaserInfoChanged(info: PurchaserInfoModel)
                = preferenceManager.purchaserInfo != info

        private fun sendSyncMetaInstallRequest(callback: ((error: AdaptyError?) -> Unit)? = null) {
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
                        callback?.invoke(null)
                    }
                }

                override fun fail(error: AdaptyError, reqID: Int) {
                    callback?.invoke(error)
                }

            })
        }

        @JvmStatic
        fun identify(customerUserId: String, adaptyCallback: (error: AdaptyError?) -> Unit) {
            LogHelper.logVerbose("identify()")
            addToQueue { identifyInQueue(customerUserId, adaptyCallback) }
        }

        private fun identifyInQueue(
            customerUserId: String,
            adaptyCallback: (error: AdaptyError?) -> Unit
        ) {
            if (customerUserId.isBlank()) {
                LogHelper.logError("customerUserId should not be empty")
                adaptyCallback.invoke(AdaptyError(message = "customerUserId should not be empty", adaptyErrorCode = AdaptyErrorCode.EMPTY_PARAMETER))
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

                    if (!profileIdChanged) {
                        adaptyCallback.invoke(null)
                        nextQueue()
                        return
                    }

                    preferenceManager.products = arrayListOf()
                    preferenceManager.containers = null

                    makeStartRequests(adaptyCallback)
                }

                override fun fail(error: AdaptyError, reqID: Int) {
                    adaptyCallback.invoke(error)

                    nextQueue()
                }

            })
        }

        @JvmStatic
        fun updateProfile(params: ProfileParameterBuilder, adaptyCallback: (error: AdaptyError?) -> Unit) {
            addToQueue {
                apiClientRepository.updateProfile(
                    params.email,
                    params.phoneNumber,
                    params.facebookUserId,
                    params.facebookAnonymousId,
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
                        override fun onResult(response: UpdateProfileResponse?, error: AdaptyError?) {
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

        private fun getPurchaserInfoInternal(
            needQueue: Boolean,
            adaptyCallback: ((purchaserInfo: PurchaserInfoModel?, error: AdaptyError?) -> Unit)?
        ) {
            apiClientRepository.getProfile(
                object : AdaptyPurchaserInfoCallback {
                    override fun onResult(response: AttributePurchaserInfoRes?, error: AdaptyError?) {
                        response?.let {
                            val purchaserInfo = generatePurchaserInfoModel(it)
                            adaptyCallback?.invoke(purchaserInfo, error)
                            checkChangesPurchaserInfo(purchaserInfo)
                        } ?: kotlin.run {
                            adaptyCallback?.invoke(null, error)
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
            forceUpdate: Boolean = false,
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, error: AdaptyError?) -> Unit
        ) {
            if (forceUpdate) {
                addToQueue { getPurchaserInfoInternal(true, adaptyCallback) }
            } else {
                preferenceManager.purchaserInfo?.let {
                    adaptyCallback.invoke(it, null)
                    getPurchaserInfoInternal(false, null)
                } ?: kotlin.run {
                    addToQueue { getPurchaserInfoInternal(true, adaptyCallback) }
                }
            }
        }

        private fun getStartedPaywalls() {
            getPaywallsInQueue(
                false
            ) { paywalls, products, error ->
                paywallsSyncedDuringThisSession = true
                paywallsSyncCallbackOnStart?.let { callback ->
                    if (error != null) {
                        callback.invoke(paywalls, products, error)
                    } else {
                        getPaywalls(true, callback)
                    }
                }
                paywallsSyncCallbackOnStart = null
            }
        }

        private fun getPromoOnStart() {
            getPromoInQueue(false) { promo, error ->
                if (error != null || promo == null)
                    return@getPromoInQueue
                onPromoReceivedListener?.onPromoReceived(promo)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun getPaywalls(
            forceUpdate: Boolean = false,
            adaptyCallback: (paywalls: List<PaywallModel>, products: ArrayList<ProductModel>, error: AdaptyError?) -> Unit
        ) {
            LogHelper.logVerbose("getPaywalls()")
            when {
                forceUpdate -> addToQueue { getPaywallsInQueue(true, adaptyCallback) }
                !paywallsSyncedDuringThisSession -> {
                    paywallsSyncCallbackOnStart = adaptyCallback
                    if (readyToActivate && shouldRunImmediately()) {
                        isRunning = true
                        requestQueue[0].invoke()
                    }
                }
                else -> preferenceManager.containers?.toPaywalls()?.let {
                    adaptyCallback.invoke(it, preferenceManager.products, null)
                } ?: kotlin.run {
                    addToQueue { getPaywallsInQueue(true, adaptyCallback) }
                }
            }
        }

        private fun getPaywallsInQueue(
            needQueue: Boolean,
            adaptyCallback: (paywalls: List<PaywallModel>, products: ArrayList<ProductModel>, error: AdaptyError?) -> Unit
        ) {
            apiClientRepository.getPaywalls(
                object : AdaptyPaywallsCallback {
                    override fun onResult(
                        containers: ArrayList<DataContainer>,
                        products: ArrayList<ProductModel>,
                        error: AdaptyError?
                    ) {

                        if (error != null) {
                            adaptyCallback.invoke(arrayListOf(), arrayListOf(), error)
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
                            adaptyCallback.invoke(containers.toPaywalls(), products, error)
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
                                override fun onResult(data: ArrayList<Any>, error: AdaptyError?) {
                                    if (error != null) {
                                        adaptyCallback.invoke(containers.toPaywalls(), products, error)
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

                                    adaptyCallback.invoke(ar.toPaywalls(), pArray, null)
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
            adaptyCallback: (promo: PromoModel?, error: AdaptyError?) -> Unit
        ) {
            LogHelper.logVerbose("getPromos()")
            addToQueue {
                getPromoInQueue(true, adaptyCallback)
            }
        }

        private var currentPromo: PromoModel? = null
        private var paywallsSyncedDuringThisSession = false
        private var paywallsSyncCallbackOnStart: ((paywalls: List<PaywallModel>, products: ArrayList<ProductModel>, error: AdaptyError?) -> Unit)? = null

        private fun getPromoInQueue(
            needQueue: Boolean,
            adaptyCallback: (promo: PromoModel?, error: AdaptyError?) -> Unit
        ) {
            apiClientRepository.getPromo(
                object : AdaptyPromosCallback {
                    override fun onResult(
                        promo: PromoModel?,
                        error: AdaptyError?
                    ) {

                        if (error != null || promo == null) {
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
                            getPaywallsInQueue(needQueue) { paywalls, products, error ->
                                if (error == null) {
                                    paywalls
                                        .firstOrNull { it.variationId == promo.variationId }
                                        ?.let {
                                            finishSettingPaywallToPromo(it)
                                        } ?: adaptyCallback.invoke(null, AdaptyError(message = "Paywall not found", adaptyErrorCode = AdaptyErrorCode.PAYWALL_NOT_FOUND))
                                } else {
                                    adaptyCallback.invoke(null, error)
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
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, purchaseToken: String?, googleValidationResult: GoogleValidationResult?, product: ProductModel, error: AdaptyError?) -> Unit
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
                            error: AdaptyError?
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

        @Deprecated(message = "Will be removed in newer versions", level = DeprecationLevel.WARNING)
        @JvmStatic
        @JvmOverloads
        fun syncPurchases(adaptyCallback: ((error: AdaptyError?) -> Unit)? = null) {
            addToQueue {
                syncPurchasesBody(true, adaptyCallback)
            }
        }

        private fun syncPurchasesBody(
            needQueue: Boolean,
            adaptyCallback: ((error: AdaptyError?) -> Unit)?
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
                    override fun onResult(response: RestoreReceiptResponse?, error: AdaptyError?) {
                        if (adaptyCallback != null) {
                            if (error == null)
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
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, googleValidationResultList: List<GoogleValidationResult>?, error: AdaptyError?) -> Unit
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
                        override fun onResult(response: RestoreReceiptResponse?, error: AdaptyError?) {
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
        fun updateAttribution(
            attribution: Any,
            source: AttributionType,
            networkUserId: String? = null,
            adaptyCallback: (error: AdaptyError?) -> Unit
        ) {
            LogHelper.logVerbose("updateAttribution()")
            if (!::context.isInitialized) {
                adaptyCallback.invoke(AdaptyError(message = "Adapty was not initialized", adaptyErrorCode = AdaptyErrorCode.ADAPTY_NOT_INITIALIZED))
                return
            }

            if (!::preferenceManager.isInitialized) {
                preferenceManager = PreferenceManager(context)
            }

            val attributionData = createAttributionData(attribution, source, networkUserId)
            preferenceManager.saveAttributionData(attributionData)

            addToQueue {
                apiClientRepository.updateAttribution(attributionData) { error ->
                    adaptyCallback.invoke(error)
                    nextQueue()
                }
            }
        }

        @JvmStatic
        fun setExternalAnalyticsEnabled(enabled: Boolean, adaptyCallback: (error: AdaptyError?) -> Unit) {
            LogHelper.logVerbose("setExternalAnalyticsEnabled()")
            if (!::context.isInitialized) {
                LogHelper.logError("Adapty was not initialized")
                return
            }

            addToQueue {
                apiClientRepository.setExternalAnalyticsEnabled(enabled) { error ->
                    adaptyCallback.invoke(error)
                    nextQueue()
                }
            }
        }

        @JvmStatic
        fun setTransactionVariationId(transactionId: String, variationId: String, adaptyCallback: (error: AdaptyError?) -> Unit) {
            LogHelper.logVerbose("setTransactionVariationId()")
            if (!::context.isInitialized) {
                adaptyCallback.invoke(AdaptyError(message = "Adapty was not initialized", adaptyErrorCode = AdaptyErrorCode.ADAPTY_NOT_INITIALIZED))
                return
            }

            addToQueue {
                apiClientRepository.setTransactionVariationId(transactionId, variationId) { error ->
                    adaptyCallback.invoke(error)
                    nextQueue()
                }
            }
        }

        @JvmStatic
        fun logout(adaptyCallback: (error: AdaptyError?) -> Unit) {
            addToQueue { logoutInQueue(adaptyCallback) }
        }

        private fun logoutInQueue(adaptyCallback: (error: AdaptyError?) -> Unit) {
            if (!::context.isInitialized) {
                adaptyCallback.invoke(AdaptyError(message = "Adapty was not initialized", adaptyErrorCode = AdaptyErrorCode.ADAPTY_NOT_INITIALIZED))
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
            adaptyCallback: (promo: PromoModel?, error: AdaptyError?) -> Unit
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

        @JvmStatic
        fun logShowPaywall(paywall: PaywallModel) {
            kinesisManager.trackEvent(
                "paywall_showed",
                mapOf(
                    "is_promo" to "${paywall.isPromo ?: false}",
                    "variation_id" to paywall.variationId.orEmpty()
                )
            )
        }
    }
}