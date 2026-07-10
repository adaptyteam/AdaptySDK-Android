@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.internal.crossplatform

import android.content.Context
import com.adapty.Adapty
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.crossplatform.ui.AdaptyUiBridgeError
import com.adapty.internal.crossplatform.ui.CreateOnboardingViewArgs
import com.adapty.internal.crossplatform.ui.CreateFlowViewArgs
import com.adapty.internal.crossplatform.ui.CrossplatformUiHelper
import com.adapty.internal.crossplatform.ui.DismissViewArgs
import com.adapty.internal.crossplatform.ui.PresentViewArgs
import com.adapty.internal.crossplatform.ui.ShowDialogArgs
import com.adapty.internal.utils.DEFAULT_PLACEMENT_TIMEOUT
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptySdkVersion
import com.adapty.listeners.OnInstallationDetailsListener
import com.adapty.listeners.OnProfileUpdatedListener
import com.adapty.models.AdaptyAttributionSource
import com.adapty.models.AdaptyInstallationDetails
import com.adapty.models.AdaptyIntegrationIdentifier
import com.adapty.models.AdaptyPlacementFetchPolicy
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyFlowDefaultEventListener
import com.adapty.utils.AdaptyResult
import com.adapty.utils.TransactionInfo

internal class AdaptyCallHandler(
    private val appContext: Context,
    private val serializationHelper: SerializationHelper,
    private val uiHelper: CrossplatformUiHelper,
    private val urlHandler: UrlHandler,
    val onNewEvent: MutableMap<String?, EventCallback<String>>,
    private val transformFileLocation: FileLocationTransformer,
) {

    var activity: ActivityProvider = ActivityProvider.Empty
        set(value) {
            field = value
            uiHelper.activity = value
        }

    fun onMethodCall(
        argument: Any?,
        methodName: String?,
        onResult: ResultCallback<String>,
    ) {
        val method = methodName ?: parseJsonArgument<MethodArg>(argument)?.method ?: kotlin.run {
            onResult(methodNotFoundError())
            return
        }

        when (method) {
            ACTIVATE -> handleActivate(argument, onResult)
            IDENTIFY -> handleIdentify(argument, onResult)
            SET_LOG_LEVEL -> handleSetLogLevel(argument, onResult)
            LOG_SHOW_FLOW -> handleLogShowFlow(argument, onResult)
            GET_FLOW -> handleGetFlow(argument, onResult)
            GET_FLOW_FOR_DEFAULT_AUDIENCE -> handleGetFlowForDefaultAudience(argument, onResult)
            GET_PAYWALL_PRODUCTS -> handleGetPaywallProducts(argument, onResult)
            GET_ONBOARDING -> handleGetOnboarding(argument, onResult)
            GET_ONBOARDING_FOR_DEFAULT_AUDIENCE -> handleGetOnboardingForDefaultAudience(argument, onResult)
            SET_FALLBACK -> handleSetFallback(argument, onResult)
            OPEN_WEB_PAYWALL -> handleOpenWebPaywall(argument, onResult)
            CREATE_WEB_PAYWALL_URL -> handleCreateWebPaywallUrl(argument, onResult)
            MAKE_PURCHASE -> handleMakePurchase(argument, onResult)
            RESTORE_PURCHASES -> handleRestorePurchases(onResult)
            GET_PROFILE -> handleGetProfile(onResult)
            SET_INTEGRATION_ID -> handleSetIntegrationId(argument, onResult)
            UPDATE_ATTRIBUTION -> handleUpdateAttribution(argument, onResult)
            UPDATE_PROFILE -> handleUpdateProfile(argument, onResult)
            REPORT_TRANSACTION -> handleReportTransaction(argument, onResult)
            GET_CURRENT_INSTALLATION_STATUS -> handleGetCurrentInstallationStatus(onResult)
            LOGOUT -> handleLogout(onResult)
            IS_ACTIVATED -> handleIsActivated(onResult)
            GET_SDK_VERSION -> handleGetSdkVersion(onResult)
            ADAPTY_UI_CREATE_FLOW_VIEW -> handleCreateFlowView(argument, onResult)
            ADAPTY_UI_PRESENT_FLOW_VIEW -> handlePresentFlowView(argument, onResult)
            ADAPTY_UI_DISMISS_FLOW_VIEW -> handleDismissFlowView(argument, onResult)
            ADAPTY_UI_CREATE_ONBOARDING_VIEW -> handleCreateOnboardingView(argument, onResult)
            ADAPTY_UI_PRESENT_ONBOARDING_VIEW -> handlePresentOnboardingView(argument, onResult)
            ADAPTY_UI_DISMISS_ONBOARDING_VIEW -> handleDismissOnboardingView(argument, onResult)
            ADAPTY_UI_SHOW_DIALOG -> handleShowDialog(argument, onResult)
            ADAPTY_UI_OPEN_URL -> handleAdaptyUiOpenUrl(argument, onResult)
            ADAPTY_UI_REQUEST_APP_REVIEW -> handleAdaptyUiRequestAppReview(onResult)
            FLOW_VIEW_DID_ANSWER_PERMISSION -> handleFlowViewDidAnswerPermission(argument, onResult)
            OBSERVER_PURCHASE_DID_START -> handleObserverPurchaseDidStart(argument, onResult)
            OBSERVER_PURCHASE_DID_FINISH -> handleObserverPurchaseDidFinish(argument, onResult)
            OBSERVER_RESTORE_DID_START -> handleObserverRestoreDidStart(argument, onResult)
            OBSERVER_RESTORE_DID_FINISH -> handleObserverRestoreDidFinish(argument, onResult)
            else -> onResult(methodNotFoundError(method))
        }
    }

    private fun handleActivate(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<ActivateArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(ACTIVATE))
            return
        }

        val config = args.configuration

        if (!config.crossPlatformSdkName.isNullOrEmpty() && !config.crossPlatformSdkVersion.isNullOrEmpty()) {
            CrossplatformHelper.meta = MetaInfo.from(config.crossPlatformSdkName, config.crossPlatformSdkVersion)
        }

        config.logLevel?.let { logLevel -> Adapty.logLevel = logLevel }
        uiHelper.isObserverMode = config.baseConfig.observerMode
        Adapty.activate(
            appContext,
            config.baseConfig,
        )
        (config as? CrossplatformConfigWithUi)?.mediaCache?.let { mediaCacheConfig ->
            AdaptyUI.configureMediaCache(mediaCacheConfig)
        }

        handleProfileUpdates()
        handleInstallationDetailsUpdates()
        handleUiEvents()
        onResult(success())
    }

    private fun handleProfileUpdates() =
        Adapty.setOnProfileUpdatedListener(object : OnProfileUpdatedListener {
            override fun onProfileReceived(profile: AdaptyProfile) {
                onNewEvent.forEach { (_, value) ->
                    value(
                        DID_LOAD_LATEST_PROFILE,
                        serializationHelper.toJson(
                            mapOf("id" to DID_LOAD_LATEST_PROFILE, "profile" to profile)
                        )
                    )
                }
            }
        })

    private fun handleInstallationDetailsUpdates() =
        Adapty.setOnInstallationDetailsListener(object : OnInstallationDetailsListener {
            override fun onInstallationDetailsSuccess(details: AdaptyInstallationDetails) {
                onNewEvent.forEach { (_, value) ->
                    value(
                        ON_INSTALLATION_DETAILS_SUCCESS,
                        serializationHelper.toJson(
                            mapOf("id" to ON_INSTALLATION_DETAILS_SUCCESS, "details" to details)
                        )
                    )
                }
            }

            override fun onInstallationDetailsFailure(error: AdaptyError) {
                onNewEvent.forEach { (_, value) ->
                    value(
                        ON_INSTALLATION_DETAILS_FAIL,
                        serializationHelper.toJson(
                            mapOf("id" to ON_INSTALLATION_DETAILS_FAIL, "error" to error)
                        )
                    )
                }
            }
        })

    private fun handleIdentify(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<IdentifyArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(IDENTIFY))
            return
        }

        Adapty.identify(args.customerUserId, args.gpObfuscatedAccountId) { error ->
            onResult(emptyResultOrError(error))
        }
    }

    private fun handleSetLogLevel(argument: Any?, onResult: ResultCallback<String>) {
        val logLevel = parseJsonArgument<SetLogLevelArgs>(argument)?.value ?: kotlin.run {
            onResult(callParameterError(SET_LOG_LEVEL))
            return
        }

        Adapty.logLevel = logLevel
        onResult(success())
    }

    private fun handleLogShowFlow(argument: Any?, onResult: ResultCallback<String>) {
        val flow = parseJsonArgument<LogShowFlowArgs>(argument)?.flow ?: kotlin.run {
            onResult(callParameterError(LOG_SHOW_FLOW))
            return
        }

        Adapty.logShowFlow(flow) { error ->
            onResult(emptyResultOrError(error))
        }
    }

    private fun handleGetFlow(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<GetPlacementArgs>(argument)?.takeIf { !it.placementId.isNullOrEmpty() } ?: kotlin.run {
            onResult(callParameterError(GET_FLOW))
            return
        }

        Adapty.getFlow(
            args.placementId,
            args.fetchPolicy ?: AdaptyPlacementFetchPolicy.Default,
            args.loadTimeout ?: DEFAULT_PLACEMENT_TIMEOUT,
        ) { result ->
            onResult(adaptyResult(result))
        }
    }

    private fun handleGetFlowForDefaultAudience(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<GetPlacementForDefaultAudienceArgs>(argument)?.takeIf { !it.placementId.isNullOrEmpty() } ?: kotlin.run {
            onResult(callParameterError(GET_FLOW_FOR_DEFAULT_AUDIENCE))
            return
        }

        Adapty.getFlowForDefaultAudience(
            args.placementId,
            args.fetchPolicy ?: AdaptyPlacementFetchPolicy.Default,
        ) { result ->
            onResult(adaptyResult(result))
        }
    }

    private fun handleGetPaywallProducts(argument: Any?, onResult: ResultCallback<String>) {
        val flow = parseJsonArgument<GetPaywallProductsArgs>(argument)?.flow ?: kotlin.run {
            onResult(callParameterError(GET_PAYWALL_PRODUCTS))
            return
        }

        Adapty.getPaywallProducts(flow) { result ->
            onResult(adaptyResult(result))
        }
    }

    private fun handleGetOnboarding(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<GetPlacementArgs>(argument)?.takeIf { !it.placementId.isNullOrEmpty() } ?: kotlin.run {
            onResult(callParameterError(GET_ONBOARDING))
            return
        }

        Adapty.getOnboarding(
            args.placementId,
            args.locale,
            args.fetchPolicy ?: AdaptyPlacementFetchPolicy.Default,
            args.loadTimeout ?: DEFAULT_PLACEMENT_TIMEOUT,
        ) { result ->
            onResult(adaptyResult(result))
        }
    }

    private fun handleGetOnboardingForDefaultAudience(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<GetPlacementForDefaultAudienceArgs>(argument)?.takeIf { !it.placementId.isNullOrEmpty() } ?: kotlin.run {
            onResult(callParameterError(GET_ONBOARDING_FOR_DEFAULT_AUDIENCE))
            return
        }

        Adapty.getOnboardingForDefaultAudience(
            args.placementId,
            args.locale,
            args.fetchPolicy ?: AdaptyPlacementFetchPolicy.Default,
        ) { result ->
            onResult(adaptyResult(result))
        }
    }

    private fun handleMakePurchase(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<MakePurchaseArgs>(argument)?.takeIf { it.product != null } ?: kotlin.run {
            onResult(callParameterError(MAKE_PURCHASE))
            return
        }

        activity()?.let { activity ->
            activity.runOnUiThread {
                Adapty.makePurchase(
                    activity,
                    args.product,
                    args.parameters ?: AdaptyPurchaseParameters.Empty,
                ) { result ->
                    onResult(adaptyResult(result))
                }
            }
        }
    }

    private fun handleRestorePurchases(onResult: ResultCallback<String>) {
        Adapty.restorePurchases { result ->
            onResult(adaptyResult(result))
        }
    }

    private fun handleGetProfile(onResult: ResultCallback<String>) {
        Adapty.getProfile { result ->
            onResult(adaptyResult(result))
        }
    }

    private fun handleSetIntegrationId(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<SetIntegrationIdArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(SET_INTEGRATION_ID))
            return
        }

        Adapty.setIntegrationIdentifier(
            AdaptyIntegrationIdentifier(AdaptyIntegrationIdentifier.Key(args.key), args.value)
        ) { error ->
            onResult(emptyResultOrError(error))
        }
    }

    private fun handleUpdateAttribution(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<UpdateAttributionArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(UPDATE_ATTRIBUTION))
            return
        }

        Adapty.updateAttribution(args.attribution, AdaptyAttributionSource(args.source)) { error ->
            onResult(emptyResultOrError(error))
        }
    }

    private fun handleUpdateProfile(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<UpdateProfileArgs>(argument)?.takeIf { it.params != null } ?: kotlin.run {
            onResult(callParameterError(UPDATE_PROFILE))
            return
        }

        Adapty.updateProfile(args.params) { error ->
            onResult(emptyResultOrError(error))
        }
    }

    private fun handleReportTransaction(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<SetVariationIdArgs>(argument)
            ?.takeIf { !it.transactionId.isNullOrEmpty() }
            ?: kotlin.run {
                onResult(callParameterError(REPORT_TRANSACTION))
                return
            }

        Adapty.reportTransaction(TransactionInfo.fromId(args.transactionId), args.variationId) { result ->
            onResult(emptyResultOrError((result as? AdaptyResult.Error)?.error))
        }
    }

    private fun handleGetCurrentInstallationStatus(onResult: ResultCallback<String>) {
        Adapty.getCurrentInstallationStatus { result ->
            onResult(adaptyResult(result))
        }
    }

    private fun handleSetFallback(argument: Any?, onResult: ResultCallback<String>) {
        val assetId = parseJsonArgument<FileLocationArgs>(argument)?.value ?: kotlin.run {
            onResult(callParameterError(SET_FALLBACK))
            return
        }

        Adapty.setFallback(transformFileLocation(assetId)) { error ->
            onResult(emptyResultOrError(error))
        }
    }

    private fun handleOpenWebPaywall(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<WebPaywallArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(CREATE_WEB_PAYWALL_URL))
            return
        }

        activity()?.let { activity ->
            activity.runOnUiThread {
                val presentation = args.presentation ?: AdaptyWebPresentation.ExternalBrowser
                when (args) {
                    is WebPaywallArgs.Paywall -> {
                        Adapty.openWebPaywall(activity, args.value, presentation) { error ->
                            onResult(emptyResultOrError(error))
                        }
                    }
                    is WebPaywallArgs.Product -> {
                        Adapty.openWebPaywall(activity, args.value, presentation) { error ->
                            onResult(emptyResultOrError(error))
                        }
                    }
                }
            }
        }
    }

    private fun handleCreateWebPaywallUrl(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<WebPaywallArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(CREATE_WEB_PAYWALL_URL))
            return
        }

        when (args) {
            is WebPaywallArgs.Paywall -> {
                Adapty.createWebPaywallUrl(args.value) { result ->
                    onResult(adaptyResult(result))
                }
            }
            is WebPaywallArgs.Product -> {
                Adapty.createWebPaywallUrl(args.value) { result ->
                    onResult(adaptyResult(result))
                }
            }
        }
    }

    private fun handleLogout(onResult: ResultCallback<String>) {
        Adapty.logout { error ->
            onResult(emptyResultOrError(error))
        }
    }

    private fun handleIsActivated(onResult: ResultCallback<String>) {
        onResult(success(Adapty.isActivated))
    }

    private fun handleGetSdkVersion(onResult: ResultCallback<String>) {
        onResult(success(adaptySdkVersion))
    }

    private fun handleUiEvents() {
        uiHelper.uiEventsObserver = { event ->
            onNewEvent.forEach { (_, value) -> value(event.id, serializationHelper.toJson(event.data)) }
        }
    }

    private fun handleCreateFlowView(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<CreateFlowViewArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(ADAPTY_UI_CREATE_FLOW_VIEW))
            return
        }

        uiHelper.handleCreateFlowView(
            args,
            { view -> onResult(success(view)) },
            { error -> onResult(adaptyError(error)) },
        )
    }

    private fun handlePresentFlowView(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<PresentViewArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(ADAPTY_UI_PRESENT_FLOW_VIEW))
            return
        }

        uiHelper.handlePresentView(
            args.id,
            { onResult(success()) },
            { error -> onResult(uiBridgeError(error)) },
        )
    }

    private fun handleDismissFlowView(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<DismissViewArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(ADAPTY_UI_DISMISS_FLOW_VIEW))
            return
        }

        uiHelper.handleDismissView(
            args.id,
            { onResult(success()) },
            { error -> onResult(uiBridgeError(error)) },
        )
    }

    private fun handleCreateOnboardingView(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<CreateOnboardingViewArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(ADAPTY_UI_CREATE_ONBOARDING_VIEW))
            return
        }

        uiHelper.handleCreateOnboardingView(
            args,
            { view -> onResult(success(view)) },
            { error -> onResult(adaptyError(error)) },
        )
    }

    private fun handlePresentOnboardingView(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<PresentViewArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(ADAPTY_UI_PRESENT_ONBOARDING_VIEW))
            return
        }

        uiHelper.handlePresentOnboardingView(
            args.id,
            { onResult(success()) },
            { error -> onResult(uiBridgeError(error)) },
        )
    }

    private fun handleDismissOnboardingView(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<DismissViewArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(ADAPTY_UI_DISMISS_ONBOARDING_VIEW))
            return
        }

        uiHelper.handleDismissOnboardingView(
            args.id,
            { onResult(success()) },
            { error -> onResult(uiBridgeError(error)) },
        )
    }

    private fun handleFlowViewDidAnswerPermission(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<FlowViewDidAnswerPermissionArgs>(argument)
            ?.takeIf { !it.eventId.isNullOrEmpty() && !it.status.isNullOrEmpty() }
            ?: kotlin.run {
                onResult(callParameterError(FLOW_VIEW_DID_ANSWER_PERMISSION))
                return
            }

        uiHelper.answerPermission(args.eventId, args.status == "granted", args.detail)
        onResult(success())
    }

    private fun handleObserverPurchaseDidStart(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseObserverModeArgs(argument, OBSERVER_PURCHASE_DID_START, onResult) ?: return
        uiHelper.observerPurchaseDidStart(args.eventId)
        onResult(success())
    }

    private fun handleObserverPurchaseDidFinish(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseObserverModeArgs(argument, OBSERVER_PURCHASE_DID_FINISH, onResult) ?: return
        uiHelper.observerPurchaseDidFinish(args.eventId)
        onResult(success())
    }

    private fun handleObserverRestoreDidStart(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseObserverModeArgs(argument, OBSERVER_RESTORE_DID_START, onResult) ?: return
        uiHelper.observerRestoreDidStart(args.eventId)
        onResult(success())
    }

    private fun handleObserverRestoreDidFinish(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseObserverModeArgs(argument, OBSERVER_RESTORE_DID_FINISH, onResult) ?: return
        uiHelper.observerRestoreDidFinish(args.eventId)
        onResult(success())
    }

    private fun parseObserverModeArgs(
        argument: Any?,
        methodName: String,
        onResult: ResultCallback<String>,
    ): ObserverModeRequestArgs? =
        parseJsonArgument<ObserverModeRequestArgs>(argument)
            ?.takeIf { it.eventId.isNotEmpty() }
            ?: kotlin.run {
                onResult(callParameterError(methodName))
                null
            }

    private fun handleAdaptyUiRequestAppReview(onResult: ResultCallback<String>) {
        activity()?.let { activity ->
            AdaptyFlowDefaultEventListener().onShowAppRate(activity)
        }
        onResult(success())
    }

    private fun handleAdaptyUiOpenUrl(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<OpenUrlArgs>(argument)?.takeIf { it.url.isNotEmpty() }
            ?: kotlin.run {
                onResult(callParameterError(ADAPTY_UI_OPEN_URL))
                return
            }

        handleOpenUrl(args.url, args.openIn ?: AdaptyWebPresentation.ExternalBrowser)
        onResult(success())
    }

    private fun handleShowDialog(argument: Any?, onResult: ResultCallback<String>) {
        val args = parseJsonArgument<ShowDialogArgs>(argument) ?: kotlin.run {
            onResult(callParameterError(ADAPTY_UI_SHOW_DIALOG))
            return
        }

        uiHelper.handleShowDialog(
            args.id,
            args.configuration,
            { action -> onResult(success(action)) },
            { error -> onResult(uiBridgeError(error)) },
        )
    }

    private fun adaptyResult(adaptyResult: AdaptyResult<Any>): String {
        return when (adaptyResult) {
            is AdaptyResult.Success -> success(adaptyResult.value)
            is AdaptyResult.Error -> adaptyError(adaptyResult.error)
        }
    }

    internal inline fun <reified T: Any> parseJsonArgument(argument: Any?): T? =
        serializationHelper.parseJsonArgument(argument)

    private fun emptyResultOrError(error: AdaptyError?): String {
        return if (error == null) {
            success()
        } else {
            adaptyError(error)
        }
    }

    private fun success(): String {
        return successJson()
    }
    
    private fun success(value: Any): String {
        return toSuccessJson(value)
    }

    private fun adaptyError(error: AdaptyError): String {
        return toErrorJson(error)
    }

    private fun callParameterError(
        methodName: String,
        originalError: Throwable? = null
    ): String {
        val message = "Error while parsing parameter"
        val detail =
            "Method: ${methodName}, OriginalError: ${originalError?.localizedMessage ?: originalError?.message}"
        return toErrorJson(AdaptyErrorCode.DECODING_FAILED, message, detail)
    }

    private fun unsupportedMethodError(message: String): String {
        return toErrorJson(AdaptyErrorCode.WRONG_PARAMETER, message)
    }

    private fun uiBridgeError(bridgeError: AdaptyUiBridgeError): String {
        return toErrorJson(bridgeError.rawCode, bridgeError.message)
    }

    private fun methodNotFoundError(methodName: String? = null): String {
        return toErrorJson(AdaptyErrorCode.DECODING_FAILED, "Method $methodName not found")
    }

    private fun toSuccessJson(src: Any) = serializationHelper.toJson(mapOf("success" to src))

    private fun successJson() = SUCCESSFUL_EMPTY_RESPONSE

    private fun toErrorJson(error: AdaptyError) =
        serializationHelper.toJson(
            mapOf("error" to error)
        )

    private fun toErrorJson(code: AdaptyErrorCode, message: String, detail: String? = null) =
        serializationHelper.toJson(
            mapOf(
                "error" to mutableMapOf(
                    "adapty_code" to code,
                    "message" to message,
                ).apply {
                    detail?.let { put("detail", detail) }
                },
            )
        )

    private fun toErrorJson(code: Int, message: String, detail: String? = null) =
        serializationHelper.toJson(
            mapOf(
                "error" to mutableMapOf(
                    "adapty_code" to code,
                    "message" to message,
                ).apply {
                    detail?.let { put("detail", detail) }
                },
            )
        )

    fun handleOpenUrl(url: String, presentation: AdaptyWebPresentation) {
        activity()?.let { activity ->
            urlHandler.openUrl(activity, url, presentation)
        }
    }

    private companion object {
        //Method ids
        const val SET_LOG_LEVEL = "set_log_level"
        const val GET_PROFILE = "get_profile"
        const val UPDATE_PROFILE = "update_profile"
        const val ACTIVATE = "activate"
        const val IDENTIFY = "identify"
        const val GET_FLOW = "get_flow"
        const val GET_FLOW_FOR_DEFAULT_AUDIENCE = "get_flow_for_default_audience"
        const val GET_PAYWALL_PRODUCTS = "get_paywall_products"
        const val GET_ONBOARDING = "get_onboarding"
        const val GET_ONBOARDING_FOR_DEFAULT_AUDIENCE = "get_onboarding_for_default_audience"
        const val MAKE_PURCHASE = "make_purchase"
        const val RESTORE_PURCHASES = "restore_purchases"
        const val SET_INTEGRATION_ID = "set_integration_identifiers"
        const val UPDATE_ATTRIBUTION = "update_attribution_data"
        const val LOG_SHOW_FLOW = "log_show_flow"
        const val REPORT_TRANSACTION = "report_transaction"
        const val SET_FALLBACK = "set_fallback"
        const val OPEN_WEB_PAYWALL = "open_web_paywall"
        const val CREATE_WEB_PAYWALL_URL = "create_web_paywall_url"
        const val GET_CURRENT_INSTALLATION_STATUS = "get_current_installation_status"
        const val LOGOUT = "logout"
        const val DID_LOAD_LATEST_PROFILE = "did_load_latest_profile"
        const val ON_INSTALLATION_DETAILS_SUCCESS = "on_installation_details_success"
        const val ON_INSTALLATION_DETAILS_FAIL = "on_installation_details_fail"
        const val IS_ACTIVATED = "is_activated"
        const val GET_SDK_VERSION = "get_sdk_version"
        const val ADAPTY_UI_CREATE_FLOW_VIEW = "adapty_ui_create_flow_view"
        const val ADAPTY_UI_PRESENT_FLOW_VIEW = "adapty_ui_present_flow_view"
        const val ADAPTY_UI_DISMISS_FLOW_VIEW = "adapty_ui_dismiss_flow_view"
        const val ADAPTY_UI_CREATE_ONBOARDING_VIEW = "adapty_ui_create_onboarding_view"
        const val ADAPTY_UI_PRESENT_ONBOARDING_VIEW = "adapty_ui_present_onboarding_view"
        const val ADAPTY_UI_DISMISS_ONBOARDING_VIEW = "adapty_ui_dismiss_onboarding_view"
        const val ADAPTY_UI_SHOW_DIALOG = "adapty_ui_show_dialog"
        const val ADAPTY_UI_OPEN_URL = "adapty_ui_open_url"
        const val ADAPTY_UI_REQUEST_APP_REVIEW = "adapty_ui_request_app_review"
        const val FLOW_VIEW_DID_ANSWER_PERMISSION = "flow_view_did_answer_permission"
        const val OBSERVER_PURCHASE_DID_START = "observer_purchase_did_start"
        const val OBSERVER_PURCHASE_DID_FINISH = "observer_purchase_did_finish"
        const val OBSERVER_RESTORE_DID_START = "observer_restore_did_start"
        const val OBSERVER_RESTORE_DID_FINISH = "observer_restore_did_finish"

        const val SUCCESSFUL_EMPTY_RESPONSE = "{\"success\":true}"
    }
}