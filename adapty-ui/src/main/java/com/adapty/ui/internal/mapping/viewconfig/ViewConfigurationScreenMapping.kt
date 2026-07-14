@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.mapping.viewconfig

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.internal.utils.getAs
import com.adapty.ui.AdaptyUI.FlowConfiguration.NavigatorConfig
import com.adapty.ui.AdaptyUI.FlowConfiguration.Screen
import com.adapty.ui.AdaptyUI.FlowConfiguration.ScreenBundle
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toAnimation
import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.mapping.attributes.toHorizontalAlign
import com.adapty.ui.internal.mapping.attributes.toEdgeEntities
import com.adapty.ui.internal.mapping.attributes.toOffset
import com.adapty.ui.internal.mapping.attributes.toShape
import com.adapty.ui.internal.mapping.attributes.toStaticRotation
import com.adapty.ui.internal.mapping.attributes.toStaticScale
import com.adapty.ui.internal.mapping.attributes.toVerticalAlign
import com.adapty.ui.internal.ui.attributes.AppearanceAnimation
import com.adapty.ui.internal.ui.attributes.ScreenTransition
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.StateMap
import com.adapty.ui.internal.mapping.element.UIElementFactory
import com.adapty.ui.internal.mapping.element.extractEventHandlers
import com.adapty.ui.internal.ui.attributes.plus
import com.adapty.ui.internal.mapping.element.toOverlayItem
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.BoxElement
import com.adapty.ui.internal.ui.element.OverlayItem
import com.adapty.ui.internal.ui.element.ScreenHolderElement
import com.adapty.ui.internal.utils.ContentWrapper
import com.adapty.ui.internal.utils.NO_SHRINK
import com.adapty.ui.internal.utils.StringSource
import com.adapty.ui.internal.utils.VisualValue

private const val DEFAULT = "default"
private const val BACKGROUND = "background"
private const val CONTENT = "content"
private const val COVER = "cover"
private const val FOOTER = "footer"
private const val OVERLAY = "overlay"
private const val V_ALIGN = "v_align"
private const val H_ALIGN = "h_align"
private const val DEFAULT_CONTENT_V_ALIGN = "top"
private const val DECORATOR = "decorator"
private const val PADDING = "padding"
private const val OFFSET = "offset"
private const val OPACITY = "opacity"
private const val ROTATION = "rotation"
private const val SCALE = "scale"
private const val EVENT_HANDLERS = "event_handlers"

internal fun mapScreens(
    screens: JsonObject,
    assets: Assets,
    stateMap: StateMap,
    uiElementFactory: UIElementFactory,
    templates: Templates = emptyMap(),
): ScreenBundle {
    val mainScreens = mutableMapOf<String, Screen>()
    screens.forEach { (k, v) ->
        val screenJson = (v as? JsonObject) ?: return@forEach
        val layoutBehaviour = screenJson.getAs<String>("layout_behaviour")
            ?.let { LayoutBehaviour.from(it) }
            ?: LayoutBehaviour.DEFAULT
        val screen = mapScreen(screenJson, layoutBehaviour, assets, stateMap, uiElementFactory, templates)
        mainScreens[k] = screen
    }

    return ScreenBundle(mainScreens, stateMap)
}

private fun mapScreen(
    rawScreen: JsonObject,
    layoutBehaviour: LayoutBehaviour,
    assets: Assets,
    stateMap: StateMap,
    uiElementFactory: UIElementFactory,
    templates: Templates,
): Screen {
    val onSystemBack = rawScreen.getAs<JsonObject>("on_device_back")?.toAction()?.let { listOf(it) }
        ?: (rawScreen.getAs<Iterable<*>>("on_device_back"))?.mapNotNull { (it as? Map<*, *>)?.toAction() }
    val onOutsideTap = rawScreen.getAs<JsonObject>("on_outside_tap")?.toAction()?.let { listOf(it) }
        ?: (rawScreen.getAs<Iterable<*>>("on_outside_tap"))?.mapNotNull { (it as? Map<*, *>)?.toAction() }
    val onFocusChange = rawScreen.getAs<JsonObject>("on_focus_change")?.toAction()?.let { listOf(it) }
        ?: (rawScreen.getAs<Iterable<*>>("on_focus_change"))?.mapNotNull { (it as? Map<*, *>)?.toAction() }
    val onWillAppear = rawScreen.parseLifecycleActions("on_will_appear")
    val onDidAppear = rawScreen.parseLifecycleActions("on_did_appear")
    val onWillDisappear = rawScreen.parseLifecycleActions("on_will_disappear")
    val onDidDisappear = rawScreen.parseLifecycleActions("on_did_disappear")
    val contentScrollValue = rawScreen["content_scroll_value"]?.toTwoWayBinding()
    val footerScrollValue = rawScreen["footer_scroll_value"]?.toTwoWayBinding()

    val overlays = rawScreen.parseAlignedElements(OVERLAY, assets, stateMap, uiElementFactory, templates)
    val backgrounds = rawScreen.parseAlignedElements(BACKGROUND, assets, stateMap, uiElementFactory, templates)

    val cover = rawScreen.getAs<JsonObject>(COVER)?.toElementTree(assets, stateMap, uiElementFactory, templates) as? BoxElement
    val footer = rawScreen.getAs<JsonObject>(FOOTER)?.toElementTree(assets, stateMap, uiElementFactory, templates)
    return when (layoutBehaviour) {
        LayoutBehaviour.HERO -> {
            if (cover == null)
                throw adaptyError(
                    message = "cover in 'hero' layout in ViewConfiguration should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            val contentWrapper = rawScreen.getAs<JsonObject>(CONTENT)
                ?.toMutableMap()
                ?.let { content -> putContentIntoWrapper(content, assets, stateMap, uiElementFactory, templates, hoistDecorator = true) }
                ?: throw adaptyError(
                    message = "content in 'default' screen in ViewConfiguration should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            Screen.Hero(cover, contentWrapper, footer, onSystemBack, onOutsideTap, onFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, contentScrollValue, footerScrollValue, overlays, backgrounds)
        }
        LayoutBehaviour.TRANSPARENT -> {
            val content = rawScreen.getAs<JsonObject>(CONTENT)
                ?.toElementTree(assets, stateMap, uiElementFactory, templates)
                ?: throw adaptyError(
                    message = "content in 'default' screen in ViewConfiguration should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            Screen.Transparent(cover, content, footer, onSystemBack, onOutsideTap, onFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, contentScrollValue, footerScrollValue, overlays, backgrounds)
        }
        LayoutBehaviour.FLAT -> {
            val contentWrapper = rawScreen.getAs<JsonObject>(CONTENT)
                ?.toMutableMap()
                ?.let { content -> putContentIntoWrapper(content, assets, stateMap, uiElementFactory, templates) }
                ?: throw adaptyError(
                    message = "content in 'default' screen in ViewConfiguration should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            Screen.Flat(cover, contentWrapper, footer, onSystemBack, onOutsideTap, onFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, contentScrollValue, footerScrollValue, overlays, backgrounds)
        }
        LayoutBehaviour.DEFAULT -> {
            val content = rawScreen.getAs<JsonObject>(CONTENT)
                ?.toElementTree(assets, stateMap, uiElementFactory, templates)
                ?: throw adaptyError(
                    message = "content in 'default' screen in ViewConfiguration should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            Screen.Plain(content, onSystemBack, onOutsideTap, onFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, contentScrollValue, footerScrollValue, overlays, backgrounds)
        }
    }
}

private fun JsonObject.parseLifecycleActions(key: String): List<com.adapty.ui.internal.ui.element.Action>? =
    getAs<JsonObject>(key)?.toAction()?.let { listOf(it) }
        ?: (getAs<Iterable<*>>(key))?.mapNotNull { (it as? Map<*, *>)?.toAction() }

private fun putContentIntoWrapper(
    rawContent: MutableMap<String, Any?>,
    assets: Assets,
    stateMap: StateMap,
    uiElementFactory: UIElementFactory,
    templates: Templates,
    hoistDecorator: Boolean = false,
): ContentWrapper {
    val vAlign = rawContent.remove(V_ALIGN) ?: DEFAULT_CONTENT_V_ALIGN
    val wrapperProps = if (hoistDecorator) {
        BaseProps(
            shape = rawContent.remove(DECORATOR)?.toShape(),
            padding = rawContent.remove(PADDING)?.toEdgeEntities(),
            opacity = (rawContent[OPACITY] as? Number)?.toFloat() ?: 1f,
            rotation = rawContent.remove(ROTATION)?.toStaticRotation(),
            scale = rawContent.remove(SCALE)?.toStaticScale(),
            eventHandlers = rawContent.extractEventHandlers(),
        ).also {
            rawContent.remove(OPACITY)
            rawContent.remove(EVENT_HANDLERS)
        }
    } else null
    val offset = rawContent.remove(OFFSET)
    return ContentWrapper(
        rawContent.toElementTree(assets, stateMap, uiElementFactory, templates),
        vAlign.toVerticalAlign() + rawContent.remove(H_ALIGN).toHorizontalAlign(),
        wrapperProps,
        offset?.toOffset(),
    )
}

private fun Map<*, *>.toElementTree(
    assets: Assets,
    stateMap: StateMap,
    uiElementFactory: UIElementFactory,
    templates: Templates,
) = uiElementFactory.createElementTree(this, assets, stateMap, templates)

private fun JsonObject.parseAlignedElements(
    key: String,
    assets: Assets,
    stateMap: StateMap,
    uiElementFactory: UIElementFactory,
    templates: Templates,
): List<OverlayItem> {
    val list = getAs<List<*>>(key) ?: return emptyList()
    return list.mapNotNull { item ->
        (item as? Map<*, *>)?.toOverlayItem { childConfig, _ ->
            uiElementFactory.createElementTree(childConfig, assets, stateMap, templates)
        }
    }
}

private const val ORDER = "order"

internal fun mapNavigators(
    navigatorsConfig: JsonObject?,
    assets: Assets,
    stateMap: StateMap,
    uiElementFactory: UIElementFactory,
    templates: Templates,
): Map<String, NavigatorConfig> {
    val result = mutableMapOf<String, NavigatorConfig>()
    navigatorsConfig?.forEach { (id, value) ->
        val nav = (value as? JsonObject) ?: return@forEach
        val background = nav[BACKGROUND]?.toVisualValue() ?: return@forEach
        val order = (nav.getAs<Number>(ORDER))?.toInt() ?: 0
        val content = nav.getAs<JsonObject>(CONTENT)
            ?.toElementTree(assets, stateMap, uiElementFactory, templates)
            ?: ScreenHolderElement(BaseProps.EMPTY)
        val overlays = nav.parseAlignedElements(OVERLAY, assets, stateMap, uiElementFactory, templates)
        val defaultScreenActions = nav.getAs<JsonObject>("default_screen_actions")
        val onOutsideTap = defaultScreenActions?.getAs<JsonObject>("on_outside_tap")?.toAction()?.let { listOf(it) }
            ?: (defaultScreenActions?.getAs<Iterable<*>>("on_outside_tap"))?.mapNotNull { (it as? Map<*, *>)?.toAction() }
            ?: emptyList()
        val onSystemBack = defaultScreenActions?.getAs<JsonObject>("on_device_back")?.toAction()?.let { listOf(it) }
            ?: (defaultScreenActions?.getAs<Iterable<*>>("on_device_back"))?.mapNotNull { (it as? Map<*, *>)?.toAction() }
        val defaultOnFocusChange = defaultScreenActions?.getAs<JsonObject>("on_focus_change")?.toAction()?.let { listOf(it) }
            ?: (defaultScreenActions?.getAs<Iterable<*>>("on_focus_change"))?.mapNotNull { (it as? Map<*, *>)?.toAction() }
        val onWillAppear = defaultScreenActions?.parseLifecycleActions("on_will_appear")
        val onDidAppear = defaultScreenActions?.parseLifecycleActions("on_did_appear")
        val onWillDisappear = defaultScreenActions?.parseLifecycleActions("on_will_disappear")
        val onDidDisappear = defaultScreenActions?.parseLifecycleActions("on_did_disappear")
        val appearances = nav.getAs<JsonObject>("appearances")?.toAppearanceMap() ?: emptyMap()
        val transitions = nav.getAs<JsonObject>("transitions")?.toTransitionsMap() ?: emptyMap()
        result[id] = NavigatorConfig(background, content, order, overlays, onOutsideTap, onSystemBack, defaultOnFocusChange, onWillAppear, onDidAppear, onWillDisappear, onDidDisappear, appearances, transitions)
    }
    if (DEFAULT !in result) {
        result[DEFAULT] = NavigatorConfig(
            VisualValue.any(StringSource.Value("#000000FF")),
            ScreenHolderElement(BaseProps.EMPTY),
            0,
        )
    }
    return result.entries
        .sortedBy { it.value.order }
        .mapIndexed { index, (id, config) -> id to config.copy(order = index) }
        .toMap()
}

private fun JsonObject.toAppearanceMap(): Map<String, AppearanceAnimation> {
    val result = mutableMapOf<String, AppearanceAnimation>()
    forEach { (key, value) ->
        val obj = (value as? JsonObject) ?: return@forEach
        val background = (obj["background"] as? Map<*, *>)?.toAnimation()
        val contentArray = (obj["content"] as? Iterable<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.toAnimation() }
            ?: emptyList()
        result[key] = AppearanceAnimation(background, contentArray)
    }
    return result
}

private fun JsonObject.toTransitionsMap(): Map<String, ScreenTransition> {
    val result = mutableMapOf<String, ScreenTransition>()
    forEach { (key, value) ->
        val obj = (value as? JsonObject) ?: return@forEach
        val outgoing = (obj["outgoing"] as? Iterable<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.toAnimation() }
            ?: emptyList()
        val incoming = (obj["incoming"] as? Iterable<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.toAnimation() }
            ?: emptyList()
        val isIncomingOnTop = (obj["is_incoming_on_top"] as? Boolean)
            ?: throw adaptyError(
                message = "'is_incoming_on_top' in ScreenTransition '$key' should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        result[key] = ScreenTransition(outgoing, incoming, isIncomingOnTop)
    }
    return result
}
