@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.onboardings.AdaptyOnboardingMetaParams
import java.lang.reflect.Field

internal class SerializationFieldNamingStrategyUiHelper {

    fun translateNameOrSkip(f: Field?): String? {
        return when (f?.declaringClass) {
            AdaptyOnboardingMetaParams::class.java -> when (f.name) {
                "screenClientId" -> "screen_cid"
                else -> null
            }
            else -> null
        }
    }
}