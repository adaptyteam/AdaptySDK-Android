@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform

import android.app.Activity
import androidx.core.net.toUri
import com.adapty.internal.data.cloud.BrowserLauncher
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

internal class UrlHandler {

    fun openUrl(activity: Activity, url: String, presentation: AdaptyWebPresentation) {
        runCatching {
            Dependencies.injectInternal<BrowserLauncher>()
                .openUrl(activity, url.toUri(), presentation)
        }.getOrElse { e ->
            log(ERROR) { "$LOG_PREFIX_ERROR couldn't process this url (${url}): (${e.localizedMessage})" }
        }
    }
}