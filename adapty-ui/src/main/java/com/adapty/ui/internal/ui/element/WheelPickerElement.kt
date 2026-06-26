@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.text.ComposeTextAttrs
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.TagValueSource
import com.adapty.ui.internal.text.toPlainString
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.LocalUiEnabled
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.ui.resolveText
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.parseColorInt
import com.adapty.ui.internal.utils.resolve

@InternalAdaptyApi
public sealed class WheelPickerDataSource {
    internal abstract fun generateItems(): List<WheelPickerItem>

    internal class Range(
        val min: Double,
        val max: Double,
        val step: Double,
    ) : WheelPickerDataSource() {
        override fun generateItems(): List<WheelPickerItem> {
            val items = mutableListOf<WheelPickerItem>()
            val isWholeStep = step == step.toLong().toDouble()
            var current = min
            while (current <= max + step / 2.0) {
                val value: Any = if (isWholeStep) current.toLong() else current
                val label = if (isWholeStep) current.toLong().toString() else current.toString()
                items.add(WheelPickerItem(value, label))
                current += step
            }
            return items
        }
    }

    internal class StringList(
        val items: List<String>,
    ) : WheelPickerDataSource() {
        override fun generateItems(): List<WheelPickerItem> {
            return items.map { WheelPickerItem(it, it) }
        }
    }

    internal class Items(
        val items: List<Item>,
    ) : WheelPickerDataSource() {
        internal class Item(val value: Any, val stringId: StringId?)

        override fun generateItems(): List<WheelPickerItem> {
            return items.map { item ->
                WheelPickerItem(item.value, item.value.toString(), item.stringId)
            }
        }
    }

    internal class RangeWithFormat(
        val min: Double,
        val max: Double,
        val step: Double,
        val formatItemsDesc: List<FormatItem>,
    ) : WheelPickerDataSource() {
        internal class FormatItem(val from: Double, val stringId: StringId)

        override fun generateItems(): List<WheelPickerItem> {
            val items = mutableListOf<WheelPickerItem>()
            val isWholeStep = step == step.toLong().toDouble()
            var current = min
            while (current <= max + step / 2.0) {
                val value: Any = if (isWholeStep) current.toLong() else current
                val label = if (isWholeStep) current.toLong().toString() else current.toString()
                val stringId = (formatItemsDesc.firstOrNull { current >= it.from }
                    ?: formatItemsDesc.lastOrNull())?.stringId
                items.add(WheelPickerItem(value, label, stringId))
                current += step
            }
            return items
        }
    }
}

internal class WheelPickerItem(
    val value: Any,
    val label: String,
    val stringId: StringId? = null,
)

@InternalAdaptyApi
public class WheelPickerElement internal constructor(
    internal val value: TwoWayBinding,
    internal val dataSource: WheelPickerDataSource,
    internal val itemHeightDp: Float,
    internal val visibleItems: Int,
    internal val textAttributes: BaseTextElement.Attributes?,
    internal val selectedColor: VisualValue?,
    internal val indicatorColor: VisualValue?,
    internal val indicatorCornerRadius: Shape.CornerRadius?,
    internal val stringId: StringId?,
    override val baseProps: BaseProps,
) : UIElement {

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        WheelPickerContent(this@WheelPickerElement, dispatch, modifier)
    }
}

@Composable
private fun WheelPickerContent(
    element: WheelPickerElement,
    dispatch: (Message) -> Unit,
    modifier: Modifier,
) {
    val state = resolveState()
    val screen = LocalScreenInstance.current
    val assets = resolveAssets()
    val resolveTextFn = resolveText

    val items = remember(element.dataSource) { element.dataSource.generateItems() }
    if (items.isEmpty()) return

    val currentBindingValue = state[element.value]
    val currentIndex = items.indexOfFirst { matchesValue(it.value, currentBindingValue) }
        .let { if (it < 0) 0 else it }

    val textAttrs = element.textAttributes?.let { ComposeTextAttrs.from(it, assets) }
    val textColor = textAttrs?.textColor ?: WheelColumnDefaults.TextColor
    val selectedTextColor = resolveColorValue(element.selectedColor, assets)
        ?: textAttrs?.textColor
        ?: WheelColumnDefaults.SelectedTextColor
    val backgroundColor = textAttrs?.backgroundColor
    val textDecoration = textAttrs?.textDecoration
    val indicatorBgColor = resolveColorValue(element.indicatorColor, assets) ?: WheelColumnDefaults.IndicatorColor
    val fontFamily = textAttrs?.fontFamily
    val fontSize = (textAttrs?.fontSize ?: WheelColumnDefaults.FontSize).sp

    val indicatorShape = element.indicatorCornerRadius?.let { r ->
        RoundedCornerShape(
            topStart = r.topLeft.dp,
            topEnd = r.topRight.dp,
            bottomEnd = r.bottomRight.dp,
            bottomStart = r.bottomLeft.dp,
        )
    } ?: RoundedCornerShape(WheelColumnDefaults.IndicatorCornerRadius.dp)

    val labels = items.map { item ->
        resolveItemLabel(element.stringId, item, resolveTextFn) ?: item.label
    }

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        WheelColumn(
            labels = labels,
            selectedIndex = currentIndex,
            onSelectedIndexChange = { index ->
                val clampedIndex = index.coerceIn(0, items.lastIndex)
                dispatch(Message.ValueChanged(element.value, items[clampedIndex].value, screen))
            },
            enabled = LocalUiEnabled.current,
            itemHeightDp = element.itemHeightDp,
            visibleItems = element.visibleItems,
            textColor = textColor,
            selectedTextColor = selectedTextColor,
            backgroundColor = backgroundColor,
            textDecoration = textDecoration,
            indicatorColor = indicatorBgColor,
            indicatorShape = indicatorShape,
            fontFamily = fontFamily,
            fontSize = fontSize,
        )
    }
}

@Composable
private fun resolveColorValue(visualValue: VisualValue?, assets: Assets): Color? {
    if (visualValue == null) return null
    val resolved = visualValue.source.resolve() ?: return null
    val type = visualValue.orderedTypes.firstOrNull { it.condition(resolved) } ?: return null
    return when (type) {
        VisualValue.Type.ColorLiteral -> remember(resolved) { Color(resolved.parseColorInt()) }
        VisualValue.Type.AssetId ->
            assets.getAsset<Asset.Color>(resolved)?.main?.value?.let { Color(it) }
    }
}

@Composable
private fun resolveItemLabel(
    stringId: StringId?,
    item: WheelPickerItem,
    resolveTextFn: ResolveText,
): String? {
    val itemStringId = item.stringId
    if (itemStringId is StringId.Str) {
        val tagValues = (itemStringId.tagValues?.toMutableMap() ?: mutableMapOf()).apply {
            put("VALUE", TagValueSource.Literal(item.label))
        }
        val modifiedStringId = StringId.Str(itemStringId.source, tagValues)
        return resolveTextFn(modifiedStringId, null)?.toPlainString()
    }
    if (stringId !is StringId.Str) return null
    val tagValues = (stringId.tagValues?.toMutableMap() ?: mutableMapOf()).apply {
        put("WHEEL_VALUE", TagValueSource.Literal(item.label))
    }
    val modifiedStringId = StringId.Str(stringId.source, tagValues)
    return resolveTextFn(modifiedStringId, null)?.toPlainString()
}

private fun matchesValue(itemValue: Any, bindingValue: Any?): Boolean {
    if (bindingValue == null) return false
    if (itemValue == bindingValue) return true
    if (itemValue is Number && bindingValue is Number) {
        return itemValue.toDouble() == bindingValue.toDouble()
    }
    return itemValue.toString() == bindingValue.toString()
}
