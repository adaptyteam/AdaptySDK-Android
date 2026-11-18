@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui

import androidx.annotation.ColorInt
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset

/**
 * An asset of the flow as seen by the content of a `custom` element.
 *
 * Read-only: the values come from the flow configuration with the view's [AdaptyCustomAssets]
 * substitutes applied. To replace an asset of the configuration, pass an [AdaptyCustomAsset] in
 * [AdaptyCustomAssets].
 */
public sealed class AdaptyUiAsset {

    /**
     * [value] is an ARGB color.
     */
    public class Color internal constructor(
        @ColorInt public val value: Int,
    ) : AdaptyUiAsset()

    /**
     * Points are fractions of the element's size (`0`–`1`, where `0, 0` is the top-left corner);
     * radii are in dp. What the points mean depends on [type]:
     * - [Type.LINEAR]: start (`x0`, `y0`) and end (`x1`, `y1`);
     * - [Type.RADIAL]: center (`x0`, `y0`), start radius `x1` and end radius `y1`;
     * - [Type.SWEEP]: center (`x0`, `y0`); [angle] is in degrees, clockwise from 3 o'clock.
     */
    public class Gradient internal constructor(
        public val type: Type,
        public val colorStops: List<ColorStop>,
        public val x0: Float,
        public val y0: Float,
        public val x1: Float,
        public val y1: Float,
        public val angle: Float,
    ) : AdaptyUiAsset() {

        public enum class Type { LINEAR, RADIAL, SWEEP }

        /**
         * [position] is from `0` to `1` along the gradient; [color] is ARGB.
         */
        public class ColorStop internal constructor(
            public val position: Float,
            @ColorInt public val color: Int,
        )
    }

    public sealed class Image : AdaptyUiAsset() {

        public class Remote internal constructor(
            public val url: String,
            public val preview: Local?,
        ) : Image()

        public class Local internal constructor(
            public val source: Source,
        ) : Image()

        public sealed class Source {
            public class Uri internal constructor(public val uri: android.net.Uri) : Source()
            public class AndroidAsset internal constructor(public val path: String) : Source()
            public class Bitmap internal constructor(public val bitmap: android.graphics.Bitmap) : Source()
            /**
             * [value] is `null` when the configuration declares the image without data.
             */
            public class Base64 internal constructor(public val value: String?) : Source()
        }
    }

    /**
     * [width] and [height] are the intrinsic resolution in pixels, or `null` when unknown.
     */
    public class Video internal constructor(
        public val source: Source,
        public val width: Int?,
        public val height: Int?,
        public val preview: Image.Local?,
    ) : AdaptyUiAsset() {

        public sealed class Source {
            public class Uri internal constructor(public val uri: android.net.Uri) : Source()
            public class AndroidAsset internal constructor(public val path: String) : Source()
        }
    }

    /**
     * A font as declared in the configuration.
     *
     * The SDK resolves it by looking up each name in [resources] among the app's font resources
     * (`R.font`) and, if none is found, by the system font family [familyName]. [weight] (1–1000:
     * 400 is regular, 700 is bold) and [isItalic] are applied on top.
     *
     * [size], [letterSpacing] and [lineHeight] are in sp; [color] is ARGB. `null` means the
     * configuration does not set the value.
     */
    public class Font internal constructor(
        public val familyName: String,
        public val resources: List<String>,
        public val weight: Int,
        public val isItalic: Boolean,
        public val size: Float,
        @ColorInt public val color: Int?,
        public val letterSpacing: Float?,
        public val lineHeight: Float?,
    ) : AdaptyUiAsset()

    /**
     * Opaque data that the app interprets according to [format]. The SDK neither parses [value]
     * nor loads [url]; either can be `null`.
     */
    public class Data internal constructor(
        public val format: String,
        public val value: ByteArray?,
        public val url: String?,
    ) : AdaptyUiAsset()
}

internal fun Asset.toUiAsset(): AdaptyUiAsset? = when (this) {
    is Asset.Color -> AdaptyUiAsset.Color(value)
    is Asset.Gradient -> toUiGradient()
    is Asset.Image -> toUiImage()
    is Asset.RemoteImage -> toUiRemoteImage()
    is Asset.Video -> toUiVideo(preview = null)
    is Asset.Font -> toUiFont()
    is Asset.Data -> toUiData()
    is Asset.Unknown -> null
}

internal fun AdaptyCustomAsset.toUiAsset(): AdaptyUiAsset = when (this) {
    is AdaptyCustomColorAsset -> AdaptyUiAsset.Color(value.value)
    is AdaptyCustomGradientAsset -> value.toUiGradient()
    is AdaptyCustomImageAsset.Local -> value.toUiImage()
    is AdaptyCustomImageAsset.Remote -> value.toUiRemoteImage()
    is AdaptyCustomVideoAsset -> value.toUiVideo(preview = (preview as? AdaptyCustomImageAsset.Local)?.value?.toUiImage())
    is AdaptyCustomFontAsset -> value.toUiFont()
    is AdaptyCustomDataAsset -> value.toUiData()
}

private fun Asset.Gradient.toUiGradient() = AdaptyUiAsset.Gradient(
    type = when (type) {
        Asset.Gradient.Type.LINEAR -> AdaptyUiAsset.Gradient.Type.LINEAR
        Asset.Gradient.Type.RADIAL -> AdaptyUiAsset.Gradient.Type.RADIAL
        Asset.Gradient.Type.CONIC -> AdaptyUiAsset.Gradient.Type.SWEEP
    },
    colorStops = values.map { AdaptyUiAsset.Gradient.ColorStop(it.p, it.color.value) },
    x0 = points.x0,
    y0 = points.y0,
    x1 = points.x1,
    y1 = points.y1,
    angle = sweepAngle,
)

private fun Asset.RemoteImage.toUiRemoteImage() = AdaptyUiAsset.Image.Remote(url, preview?.toUiImage())

private fun Asset.Font.toUiFont() =
    AdaptyUiAsset.Font(familyName, resources, weight, isItalic, size, color, letterSpacing, lineHeight)

private fun Asset.Data.toUiData() = AdaptyUiAsset.Data(format, value, url)

private fun Asset.Image.toUiImage() = AdaptyUiAsset.Image.Local(
    when (val source = source) {
        is Asset.Image.Source.Uri -> AdaptyUiAsset.Image.Source.Uri(source.uri)
        is Asset.Image.Source.AndroidAsset -> AdaptyUiAsset.Image.Source.AndroidAsset(source.path)
        is Asset.Image.Source.Bitmap -> AdaptyUiAsset.Image.Source.Bitmap(source.bitmap)
        is Asset.Image.Source.Base64Str -> AdaptyUiAsset.Image.Source.Base64(source.imageBase64)
    }
)

private fun Asset.Video.toUiVideo(preview: AdaptyUiAsset.Image.Local?) = AdaptyUiAsset.Video(
    source = when (val source = source) {
        is Asset.Video.Source.Uri -> AdaptyUiAsset.Video.Source.Uri(source.uri)
        is Asset.Video.Source.AndroidAsset -> AdaptyUiAsset.Video.Source.AndroidAsset(source.path)
    },
    width = hRes.takeIf { it > 0 },
    height = vRes.takeIf { it > 0 },
    preview = preview,
)
