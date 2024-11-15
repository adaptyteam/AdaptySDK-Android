@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CrossplatformMetaRetriever {

    private val crossplatformClass: Class<*>? by lazy {
        getClassForNameOrNull("com.adapty.internal.crossplatform.CrossplatformHelper")
    }

    private val metaClass: Class<*>? by lazy {
        getClassForNameOrNull("com.adapty.internal.crossplatform.MetaInfo")
    }

    @get:JvmSynthetic
    val crossplatformNameAndVersion: Pair<String, String>?
        get() {
            val meta = getDeclaredFieldOrNull<Any>(crossplatformClass, "meta", crossplatformClass) ?: return null
            val name = getDeclaredFieldOrNull<String>(metaClass, "name", meta) ?: return null
            val version = getDeclaredFieldOrNull<String>(metaClass, "version", meta) ?: return null
            return name to version
        }

    private fun <T> getDeclaredFieldOrNull(sourceClass: Class<*>?, name: String, obj: Any?) =
        try {
            sourceClass?.getDeclaredField(name)?.apply { isAccessible = true }?.get(obj) as? T
        } catch (e: Exception) { null }
}