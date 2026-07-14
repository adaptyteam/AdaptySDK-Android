package com.adapty.internal.crossplatform

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import androidx.annotation.ColorInt
import com.adapty.ui.AdaptyCustomAsset
import com.adapty.ui.AdaptyCustomColorAsset
import com.adapty.ui.AdaptyCustomGradientAsset
import com.adapty.ui.AdaptyCustomImageAsset
import com.adapty.ui.AdaptyCustomVideoAsset
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
        const val TYPE = "type"
        const val VALUE = "value"
        const val VALUES = "values"
        const val POINTS = "points"
        const val INVALID_COLOR = -1
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
                            ?: return null
                        AdaptyCustomColorAsset.of(color)
                    }
                    "linear-gradient" -> {
                        val colorStops = kotlin.runCatching { jsonObject.getAsJsonArray(VALUES) }.getOrNull()
                            ?.mapNotNull { element ->
                                if (element !is JsonObject) return@mapNotNull null
                                val color = jsonObject.getStringOrNull("color")
                                    ?.asColorOrNull()
                                    ?: return null
                                val position = element.getFloatOrNull("p")
                                    ?: return null
                                AdaptyCustomGradientAsset.ColorStop(position, color)
                            }
                            ?: return null

                        val points = kotlin.runCatching { jsonObject.getAsJsonObject(POINTS) }.getOrNull()
                            ?: return null
                        val x0 = points.getFloatOrNull("x0") ?: return null
                        val x1 = points.getFloatOrNull("x1") ?: return null
                        val y0 = points.getFloatOrNull("y0") ?: return null
                        val y1 = points.getFloatOrNull("y1") ?: return null

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
                            val bitmap = base64.asBitmapOrNull() ?: return null
                            return AdaptyCustomImageAsset.bitmap(bitmap)
                        }
                        
                        val fileLocation = kotlin.runCatching { gson.fromJson(jsonObject, FileLocationArgs::class.java) }
                            .getOrNull()
                            ?: return null

                        AdaptyCustomImageAsset.file(transformFileLocation(fileLocation.value))
                    }
                    "video" -> {
                        val fileLocation = kotlin.runCatching { gson.fromJson(jsonObject, FileLocationArgs::class.java) }
                            .getOrNull()
                            ?: return null

                        AdaptyCustomVideoAsset.file(transformFileLocation(fileLocation.value), null)
                    }
                    else -> null
                }
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }

    private fun JsonObject.getStringOrNull(key: String) =
        kotlin.runCatching { this.getAsJsonPrimitive(key).asString }.getOrNull()

    private fun JsonObject.getFloatOrNull(key: String) =
        kotlin.runCatching { this.getAsJsonPrimitive(key).asNumber }.getOrNull()
            ?.toFloat()

    private fun String.asBitmapOrNull() =
        runCatching {
            val byteArray = Base64.decode(this, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }.getOrNull()

    private fun String.asColorOrNull() =
        extractColor(this).takeIf { it != INVALID_COLOR }

    @ColorInt
    private fun extractColor(colorString: String): Int {
        return kotlin.runCatching {
            Color.parseColor(
                when (colorString.length) {
                    9 -> rgbaToArgbStr(colorString)
                    else -> colorString
                }
            )
        }.getOrNull() ?: INVALID_COLOR
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