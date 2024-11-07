package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

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
                    listOf(Map::class.java),
                    "preloadMedia",
                    adaptyUiInstance,
                    listOf(rawConfig),
                )
            }
        } catch (e: Throwable) { }
    }

    private fun <T> getDeclaredFieldOrNull(sourceClass: Class<*>?, name: String, obj: Any?) =
        try {
            sourceClass?.getDeclaredField(name)?.apply { isAccessible = true }?.get(obj) as? T
        } catch (e: Exception) { null }

    private inline fun <reified T> invokeDeclaredMethodIfExists(
            sourceClass: Class<*>?,
            parameterTypes: Collection<Class<*>>,
            name: String,
            obj: Any?,
            args: Collection<Any?>,
        ): T? {
            return kotlin.runCatching {
                invokeDeclaredMethod(sourceClass, parameterTypes, name, obj, args) as T
            }.getOrElse { e ->
                Logger.log(ERROR) { "couldn't invoke method '$name': (${e.localizedMessage})" }
                return null
            }
        }

    private fun invokeDeclaredMethod(
        sourceClass: Class<*>?,
        parameterTypes: Collection<Class<*>>,
        name: String,
        obj: Any?,
        args: Collection<Any?>,
    ) =
        sourceClass?.getDeclaredMethod(name, *parameterTypes.toTypedArray())
            ?.apply { isAccessible = true }?.invoke(obj, *args.toTypedArray())
}