@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.utils

import android.content.ContextWrapper
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.adapty.ui.R
import com.adapty.ui.internal.platform.Activity
import com.adapty.ui.internal.platform.Context
import com.adapty.ui.internal.platform.ERROR
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.WARN
import com.adapty.ui.internal.platform.defaultLocale

// Android Context helpers moved from utils.kt (shared code reaches them through platform seams).

internal fun Context.getCurrentLocale() =
    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        resources.configuration.locale
    })
        ?: defaultLocale().also {
            log(WARN) { "Failed to get locale from resources. Falling back to default." }
        }

internal fun Context.areAnimationsDisabled() =
    runCatching {
        Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)

internal fun Context.getActivityOrNull(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    log(ERROR) { "$LOG_PREFIX couldn't get Activity from $this" }
    return null
}

internal fun Context.getProgressCustomColorOrNull(): Int? {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(R.attr.adapty_progressIndicatorColor, typedValue, true))
        return null
    return when (typedValue.type) {
        in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT ->
            typedValue.data
        TypedValue.TYPE_REFERENCE ->
            kotlin.runCatching {
                ContextCompat.getColor(this, typedValue.resourceId)
            }.getOrNull()
        else -> null
    }
}
