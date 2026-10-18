@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.preview

import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.mapping.viewconfig.ViewConfigurationMapper
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.currentTimeMillis
import com.adapty.ui.internal.utils.AdaptyUiVideoAccessor
import com.adapty.ui.internal.utils.FlowMode
import com.adapty.ui.internal.utils.isSameOrNewerVersionThan

/**
 * Builds [AdaptyUI.FlowConfiguration] objects from raw builder JSON (Gson-shaped maps) for previews.
 */
public object AdaptyUIPreview {

    public const val TemplateBasic: String = "basic"
    public const val TemplateTransparent: String = "transparent"
    public const val TemplateFlat: String = "flat"

    public const val Format_4_4_1: String = "4.4.1"
    public const val Format_5_0_0: String = "5.0.0"

    private const val DEFAULT_ID = "default"
    private const val DEFAULT_LOCALIZATION = "en"

    private val viewConfigMapper: ViewConfigurationMapper by lazy {
        ViewConfigurationMapper.createDefault(AdaptyUiVideoAccessor().createVideoMapperFnOrNull())
    }

    public fun createFromScreen(
        screenContent: Map<String, Any?>,
        templateId: String,
        format: String,
        assets: List<Map<String, Any?>>,
        localizations: List<Map<String, Any?>>,
        defaultLocalization: String? = null,
    ): AdaptyUI.FlowConfiguration =
        createFromContent(screenContent, templateId, format, assets, localizations, defaultLocalization)

    public fun createFromElement(
        uiChildNodeContent: Map<String, Any?>,
        format: String,
        assets: List<Map<String, Any?>>,
        localizations: List<Map<String, Any?>>,
        defaultLocalization: String? = null,
    ): AdaptyUI.FlowConfiguration =
        createFromScreen(
            wrapComponentInScreen(uiChildNodeContent),
            TemplateTransparent,
            format,
            assets,
            localizations,
            defaultLocalization,
        )

    public fun createFromBundle(
        format: String,
        elements: Map<String, Map<String, Any?>> = emptyMap(),
        screens: Map<String, Map<String, Any?>> = emptyMap(),
        navigators: Map<String, Map<String, Any?>> = emptyMap(),
        assets: List<Map<String, Any?>>,
        localizations: List<Map<String, Any?>>,
        defaultLocalization: String? = null,
        script: String? = null,
        initialScreenId: String? = null,
        initialNavigatorId: String? = null,
        templateId: String? = null,
        behavior: Map<String, Any?>? = null,
    ): AdaptyUI.FlowConfiguration {
        val builderConfig: Map<String, Any?> = if (format.isSameOrNewerVersionThan(Format_5_0_0)) {
            val navId = initialNavigatorId ?: DEFAULT_ID
            val screenId = initialScreenId
                ?: elements.keys.firstOrNull()
                ?: screens.keys.firstOrNull()
                ?: DEFAULT_ID

            val screensConfig = buildMap<String, Any?> {
                elements.forEach { (id, element) -> put(id, mapOf("content" to element)) }
                screens.forEach { (id, screen) -> put(id, screen) }
            }
            val actualNavigators = navigators.ifEmpty { mapOf(navId to defaultNavigator()) }

            val openScreenCall = "SDK.openScreen({ instanceId: \"$screenId\", type: \"$screenId\", " +
                "navigatorId: \"$navId\", transitionId: \"on_appear\" });"
            val scriptContent = if (script != null) "$script\n\n$openScreenCall" else openScreenCall

            buildMap {
                put("format", format)
                put("default_localization", defaultLocalization ?: DEFAULT_LOCALIZATION)
                put("assets", assets)
                put("localizations", localizations)
                put("screens", screensConfig)
                put("navigators", actualNavigators)
                put("scripts", listOf(mapOf("content" to scriptContent, "type" to "js")))
                if (behavior != null) put("behavior", behavior)
            }
        } else {
            hashMapOf<String, Any?>().apply {
                put("format", format)
                put("template_id", templateId ?: DEFAULT_ID)
                put("default_localization", defaultLocalization ?: DEFAULT_LOCALIZATION)
                put("assets", assets)
                put("localizations", localizations)
                put("styles", hashMapOf<String, Any?>().apply {
                    putAll(elements.mapValues { (_, element) -> wrapComponentInScreen(element) })
                    putAll(screens)
                })
                if (behavior != null) put("behavior", behavior)
            }
        }
        return mapWrapped(builderConfig)
    }

    public fun createFromFlowBuilderConfig(
        flowBuilderConfig: Map<String, Any>,
        locale: String? = null,
    ): AdaptyUI.FlowConfiguration {
        val wrappedConfig = buildMap<String, Any?> {
            put("paywall_builder_id", previewId())
            put("paywall_builder_config", flowBuilderConfig)
            if (locale != null) put("lang", locale)
        }
        return viewConfigMapper.map(wrappedConfig, FlowMode.Preview)
    }

    private fun createFromContent(
        screenContent: Map<String, Any?>,
        templateId: String,
        format: String,
        assets: List<Map<String, Any?>>,
        localizations: List<Map<String, Any?>>,
        defaultLocalization: String? = null,
    ): AdaptyUI.FlowConfiguration {
        val builderConfig: Map<String, Any?> = if (format.isSameOrNewerVersionThan(Format_5_0_0)) {
            val layoutBehaviour = if (templateId == TemplateBasic) "hero" else templateId
            val screenWithLayout = screenContent.toMutableMap().apply { put("layout_behaviour", layoutBehaviour) }
            val screenId = DEFAULT_ID
            val openScreenCall =
                "SDK.openScreen({ instanceId: \"$screenId\", type: \"$screenId\", transitionId: \"on_appear\" });"
            mapOf(
                "format" to format,
                "default_localization" to (defaultLocalization ?: DEFAULT_LOCALIZATION),
                "assets" to assets,
                "localizations" to localizations,
                "screens" to mapOf(screenId to mapOf("content" to screenWithLayout)),
                "navigators" to mapOf(DEFAULT_ID to defaultNavigator()),
                "scripts" to listOf(mapOf("content" to openScreenCall, "type" to "js")),
            )
        } else {
            hashMapOf(
                "format" to format,
                "template_id" to templateId,
                "default_localization" to (defaultLocalization ?: DEFAULT_LOCALIZATION),
                "assets" to assets,
                "localizations" to localizations,
                "styles" to hashMapOf(DEFAULT_ID to screenContent),
            )
        }
        return mapWrapped(builderConfig)
    }

    private fun mapWrapped(builderConfig: Map<String, Any?>): AdaptyUI.FlowConfiguration =
        viewConfigMapper.map(
            mapOf("paywall_builder_id" to previewId(), "paywall_builder_config" to builderConfig),
            FlowMode.Preview,
        )

    private fun previewId() = "preview_${currentTimeMillis()}"

    private fun defaultNavigator(): Map<String, Any?> = mapOf(
        "background" to "#000000FF",
        "order" to 0,
        "content" to mapOf("type" to "screen_holder"),
    )

    private fun wrapComponentInScreen(
        component: Map<String, Any?>,
        background: String = "\$white",
    ): Map<String, Any?> = mapOf(
        "background" to background,
        "layout_behaviour" to "default",
        "content" to mapOf(
            "type" to "box",
            "v_align" to "center",
            "h_align" to "center",
            "width" to mapOf("fill_max" to true),
            "height" to mapOf("fill_max" to true),
            "decorator" to mapOf("type" to "rect", "background" to background),
            "content" to mapOf("type" to "z_stack", "content" to listOf(component)),
        ),
    )
}
