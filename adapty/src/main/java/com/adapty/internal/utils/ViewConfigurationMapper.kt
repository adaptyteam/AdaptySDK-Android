package com.adapty.internal.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.ViewConfigurationConfig
import com.adapty.internal.data.models.ViewConfigurationDto
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.models.AdaptyViewConfiguration.*
import kotlin.math.abs

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ViewConfigurationMapper {

    @JvmSynthetic
    fun map(viewConfig: ViewConfigurationDto): AdaptyViewConfiguration {
        val config = viewConfig.config

        return AdaptyViewConfiguration(
            id = viewConfig.id ?: throw AdaptyError(
                message = "id in ViewConfiguration should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            isHard = config?.isHard ?: false,
            templateId = config?.templateId,
            defaultLocalization = config?.defaultLocalization,
            mainImageRelativeHeight = config?.mainImageRelativeHeight ?: 1f,
            assets = mapVisualAssets(config?.assets),
            localizations = config?.localizations?.associate { localization ->
                if (localization.id == null) {
                    throw AdaptyError(
                        message = "id in Localization should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }

                val strings = localization.strings?.associate { str ->
                    if (str.id == null || str.value == null) {
                        throw AdaptyError(
                            message = "id and value in strings in Localization should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    }

                    str.id to str.value
                }.orEmpty()

                val assets = mapVisualAssets(localization.assets)

                localization.id to Localization(strings, assets)
            }.orEmpty(),
            styles = config?.styles?.mapValues { (_, styleValue) ->
                (styleValue as? Map<*, *>)?.let(::mapStyle)
            }.orEmpty()
        )
    }

    private fun mapStyle(value: Map<*, *>): Style {
        val value = value.toMutableMap()

        val productBlock = (value.remove("products_block") as? Map<*, *>)?.let { productBlock ->
            val type = when (val typeStr = productBlock["type"]) {
                "horizontal" -> ProductBlock.Type.HORIZONTAL
                "single" -> ProductBlock.Type.SINGLE
                "vertical" -> ProductBlock.Type.VERTICAL
                else -> throw AdaptyError(
                    message = "Unsupported type (\"$typeStr\") in products_block",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }

            val mainProductIndex = (productBlock["main_product_index"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0

            val orderedItems =
                (productBlock["infos"] as? Map<*, *>)?.let(::mapCustomObjectComponent)?.properties?.map { (_, v) -> v } ?: throw AdaptyError(
                    message = "infos in ProductBlock should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )

            ProductBlock(type, mainProductIndex, orderedItems)
        } ?: throw AdaptyError(
            message = "products_block in style should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

        val featureBlock = (value.remove("features_block") as? Map<*, *>)?.let { featureBlock ->
            val type = when (val typeStr = featureBlock["type"]) {
                "list" -> FeatureBlock.Type.LIST
                "timeline" -> FeatureBlock.Type.TIMELINE
                else -> throw AdaptyError(
                    message = "Unsupported type (\"$typeStr\") in features_block",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }

            val orderedItems = featureBlock.values.filterIsInstance<Map<*, *>>().sortedWith(Comparator { firstMap, secondMap ->
                compareByOrder(firstMap, secondMap)
            }).mapNotNull(::mapVisualStyleComponent)

            FeatureBlock(type, orderedItems)
        }

        val footerBlock = (value.remove("footer_block") as? Map<*, *>)?.let { footerBlock ->
            val orderedItems = footerBlock.values.filterIsInstance<Map<*, *>>().sortedWith(Comparator { firstMap, secondMap ->
                compareByOrder(firstMap, secondMap)
            }).mapNotNull(::mapVisualStyleComponent)

            FooterBlock(orderedItems)
        }

        val items = value.mapNotNull { (k, v) ->
            when {
                k !is String -> null
                v == null -> null
                else -> {
                    mapVisualStyleComponent(v)?.let { mappedValue -> k to mappedValue }
                }
            }
        }.toMap()

        return Style(featureBlock, productBlock, footerBlock, items)
    }

    private fun mapVisualStyleComponent(value: Any?): Component? =
        when (value) {
            is String -> Component.Reference(value)
            is Map<*, *> -> {
                when(value["type"]) {
                    "shape", "rectangle", "rect", "circle", "curve_up", "curve_down" -> mapShapeComponent(value)
                    "text" -> mapTextComponent(value)
                    "button" -> mapButtonComponent(value)
                    else -> mapCustomObjectComponent(value)
                }
            }
            else -> null
        }

    private fun mapShapeComponent(map: Map<*, *>): Component.Shape {
        val rawType = map["type"]?.takeIf { it != "shape" } ?: map["value"]

        val type = when(rawType) {
            "circle" -> Component.Shape.Type.Circle
            "curve_up" -> Component.Shape.Type.RectWithArc(Component.Shape.Type.RectWithArc.ABS_ARC_HEIGHT)
            "curve_down" -> Component.Shape.Type.RectWithArc(-Component.Shape.Type.RectWithArc.ABS_ARC_HEIGHT)
            else -> {
                val cornerRadius = map["rect_corner_radius"]
                Component.Shape.Type.Rectangle(
                    when(cornerRadius) {
                        is Number -> Component.Shape.CornerRadius.Same(cornerRadius.toFloat())
                        is List<*> -> {
                            when {
                                cornerRadius.isEmpty() -> Component.Shape.CornerRadius.None
                                cornerRadius.size == 1 -> Component.Shape.CornerRadius.Same((cornerRadius[0] as? Number)?.toFloat() ?: 0f)
                                else -> {
                                    val tl = (cornerRadius.getOrNull(0) as? Number)?.toFloat() ?: 0f
                                    val tr = (cornerRadius.getOrNull(1) as? Number)?.toFloat() ?: 0f
                                    val br = (cornerRadius.getOrNull(2) as? Number)?.toFloat() ?: 0f
                                    val bl = (cornerRadius.getOrNull(3) as? Number)?.toFloat() ?: 0f

                                    Component.Shape.CornerRadius.Different(tl, tr, br, bl)
                                }
                            }
                        }
                        is Map<*, *> -> {
                            when {
                                cornerRadius.isEmpty() -> Component.Shape.CornerRadius.None
                                else -> {
                                    val tl = (cornerRadius["tl"] as? Number)?.toFloat() ?: 0f
                                    val tr = (cornerRadius["tr"] as? Number)?.toFloat() ?: 0f
                                    val br = (cornerRadius["br"] as? Number)?.toFloat() ?: 0f
                                    val bl = (cornerRadius["bl"] as? Number)?.toFloat() ?: 0f

                                    Component.Shape.CornerRadius.Different(tl, tr, br, bl)
                                }
                            }
                        }
                        else -> Component.Shape.CornerRadius.None
                    }
                )
            }
        }

        return Component.Shape(
            backgroundAssetId = map["background"] as? String,
            type = type,
            (map["border"] as? String)?.let { assetId ->
                Component.Shape.Border(assetId, (map["thickness"] as? Number)?.toFloat() ?: 0f)
            }
        )
    }

    private fun mapTextComponent(value: Map<*, *>) : Component.Text {
        val size = (value["size"] as? Number)?.toFloat()
        val textColorId = value["color"] as? String
        val fontId = value["font"] as? String
        val horizontalAlign = mapHorizontalAlign(value["horizontal_align"] as? String)
        val bulletSpace = (value["bullet_space"] as? Number)?.toFloat()

        var currentBullet: Component.Text.Item.BulletedText.Bullet? = null
        var spaceForCurrentBullet: Component.Text.Item.Space? = null
        val items = (value["items"] as? List<*>)?.mapIndexedNotNull { i, item ->
            (item as? Map<*, *>)?.let { map ->
                if (map["newline"] != null) {
                    currentBullet = null
                    spaceForCurrentBullet = null
                    return@mapIndexedNotNull Component.Text.Item.NewLine
                }

                val space = (map["space"] as? Number)?.toFloat()

                if (space != null) {
                    val space = Component.Text.Item.Space(space)

                    if (currentBullet == null) {
                        return@mapIndexedNotNull space
                    } else {
                        spaceForCurrentBullet = space
                        return@mapIndexedNotNull null
                    }
                }

                val stringId = map["string_id"] as? String

                if (stringId != null) {
                    val text = Component.Text.Item.Text(
                        stringId,
                        ((map["font"] as? String) ?: fontId) ?: throw AdaptyError(
                            message = "fontId in TextItem should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        (map["size"] as? Number)?.toFloat() ?: size,
                        (map["color"] as? String) ?: textColorId,
                        (map["horizontal_align"] as? String)?.let(::mapHorizontalAlign) ?: horizontalAlign,
                    )

                    if ((map["bullet"] as? Boolean) == true) {
                        currentBullet = Component.Text.Item.BulletedText.TextBullet(text)
                        return@mapIndexedNotNull Component.Text.Item.NewLine.takeIf { i > 0 }
                    } else {
                        val bullet = currentBullet
                        if (bullet != null) {
                            val bulletedText = Component.Text.Item.BulletedText(
                                bullet,
                                spaceForCurrentBullet ?: bulletSpace?.let { bulletSpace -> Component.Text.Item.Space(bulletSpace) },
                                text
                            )
                            currentBullet = null
                            spaceForCurrentBullet = null
                            return@mapIndexedNotNull bulletedText
                        } else {
                            return@mapIndexedNotNull text
                        }
                    }
                }

                val imageId = map["image"] as? String

                if (imageId != null) {
                    val image = Component.Text.Item.Image(
                        imageId,
                        map["color"] as? String,
                        (map["width"] as? Number)?.toFloat() ?: throw AdaptyError(
                            message = "width in ImageItem should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        (map["height"] as? Number)?.toFloat() ?: throw AdaptyError(
                            message = "height in ImageItem should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                    )

                    if ((map["bullet"] as? Boolean) == true) {
                        currentBullet = Component.Text.Item.BulletedText.ImageBullet(image)
                        return@mapIndexedNotNull Component.Text.Item.NewLine.takeIf { i > 0 }
                    } else {
                        return@mapIndexedNotNull image
                    }
                }

                return@mapIndexedNotNull null
            }
        }

        return if (items != null) {
            Component.Text.Multiple(items, horizontalAlign)
        } else {
            Component.Text.Single(
                (value["string_id"] as? String) ?: throw AdaptyError(
                    message = "stringId in Text should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
                fontId ?: throw AdaptyError(
                    message = "font in Text should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
                size,
                textColorId,
                horizontalAlign,
            )
        }
    }

    private fun mapButtonComponent(value: Map<*, *>) : Component.Button {
        return Component.Button(
            (value["shape"] as? Map<*, *>)?.let(::mapShapeComponent),
            (value["selected_shape"] as? Map<*, *>)?.let(::mapShapeComponent),
            (value["title"] as? Map<*, *>)?.let(::mapTextComponent),
            (value["selected_title"] as? Map<*, *>)?.let(::mapTextComponent),
            mapButtonAlign(value["align"] as? String),
            (value["action"] as? Map<*, *>)?.let(::mapButtonAction),
        )
    }

    private fun mapCustomObjectComponent(value: Map<*, *>) : Component.CustomObject? {
        val type = value["type"] as? String ?: return null

        val properties = value.mapNotNull { (k, v) ->
            when (k) {
                !is String -> null
                "type", "order" -> null
                else -> k to v
            }
        }.sortedWith(Comparator { (_, firstValue), (_, secondValue) ->
            compareByOrder(firstValue, secondValue)
        }).mapNotNull { (k, v) -> mapVisualStyleComponent(v)?.let { k to it }  }

        return Component.CustomObject(type, properties)
    }

    private fun compareByOrder(firstMap: Any?, secondMap: Any?): Int {
        val firstOrder = ((firstMap as? Map<*, *>)?.get("order") as? Number)?.toInt() ?: return 0
        val secondOrder = ((secondMap as? Map<*, *>)?.get("order") as? Number)?.toInt() ?: return 0
        val diff = firstOrder - secondOrder
        return if (diff == 0) 0 else diff / abs(diff)
    }

    private fun mapHorizontalAlign(value: String?) =
        when (value) {
            "center" -> HorizontalAlign.CENTER
            "right" -> HorizontalAlign.RIGHT
            else -> HorizontalAlign.LEFT
        }

    private fun mapButtonAlign(value: String?) =
        when (value) {
            "leading" -> Component.Button.Align.LEADING
            "trailing" -> Component.Button.Align.TRAILING
            "fill" -> Component.Button.Align.FILL
            else -> Component.Button.Align.CENTER
        }

    private fun mapButtonAction(value: Map<*, *>): Component.Button.Action? {
        return when (val type = value["type"]) {
            "open_url" -> Component.Button.Action.OpenUrl(
                (value["url"] as? String) ?: throw AdaptyError(
                    message = "url value should not be null when type is open_url",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            )
            "restore" -> Component.Button.Action.Restore
            "close" -> Component.Button.Action.Close
            "custom" -> Component.Button.Action.Custom(
                (value["custom_id"] as? String) ?: throw AdaptyError(
                    message = "custom_id value should not be null when type is custom",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            )
            else -> null
        }
    }

    private fun mapVisualAssets(assets: List<ViewConfigurationConfig.Asset>?): Map<String, Asset> {
        return assets?.mapNotNull { asset ->
            if (asset.id != null && asset.type != null) {
                (when (asset.type) {
                    "image" -> Asset.Image(asset.value as? String)
                    "color" -> Asset.Color(
                        (asset.value as? String)?.let(::mapVisualAssetColorString)
                            ?: throw AdaptyError(
                                message = "color value should not be null",
                                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                            )
                    )
                    "linear-gradient", "radial-gradient", "conic-gradient" -> Asset.Gradient(
                        when (asset.type) {
                            "radial-gradient" -> Asset.Gradient.Type.RADIAL
                            "conic-gradient" -> Asset.Gradient.Type.CONIC
                            else -> Asset.Gradient.Type.LINEAR
                        },
                        (asset.values as? List<*>)?.mapNotNull {
                            (it as? HashMap<*, *>)?.let { value ->
                                val p = (value["p"] as? Number)?.toFloat() ?: return@mapNotNull null
                                val color = Asset.Color((value["color"] as? String)?.let(::mapVisualAssetColorString) ?: return@mapNotNull null)

                                Asset.Gradient.Value(p, color)
                            }
                        } ?: throw AdaptyError(
                            message = "gradient values should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        (asset.points as? HashMap<*, *>)?.let {
                            val x0 = (it["x0"] as? Number)?.toFloat() ?: return@let null
                            val y0 = (it["y0"] as? Number)?.toFloat() ?: return@let null
                            val x1 = (it["x1"] as? Number)?.toFloat() ?: return@let null
                            val y1 = (it["y1"] as? Number)?.toFloat() ?: return@let null

                            Asset.Gradient.Points(x0, y0, x1, y1)
                        } ?: throw AdaptyError(
                            message = "gradient points should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                    )
                    "font" -> Asset.Font(
                        (asset.value as? String) ?: throw AdaptyError(
                            message = "font value should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        asset.style ?: throw AdaptyError(
                            message = "font style should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        asset.size,
                        mapHorizontalAlign(asset.horizontalAlign),
                        asset.color?.let(::mapVisualAssetColorString),
                    )
                    else -> null
                })?.let { asset.id to it }
            } else
                null
        }?.toMap().orEmpty()
    }

    @ColorInt
    private fun mapVisualAssetColorString(colorString: String): Int {
        return try {
            Color.parseColor(
                when (colorString.length) {
                    9 -> rgbaToArgbStr(colorString)
                    else -> colorString
                }
            )
        } catch (e: Exception) {
            throw AdaptyError(
                message = "color value should be a valid #RRGGBB or #RRGGBBAA",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
                originalError = e
            )
        }
    }

    private fun rgbaToArgbStr(rgbaColorString: String): String {
        return rgbaColorString.toCharArray().let { chars ->
            val a1 = chars[7]
            val a2 = chars[8]
            for (i in 8 downTo 3) {
                chars[i] = chars[i - 2]
            }
            chars[1] = a1
            chars[2] = a2
            String(chars)
        }
    }
}