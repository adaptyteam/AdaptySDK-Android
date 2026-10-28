package com.adapty.internal.utils

import android.app.Activity

internal fun interface HostActivityValidator {

    fun isValidHost(activity: Activity): Boolean

    companion object {
        val Default: HostActivityValidator = HostActivityValidator { activity ->
            !activity.isFinishing
                    && !activity.isDestroyed
                    && activity.window?.decorView?.windowToken != null
        }
    }
}
