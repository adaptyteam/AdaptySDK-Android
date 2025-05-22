@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.onboardings

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.net.http.SslError
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.getProgressCustomColorOrNull
import com.adapty.ui.internal.utils.log
import com.adapty.ui.onboardings.actions.AdaptyOnboardingAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCloseAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingCustomAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingOpenPaywallAction
import com.adapty.ui.onboardings.actions.AdaptyOnboardingStateUpdatedAction
import com.adapty.ui.onboardings.errors.AdaptyOnboardingError
import com.adapty.ui.onboardings.internal.serialization.OnboardingCommonDeserializer
import com.adapty.ui.onboardings.internal.ui.OnboardingViewModel
import com.adapty.ui.onboardings.internal.util.toLog
import com.adapty.ui.onboardings.listeners.AdaptyOnboardingEventListener
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
public class AdaptyOnboardingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val webView: WebView
    private val progressBar: ProgressBar

    private var delegate: AdaptyOnboardingEventListener? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var backCallback: OnBackPressedCallback? = null

    private val viewModel: OnboardingViewModel? by lazy {
        val viewModelStoreOwner = findViewTreeViewModelStoreOwner()
            ?: run {
                log(ERROR) { "$LOG_PREFIX OnboardingView (${hashCode()}) rendering error: No ViewModelStoreOwner found" }
                return@lazy null
            }
        val onboardingCommonDeserializer = Dependencies.injectInternal<OnboardingCommonDeserializer>()
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
            isFocusableInTouchMode = true

            addJavascriptInterface(object {
                @JavascriptInterface
                fun postMessageString(message: String) {
                    log(VERBOSE) { "$LOG_PREFIX Message received: ${message})" }
                    withViewModel { it.processMessage(message) }
                }
            }, "Android")

            webViewClient = object : WebViewClientCompat() {
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
                    log(ERROR) { "$LOG_PREFIX_ERROR onReceivedError: ${error.toLog()})" }
                    withViewModel { it.emitError(AdaptyOnboardingError.WebKit.WebResource(error)) }
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                    if (errorResponse.statusCode == 522 && !request.isForMainFrame) return
                    log(ERROR) { "$LOG_PREFIX_ERROR onReceivedHttpError: ${errorResponse.toLog()})" }
                    withViewModel { it.emitError(AdaptyOnboardingError.WebKit.Http(errorResponse)) }
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    log(ERROR) { "$LOG_PREFIX_ERROR onReceivedSslError: ${error.toLog()})" }
                    withViewModel { it.emitError(AdaptyOnboardingError.WebKit.Ssl(error)) }
                }
            }
        }

        progressBar = ProgressBar(context).apply {
            isIndeterminate = true
            visibility = View.VISIBLE
            context.getProgressCustomColorOrNull()?.let { color ->
                indeterminateTintList = ColorStateList.valueOf(color)
            }
        }

        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(progressBar, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })

        observeViewModel()
    }

    public fun show(viewConfig: AdaptyOnboardingConfiguration, delegate: AdaptyOnboardingEventListener) {
        this.delegate = delegate
        val previousUrl = viewModel?.onboardingConfig?.url
        val newUrl = viewConfig.url

        viewModel?.onboardingConfig = viewConfig

        if (previousUrl != newUrl || viewModel?.hasFinishedLoading != true) {
            progressBar.visibility = View.VISIBLE
            webView.loadUrl(newUrl)
        } else {
            progressBar.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        withViewModel { vm ->
            coroutineScope.launch {
                launch { vm.actions.collect { action -> handleAction(action) } }
                launch { vm.analytics.collect { event -> delegate?.onAnalyticsEvent(event, context) } }
                launch { vm.errors.collect { error -> delegate?.onError(error, context) } }
                launch { vm.loadedEvents.collect { triggerFinishLoading() } }
            }
        }
    }

    private fun handleAction(action: AdaptyOnboardingAction) {
        when (action) {
            is AdaptyOnboardingCloseAction -> delegate?.onCloseAction(action, context)
            is AdaptyOnboardingCustomAction -> delegate?.onCustomAction(action, context)
            is AdaptyOnboardingOpenPaywallAction -> delegate?.onOpenPaywallAction(action, context)
            is AdaptyOnboardingStateUpdatedAction -> delegate?.onStateUpdatedAction(action, context)
        }
    }

    private fun triggerFinishLoading() {
        if (viewModel?.hasFinishedLoading != true) {
            viewModel?.hasFinishedLoading = true
            progressBar.visibility = View.GONE
            delegate?.onFinishLoading(context)
            injectUniversalInsetSupport()
            sendInsetsToWebView()
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
                console.log("[Onboarding SDK] updateInsets not defined");
            }
        """.trimIndent()

        evaluateJavascript(js, null)
    }

    private fun injectUniversalInsetSupport() {
        val js = """
            window.updateInsets = function(top, bottom, left, right) {
                console.log("[Onboarding SDK] Applying insets: top=" + top + " bottom=" + bottom);
    
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
    
                console.log("[Onboarding SDK] Layout adjusted");
            };
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val dispatcherOwner = findViewTreeOnBackPressedDispatcherOwner()
        val dispatcher = dispatcherOwner?.onBackPressedDispatcher

        if (dispatcher != null) {
            backCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!webView.canGoBack()) {
                        isEnabled = false
                        dispatcher.onBackPressed()
                    } else {
                        webView.goBack()
                    }
                }
            }.also {
                dispatcher.addCallback(it)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope.cancel()
        backCallback?.remove()
        backCallback = null
        delegate = null
    }

    private inline fun withViewModel(crossinline block: (OnboardingViewModel) -> Unit) {
        runOnceWhenAttached { viewModel?.let(block) }
    }

    private fun runOnceWhenAttached(action: () -> Unit) {
        if (isAttachedToWindow) {
            action()
        } else {
            addOnAttachStateChangeListener(object: OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View?) {
                    action()
                    removeOnAttachStateChangeListener(this)
                }

                override fun onViewDetachedFromWindow(v: View?) {
                    removeOnAttachStateChangeListener(this)
                }
            })
        }
    }
}
