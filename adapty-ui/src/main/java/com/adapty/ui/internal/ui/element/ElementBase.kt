@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.adapty.ui.internal.ui.attributes.easing
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import kotlinx.coroutines.delay

public object UnknownElement: UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public class ReferenceElement internal constructor(internal val id: String): UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public object SkippedElement: UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
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

@InternalAdaptyApi
public interface Container<T> {
    public var content: T
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
    internal fun resolve(resolveText: ResolveText): Action? {
        return when(this) {
            is OpenUrl -> {
                val actualUrl = kotlin.runCatching { url.toStringId() }.getOrElse { e ->
                    log(ERROR) { "$LOG_PREFIX_ERROR couldn't extract value for${url}: ${e.localizedMessage})" }
                    null
                }?.let { resolveText(it, null) }?.toPlainString()
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

@Composable
internal fun UIElement.render(
    resolveAssets: ResolveAssets,
    resolveText: ResolveText,
    resolveState: ResolveState,
    eventCallback: EventCallback,
) {
    render(
        resolveAssets,
        resolveText,
        resolveState,
        eventCallback,
        Modifier.fillWithBaseParams(this, resolveAssets)
    )
}

@Composable
internal fun UIElement.render(
    resolveAssets: ResolveAssets,
    resolveText: ResolveText,
    resolveState: ResolveState,
    eventCallback: EventCallback,
    modifier: Modifier,
) {
    render(
        toComposable(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
            modifier,
        )
    )
}

@Composable
internal fun UIElement.render(
    toComposable: @Composable () -> Unit,
) {
    toComposable
        .withTransitions(transitions)
        .invoke()
}

@Composable
internal fun (@Composable () -> Unit).withTransitions(transitions: Transitions): @Composable () -> Unit  = {
    val (transitionsIn) = transitions
    val transitionIn = transitionsIn?.firstOrNull { it is Transition.Fade }
    if (transitionIn != null) {
        val visibilityState = remember {
            mutableStateOf(false)
        }
        LaunchedEffect(Unit) {
            if (transitionIn.startDelayMillis > 0)
                delay(transitionIn.startDelayMillis.toLong())
            visibilityState.value = true
        }
        AnimatedVisibility(
            visible = visibilityState.value,
            enter = fadeIn(
                animationSpec = tween(durationMillis = transitionIn.durationMillis, easing = transitionIn.easing)
            )
        ) {
            this@withTransitions()
        }
    } else {
        this()
    }
}

internal val UIElement.transitions get() = Transitions(baseProps.transitionIn)

internal data class Transitions(
    val transitionIn: List<Transition>?,
)

public typealias ResolveAssets = () -> Assets
public typealias ResolveText = @Composable (stringId: StringId, textAttrs: Attributes?) -> StringWrapper?
public typealias ResolveState = () -> Map<String, Any>

