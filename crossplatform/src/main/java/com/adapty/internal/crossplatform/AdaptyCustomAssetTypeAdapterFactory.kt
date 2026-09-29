@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import androidx.annotation.ColorInt
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.log
import com.adapty.ui.AdaptyCustomAsset
import com.adapty.ui.AdaptyCustomColorAsset
import com.adapty.ui.AdaptyCustomGradientAsset
import com.adapty.ui.AdaptyCustomImageAsset
import com.adapty.ui.AdaptyCustomVideoAsset
import com.adapty.utils.AdaptyLogLevel
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyCustomAssetTypeAdapterFactory(
    private val transformFileLocation: FileLocationTransformer,
) : TypeAdapterFactory {

    companion object {
        const val ID = "id"
        const val TYPE = "type"
        const val VALUE = "value"
        const val VALUES = "values"
        const val POINTS = "points"
        const val H_RES = "h_res"
        const val V_RES = "v_res"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!AdaptyCustomAsset::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<AdaptyCustomAsset>() {

            override fun write(out: JsonWriter, value: AdaptyCustomAsset) {
                out.nullValue()
            }

            override fun read(`in`: JsonReader): AdaptyCustomAsset? {
                val jsonObject = kotlin.runCatching { elementAdapter.read(`in`).asJsonObject }.getOrNull()
                    ?: return null
                return when (jsonObject.getStringOrNull(TYPE)) {
                    "color" -> {
                        val color = jsonObject.getStringOrNull(VALUE)
                            ?.asColorOrNull()
                            ?: return jsonObject.logAndSkip("invalid or missing color value")
                        AdaptyCustomColorAsset.of(color)
                    }
                    "linear-gradient" -> {
                        val colorStops = kotlin.runCatching { jsonObject.getAsJsonArray(VALUES) }.getOrNull()
                            ?.map { element ->
                                if (element !is JsonObject) return jsonObject.logAndSkip("invalid color stop")
                                val color = element.getStringOrNull("color")
                                    ?.asColorOrNull()
                                    ?: return jsonObject.logAndSkip("invalid or missing color in a color stop")
                                val position = element.getFloatOrNull("p")
                                    ?: return jsonObject.logAndSkip("invalid or missing position in a color stop")
                                AdaptyCustomGradientAsset.ColorStop(position, color)
                            }
                            ?: return jsonObject.logAndSkip("missing color stops")

                        val points = kotlin.runCatching { jsonObject.getAsJsonObject(POINTS) }.getOrNull()
                            ?: return jsonObject.logAndSkip("missing points")
                        val x0 = points.getFloatOrNull("x0") ?: return jsonObject.logAndSkip("invalid points")
                        val x1 = points.getFloatOrNull("x1") ?: return jsonObject.logAndSkip("invalid points")
                        val y0 = points.getFloatOrNull("y0") ?: return jsonObject.logAndSkip("invalid points")
                        val y1 = points.getFloatOrNull("y1") ?: return jsonObject.logAndSkip("invalid points")

                        if (colorStops.size < 2)
                            log(AdaptyLogLevel.WARN, { "custom asset (id: ${jsonObject.getStringOrNull(ID)}): a linear gradient with ${colorStops.size} color stop(s) won't render as a gradient" })

                        AdaptyCustomGradientAsset.linear(
                            colorStops = colorStops,
                            startX = x0,
                            startY = y0,
                            endX = x1,
                            endY = y1,
                        )
                    }
                    "image" -> {
                        val base64 = jsonObject.getStringOrNull(VALUE)

                        if (base64 != null) {
                            val bitmap = base64.asBitmapOrNull() ?: return jsonObject.logAndSkip("corrupted image data")
                            return AdaptyCustomImageAsset.bitmap(bitmap)
                        }

                        val fileLocation = kotlin.runCatching { gson.fromJson(jsonObject, FileLocationArgs::class.java) }
                            .getOrNull()
                            ?: return jsonObject.logAndSkip("invalid or missing file location")

                        AdaptyCustomImageAsset.file(transformFileLocation(fileLocation.value))
                    }
                    "video" -> {
                        val fileLocation = kotlin.runCatching { gson.fromJson(jsonObject, FileLocationArgs::class.java) }
                            .getOrNull()
                            ?: return jsonObject.logAndSkip("invalid or missing file location")

                        AdaptyCustomVideoAsset.file(transformFileLocation(fileLocation.value), null, jsonObject.getVideoResolutionOrNull())
                    }
                    else -> jsonObject.logAndSkip("unknown asset type")
                }
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }

    private fun JsonObject.logAndSkip(reason: String): Nothing? {
        log(AdaptyLogLevel.WARN, { "couldn't deserialize custom asset (id: ${getStringOrNull(ID)}, type: ${getStringOrNull(TYPE)}): $reason" })
        return null
    }

    private fun JsonObject.getVideoResolutionOrNull(): AdaptyCustomVideoAsset.Resolution? {
        val width = getFloatOrNull(H_RES)?.toInt()?.takeIf { it > 0 } ?: return null
        val height = getFloatOrNull(V_RES)?.toInt()?.takeIf { it > 0 } ?: return null
        return AdaptyCustomVideoAsset.Resolution(width, height)
    }

    private fun JsonObject.getStringOrNull(key: String) =
        kotlin.runCatching { this.getAsJsonPrimitive(key).asString }.getOrNull()

    private fun JsonObject.getFloatOrNull(key: String) =
        kotlin.runCatching { this.getAsJsonPrimitive(key).asNumber.toFloat() }.getOrNull()

    private fun String.asBitmapOrNull() =
        runCatching {
            val byteArray = Base64.decode(this, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }.getOrNull()

    @ColorInt
    private fun String.asColorOrNull(): Int? {
        return kotlin.runCatching {
            Color.parseColor(
                when (length) {
                    9 -> rgbaToArgbStr(this)
                    else -> this
                }
            )
        }.getOrNull()
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