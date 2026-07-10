package com.adapty.internal.crossplatform

internal class RetryLazyVal<T : Any>(private val initializer: () -> T?) : Lazy<T?> {

    private var cachedValue: T? = null

    override val value: T?
        get() = cachedValue ?: initializer()?.also { cachedValue = it }

    override fun isInitialized(): Boolean = cachedValue != null
}

internal fun <T: Any> retryLazy(initializer: () -> T?) = RetryLazyVal(initializer)
