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
    @property:InternalAdaptyApi public val mainImageRelativeHeight: Float,
    private val defaultLocalization: String?,
    private val assets: Map<String, Asset>,
    private val localizations: Map<String, Localization>,
    private val styles: Map<String, Style?>,
) {

    public class Style(
        public val featureBlock: FeatureBlock?,
        public val productBlock: ProductBlock,
        public val footerBlock: FooterBlock?,
        public val items: Map<String, Component>,
    )

    public class FeatureBlock(
        public val type: Type,
        public val orderedItems: List<Component>,
    ) {
        public enum class Type { LIST, TIMELINE }
    }
    public class ProductBlock(
        public val type: Type,
        public val mainProductIndex: Int,
        public val orderedItems: List<Component>,
    ) {
        public enum class Type { SINGLE, VERTICAL, HORIZONTAL }
    }
    public class FooterBlock(
        public val orderedItems: List<Component>,
    )

    public sealed class Component {

        public sealed class Text(
            public val horizontalAlign: HorizontalAlign,
        ) : Component() {

            public class Single(
                public val stringId: String,
                public val fontId: String,
                public val size: Float?,
                public val textColorId: String?,
                horizontalAlign: HorizontalAlign,
            ): Text(horizontalAlign)

            public class Multiple(
                public val items: List<Item>,
                horizontalAlign: HorizontalAlign,
            ): Text(horizontalAlign)
            public sealed class Item {
                public class Text(
                    public val stringId: String,
                    public val fontId: String,
                    public val size: Float?,
                    public val textColorId: String?,
                    public val horizontalAlign: HorizontalAlign,
                ) : Item()

                public class Image(
                    public val imageId: String,
                    public val tintColorId: String?,
                    public val width: Float,
                    public val height: Float,
                ): Item()
                public object NewLine: Item()
                public class Space(public val value: Float): Item()
                public class BulletedText(
                    public val bullet: Bullet,
                    public val space: Space?,
                    public val text: Text,
                ) : Item() {
                    public sealed class Bullet

                    public class ImageBullet(public val image: Image): Bullet()
                    public class TextBullet(public val text: Text): Bullet()
                }
            }
        }

        public class Shape(
            public val backgroundAssetId: String?,
            public val type: Type,
            public val border: Border?,
        ): Component() {
            public sealed class Type {
                public class Rectangle(public val cornerRadius: CornerRadius): Type()
                public object Circle: Type()
                public class RectWithArc(public val arcHeight: Float): Type() {
                    internal companion object {
                        const val ABS_ARC_HEIGHT = 20f
                    }
                }
            }

            public sealed class CornerRadius {
                public object None: CornerRadius()
                public class Same(public val value: Float): CornerRadius()
                public class Different(
                    public val topLeft: Float,
                    public val topRight: Float,
                    public val bottomRight: Float,
                    public val bottomLeft: Float,
                ) : CornerRadius()
            }

            public class Border(
                public val assetId: String,
                public val thickness: Float,
            )
        }

        public class Button(
            public val shape: Shape?,
            public val selectedShape: Shape?,
            public val title: Text?,
            public val selectedTitle: Text?,
            public val align: Align,
            public val action: Action?,
        ): Component() {
            public sealed class Action {
                public object Close: Action()
                public object Restore: Action()
                public class OpenUrl(public val urlId: String): Action()
                public class Custom(public val customId: String): Action()
            }

            public enum class Align {
                LEADING, TRAILING, CENTER, FILL
            }
        }

        public class Reference(
            public val assetId: String,
        ): Component()

        public class CustomObject(
            public val type: String,
            public val properties: List<Pair<String, Component>>,
        ): Component()
    }

    public enum class HorizontalAlign { LEFT, CENTER, RIGHT }

    public sealed class Asset {

        public class Color(
            @ColorInt public val value: Int,
        ): Filling()

        public class Gradient(
            public val type: Type,
            public val values: List<Value>,
            public val points: Points,
        ): Filling() {

            public val colors: IntArray get() = values.map { it.color.value }.toIntArray()
            public val positions: FloatArray get() = values.map { it.p }.toFloatArray()
            public enum class Type { LINEAR, RADIAL, CONIC }

            public class Value(
                public val p: Float,
                public val color: Color,
            )

            public class Points(
                public val x0: Float,
                public val y0: Float,
                public val x1: Float,
                public val y1: Float,
            ) {
                public operator fun component1(): Float = x0
                public operator fun component2(): Float = y0
                public operator fun component3(): Float = x1
                public operator fun component4(): Float = y1
            }
        }

        public class Font(
            public val value: String,
            public val style: String,
            public val size: Float?,
            public val horizontalAlign: HorizontalAlign?,
            @ColorInt public val color: Int?,
        ): Asset()

        public class Image(
            private val imageBase64: String?
        ): Filling() {
            public val bitmap: Bitmap?
                get() {
                    return getBitmap(0, Dimension.WIDTH)
                }

            public fun getBitmap(boundsW: Int, boundsH: Int, scaleType: ScaleType): Bitmap? {
                val dim: Dimension
                val reqDim: Int

                val coef = when (scaleType) {
                    ScaleType.FIT_MAX -> 1
                    ScaleType.FIT_MIN -> -1
                }

                if ((boundsW - boundsH) * coef > 0) {
                    dim = Dimension.WIDTH
                    reqDim = boundsW
                } else {
                    dim = Dimension.HEIGHT
                    reqDim = boundsH
                }

                return getBitmap(reqDim, dim)
            }

            public fun getBitmap(reqDim: Int, dim: Dimension) : Bitmap? {
                if (imageBase64 == null) return null

                val decodedString = Base64.decode(imageBase64, Base64.DEFAULT)

                if (reqDim <= 0) {
                    return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                }

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size, options)
                with(options) {
                    inSampleSize = calculateInSampleSize(
                        when (dim) {
                            Dimension.WIDTH -> options.outWidth
                            Dimension.HEIGHT -> options.outHeight
                        },
                        reqDim,
                    )
                    inJustDecodeBounds = false
                }

                return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size, options)
            }

            private fun calculateInSampleSize(initialDimValue: Int, reqDimValue: Int): Int {
                var inSampleSize = 1

                if (initialDimValue > reqDimValue) {
                    val half: Int = initialDimValue / 2

                    while (half / inSampleSize >= reqDimValue) {
                        inSampleSize *= 2
                    }
                }

                return inSampleSize
            }

            public enum class Dimension { WIDTH, HEIGHT }

            public enum class ScaleType { FIT_MIN, FIT_MAX }
        }

        public sealed class Filling: Asset()
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
    public fun getStyle(styleId: String): Style? = styles[styleId]

    @InternalAdaptyApi
    public fun hasStyle(styleId: String): Boolean = styles[styleId] != null
}