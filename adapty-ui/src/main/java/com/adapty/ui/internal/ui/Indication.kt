@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.foundation.Indication
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

@Suppress("DEPRECATION_ERROR")
@Composable
internal fun clickIndication(): Indication = runCatching { ripple() }.getOrElse { e ->
    log(ERROR) { "$LOG_PREFIX Switching to fallback indication (${e.localizedMessage})" }
    rememberRipple()
}