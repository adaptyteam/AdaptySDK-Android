@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.onboardings

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.adapty.internal.data.cloud.BrowserLauncher
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.internal.utils.getProgressCustomColorOrNull
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.withAdaptyUIActivated
import com.adapty.ui.onboardings.actions.AdaptyOnboardingAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCloseAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCustomAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingLoadedAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingOpenPaywallAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingStateUpdatedAction
import com.adapty.ui.onboardings.errors.AdaptyOnboardingError
import com.adapty.ui.onboardings.internal.serialization.OnboardingCommonDeserializer
import com.adapty.ui.onboardings.internal.ui.OnboardingViewModel
import com.adapty.ui.onboardings.internal.util.toLog
import com.adapty.ui.onboardings.listeners.AdaptyOnboardingEventListener
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Queue
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
public class AdaptyOnboardingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val webView: WebView
    private val placeholderView: PlaceholderView

    private var delegate: AdaptyOnboardingEventListener? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val pendingActions: Queue<() -> Unit> = ArrayDeque()

    private val browserLauncher: BrowserLauncher by lazy {
        withAdaptyUIActivated {
            Dependencies.injectInternal<BrowserLauncher>()
        }
    }

    private val viewModel: OnboardingViewModel? by lazy {
        val viewModelStoreOwner = findViewTreeViewModelStoreOwner()
            ?: run {
                log(ERROR) { "$LOG_PREFIX OnboardingView (${hashCode()}) rendering error: No ViewModelStoreOwner found" }
                return@lazy null
            }
        val onboardingCommonDeserializer = withAdaptyUIActivated {
            Dependencies.injectInternal<OnboardingCommonDeserializer>()
        }
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OnboardingViewModel(onboardingCommonDeserializer) as T
            }
        }
        ViewModelProvider(viewModelStoreOwner, factory)[OnboardingViewModel::class.java]
    }

    init {
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportMultipleWindows(true)
            isFocusableInTouchMode = true

            addJavascriptInterface(object {
                @JavascriptInterface
                fun postMessageString(message: String) {
                    log(VERBOSE) { "$LOG_PREFIX Message received: ${message})" }
                    withViewModel { it.processMessage(message) }
                }
            }, "Android")

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    log(ERROR) { "$LOG_PREFIX_ERROR onReceivedError: ${error.toLog()})" }
                    if (!request.isForMainFrame) return
                    withViewModel { it.emitError(AdaptyOnboardingError.WebKit.WebResource(error)) }
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                    log(ERROR) { "$LOG_PREFIX_ERROR onReceivedHttpError: ${errorResponse.toLog()})" }
                    if (!request.isForMainFrame) return
                    withViewModel { it.emitError(AdaptyOnboardingError.WebKit.Http(errorResponse)) }
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    log(ERROR) { "$LOG_PREFIX_ERROR onReceivedSslError: ${error.toLog()})" }
                    withViewModel { it.emitError(AdaptyOnboardingError.WebKit.Ssl(error)) }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    if (isUserGesture) {
                        val url = view?.hitTestResult?.extra
                        if (url != null) {
                            if (url.contains("://")) {
                                openExternalUrl(url)
                            } else {
                                view.loadUrl(url)
                            }
                            return false
                        }
                    }

                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                    val mainWebView = view
                    val tempWebView = WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString()
                                if (url != null) {
                                    if (isUserGesture && url.contains("://")) {
                                        openExternalUrl(url)
                                    } else {
                                        mainWebView?.loadUrl(url)
                                    }
                                }
                                view?.destroy()
                                return true
                            }
                        }
                    }
                    transport.webView = tempWebView
                    resultMsg.sendToTarget()
                    return true
                }

                private fun openExternalUrl(url: String) {
                    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: run {
                        log(WARN) { "$LOG_PREFIX couldn't parse url: $url" }
                        return
                    }
                    withViewModel { vm ->
                        val presentation = vm.onboardingConfig?.externalUrlsPresentation
                            ?: AdaptyWebPresentation.InAppBrowser
                        browserLauncher.openUrl(context, uri, presentation)
                    }
                }
            }
        }

        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        placeholderView = tryCreateCustomPlaceholderView(context)?.also {
            addView(it.view)
        } ?: createDefaultPlaceholderView(context).also {
            addView(it.view, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            })
        }

        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View?) {
                executePendingActions()
            }

            override fun onViewDetachedFromWindow(v: View?) {
                pendingActions.clear()
            }
        })

        overrideSafeAreaPaddingsIfNeeded(context)

        observeViewModel()
    }

    private fun createDefaultPlaceholderView(context: Context) =
        PlaceholderView.Default(
            ProgressBar(context).apply {
                isIndeterminate = true
                visibility = View.VISIBLE
                context.getProgressCustomColorOrNull()?.let { color ->
                    indeterminateTintList = ColorStateList.valueOf(color)
                }
            }
        )

    private fun tryCreateCustomPlaceholderView(context: Context) =
        kotlin.runCatching {
            context.resources.getIdentifier("adapty_onboarding_placeholder_view", "layout", context.packageName)
                .takeIf { it > 0 }
                ?.let { resId ->
                    PlaceholderView.Custom(LayoutInflater.from(context).inflate(resId, this, false))
                }
        }
            .getOrElse { e ->
                log(ERROR) { "$LOG_PREFIX_ERROR couldn't create view from 'adapty_onboarding_placeholder_view': (${e.localizedMessage})" }
                null
            }

    private fun overrideSafeAreaPaddingsIfNeeded(context: Context) =
        kotlin.runCatching {
            context.resources.getIdentifier("adapty_onboarding_enable_safe_area_paddings", "bool", context.packageName)
                .takeIf { it > 0 }
                ?.let { resId ->
                    withViewModel { it.safeAreaPaddings = context.resources.getBoolean(resId) }
                }
        }
            .getOrElse { e ->
                log(ERROR) { "$LOG_PREFIX_ERROR couldn't parse custom safe area paddings from 'adapty_onboarding_enable_safe_area_paddings': (${e.localizedMessage})" }
                null
            }

    public fun show(viewConfig: AdaptyOnboardingConfiguration, delegate: AdaptyOnboardingEventListener) {
        this.delegate = delegate
        val url = viewConfig.url

        withViewModel { it.onboardingConfig = viewConfig }

        placeholderView.view.visibility = View.VISIBLE
        val requestedLocale = viewConfig.requestedLocale
        if (requestedLocale == null) webView.loadUrl(url)
        else webView.loadUrl(url, mapOf("Accept-Language" to requestedLocale))
    }

    public fun show(
        viewConfig: AdaptyOnboardingConfiguration,
        delegate: AdaptyOnboardingEventListener,
        safeAreaPaddings: Boolean,
    ) {
        withViewModel { it.safeAreaPaddings = safeAreaPaddings }
        show(viewConfig, delegate)
    }

    private fun observeViewModel() {
        withViewModel { vm ->
            coroutineScope.launch {
                launch { vm.actions.collect { action -> handleAction(action) } }
                launch { vm.analytics.collect { event -> delegate?.onAnalyticsEvent(event, context) } }
                launch { vm.errors.collect { error -> delegate?.onError(error, context) } }
                launch { vm.onboardingLoaded.collect { triggerFinishLoading(it) } }
                
                if (vm.hasFinishedLoading) {
                    val webViewHasContent = webView.url?.startsWith("http") == true
                    if (webViewHasContent) {
                        placeholderView.view.visibility = View.GONE
                    } else {
                        vm.onboardingConfig?.let { config ->
                            placeholderView.view.visibility = View.VISIBLE
                            if (config.requestedLocale == null) webView.loadUrl(config.url)
                            else webView.loadUrl(config.url, mapOf("Accept-Language" to config.requestedLocale))
                        }
                    }
                }
            }
        }
    }

    private fun handleAction(action: AdaptyOnboardingAction) {
        when (action) {
            is AdaptyOnboardingCloseAction -> delegate?.onCloseAction(action, context)
            is AdaptyOnboardingCustomAction -> delegate?.onCustomAction(action, context)
            is AdaptyOnboardingOpenPaywallAction -> delegate?.onOpenPaywallAction(action, context)
            is AdaptyOnboardingStateUpdatedAction -> delegate?.onStateUpdatedAction(action, context)
            is AdaptyOnboardingLoadedAction -> delegate?.onFinishLoading(action, context)
        }
    }

    private fun triggerFinishLoading(action: AdaptyOnboardingLoadedAction) {
        placeholderView.view.visibility = View.GONE
        delegate?.onFinishLoading(action, context)
        withViewModel {
            it.hasFinishedLoading = true
            if (it.safeAreaPaddings) {
                injectUniversalInsetSupport()
                sendInsetsToWebView()
            }
        }
    }

    private fun sendInsetsToWebView() {
        val insets = ViewCompat.getRootWindowInsets(this)
            ?.getInsets(WindowInsetsCompat.Type.systemBars())
            ?: return
        webView.applyInsetsDp(insets, context)
    }

    private fun WebView.applyInsetsDp(insets: Insets, context: Context) {
        val density = context.resources.displayMetrics.density

        val topDp = (insets.top / density).roundToInt()
        val bottomDp = (insets.bottom / density).roundToInt()
        val leftDp = (insets.left / density).roundToInt()
        val rightDp = (insets.right / density).roundToInt()

        val js = """
            if (typeof window.updateInsets === 'function') {
                window.updateInsets($topDp, $bottomDp, $leftDp, $rightDp);
            } else {
                console.log("[OnboardingView] updateInsets not defined");
            }
        """.trimIndent()

        evaluateJavascript(js, null)
    }

    private fun injectUniversalInsetSupport() {
        val js = """
            window.updateInsets = function(top, bottom, left, right) {
                console.log("[OnboardingView] Applying insets: top=" + top + " bottom=" + bottom);
    
                const root = document.documentElement;
                root.style.boxSizing = "border-box";
    
                document.body.style.margin = "0";
                document.body.style.paddingTop = top + "px";
                document.body.style.paddingBottom = bottom + "px";
                document.body.style.paddingLeft = left + "px";
                document.body.style.paddingRight = right + "px";
                document.body.style.boxSizing = "border-box";
                document.body.style.overflowX = "hidden";
    
                const mainContainer = document.querySelector("#app, .main, .container, main, .content, body");
    
                if (mainContainer) {
                    mainContainer.style.boxSizing = "border-box";
                    mainContainer.style.paddingBottom = bottom + "px";
                    mainContainer.style.maxHeight = "100vh";
                    mainContainer.style.overflowY = "auto";
                }
    
                console.log("[OnboardingView] Layout adjusted");
            };
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    public fun canGoBack(): Boolean = webView.canGoBack()

    public fun goBack() {
        webView.goBack()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val isChangingConfig = context.getActivityOrNull()?.isChangingConfigurations ?: false
        if (!isChangingConfig) {
            viewModel?.clearState()
        }
        coroutineScope.cancel()
        delegate = null
    }

    private inline fun withViewModel(crossinline block: (OnboardingViewModel) -> Unit) {
        runOnceWhenAttached { viewModel?.let(block) }
    }

    private fun runOnceWhenAttached(action: () -> Unit) {
        if (isAttachedToWindow) {
            action()
        } else {
            pendingActions.offer(action)
        }
    }

    private fun executePendingActions() {
        while (pendingActions.isNotEmpty()) {
            pendingActions.poll()?.let { action -> action() }
        }
    }
}

private sealed class PlaceholderView(val view: View) {
    class Default(progressBar: ProgressBar): PlaceholderView(progressBar)
    class Custom(view: View): PlaceholderView(view)
}
