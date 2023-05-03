package com.adapty.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.ColorInt
import com.adapty.internal.utils.InternalAdaptyApi

public class AdaptyViewConfiguration(
    public val id: String,
    public val isHard: Boolean,
    @property:InternalAdaptyApi public val templateId: String?,
    private val defaultLocalization: String?,
    private val privacyUrlId: String?,
    private val termsUrlId: String?,
    private val assets: Map<String, Asset>,
    private val localizations: Map<String, Localization>,
    private val styles: Map<String, Map<String, Any>>,
) {

    public sealed class Component {

        public class Text(
            public val stringId: String,
            public val fontId: String,
            public val size: Float?,
            public val textColorId: String?,
        ): Component()

        public class TextCollection(
            public val texts: List<Text>,
        ): Component()

        public class Reference(
            public val assetId: String,
        ): Component()
    }

    public sealed class Asset {

        public class Color(
            @ColorInt public val value: Int,
        ): Asset()

        public class Font(
            public val value: String,
            public val style: String,
            public val size: Float?,
            @ColorInt public val color: Int?,
        ): Asset()

        public class Image(
            private val imageBase64: String?
        ): Asset() {
            public val bitmap: Bitmap?
                get() {
                    return getBitmap(0)
                }

            public fun getBitmap(reqWidth: Int) : Bitmap? {
                if (imageBase64 == null) return null

                val decodedString = Base64.decode(imageBase64, Base64.DEFAULT)

                if (reqWidth <= 0) {
                    return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                }

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size, options)
                with(options) {
                    inSampleSize = calculateInSampleSize(options.outWidth, reqWidth)
                    inJustDecodeBounds = false
                }

                return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size, options)
            }

            private fun calculateInSampleSize(width: Int, reqWidth: Int): Int {
                var inSampleSize = 1

                if (width > reqWidth) {
                    val halfWidth: Int = width / 2

                    while (halfWidth / inSampleSize >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                return inSampleSize
            }

        }
    }

    public class Localization(
        public val strings: Map<String, String>,
        public val assets: Map<String, Asset>,
    )

    @InternalAdaptyApi
    public fun <T : Asset> getAsset(assetId: String): T? {
        val localeStr = defaultLocalization
        return (localizations[localeStr]?.assets?.get(assetId)
            ?: localizations[defaultLocalization]?.assets?.get(assetId) ?: assets[assetId]) as? T
    }

    @InternalAdaptyApi
    public fun getString(strId: String): String? {
        val localeStr = defaultLocalization
        return (localizations[localeStr]?.strings?.get(strId)
            ?: localizations[defaultLocalization]?.strings?.get(strId))
    }

    @InternalAdaptyApi
    public fun <T : Component> getComponent(componentId: String, styleId: String): T? {
        val style = styles[styleId] ?: return null
        return (style[componentId] ?: (style["custom_properties"] as? Map<*, *>)?.get(componentId)) as? T
    }

    @InternalAdaptyApi
    public fun getPrivacyUrl(): String? =
        privacyUrlId?.let { getString(privacyUrlId) }

    @InternalAdaptyApi
    public fun getTermsUrl(): String? =
        termsUrlId?.let { getString(termsUrlId) }

    @InternalAdaptyApi
    public fun hasStyle(styleId: String): Boolean = styles[styleId] != null
}