package com.adapty.internal.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyUiAccessor {

    private val buildConfigClass: Class<*>? by lazy {
        getClassForNameOrNull("com.adapty.ui.BuildConfig")
    }

    private val adaptyUiClass: Class<*>? by lazy {
        getClassForNameOrNull("com.adapty.ui.AdaptyUI")
    }

    @get:JvmSynthetic
    val adaptyUiVersion: String?
        get() = getDeclaredFieldOrNull<String>(buildConfigClass, "VERSION_NAME", buildConfigClass)

    @get:JvmSynthetic
    val builderVersion: String
        get() = getDeclaredFieldOrNull<String>(buildConfigClass, "BUILDER_VERSION", buildConfigClass) ?: "3"

    fun preloadMedia(rawConfig: Map<String, Any>) {
        try {
            getDeclaredFieldOrNull<Any>(adaptyUiClass, "INSTANCE", adaptyUiClass)?.let { adaptyUiInstance ->
                invokeDeclaredMethodIfExists(
                    adaptyUiClass,
                    "preloadMedia",
                    adaptyUiInstance,
                    rawConfig,
                )
            }
        } catch (e: Throwable) { }
    }

    private fun <T> getDeclaredFieldOrNull(sourceClass: Class<*>?, name: String, obj: Any?) =
        try {
            sourceClass?.getDeclaredField(name)?.apply { isAccessible = true }?.get(obj) as? T
        } catch (e: Exception) { null }

    private fun invokeDeclaredMethodIfExists(sourceClass: Class<*>?, name: String, obj: Any?, vararg args: Any?) {
        try {
            sourceClass?.getDeclaredMethod(name, Map::class.java)?.apply { isAccessible = true }?.invoke(obj, *args)
        } catch (e: Exception) { }
    }
}