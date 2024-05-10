package com.adapty.internal.di

import androidx.annotation.RestrictTo
import com.adapty.internal.utils.InternalAdaptyApi

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalAdaptyApi
public class DIObject<T>(
    private val initializer: () -> T,
    private val initType: InitType = InitType.SINGLETON
) {
    private var cachedObject: T? = null

    @JvmSynthetic
    internal fun provide(): T {
        return when (initType) {
            InitType.NEW -> {
                initializer()
            }
            else -> {
                cachedObject ?: initializer().also { cachedObject = it }
            }
        }
    }

    /**
     * @suppress
     */
    @InternalAdaptyApi
    public enum class InitType {
        NEW, SINGLETON
    }
}