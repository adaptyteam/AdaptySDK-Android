package com.adapty.internal.di

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DIObject<T>(
    private val initializer: () -> T,
    private val initType: InitType = InitType.SINGLETON
) {
    private var cachedObject: T? = null

    @JvmSynthetic
    fun provide(): T {
        return when (initType) {
            InitType.NEW -> {
                initializer()
            }
            else -> {
                cachedObject ?: initializer().also { cachedObject = it }
            }
        }
    }

    internal enum class InitType {
        NEW, SINGLETON
    }
}