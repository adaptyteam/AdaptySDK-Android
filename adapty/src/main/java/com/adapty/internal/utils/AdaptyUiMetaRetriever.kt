package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.utils.AdaptyLogLevel

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyUiMetaRetriever {

    private val buildConfigClass: Class<*>? by lazy {
        getClassForNameOrNull("com.adapty.ui.BuildConfig")
    }

    @get:JvmSynthetic
    val adaptyUiAndBuilderVersion: Pair<String, String>
        get() {
            val adaptyUiVersion = getDeclaredFieldOrNull<String>(buildConfigClass, "VERSION_NAME", buildConfigClass)
                    ?: throwWrongParamError("Unable to retrieve the version of Adapty UI. Please ensure that the dependency is added to the project.")
            val builderVersion = getDeclaredFieldOrNull<String>(buildConfigClass, "BUILDER_VERSION", buildConfigClass) ?: "1"
            return adaptyUiVersion to builderVersion
        }

    private fun <T> getDeclaredFieldOrNull(sourceClass: Class<*>?, name: String, obj: Any?) =
        try {
            sourceClass?.getDeclaredField(name)?.apply { isAccessible = true }?.get(obj) as? T
        } catch (e: Exception) { null }

    private fun throwWrongParamError(message: String): Nothing {
        Logger.log(AdaptyLogLevel.ERROR) { message }
        throw AdaptyError(
            message = message,
            adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER
        )
    }
}