package com.adapty.ui.onboardings.internal.util

import android.net.http.SslError
import android.webkit.WebResourceResponse
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE
import androidx.webkit.WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION

internal fun WebResourceErrorCompat.toLog(): String =
    "WebResourceErrorCompat(errorCode=${getErrorCodeIfSupported()}, description=${getDescriptionIfSupported()})"

private fun WebResourceErrorCompat.getErrorCodeIfSupported() =
    if (WebViewFeature.isFeatureSupported(WEB_RESOURCE_ERROR_GET_CODE)) errorCode else -1

private fun WebResourceErrorCompat.getDescriptionIfSupported() =
    if (WebViewFeature.isFeatureSupported(WEB_RESOURCE_ERROR_GET_DESCRIPTION)) description else "unknown"

internal fun WebResourceResponse.toLog(): String =
    "WebResourceResponse(mimeType=$mimeType, statusCode=$statusCode, reason=$reasonPhrase, headers={${responseHeaders.entries.joinToString { "${it.key}=${it.value}" }}})"

internal fun SslError.toLog(): String =
    "SslError(primaryError=$primaryError, url=$url, certificate=${certificate?.toString()})"
