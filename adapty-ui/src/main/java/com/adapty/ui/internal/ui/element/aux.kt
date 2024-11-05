@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.text.toPlainString
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

public object UnknownElement: UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public class ReferenceElement internal constructor(internal val id: String): UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public object SkippedElement: UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public data class BaseProps internal constructor(
     internal val widthSpec: DimSpec? = null,
     internal val heightSpec: DimSpec? = null,
     internal val weight: Float? = null,
     internal val shape: Shape? = null,
     internal val padding: EdgeEntities? = null,
     internal val offset: Offset? = null,
     internal val visibility: Boolean? = null,
     internal val transitionIn: List<Transition>? = null,
) {
    internal companion object {
        val EMPTY = BaseProps()
    }
}

internal interface Container<T> {
    var content: T
}
internal interface SingleContainer: Container<UIElement>
internal interface MultiContainer: Container<List<UIElement>>

@InternalAdaptyApi
public sealed class Condition {
    public class SelectedSection internal constructor(internal val sectionId: String, internal val index: Int): Condition()
    public class SelectedProduct internal constructor(internal val productId: String, internal val groupId: String): Condition()
    public object Unknown: Condition()
}

@InternalAdaptyApi
public sealed class Action {
    public class OpenUrl internal constructor(internal val url: String): Action()
    public class Custom internal constructor(internal val customId: String): Action()
    public class SelectProduct internal constructor(internal val productId: String, internal val groupId: String): Action()
    public class UnselectProduct internal constructor(internal val groupId: String): Action()
    public class PurchaseProduct internal constructor(internal val productId: String): Action()
    public class PurchaseSelectedProduct internal constructor(internal val groupId: String): Action()
    public object RestorePurchases: Action()
    public class OpenScreen internal constructor(internal val screenId: String): Action()
    public object CloseCurrentScreen: Action()
    public class SwitchSection internal constructor(internal val sectionId: String, internal val index: Int): Action()
    public object ClosePaywall: Action()
    public object Unknown: Action()

    @Composable
    internal fun resolve(resolveText: @Composable (StringId) -> StringWrapper?): Action? {
        return when(this) {
            is OpenUrl -> {
                val actualUrl = kotlin.runCatching { url.toStringId() }.getOrElse { e ->
                    log(ERROR) { "$LOG_PREFIX_ERROR couldn't extract value for${url}: ${e.localizedMessage})" }
                    null
                }?.let { resolveText(it) }?.toPlainString()
                return if (actualUrl != null)
                    OpenUrl(actualUrl)
                else {
                    log(ERROR) { "$LOG_PREFIX_ERROR couldn't find a string value for this id (${url})" }
                    null
                }
            }
            else -> this
        }
    }
}

internal fun RowScope.fillModifierWithScopedParams(element: UIElement, modifier: Modifier): Modifier {
    var modifier = modifier
    val weight = element.baseProps.weight
    if (weight != null)
        modifier = modifier.weight(weight)
    return modifier
}

internal fun ColumnScope.fillModifierWithScopedParams(element: UIElement, modifier: Modifier): Modifier {
    var modifier = modifier
    val weight = element.baseProps.weight
    if (weight != null)
        modifier = modifier.weight(weight)
    return modifier
}