package com.adapty.internal.crossplatform

class MetaInfo private constructor(private val name: String, private val version: String) {
    companion object {
        @JvmStatic
        fun from(crossplatformName: String, sdkVersion: String) =
            MetaInfo(crossplatformName, sdkVersion)
    }
}
