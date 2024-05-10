package com.adapty.utils

import com.adapty.errors.AdaptyError
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.asAdaptyError
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

public sealed class AdaptyResult<out T> {
    public class Success<T> internal constructor(public val value: T) : AdaptyResult<T>()
    public class Error internal constructor(public val error: AdaptyError) : AdaptyResult<Nothing>()

    /**
     * @suppress
     */
    @InternalAdaptyApi
    public fun <R> map(action: (T) -> R): AdaptyResult<R> {
        return when (this) {
            is Error -> this
            is Success -> runCatching { action(value) }.fold(
                onSuccess = { newValue -> Success(newValue) },
                onFailure = { e ->
                    Logger.log(ERROR) { e.localizedMessage.orEmpty() }
                    Error(e.asAdaptyError())
                }
            )
        }
    }
}
