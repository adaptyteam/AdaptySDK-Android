package com.adapty.ui.internal.ui.attributes

public enum class TextAlign {
    CENTER,
    JUSTIFY,
    START,
    LEFT,
    END,
    RIGHT,
}

internal fun TextAlign.toComposeTextAlign() =
    when(this) {
        TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
        TextAlign.JUSTIFY -> androidx.compose.ui.text.style.TextAlign.Justify
        TextAlign.START -> androidx.compose.ui.text.style.TextAlign.Start
        TextAlign.END -> androidx.compose.ui.text.style.TextAlign.End
        TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Left
        TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.Right
    }