package com.adapty.models

import java.util.*

public enum class AdaptyAttributionSource {
    ADJUST, APPSFLYER, BRANCH, CUSTOM;

    override fun toString(): String = this.name.lowercase(Locale.ENGLISH)
}