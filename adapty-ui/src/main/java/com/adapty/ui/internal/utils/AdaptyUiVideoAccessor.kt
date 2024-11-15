@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.di.DIObject
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.getClassForNameOrNull
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.mapping.element.UIElementMapper
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import kotlin.reflect.KClass

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyUiVideoAccessor {

    private val videoElementMapperClass: Class<*>? by lazy {
        getClassForNameOrNull("com.adapty.ui.internal.mapping.element.VideoElementMapper")
    }

    private val utilClass: Class<*>? by lazy {
        getClassForNameOrNull("com.adapty.ui.internal.utils.VideoUtils")
    }

    fun createVideoElementMapperOrNull(commonAttributeMapper: CommonAttributeMapper): UIElementMapper? =
        kotlin.runCatching {
            videoElementMapperClass
                ?.getDeclaredConstructor(CommonAttributeMapper::class.java)
                ?.newInstance(commonAttributeMapper)
        }.getOrNull() as? UIElementMapper

    fun provideDeps(context: Context): Iterable<Pair<KClass<*>, Map<String?, DIObject<*>>>>? {
        val utilClass = utilClass ?: return null
        return invokeDeclaredMethodIfExists<Iterable<Pair<KClass<*>, Map<String?, DIObject<*>>>>>(
            utilClass,
            listOf(Context::class.java),
            "providePlayerDeps",
            null,
            listOf(context),
        )
    }

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
            log(ERROR) { "$LOG_PREFIX_ERROR couldn't invoke method '$name': (${e.localizedMessage})" }
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