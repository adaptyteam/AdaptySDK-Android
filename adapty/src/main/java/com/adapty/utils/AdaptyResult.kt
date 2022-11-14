package com.adapty.utils

import com.adapty.errors.AdaptyError

public sealed class AdaptyResult<out T> {
    public class Success<T> internal constructor(public val value: T) : AdaptyResult<T>()
    public class Error internal constructor(public val error: AdaptyError) : AdaptyResult<Nothing>()
}
