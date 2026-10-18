@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.compose.ui.geometry.Offset
import androidx.core.net.toUri
import com.adapty.ui.AdaptyUI.FlowConfiguration
import com.adapty.ui.AdaptyCustomImageAsset.Local
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Gradient
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Gradient.Points
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Gradient.Type
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Gradient.Value
import com.adapty.utils.FileLocation
import kotlin.math.cos
import kotlin.math.sin

public class AdaptyCustomAssets internal constructor(
    private val assets: Map<String, AdaptyCustomAsset>,
) {
    public companion object {
        @JvmField
        public val Empty: AdaptyCustomAssets = of()

        @JvmStatic
        public fun of(vararg assets: Pair<String, AdaptyCustomAsset>): AdaptyCustomAssets =
            of(assets.toMap())

        @JvmStatic
        public fun of(assets: Map<String, AdaptyCustomAsset>): AdaptyCustomAssets =
            AdaptyCustomAssets(assets)
    }

    internal fun getColor(id: String) = assets.firstOrNull<AdaptyCustomColorAsset>(id)
    internal fun getGradient(id: String) = assets.firstOrNull<AdaptyCustomGradientAsset>(id)
    internal fun getImage(id: String) = assets.firstOrNull<AdaptyCustomImageAsset<*>>(id)
    internal fun getVideo(id: String) = assets.firstOrNull<AdaptyCustomVideoAsset>(id)
    internal fun getFont(id: String) = assets.firstOrNull<AdaptyCustomFontAsset>(id)

    private inline fun <reified T: AdaptyCustomAsset> Map<String, AdaptyCustomAsset>.firstOrNull(id: String) =
        firstNotNullOfOrNull { (k, v) -> if (v is T && k == id) v else null }

    internal fun getFirstAvailable(id: String, priorities: List<AssetType>): AdaptyCustomAsset? {
        for (type in priorities) {
            val result: AdaptyCustomAsset? = when (type) {
                AssetType.COLOR -> getColor(id)
                AssetType.GRADIENT -> getGradient(id)
                AssetType.IMAGE -> getImage(id)
                AssetType.VIDEO -> getVideo(id)
                AssetType.FONT -> getFont(id)
            }
            if (result != null) return result
        }
        return null
    }

    internal enum class AssetType {
        COLOR, GRADIENT, IMAGE, VIDEO, FONT
    }
}

public sealed class AdaptyCustomAsset

public sealed class AdaptyCustomImageAsset<T>(
    internal val value: T,
): AdaptyCustomAsset() {
    public class Remote internal constructor(value: FlowConfiguration.Asset.RemoteImage): AdaptyCustomImageAsset<FlowConfiguration.Asset.RemoteImage>(value)
    public class Local internal constructor(value: FlowConfiguration.Asset.Image): AdaptyCustomImageAsset<FlowConfiguration.Asset.Image>(value)

    public companion object {
        @JvmStatic
        public fun remote(url: String, preview: Local?): Remote {
            return Remote(
                FlowConfiguration.Asset.RemoteImage(
                    url,
                    preview?.value,
                ),
            )
        }

        @JvmStatic
        public fun file(fileLocation: FileLocation): Local {
            val source = when (fileLocation) {
                is FileLocation.Asset -> FlowConfiguration.Asset.Image.Source.AndroidAsset(fileLocation.relativePath)
                is FileLocation.Uri -> FlowConfiguration.Asset.Image.Source.Uri(fileLocation.uri)
            }

            return Local(
                FlowConfiguration.Asset.Image(source),
            )
        }

        @JvmStatic
        public fun bitmap(bitmap: Bitmap): Local {
            return Local(
                FlowConfiguration.Asset.Image(
                    FlowConfiguration.Asset.Image.Source.Bitmap(bitmap),
                ),
            )
        }
    }
}

public class AdaptyCustomVideoAsset internal constructor(
    internal val value: FlowConfiguration.Asset.Video,
    internal val preview: AdaptyCustomImageAsset<*>?,
): AdaptyCustomAsset() {

    public class Resolution(
        public val width: Int,
        public val height: Int,
    )

    public companion object {
        @JvmStatic
        @JvmOverloads
        public fun remote(url: String, preview: Local?, resolution: Resolution? = null): AdaptyCustomVideoAsset {
            return AdaptyCustomVideoAsset(
                FlowConfiguration.Asset.Video(
                    FlowConfiguration.Asset.Video.Source.Uri(url.toUri()),
                    vRes = resolution?.height ?: 0,
                    hRes = resolution?.width ?: 0,
                ),
                preview,
            )
        }

        @JvmStatic
        @JvmOverloads
        public fun file(fileLocation: FileLocation, preview: Local?, resolution: Resolution? = null): AdaptyCustomVideoAsset {
            val source = when (fileLocation) {
                is FileLocation.Asset -> FlowConfiguration.Asset.Video.Source.AndroidAsset(fileLocation.relativePath)
                is FileLocation.Uri -> FlowConfiguration.Asset.Video.Source.Uri(fileLocation.uri)
            }

            return AdaptyCustomVideoAsset(
                FlowConfiguration.Asset.Video(
                    source,
                    vRes = resolution?.height ?: 0,
                    hRes = resolution?.width ?: 0,
                ),
                preview,
            )
        }
    }
}

/**
 * @suppress
 */
public class AdaptyCustomColorAsset private constructor(
    internal val value: FlowConfiguration.Asset.Color,
): AdaptyCustomAsset() {

    public companion object {
        public fun of(@ColorInt color: Int): AdaptyCustomColorAsset =
            AdaptyCustomColorAsset(FlowConfiguration.Asset.Color(color))
    }
}

/**
 * @suppress
 */
public class AdaptyCustomGradientAsset internal constructor(
    internal val value: Gradient,
): AdaptyCustomAsset() {

    public class ColorStop(
        internal val position: Float,
        @ColorInt internal val color: Int,
    )

    public companion object {
        @JvmStatic
        public fun linear(colorStops: List<ColorStop>, startX: Float, startY: Float, endX: Float, endY: Float): AdaptyCustomGradientAsset =
            linear(colorStops, Points(startX, startY, endX, endY))

        private fun linear(values: List<ColorStop>, points: Points): AdaptyCustomGradientAsset =
            of(Type.LINEAR, values, points)

        @JvmStatic
        public fun radial(
            colorStops: List<ColorStop>,
            centerX: Float,
            centerY: Float,
            startRadius: Float,
            endRadius: Float,
        ): AdaptyCustomGradientAsset {
            val direction = Offset(1f, 0f)

            val x1 = centerX + direction.x * endRadius
            val y1 = centerY + direction.y * endRadius

            return of(
                Type.RADIAL,
                if (startRadius == 0f) colorStops else withFakeStartRadius(colorStops, startRadius / endRadius),
                Points(centerX, centerY, x1, y1),
            )
        }

        @JvmStatic
        public fun sweep(
            colorStops: List<ColorStop>,
            centerX: Float,
            centerY: Float,
            angle: Float = 0f,
        ): AdaptyCustomGradientAsset {
            val radians = Math.toRadians(angle.toDouble())
            val dx = cos(radians).toFloat()
            val dy = sin(radians).toFloat()

            val x1 = centerX + dx
            val y1 = centerY + dy

            return of(
                Type.CONIC,
                colorStops,
                Points(centerX, centerY, x1, y1),
            )
        }

        private fun of(type: Type, colorStops: List<ColorStop>, points: Points): AdaptyCustomGradientAsset {
            val values = colorStops.map { colorStop ->
                Value(
                    colorStop.position,
                    FlowConfiguration.Asset.Color(colorStop.color)
                )
            }
            return AdaptyCustomGradientAsset(
                Gradient(type, values, points)
            )
        }

        private fun withFakeStartRadius(
            colorStops: List<ColorStop>,
            normalizedStart: Float
        ): List<ColorStop> {
            val firstColor = colorStops.firstOrNull()?.color ?: android.graphics.Color.BLACK
            val padded = mutableListOf<ColorStop>()

            padded += ColorStop(0f, firstColor)
            padded += ColorStop(normalizedStart, firstColor)

            colorStops.forEach { colorStop ->
                val remapped = normalizedStart + (1f - normalizedStart) * colorStop.position
                padded += ColorStop(remapped, colorStop.color)
            }

            return padded
        }

    }
}

/**
 * @suppress
 */
public class AdaptyCustomFontAsset internal constructor(
    internal val value: FlowConfiguration.Asset.Font,
): AdaptyCustomAsset() {
    public companion object {
        public fun of(font: FlowConfiguration.Asset.Font): AdaptyCustomFontAsset =
            AdaptyCustomFontAsset(font)
    }
}