package com.adapty.ui.onboardings.internal.serialization

internal interface Deserializer<T> {

    fun deserialize(input: String): Result<T>
}