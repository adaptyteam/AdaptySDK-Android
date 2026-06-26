package com.adapty.ui.internal.mapping.attributes

import com.adapty.ui.internal.ui.attributes.TextAlign

internal fun Any?.toTextAlign(default: TextAlign = TextAlign.START): TextAlign {
    return when (this) {
        "leading" -> TextAlign.START
        "left" -> TextAlign.LEFT
        "trailing" -> TextAlign.END
        "right" -> TextAlign.RIGHT
        "center" -> TextAlign.CENTER
        "justified" -> TextAlign.JUSTIFY
        else -> default
    }
}
