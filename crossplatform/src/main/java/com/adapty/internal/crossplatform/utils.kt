package com.adapty.internal.crossplatform

import android.util.Base64
import com.google.gson.JsonElement
import com.google.gson.JsonObject

internal fun String.fromBase64(): String {
    return Base64.decode(this, Base64.DEFAULT).toString(charset("UTF-8"))
}

internal fun String.toBase64(): String {
    return Base64.encodeToString(this.toByteArray(charset("UTF-8")), Base64.DEFAULT)
}

internal fun getClassForNameOrNull(className: String): Class<*>? =
    try {
        Class.forName(className)
    } catch (e: ClassNotFoundException) {
        null
    }

internal fun JsonObject.removeNode(property: String) = remove(property).let { value -> property to value }

internal fun JsonObject.addNodeIfNotEmpty(node: Pair<String, JsonElement?>) {
    node.second?.let { add(node.first, it) }
}

internal fun JsonObject.addNode(node: Pair<String, JsonElement?>, fallback: JsonElement) {
    add(node.first, node.second ?: fallback)
}

internal fun JsonObject.moveNodeIfExists(targetJsonObject: JsonObject, property: String) {
    val node = removeNode(property)
    targetJsonObject.addNodeIfNotEmpty(node)
}

internal fun JsonObject.moveNode(targetJsonObject: JsonObject, property: String, fallback: JsonElement) {
    val node = removeNode(property)
    targetJsonObject.addNode(node, fallback)
}

internal const val CLASS_KEY = "class"