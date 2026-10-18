@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui.attributes

import com.adapty.ui.internal.platform.InternalAdaptyUiApi

internal enum class MainAxisBehaviour {
    FILL, HUG;

    companion object {
        fun fromValueOrNull(value: String?): MainAxisBehaviour? = when (value) {
            "fill" -> FILL
            "hug" -> HUG
            else -> null
        }
    }
}
