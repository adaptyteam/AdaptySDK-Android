package com.adapty.internal.crossplatform

import android.app.Activity
import com.adapty.utils.FileLocation

fun interface ResultCallback<T> {
    operator fun invoke(result: T)
}

fun interface EventCallback<T> {
    operator fun invoke(name: String, data: T)
}

fun interface FileLocationTransformer {
    operator fun invoke(value: String): FileLocation
}

fun interface ActivityProvider {
    operator fun invoke(): Activity?

    companion object {
        val Empty = ActivityProvider { null }
    }
}
