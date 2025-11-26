package com.adapty.ui.onboardings.internal.util

import android.net.http.SslError
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse

internal fun WebResourceError.toLog(): String =
    "WebResourceError(errorCode=${getErrorCodeIfSupported()}, description=${getDescriptionIfSupported()})"

private fun WebResourceError.getErrorCodeIfSupported() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) errorCode else -1

private fun WebResourceError.getDescriptionIfSupported() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) description else "unknown"

internal fun WebResourceResponse.toLog(): String =
    "WebResourceResponse(mimeType=$mimeType, statusCode=$statusCode, reason=$reasonPhrase, headers={${responseHeaders.entries.joinToString { "${it.key}=${it.value}" }}})"

internal fun SslError.toLog(): String =
    "SslError(primaryError=$primaryError, url=$url, certificate=${certificate?.toString()})"
