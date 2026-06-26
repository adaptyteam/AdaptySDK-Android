package com.adapty.ui.internal.utils

internal const val LOADING_BG_COLOR = 0x80000000
internal const val LOADING_SIZE = 64
internal const val DISABLED_ALPHA = 0.4f
internal const val LOADING_PRODUCTS_RETRY_DELAY = 2000L
internal const val DEFAULT_PRODUCT_GROUP = "group_A"
internal const val NAVIGATION_TRANSITION_KEY_PREFIX = "navigation_transition_"
internal const val DARK_THEME_ASSET_SUFFIX = "@dark"
internal const val RTL_VARIANT_SUFFIX = "@rtl"
internal const val CUSTOM_ASSET_SUFFIX = "\$\$custom"
internal const val NO_SHRINK = 0b0
internal const val HOUR_MILLIS = 3600 * 1000L
internal const val VERSION_NAME = "4.0.0-beta.1"
internal const val LOG_PREFIX = "UI v${VERSION_NAME}:"
internal const val LOG_PREFIX_ERROR = "UI v${VERSION_NAME} error:"
internal const val CONFIGURATION_FORMAT_VERSION = "5.0.0"
internal const val BUILDER_VERSION = "5_0"

internal const val FORMAT_VERSION_5_0_0 = "5.0.0"

internal fun String.isSameOrNewerVersionThan(older: String): Boolean {
    fun stringVersionToList(value: String): List<Int> {
        return value.split('.', '-', ' ')
            .map { it.toIntOrNull() ?: 0 }
    }
    val newer = stringVersionToList(this).toMutableList()
    val olderList = stringVersionToList(older)
    val diffCount = olderList.size - newer.size
    if (diffCount > 0) {
        repeat(diffCount) { newer.add(0) }
    }
    for ((index, newerElement) in newer.withIndex()) {
        val olderElement = olderList.getOrNull(index) ?: return true
        when {
            newerElement > olderElement -> return true
            newerElement < olderElement -> return false
        }
    }
    return true
}
