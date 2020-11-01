package com.adapty.api

import java.util.*

enum class AttributionType {
    ADJUST, APPSFLYER, BRANCH, CUSTOM;

    override fun toString(): String = this.name.toLowerCase(Locale.ENGLISH)
}