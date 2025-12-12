package com.adapty.internal.data.cloud

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import com.adapty.internal.utils.Logger
import com.adapty.models.AdaptyWebPresentation
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BrowserLauncher {

    fun openUrl(context: Context, url: Uri, presentation: AdaptyWebPresentation) {
        when (presentation) {
            is AdaptyWebPresentation.InAppBrowser -> openInAppBrowser(context, url)
            is AdaptyWebPresentation.ExternalBrowser -> openExternalBrowser(context, url)
        }
    }

    private fun openInAppBrowser(context: Context, url: Uri) {
        try {
            val packageName = getCustomTabsPackage(context, url)
            if (packageName == null) {
                Logger.log(WARN) { "No Custom Tabs browser found, falling back to external browser" }
                openExternalBrowser(context, url)
                return
            }
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.intent.setPackage(packageName)
            customTabsIntent.launchUrl(context, url)
        } catch (e: Throwable) {
            Logger.log(WARN) { "In-app browser failed, falling back to external browser: ${e.message}" }
            openExternalBrowser(context, url)
        }
    }

    private fun openExternalBrowser(context: Context, url: Uri) {
        val intent = createBrowsableIntent(url)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getBrowserPackage(context, url)?.let { intent.setPackage(it) }
        try {
            context.startActivity(intent)
        } catch (e: Throwable) {
            Logger.log(ERROR) { "Couldn't find an app that can process this url" }
        }
    }

    private fun getCustomTabsPackage(context: Context, url: Uri): String? =
        runCatching {
            val pm = context.packageManager
            val intent = createBrowsableIntent(url)

            val browsers = pm.queryIntentActivities(intent)
                .mapNotNull { it.activityInfo?.packageName?.takeIf { pkg -> pkg != "android" } }

            CustomTabsClient.getPackageName(context, browsers)
        }.getOrNull()

    private fun getBrowserPackage(context: Context, url: Uri): String? =
        runCatching {
            val pm = context.packageManager
            val intent = createBrowsableIntent(url)

            val defaultBrowser = pm.resolveDefaultActivity(intent)
                ?.activityInfo?.packageName?.takeIf { it != "android" }

            defaultBrowser ?: pm.queryIntentActivities(intent)
                .firstOrNull { it.activityInfo?.packageName != "android" }
                ?.activityInfo?.packageName
        }.getOrNull()

    private fun createBrowsableIntent(url: Uri) =
        Intent(Intent.ACTION_VIEW, url).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

    private fun PackageManager.resolveDefaultActivity(intent: Intent): ResolveInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolveActivity(intent, PackageManager.ResolveInfoFlags.of(
                PackageManager.MATCH_DEFAULT_ONLY.toLong()
            ))
        } else {
            @Suppress("DEPRECATION")
            resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

    private fun PackageManager.queryIntentActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, 0)
        }
}
