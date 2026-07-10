@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform

import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyRemoteConfig
import com.android.billingclient.api.ProductDetails
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

internal class SerializationExclusionStrategy : ExclusionStrategy {

    override fun shouldSkipField(f: FieldAttributes): Boolean {
        return (f.declaringClass == AdaptyRemoteConfig::class.java && f.name == "dataMap")
                || (f.declaringClass == BackendProduct::class.java && f.name == "paywallProductIndex")
                || (f.declaringClass == BackendProduct::class.java && f.name == "duration")
    }

    override fun shouldSkipClass(clazz: Class<*>): Boolean {
        return clazz == ProductDetails::class.java
    }
}