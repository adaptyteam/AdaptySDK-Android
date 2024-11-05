package com.adapty.ui.internal.mapping.attributes

import com.adapty.ui.internal.ui.attributes.TextAlign

internal class TextAttributeMapper {
    fun mapTextAlign(item: Any?, default: TextAlign = TextAlign.START): TextAlign {
        return when (item) {
            "leading" -> TextAlign.START
            "left" -> TextAlign.LEFT
            "trailing" -> TextAlign.END
            "right" -> TextAlign.RIGHT
            "center" -> TextAlign.CENTER
            "justified" -> TextAlign.JUSTIFY
            else -> default
        }
    }
}