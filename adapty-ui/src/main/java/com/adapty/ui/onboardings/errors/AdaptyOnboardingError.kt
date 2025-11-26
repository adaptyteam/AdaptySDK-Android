package com.adapty.ui.onboardings.errors

import android.net.http.SslError
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import com.adapty.ui.onboardings.internal.util.toLog

public sealed class AdaptyOnboardingError {
    public class WrongApiKey: AdaptyOnboardingError()
    public class NotActivated: AdaptyOnboardingError()
    public class ActivateOnce: AdaptyOnboardingError()
    public sealed class WebKit: AdaptyOnboardingError() {
        public class WebResource(public val originalError: WebResourceError): WebKit() {
            override fun toString(): String {
                return "AdaptyOnboardingError.WebResource(originalError=${originalError.toLog()})"
            }
        }
        public class Http(public val errorResponse: WebResourceResponse): WebKit() {
            override fun toString(): String {
                return "AdaptyOnboardingError.Http(errorResponse=${errorResponse.toLog()})"
            }
        }
        public class Ssl(public val originalError: SslError): WebKit() {
            override fun toString(): String {
                return "Ssl(originalError=${originalError.toLog()})"
            }
        }
    }
    public class Unknown(public val originalError: Throwable): AdaptyOnboardingError() {
        override fun toString(): String {
            return "Unknown(originalError=${originalError.message})"
        }
    }
}
