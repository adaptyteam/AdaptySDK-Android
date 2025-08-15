@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.CONFIGURATION_FORMAT_VERSION

internal class IfElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
    private val hasVideoSupport: Boolean,
) : BaseUIComplexElementMapper("if", commonAttributeMapper), UIComplexShrinkableElementMapper {
    override fun map(
        config: Map<*, *>,
        assets: Assets,
        refBundles: ReferenceBundles,
        stateMap: MutableMap<String, Any>,
        inheritShrink: Int,
        childMapper: ChildMapperShrinkable,
    ): UIElement {
        val platform = config["platform"]
        val version = config["version"]
        val key = when {
            (platform != null && platform != "android")
                    || (version is String && !CONFIGURATION_FORMAT_VERSION.isSameOrNewerVersionThan(version)) -> "else"
            else -> {
                listOf("then", "else").firstOrNull { key ->
                    hasVideoSupport || (config[key] as? Map<*, *>)?.get("type") != "video"
                } ?: "then"
            }
        }
        return (config[key] as? Map<*, *>)?.let { item -> childMapper(item, inheritShrink) }
            ?: throw adaptyError(
                message = "$key in If must not be empty",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
    }

    private fun String.isSameOrNewerVersionThan(older: String): Boolean {
        fun stringVersionToList(value: String): List<Int> {
            return value.split('.', '-', ' ')
                .map { it.toIntOrNull() ?: 0 }
        }
        return stringVersionToList(this)
            .isSameOrNewerVersionThan(stringVersionToList(older))
    }

    private fun List<Int>.isSameOrNewerVersionThan(older: List<Int>): Boolean {
        val newer = this.toMutableList()
        val diffCount = older.size - newer.size
        if (diffCount > 0) {
            repeat(diffCount) { newer.add(0) }
        }
        for ((index, newerElement) in newer.withIndex()) {
            val olderElement = older.getOrNull(index) ?: return true
            when {
                newerElement > olderElement -> return true
                newerElement < olderElement -> return false
            }
        }
        return true
    }
}