@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.RestrictTo
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.TypefaceCompat
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object TypefaceHolder {

    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun getOrPut(context: Context, font: Asset.Font): Typeface {
        val fontKey = "${font.resources.joinToString()}-${font.familyName}-${font.weight}-${font.isItalic}"

        return typefaceCache.getOrPut(fontKey) {
            TypefaceCompat.create(
                context,
                getFontFromResOrNull(context, font.resources)
                    ?: Typeface.create(font.familyName.lowercase(Locale.ENGLISH), Typeface.NORMAL),
                font.weight,
                font.isItalic
            )
        }
    }

    private fun getFontFromResOrNull(context: Context, resourceIds: Iterable<String>): Typeface? {
        for (fontRes in resourceIds) {
            val resId = context.resources.getIdentifier(fontRes, "font", context.packageName)

            if (resId != 0) {
                val font = kotlin.runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull()
                if (font != null) return font
            }
        }
        return null
    }
}