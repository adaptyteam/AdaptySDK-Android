package com.adapty

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import com.adapty.api.*
import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.OnPromoReceivedListener
import com.adapty.api.entity.containers.Product
import com.adapty.api.entity.containers.Promo
import com.adapty.api.entity.profile.update.ProfileParameterBuilder
import com.adapty.api.entity.purchaserInfo.*
import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel
import com.adapty.api.responses.CreateProfileResponse
import com.adapty.api.responses.RestoreReceiptResponse
import com.adapty.api.responses.SyncMetaInstallResponse
import com.adapty.api.responses.ValidateReceiptResponse
import com.adapty.purchase.InAppPurchases
import com.adapty.purchase.InAppPurchasesInfo
import com.adapty.utils.*
import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import kotlin.collections.ArrayList

class Adapty {

    companion object {
        lateinit var context: Context
        private lateinit var preferenceManager: PreferenceManager
        private var onPurchaserInfoUpdatedListener: OnPurchaserInfoUpdatedListener? = null
        private var onPromoReceivedListener: OnPromoReceivedListener? = null
        private var requestQueue: ArrayList<() -> Unit> = arrayListOf()
        private var isActivated = false
        private var kinesisManager: KinesisManager? = null
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

            isActivated = true

            this.context = context
            this.preferenceManager = PreferenceManager(this.context)
            this.preferenceManager.appKey = appKey

            addToQueue {
                activateInQueue(context, appKey, customerUserId, adaptyCallback)
            }
        }

        private fun activateInQueue(
            context: Context,
            appKey: String,
            customerUserId: String?,
            adaptyCallback: ((String?) -> Unit)?
        ) {
            this.context = context
            this.preferenceManager = PreferenceManager(this.context)
            this.preferenceManager.appKey = appKey

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
            requestQueue.add { action() }

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

                    if (isPurchaserInfoChanged(info))
                        info?.let {
                            onPurchaserInfoUpdatedListener?.didReceiveUpdatedPurchaserInfo(it)
                        }
                }
            }
        }

        private fun checkChangesPurchaserInfo(res: AttributePurchaserInfoRes) {
            val purchaserInfo = generatePurchaserInfoModel(res)
            if (isPurchaserInfoChanged(purchaserInfo)) {
                onPurchaserInfoUpdatedListener?.didReceiveUpdatedPurchaserInfo(purchaserInfo)
            }
            preferenceManager.purchaserInfo = purchaserInfo
        }

        private fun isPurchaserInfoChanged(info: PurchaserInfoModel?): Boolean {
            val cachedInfo = preferenceManager.purchaserInfo
            if (cachedInfo == null && info != null)
                return true

            cachedInfo?.let { cached ->
                info?.let { synced ->
                    if (cached == synced)
                        return false

                    return true
                }
            }

            return false
        }

        @Deprecated(
            message = "Changed signature",
            replaceWith = ReplaceWith(
                expression = "Adapty.sendSyncMetaInstallRequest()"
            ),
            level = DeprecationLevel.WARNING
        )
        fun sendSyncMetaInstallRequest(applicationContext: Context) = sendSyncMetaInstallRequest()

        @JvmStatic
        fun sendSyncMetaInstallRequest() {
            LogHelper.logVerbose("sendSyncMetaInstallRequest()")
            apiClientRepository.syncMetaInstall(object : AdaptySystemCallback {
                override fun success(response: Any?, reqID: Int) {
                    if (response is SyncMetaInstallResponse) {
                        response.data?.let { data ->
                            data.attributes?.let { attrs ->
                                attrs.iamAccessKeyId?.let {
                                    preferenceManager.iamAccessKeyId = it
                                }
                                attrs.iamSecretKey?.let {
                                    preferenceManager.iamSecretKey = it
                                }
                                attrs.iamSessionToken?.let {
                                    preferenceManager.iamSessionToken = it
                                }
                            }
                        }

                        setupTrackingEvent()
                    }
                }

                override fun fail(msg: String, reqID: Int) {
                }

            })
        }

        private val handlerEvent = Handler()
        private const val TRACKING_INTERVAL = (60 * 1000).toLong()

        private fun setupTrackingEvent() {
            handlerEvent.removeCallbacksAndMessages(null)
            handlerEvent.post {
                if (kinesisManager == null) kinesisManager = KinesisManager(preferenceManager)
                kinesisManager?.trackEvent("live")
                handlerEvent.postDelayed({
                    setupTrackingEvent()
                }, TRACKING_INTERVAL)
            }
        }

        @JvmStatic
        fun identify(customerUserId: String?, adaptyCallback: (String?) -> Unit) {
            LogHelper.logVerbose("identify()")
            addToQueue { identifyInQueue(customerUserId, adaptyCallback) }
        }

        private fun identifyInQueue(
            customerUserId: String?,
            adaptyCallback: (String?) -> Unit
        ) {
            if (!customerUserId.isNullOrEmpty() && preferenceManager.customerUserID.isNotEmpty()) {
                if (customerUserId == preferenceManager.customerUserID) {
                    adaptyCallback.invoke(null)
                    nextQueue()
                    return
                }
            }

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

                    adaptyCallback.invoke(null)

                    nextQueue()

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

        @Deprecated(
            message = "Changed signature",
            replaceWith = ReplaceWith(
                expression = "Adapty.updateProfile(params,adaptyCallback)"
            ),
            level = DeprecationLevel.WARNING
        )
        @JvmStatic
        @JvmOverloads
        fun updateProfile(
            customerUserId: String?,
            email: String?,
            phoneNumber: String?,
            facebookUserId: String?,
            mixpanelUserId: String?,
            amplitudeUserId: String?,
            amplitudeDeviceId: String?,
            appsflyerId: String?,
            appmetricaProfileId: String? = null,
            appmetricaDeviceId: String? = null,
            firstName: String?,
            lastName: String?,
            gender: String?,
            birthday: String?,
            customAttributes: Map<String, Any>? = null,
            adaptyCallback: (String?) -> Unit
        ) {
            addToQueue {
                apiClientRepository.updateProfile(
                    email,
                    phoneNumber,
                    facebookUserId,
                    mixpanelUserId,
                    amplitudeUserId,
                    amplitudeDeviceId,
                    appmetricaProfileId,
                    appmetricaDeviceId,
                    firstName,
                    lastName,
                    gender,
                    birthday,
                    customAttributes,
                    object : AdaptyProfileCallback {
                        override fun onResult(error: String?) {
                            adaptyCallback.invoke(error)
                            nextQueue()
                        }

                    }
                )
            }
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
                        override fun onResult(error: String?) {
                            adaptyCallback.invoke(error)
                            nextQueue()
                        }

                    }
                )
            }
        }

        private fun getPurchaserInfo(
            needQueue: Boolean,
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, state: String, error: String?) -> Unit
        ) {
            val info = preferenceManager.purchaserInfo
            info?.let {
                adaptyCallback.invoke(it, "cached", null)
            }

            apiClientRepository.getProfile(
                object : AdaptyPurchaserInfoCallback {
                    override fun onResult(response: AttributePurchaserInfoRes?, error: String?) {
                        response?.let {
                            val purchaserInfo = generatePurchaserInfoModel(it)
                            adaptyCallback.invoke(purchaserInfo, "synced", error)
                            preferenceManager.purchaserInfo = purchaserInfo
                        } ?: kotlin.run {
                            adaptyCallback.invoke(null, "synced", error)
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
            adaptyCallback: (purchaserInfo: PurchaserInfoModel?, state: String, error: String?) -> Unit
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

        @Deprecated(
            message = "Renamed to getPaywalls and changed signature",
            replaceWith = ReplaceWith(
                expression = "Adapty.getPaywalls(adaptyCallback)"
            ),
            level = DeprecationLevel.WARNING
        )
        fun getPurchaseContainers(
            context: Context,
            adaptyCallback: (containers: ArrayList<DataContainer>, products: ArrayList<Product>, state: String, error: String?) -> Unit
        ) = getPaywalls(adaptyCallback)

        @JvmStatic
        fun getPaywalls(
            adaptyCallback: (paywalls: ArrayList<DataContainer>, products: ArrayList<Product>, state: String, error: String?) -> Unit
        ) {
            LogHelper.logVerbose("getPaywalls()")
            addToQueue {
                getPaywallsInQueue(true, adaptyCallback)
            }
        }

        private fun getPaywallsInQueue(
            needQueue: Boolean,
            adaptyCallback: (containers: ArrayList<DataContainer>, products: ArrayList<Product>, state: String, error: String?) -> Unit
        ) {
            val cntrs = preferenceManager.containers
            cntrs?.let {
                adaptyCallback.invoke(it, preferenceManager.products, "cached", null)
            }

            apiClientRepository.getPaywalls(
                object : AdaptyPaywallsCallback {
                    override fun onResult(
                        containers: ArrayList<DataContainer>,
                        products: ArrayList<Product>,
                        error: String?
                    ) {

                        if (!error.isNullOrEmpty()) {
                            adaptyCallback.invoke(arrayListOf(), arrayListOf(), "synced", error)
                            if (needQueue)
                                nextQueue()
                            return
                        }

                        val data = ArrayList<Any>()

                        var isContainersEmpty = true
                        for (c in containers) {
                            c.attributes?.products?.let {
                                if (it.isNotEmpty()) {
                                    isContainersEmpty = false
                                    data.add(c)
                                }
                            }
                        }

                        if (isContainersEmpty && products.isEmpty()) {
                            preferenceManager.apply {
                                this.containers = containers
                                this.products = products
                            }
                            adaptyCallback.invoke(containers, products, "synced", error)
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
                                        adaptyCallback.invoke(containers, products, "synced", error)
                                        if (needQueue)
                                            nextQueue()
                                        return
                                    }

                                    val cArray = ArrayList<DataContainer>()
                                    val pArray = ArrayList<Product>()

                                    for (d in data) {
                                        if (d is DataContainer)
                                            cArray.add(d)
                                        else if (d is ArrayList<*>)
                                            pArray.addAll(d as ArrayList<Product>)
                                    }

                                    val ar = arrayListOf<DataContainer>()

                                    for (c in containers) {
                                        var isContains = false
                                        for (d in cArray) {
                                            if (c.id == d.id)
                                                isContains = true
                                        }
                                        if (!isContains)
                                            ar.add(c)
                                    }

                                    ar.addAll(0, cArray)

                                    preferenceManager.apply {
                                        this.containers = ar
                                        this.products = pArray
                                    }

                                    adaptyCallback.invoke(ar, pArray, "synced", null)
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
            adaptyCallback: (promo: Promo?, error: String?) -> Unit
        ) {
            LogHelper.logVerbose("getPromos()")
            addToQueue {
                getPromoInQueue(true, adaptyCallback)
            }
        }

        private var currentPromo: Promo? = null

        private fun getPromoInQueue(
            needQueue: Boolean,
            adaptyCallback: (promo: Promo?, error: String?) -> Unit
        ) {
            apiClientRepository.getPromo(
                object : AdaptyPromosCallback {
                    override fun onResult(
                        promo: Promo?,
                        error: String?
                    ) {

                        if (!error.isNullOrEmpty() || promo == null) {
                            adaptyCallback.invoke(null, error)
                            if (needQueue)
                                nextQueue()
                            return
                        }

                        fun finishSettingPaywallToPromo(it: DataContainer) {
                            promo.paywall = it.attributes
                            adaptyCallback.invoke(promo, error)
                            if (currentPromo != promo) {
                                currentPromo = promo
                                onPromoReceivedListener?.onPromoReceived(promo)
                            }
                            if (needQueue)
                                nextQueue()
                        }

                        preferenceManager.containers
                            ?.firstOrNull { it.attributes?.variationId == promo.variationId }
                            ?.let {
                                finishSettingPaywallToPromo(it)
                            } ?: kotlin.run {
                            getPaywallsInQueue(needQueue) { containers, products, state, error ->
                                if (state == "synced") {
                                    if (error.isNullOrEmpty()) {
                                        containers
                                            .firstOrNull { it.attributes?.variationId == promo.variationId }
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
            product: Product,
            adaptyCallback: (Purchase?, ValidateReceiptResponse?, String?) -> Unit
        ) = makePurchase(activity, product, null, adaptyCallback)

        @Deprecated(
            message = "Changed signature",
            replaceWith = ReplaceWith(
                expression = "Adapty.makePurchase(activity,product,adaptyCallback)"
            ),
            level = DeprecationLevel.WARNING
        )
        @JvmStatic
        fun makePurchase(
            activity: Activity,
            product: Product,
            variationId: String?,
            adaptyCallback: (Purchase?, ValidateReceiptResponse?, String?) -> Unit
        ) {
            LogHelper.logVerbose("makePurchase()")
            addToQueue {
                InAppPurchases(
                    context,
                    activity,
                    false,
                    preferenceManager,
                    product,
                    variationId,
                    null,
                    object : AdaptyPurchaseCallback {
                        override fun onResult(
                            purchase: Purchase?,
                            response: ValidateReceiptResponse?,
                            error: String?
                        ) {
                            adaptyCallback.invoke(purchase, response, error)
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
                Product(),
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

        @Deprecated(
            message = "Changed signature",
            replaceWith = ReplaceWith(
                expression = "Adapty.restorePurchases(adaptyCallback)"
            ),
            level = DeprecationLevel.WARNING
        )
        fun restorePurchases(
            activity: Activity,
            adaptyCallback: (RestoreReceiptResponse?, String?) -> Unit
        ) = restorePurchases(adaptyCallback)

        @JvmStatic
        fun restorePurchases(
            adaptyCallback: (RestoreReceiptResponse?, String?) -> Unit
        ) {
            addToQueue {
                if (!::preferenceManager.isInitialized)
                    preferenceManager = PreferenceManager(context)

                InAppPurchases(
                    context,
                    null,
                    true,
                    preferenceManager,
                    Product(),
                    null,
                    apiClientRepository,
                    object : AdaptyRestoreCallback {
                        override fun onResult(response: RestoreReceiptResponse?, error: String?) {
                            adaptyCallback.invoke(response, error)

                            nextQueue()

                            response?.data?.attributes?.apply {
                                checkChangesPurchaserInfo(this)
                            }
                        }
                    })
            }
        }

        @JvmStatic
        @JvmOverloads
        fun validatePurchase(
            purchaseType: String,
            productId: String,
            purchaseToken: String,
            purchaseOrderId: String? = null,
            product: Product? = null,
            adaptyCallback: (ValidateReceiptResponse?, error: String?) -> Unit
        ) {
            validate(
                purchaseType,
                productId,
                purchaseToken,
                purchaseOrderId,
                product,
                adaptyCallback
            )
        }

        private fun validate(
            purchaseType: String,
            productId: String,
            purchaseToken: String,
            purchaseOrderId: String? = null,
            product: Product? = null,
            adaptyCallback: (ValidateReceiptResponse?, error: String?) -> Unit
        ) {
            if (purchaseOrderId == null && product == null) {
                addToQueue {
                    apiClientRepository.validatePurchase(
                        purchaseType,
                        productId,
                        purchaseToken,
                        purchaseOrderId,
                        product,
                        object : AdaptyValidateCallback {
                            override fun onResult(
                                response: ValidateReceiptResponse?,
                                error: String?
                            ) {
                                adaptyCallback.invoke(response, error)

                                response?.data?.attributes?.apply {
                                    checkChangesPurchaserInfo(this)
                                }
                                nextQueue()
                            }
                        })
                }
            } else {
                apiClientRepository.validatePurchase(
                    purchaseType,
                    productId,
                    purchaseToken,
                    purchaseOrderId,
                    product,
                    object : AdaptyValidateCallback {
                        override fun onResult(
                            response: ValidateReceiptResponse?,
                            error: String?
                        ) {
                            adaptyCallback.invoke(response, error)

                            response?.data?.attributes?.apply {
                                checkChangesPurchaserInfo(this)
                            }
                        }
                    })
            }
        }

        @JvmStatic
        fun updateAttribution(
            attribution: Any,
            source: String
        ) {
            updateAttribution(attribution, source, null)
        }

        @JvmStatic
        fun updateAttribution(
            attribution: Any,
            source: String,
            networkUserId: String?
        ) {
            LogHelper.logVerbose("updateAttribution()")
            addToQueue {
                apiClientRepository.updateAttribution(
                    attribution,
                    source,
                    networkUserId,
                    object : AdaptyProfileCallback {
                        override fun onResult(error: String?) {
                            nextQueue()
                        }

                    })
            }
        }

        @JvmStatic
        fun logout(adaptyCallback: (String?) -> Unit) {
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

            preferenceManager.customerUserID = ""
            preferenceManager.installationMetaID = ""
            preferenceManager.profileID = ""
            preferenceManager.containers = null
            preferenceManager.products = arrayListOf()

            activateInQueue(context, preferenceManager.appKey, null, adaptyCallback)
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
            adaptyCallback: (promo: Promo?, error: String?) -> Unit
        ): Boolean {
            if (intent?.getStringExtra("source") != "adapty") {
                return false
            }
            if (kinesisManager == null) kinesisManager = KinesisManager(preferenceManager)
            kinesisManager?.trackEvent(
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
        fun setLogLevel(logLevel: LogLevel) {
            LogHelper.setLogLevel(logLevel)
        }
    }
}