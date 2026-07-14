@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.mapping.viewconfig

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.internal.utils.getAs
import com.adapty.ui.internal.utils.DEFAULT_PRODUCT_GROUP
import com.adapty.ui.internal.utils.FORMAT_VERSION_5_0_0
import com.adapty.ui.internal.utils.getProductGroupKey
import com.adapty.ui.internal.utils.isSameOrNewerVersionThan
import java.net.URI
import java.util.Calendar
import java.util.TimeZone
import kotlin.collections.get

internal const val FORMAT = "format"
internal const val STYLES = "styles"
internal const val SCREENS = "screens"
private const val TEMPLATES = "templates"
private const val NAVIGATORS = "navigators"
private const val DEFAULT = "default"
private const val LEGACY_BOTTOM_SHEET = "legacy-bottom-sheet"
private const val SCRIPTS = "scripts"
private const val CONTENT = "content"
private const val TYPE = "type"
private const val FUNC = "func"
private const val PARAMS = "params"
private const val ELEMENT_ID = "element_id"
private const val SELECTED_CONDITION = "selected_condition"
private const val IS_SELECTED = "is_selected"
private const val REFERENCE_TYPE = "reference"
private const val BUTTON_TYPE = "button"
private const val SECTION_TYPE = "section"
private const val PRODUCT_TYPE = "product"
private const val TOGGLE_TYPE = "toggle"
private const val ROW_TYPE = "row"
private const val COLUMN_TYPE = "column"
private const val LEGACY_BUTTON_TYPE = "legacy_button"
private const val LEGACY_ROW_TYPE = "legacy_row"
private const val LEGACY_COLUMN_TYPE = "legacy_column"
private const val GRID_ITEM_TYPE = "grid_item"
private const val ITEMS = "items"
private const val VALUE = "value"
private const val SECTION_ID = "section_id"
private const val ON_INDEX = "on_index"
private const val OFF_INDEX = "off_index"
private const val TEMPLATE_REF_PREFIX = "#"
private const val PRODUCTS = "products"
private const val SELECTED = "selected"

private val LEGACY_SCRIPT = """
    class Legacy {}
    Legacy.productGroup = Object.create(null);
    Legacy.sections = Object.create(null);
    Legacy.selectProduct = function ({ productId, groupId }) {
        Legacy.productGroup[groupId] = productId;
        SDK.onSelectProduct({ productId: productId });
    };
    Legacy.unselectProduct = function ({ groupId }) {
        delete Legacy.productGroup[groupId];
    };
    Legacy.purchaseSelectedProduct = function ({ groupId }) {
        const productId = Legacy.productGroup[groupId];
        if (!productId) { return; }
        SDK.purchaseProduct({ productId: productId });
    };
    Legacy.webPurchaseSelectedProduct = function ({ groupId, openIn }) {
        const productId = Legacy.productGroup[groupId];
        if (!productId) { return; }
        SDK.webPurchaseProduct({ productId: productId, openIn: openIn });
    };
    Legacy.switchSection = function ({ sectionId, index }) {
        Legacy.sections[sectionId] = index;
    };
""".trimIndent()

private val ACTION_TYPE_TO_FUNC = mapOf(
    "open_url" to "SDK.openUrl",
    "custom" to "SDK.userCustomAction",
    "select_product" to "Legacy.selectProduct",
    "unselect_product" to "Legacy.unselectProduct",
    "purchase_product" to "SDK.purchaseProduct",
    "web_purchase_product" to "SDK.webPurchaseProduct",
    "purchase_selected_product" to "Legacy.purchaseSelectedProduct",
    "web_purchase_selected_product" to "Legacy.webPurchaseSelectedProduct",
    "restore" to "SDK.restorePurchases",
    "open_screen" to "SDK.openScreen",
    "close_screen" to "SDK.closeScreen",
    "switch" to "Legacy.switchSection",
    "close" to "SDK.closeAll",
)

private val PARAM_KEY_MAPPING = mapOf(
    "url" to "url",
    "open_in" to "openIn",
    "custom_id" to "userCustomId",
    "product_id" to "productId",
    "group_id" to "groupId",
    "screen_id" to "screenId",
    "navigator_id" to "navigatorId",
    "section_id" to "sectionId",
    "index" to "index",
)

internal fun JsonObject.normalizeViewConfig(): NormalizedConfig {
    rewriteLegacyTimerTagsInPlace(this)
    rewriteLegacyAppearanceInPlace(this)
    rewriteFlexAutoSizeDefaultsInPlace(this)

    val format = getAs<String>(FORMAT) ?: FORMAT_VERSION_5_0_0
    val isNewFormat = format.isSameOrNewerVersionThan(FORMAT_VERSION_5_0_0)

    val templates: MutableMap<String, JsonObject>
    val screensConfig: JsonObject
    val navigatorsConfig: JsonObject?
    val initialScript: String

    if (isNewFormat) {
        templates = extractTemplates(this)
        screensConfig = getAs<JsonObject>(SCREENS)
            ?: throw adaptyError(
                message = "screens in ViewConfiguration should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        navigatorsConfig = getAs<JsonObject>(NAVIGATORS)
        val scriptObj = getAs<JsonArray>(SCRIPTS)
            ?.firstOrNull {
                val type = it.getAs<String>(TYPE)
                type == null || type == "js"
            }
        initialScript = if (scriptObj == null) {
            ""
        } else {
            scriptObj.getAs<String>(CONTENT)
                ?: throw adaptyError(
                    message = "\"js\" script in ViewConfiguration should contain content for v5.0.0+",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
        }
    } else {
        templates = mutableMapOf()
        val stylesConfig = getAs<JsonObject>(STYLES)
            ?: throw adaptyError(
                message = "styles in ViewConfiguration should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        val templateId = getAs<String>("template_id")
            ?: throw adaptyError(
                message = "template_id in ViewConfiguration should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        val layoutBehaviour = when (templateId) {
            "basic" -> "hero"
            else -> templateId
        }
        val bottomSheetKeys = stylesConfig.keys.filter { it != DEFAULT }.toSet()
        @Suppress("UNCHECKED_CAST")
        val initialSectionIndices = (this as MutableMap<String, Any?>).getOrPut("initial_section_indices") { mutableMapOf<String, Int>() } as MutableMap<String, Int>
        val legacyTimerScripts = mutableMapOf<String, String>()
        screensConfig = transformOldFormatToNew(stylesConfig, templates, initialSectionIndices, legacyTimerScripts, bottomSheetKeys)
        val defaultScreenJson = screensConfig[DEFAULT] as? MutableMap<String, Any?>
        val defaultBackground = (defaultScreenJson?.remove("background") as? String) ?: "#000000FF"
        screensConfig.forEach { (key, screenJson) ->
            (screenJson as? MutableMap<String, Any?>)?.let { mutableScreen ->
                if (!mutableScreen.containsKey("layout_behaviour")) {
                    mutableScreen["layout_behaviour"] =
                        if (key in bottomSheetKeys) "default" else layoutBehaviour
                }
                mutableScreen.remove("background")
                (mutableScreen["overlay"] as? JsonObject)?.let { v4Overlay ->
                    mutableScreen["overlay"] = listOf(
                        mapOf(
                            "h_align" to "center",
                            "v_align" to "center",
                            "content" to v4Overlay,
                        )
                    )
                }
            }
        }
        navigatorsConfig = buildLegacyNavigators(defaultBackground, bottomSheetKeys)
        initialScript = buildLegacyScript(this, initialSectionIndices, legacyTimerScripts)
    }

    return NormalizedConfig(
        screensConfig = screensConfig,
        navigatorsConfig = navigatorsConfig,
        templates = templates,
        initialScript = initialScript,
    )
}

private fun extractTemplates(config: JsonObject): MutableMap<String, JsonObject> {
    return config.getAs<JsonObject>(TEMPLATES)
        ?.mapNotNull { (key, value) ->
            if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val content = (value as? Map<String, Any?>)?.getAs<JsonObject>(CONTENT)
                if (content != null) key to content else null
            } else null
        }
        ?.toMap()
        ?.toMutableMap()
        ?: mutableMapOf()
}

private fun transformOldFormatToNew(
    node: JsonObject,
    templates: MutableMap<String, JsonObject>,
    initialSectionIndices: MutableMap<String, Int>,
    legacyTimerScripts: MutableMap<String, String>,
    bottomSheetKeys: Set<String>,
): JsonObject {
    @Suppress("UNCHECKED_CAST")
    return transformNode(node, templates, initialSectionIndices, legacyTimerScripts, bottomSheetKeys) as JsonObject
}

@Suppress("UNCHECKED_CAST")
private fun transformNode(
    node: Any?,
    templates: MutableMap<String, JsonObject>,
    initialSectionIndices: MutableMap<String, Int>,
    legacyTimerScripts: MutableMap<String, String>,
    bottomSheetKeys: Set<String>,
): Any? {
    return when (node) {
        is Map<*, *> -> {
            val map = node as MutableMap<String, Any?>
            val type = map[TYPE] as? String

            if (type == REFERENCE_TYPE) {
                val elementId = map[ELEMENT_ID] as? String
                if (elementId != null) {
                    return mapOf(TYPE to "$TEMPLATE_REF_PREFIX$elementId")
                }
            }

            if (type != null && ACTION_TYPE_TO_FUNC.containsKey(type)) {
                return transformAction(map, bottomSheetKeys)
            }

            if (type == BUTTON_TYPE) {
                val selectedCondition = map.remove(SELECTED_CONDITION) as? JsonObject
                if (map.containsKey(SELECTED)) {
                    if (selectedCondition != null) {
                        map[IS_SELECTED] = transformSelectedCondition(selectedCondition)
                    }
                    map[TYPE] = LEGACY_BUTTON_TYPE
                } else {
                    val normalContent = map.remove("normal")
                    if (normalContent != null) {
                        map[CONTENT] = normalContent
                    }
                }
            }

            if (type == TOGGLE_TYPE) {
                val sectionId = map[SECTION_ID] as? String
                if (sectionId != null) {
                    if (sectionId.isEmpty()) {
                        throw adaptyError(
                            message = "section_id in Toggle must not be empty",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    }
                    val onIndex = (map[ON_INDEX] as? Number)?.toInt() ?: 0
                    val offIndex = (map[OFF_INDEX] as? Number)?.toInt() ?: -1

                    map.remove(SECTION_ID)
                    map.remove(ON_INDEX)
                    map.remove(OFF_INDEX)

                    map[VALUE] = mapOf(
                        "var" to "Legacy.sections.$sectionId",
                        "converter" to "is_equal",
                        "converter_params" to mapOf("value" to onIndex, "false_value" to offIndex),
                        "scope" to "global",
                    )
                }
            }

            if (type == SECTION_TYPE) {
                val indexValue = map["index"]
                if (indexValue is Number) {
                    val sectionId = (map["id"] as? String)
                        ?: throw adaptyError(
                            message = "Couldn't find 'id' for a 'section' element",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    val index = indexValue.toInt()
                    initialSectionIndices[sectionId] = index
                    map["index"] = mapOf(
                        "var" to "Legacy.sections[$sectionId]",
                        "scope" to "global",
                    )
                }
            }

            if (type == PRODUCT_TYPE) {
                return transformProductStringId(map)
            }

            if (type == "timer") {
                (map["format"] as? List<*>)?.forEach { item ->
                    @Suppress("UNCHECKED_CAST")
                    (item as? MutableMap<String, Any?>)?.let { formatItem ->
                        if (!formatItem.containsKey("from"))
                            formatItem["from"] = 60
                    }
                }

                if (!map.containsKey("max_rows")) map["max_rows"] = 1
                if (!map.containsKey("on_overflow")) map["on_overflow"] = listOf("scale")

                val timerId = (map["id"] as? String)?.takeIf { it.isNotEmpty() }
                if (timerId != null) {
                    buildLegacySetTimerScript(timerId, map)?.let { script ->
                        legacyTimerScripts[timerId] = script
                    }
                }
            }

            if (type == ROW_TYPE || type == COLUMN_TYPE) {
                @Suppress("UNCHECKED_CAST")
                (map[ITEMS] as? List<*>)?.forEach { item ->
                    (item as? MutableMap<String, Any?>)?.put(TYPE, GRID_ITEM_TYPE)
                }
                map[TYPE] = if (type == ROW_TYPE) LEGACY_ROW_TYPE else LEGACY_COLUMN_TYPE
            }

            val visibility = map["visibility"]

            val elementId = map[ELEMENT_ID] as? String

            val transformedMap = map.mapValues { (key, value) ->
                if (key == ELEMENT_ID || key == "visibility") null
                else if (type == "timer" && (key == "behaviour" || key == "duration" || key == "end_time")) null
                else transformNode(value, templates, initialSectionIndices, legacyTimerScripts, bottomSheetKeys)
            }.filterValues { it != null }.toMutableMap()

            if (visibility != null && !transformedMap.containsKey("opacity")) {
                transformedMap["opacity"] = if (visibility == true) 1.0 else 0.0
            }

            if (elementId != null) {
                templates[elementId] = transformedMap.toMap()
                return mapOf(TYPE to "$TEMPLATE_REF_PREFIX$elementId")
            }

            transformedMap.toMap()
        }
        is List<*> -> node.map { transformNode(it, templates, initialSectionIndices, legacyTimerScripts, bottomSheetKeys) }
        is Iterable<*> -> node.map { transformNode(it, templates, initialSectionIndices, legacyTimerScripts, bottomSheetKeys) }
        else -> node
    }
}

private fun convertFadeTransition(transition: Map<*, *>): Map<String, Any?>? {
    if (transition["type"] != "fade") return null
    return buildMap {
        put("type", "opacity")
        transition["duration"]?.let { put("duration", it) }
        transition["start_delay"]?.let { put("start_delay", it) }
        transition["interpolator"]?.let { put("interpolator", it) }
        put("opacity", mapOf("start" to 0, "end" to 1))
    }
}

private fun transformSelectedCondition(selectedCondition: JsonObject): Map<String, Any>? {
    return when(selectedCondition["type"]) {
                "selected_section" -> {
                    val sectionId = (selectedCondition["section_id"] as? String)
                        ?: throw adaptyError(
                            message = "Couldn't find 'section_id' for a 'selected_section' condition",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    val index = (selectedCondition["index"] as? Number)?.toInt()
                        ?: throw adaptyError(
                            message = "Couldn't find 'index' for a 'selected_section' condition",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    mapOf(
                        "var" to "Legacy.sections.$sectionId",
                        "converter" to "is_equal",
                        "converter_params" to index,
                        "scope" to "global",
                    )
                }
                "selected_product" -> {
                    val productId = (selectedCondition["product_id"] as? String)
                        ?: throw adaptyError(
                            message = "Couldn't find 'product_id' for a 'selected_product' condition",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    val groupId = (selectedCondition["group_id"] as? String) ?: DEFAULT_PRODUCT_GROUP
                    mapOf(
                        "var" to "Legacy.productGroup.$groupId",
                        "converter" to "is_equal",
                        "converter_params" to productId,
                        "scope" to "global",
                    )
                }
                else -> null
            }
}

private fun transformProductStringId(map: Map<String, Any?>): Map<String, Any?> {
    val id = map["id"] as? String
    val suffix = map["suffix"] as? String
    val product: Any = if (!id.isNullOrEmpty()) {
        id
    } else {
        val groupId = (map["group_id"] as? String)?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_PRODUCT_GROUP
        mapOf("var" to "Legacy.productGroup[$groupId]", "scope" to "global")
    }
    return buildMap {
        put("product", product)
        if (suffix != null) put("suffix", suffix)
    }
}

private fun transformAction(action: Map<String, Any?>, bottomSheetKeys: Set<String>): Map<String, Any?> {
    val type = action[TYPE] as? String ?: return action

    if (type == "open_screen") {
        val screenId = action["screen_id"] as? String ?: return action
        val isBottomSheet = screenId in bottomSheetKeys
        val navigatorId = if (isBottomSheet) LEGACY_BOTTOM_SHEET else DEFAULT
        val instanceId = if (isBottomSheet) LEGACY_BOTTOM_SHEET else screenId
        return mapOf(
            FUNC to "SDK.openScreen",
            "scope" to "global",
            PARAMS to mapOf(
                "instanceId" to instanceId,
                "type" to screenId,
                "navigatorId" to navigatorId,
                "transitionId" to "on_appear",
            )
        )
    }

    if (type == "close_screen") {
        return mapOf(
            FUNC to "SDK.closeScreen",
            "scope" to "global",
            PARAMS to mapOf("navigatorId" to LEGACY_BOTTOM_SHEET),
        )
    }

    val func = ACTION_TYPE_TO_FUNC[type] ?: return action

    val params = action.filterKeys { key ->
        key != TYPE && PARAM_KEY_MAPPING.containsKey(key)
    }.mapKeys { (key, _) ->
        PARAM_KEY_MAPPING[key] ?: key
    }.let { mapped ->
        when (type) {
            "open_url" -> {
                val url = mapped["url"]
                val isLiteralUrl = url is String &&
                    runCatching { URI(url).scheme != null }.getOrDefault(false)
                if (isLiteralUrl) mapped
                else (mapped - "url") + ("stringId" to url)
            }
            "purchase_product", "web_purchase_product" ->
                mapped
            else -> mapped
        }
    }

    return buildMap {
        put(FUNC, func)
        put(PARAMS, params)
        if (func.startsWith("SDK.")) put("scope", "global")
    }
}

private fun buildLegacyScript(
    config: JsonObject,
    initialSectionIndices: Map<String, Int>,
    legacyTimerScripts: Map<String, String>,
): String {
    val selectedProducts = config.getAs<JsonObject>(PRODUCTS)?.getAs<JsonObject>(SELECTED)

    val productCalls = if (selectedProducts.isNullOrEmpty()) {
        ""
    } else {
        val notifiedProductIds = mutableSetOf<String>()
        selectedProducts.mapNotNull { (groupId, productId) ->
            if (productId is String) {
                if (notifiedProductIds.add(productId)) {
                    """Legacy.selectProduct({ productId: "$productId", groupId: "$groupId" });"""
                } else {
                    """Legacy.productGroup["$groupId"] = "$productId";"""
                }
            } else null
        }.joinToString("\n")
    }

    val sectionCalls = if (initialSectionIndices.isEmpty()) {
        ""
    } else {
        initialSectionIndices.map { (sectionId, index) ->
            """Legacy.sections["$sectionId"] = $index;"""
        }.joinToString("\n")
    }

    val timerCalls = if (legacyTimerScripts.isEmpty()) {
        ""
    } else {
        legacyTimerScripts.values.joinToString("\n")
    }

    val openDefaultScreenCall = "SDK.openScreen({ instanceId: \"$DEFAULT\", type: \"$DEFAULT\", transitionId: \"on_appear\" });"

    val calls = listOfNotNull(
        productCalls.takeIf { it.isNotEmpty() },
        sectionCalls.takeIf { it.isNotEmpty() },
        timerCalls.takeIf { it.isNotEmpty() },
        openDefaultScreenCall
    ).joinToString("\n\n")

    return if (calls.isEmpty()) {
        LEGACY_SCRIPT
    } else {
        "$LEGACY_SCRIPT\n\n$calls"
    }
}

internal fun buildLegacySetTimerScript(id: String, map: Map<String, Any?>): String? {
    val escapedId = escapeJsString(id)
    return when (val behaviour = map["behaviour"] as? String) {
        "end_at_utc_time", "end_at_local_time" -> {
            val endTimeStr = map["end_time"] as? String ?: return null
            val endAtMs = parseLegacyEndTimeToUnixMs(endTimeStr, isUtc = behaviour == "end_at_utc_time") ?: return null
            """SDK.setTimer({"id":"$escapedId","endAt":$endAtMs});"""
        }
        "start_at_every_appear" -> {
            val duration = (map["duration"] as? Number)?.toLong() ?: return null
            """SDK.setTimer({"id":"$escapedId","duration":$duration,"behavior":"restart"});"""
        }
        "start_at_first_appear", null -> {
            val duration = (map["duration"] as? Number)?.toLong() ?: return null
            """SDK.setTimer({"id":"$escapedId","duration":$duration,"behavior":"continue"});"""
        }
        "start_at_first_appear_persisted" -> {
            val duration = (map["duration"] as? Number)?.toLong() ?: return null
            """SDK.setTimer({"id":"$escapedId","duration":$duration,"behavior":"persisted"});"""
        }
        "custom" -> {
            val duration = (map["duration"] as? Number)?.toLong() ?: return null
            """SDK.setTimer({"id":"$escapedId","duration":$duration,"behavior":"custom"});"""
        }
        else -> null
    }
}

private fun escapeJsString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

private fun buildLegacyNavigators(
    defaultBackground: String,
    bottomSheetKeys: Set<String>,
): JsonObject {
    val navigators = mutableMapOf<String, Any?>()
    navigators[DEFAULT] = buildMap<String, Any?> {
        put("background", defaultBackground)
        put("order", 0)
        put("content", mapOf("type" to "screen_holder"))
    }
    if (bottomSheetKeys.isNotEmpty()) {
        navigators.putAll(generateBottomSheetNavigator())
    }
    return navigators
}

private fun generateBottomSheetNavigator(): JsonObject = mapOf(
    LEGACY_BOTTOM_SHEET to mapOf(
        "background" to "#00000066",
        "order" to 100,
        "default_screen_actions" to mapOf(
            "on_outside_tap" to mapOf(
                "func" to "SDK.closeScreen",
                "scope" to "global",
                "params" to mapOf("navigatorId" to LEGACY_BOTTOM_SHEET, "transitionId" to "on_disappear"),
            ),
            "on_device_back" to mapOf(
                "func" to "SDK.closeScreen",
                "scope" to "global",
                "params" to mapOf("navigatorId" to LEGACY_BOTTOM_SHEET, "transitionId" to "on_disappear"),
            ),
        ),
        "content" to mapOf(
            "type" to "box",
            "width" to mapOf("fill_max" to true),
            "height" to mapOf("fill_max" to true),
            "h_align" to "center",
            "v_align" to "bottom",
            "content" to mapOf("type" to "screen_holder"),
        ),
        "appearances" to mapOf(
            "on_appear" to mapOf(
                "background" to mapOf(
                    "type" to "background",
                    "duration" to 250,
                    "interpolator" to "ease_out",
                    "color" to mapOf("start" to "#00000000", "end" to "#00000066"),
                ),
                "content" to listOf(
                    mapOf(
                        "type" to "offset",
                        "duration" to 350,
                        "interpolator" to "ease_out",
                        "offset" to mapOf(
                            "start" to mapOf("x" to 0, "y" to mapOf("screen" to 1.0)),
                            "end" to mapOf("x" to 0, "y" to 0),
                        ),
                    ),
                ),
            ),
            "on_disappear" to mapOf(
                "background" to mapOf(
                    "type" to "background",
                    "duration" to 200,
                    "interpolator" to "ease_in",
                    "color" to mapOf("start" to "#00000066", "end" to "#00000000"),
                ),
                "content" to listOf(
                    mapOf(
                        "type" to "offset",
                        "duration" to 250,
                        "interpolator" to "ease_in",
                        "offset" to mapOf(
                            "start" to mapOf("x" to 0, "y" to 0),
                            "end" to mapOf("x" to 0, "y" to mapOf("screen" to 1.0)),
                        ),
                    ),
                ),
            ),
        ),
    )
)

internal fun parseLegacyEndTimeToUnixMs(endTime: String, isUtc: Boolean): Long? {
    return try {
        val (date, time) = endTime.split(" ")
        val (year, month, day) = date.split("-").map { it.toInt() }
        val (hour, minute, second) = time.split(":").map { it.toInt() }
        val timeZone = if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
        Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (_: Exception) {
        null
    }
}

private fun rewriteLegacyTimerTagsInPlace(node: Any?) {
    when (node) {
        is MutableMap<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = node as MutableMap<String, Any?>
            (map["tag"] as? String)?.takeIf { it.startsWith("TIMER_") }?.let { tag ->
                legacyTimerTagToConverter(tag)?.let { (component, format) ->
                    map["tag"] = "TIMER"
                    map["converter"] = component
                    if (format != null) map["format"] = format
                }
            }
            map.values.forEach { rewriteLegacyTimerTagsInPlace(it) }
        }
        is List<*> -> node.forEach { rewriteLegacyTimerTagsInPlace(it) }
        is Iterable<*> -> node.forEach { rewriteLegacyTimerTagsInPlace(it) }
    }
}

private fun rewriteLegacyAppearanceInPlace(node: Any?) {
    when (node) {
        is MutableMap<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = node as MutableMap<String, Any?>

            if (!map.containsKey("event_handlers")) {
                val onAppearList = map["on_appear"] as? List<*>
                val transitionIn = map["transition_in"]

                @Suppress("UNCHECKED_CAST")
                val animations: List<Any?>? = when {
                    onAppearList != null ->
                        onAppearList.toList().takeIf { it.isNotEmpty() }
                    transitionIn is Map<*, *> ->
                        convertFadeTransition(transitionIn)?.let { listOf(it) }
                    transitionIn is List<*> ->
                        transitionIn
                            .mapNotNull { item -> (item as? Map<*, *>)?.let { convertFadeTransition(it) } }
                            .takeIf { it.isNotEmpty() }
                    else -> null
                }

                if (animations != null) {
                    map["event_handlers"] = listOf(
                        mapOf(
                            "triggers" to listOf(
                                mapOf("events" to listOf("on_will_appear")),
                            ),
                            "animations" to animations,
                        )
                    )
                    if (onAppearList != null) map.remove("on_appear")
                    if (transitionIn != null) map.remove("transition_in")
                }
            }

            map.values.forEach { rewriteLegacyAppearanceInPlace(it) }
        }
        is List<*> -> node.forEach { rewriteLegacyAppearanceInPlace(it) }
        is Iterable<*> -> node.forEach { rewriteLegacyAppearanceInPlace(it) }
    }
}

private fun rewriteFlexAutoSizeDefaultsInPlace(node: Any?) {
    when (node) {
        is MutableMap<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = node as MutableMap<String, Any?>
            when (map[TYPE] as? String) {
                ROW_TYPE -> if (!map.containsKey("width")) map["width"] = "fill"
                COLUMN_TYPE -> if (!map.containsKey("height")) map["height"] = "fill"
            }
            map.values.forEach { rewriteFlexAutoSizeDefaultsInPlace(it) }
        }
        is List<*> -> node.forEach { rewriteFlexAutoSizeDefaultsInPlace(it) }
        is Iterable<*> -> node.forEach { rewriteFlexAutoSizeDefaultsInPlace(it) }
    }
}

private fun legacyTimerTagToConverter(tag: String): Pair<String, String?>? = when (tag) {
    "TIMER_h" -> "hours" to "%01d"
    "TIMER_hh" -> "hours" to "%02d"
    "TIMER_m" -> "minutes" to "%01d"
    "TIMER_mm" -> "minutes" to "%02d"
    "TIMER_s" -> "seconds" to "%01d"
    "TIMER_ss" -> "seconds" to "%02d"
    "TIMER_S" -> "deciseconds" to null
    "TIMER_SS" -> "centiseconds" to null
    "TIMER_SSS" -> "milliseconds" to null
    else -> listOf(
        "TIMER_Total_Days_" to "total_days",
        "TIMER_Total_Hours_" to "total_hours",
        "TIMER_Total_Minutes_" to "total_minutes",
        "TIMER_Total_Seconds_" to "total_seconds",
        "TIMER_Total_Milliseconds_" to "total_milliseconds",
    ).firstNotNullOfOrNull { (prefix, component) ->
        if (!tag.startsWith(prefix)) return@firstNotNullOfOrNull null
        val width = tag.removePrefix(prefix).toIntOrNull() ?: return@firstNotNullOfOrNull null
        val format = if (width > 0) "%0${width}d" else "%d"
        component to format
    }
}

internal data class NormalizedConfig(
    val screensConfig: JsonObject,
    val navigatorsConfig: JsonObject?,
    val templates: Templates,
    val initialScript: String,
)
