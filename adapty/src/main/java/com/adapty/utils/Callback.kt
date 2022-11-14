package com.adapty.utils

import com.adapty.errors.AdaptyError

public fun interface Callback<T> {
    public fun onResult(result: T)
}

public fun interface ResultCallback<T> : Callback<AdaptyResult<@JvmSuppressWildcards T>>

public fun interface ErrorCallback : Callback<AdaptyError?> {
    override fun onResult(result: AdaptyError?)
}