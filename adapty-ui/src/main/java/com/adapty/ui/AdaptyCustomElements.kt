@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.AnnotatedString
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.StateAccessor
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsFloat
import com.adapty.ui.internal.script.toJsInt
import com.adapty.ui.internal.script.toJsLong
import com.adapty.ui.internal.store.Effect
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.BaseTextElement
import com.adapty.ui.internal.ui.element.CustomElement
import com.adapty.ui.internal.ui.element.ImageElement
import com.adapty.ui.internal.ui.element.TextElement
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.normalizeJsonNumbers
import com.adapty.ui.internal.utils.toBindingString
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.google.gson.Gson
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Supplies app-rendered content for the `custom` elements of a flow.
 *
 * [resolve] is called with the element's `custom_type` and `custom_id` each time the element
 * is composed, so it must be fast and free of side effects. Return `null` for elements the
 * resolver does not serve.
 *
 * A resolver passed to a view replaces the one set with [AdaptyUI.setCustomElementsResolver]
 * entirely: elements it does not serve are not looked up in the other one.
 */
public fun interface AdaptyCustomElementsResolver {

    /**
     * @param[type] `custom_type` of the element.
     * @param[id] `custom_id` of the element, unique within the configuration.
     * @return Content for the element, or `null` if this resolver does not serve it.
     */
    public fun resolve(type: String, id: String): AdaptyCustomElementContent?

    public companion object {

        /**
         * Creates a resolver that serves elements by their `custom_type`.
         */
        @JvmStatic
        public fun of(vararg elements: Pair<String, AdaptyCustomElementContent>): AdaptyCustomElementsResolver =
            of(elements.toMap())

        /**
         * Creates a resolver that serves elements by their `custom_type`. The map is copied, so later
         * changes to it have no effect.
         */
        @JvmStatic
        public fun of(elements: Map<String, AdaptyCustomElementContent>): AdaptyCustomElementsResolver {
            val byType = elements.toMap()
            return AdaptyCustomElementsResolver { type, _ -> byType[type] }
        }
    }
}

/**
 * Content of a `custom` element: a composable or an Android [android.view.View]. Create it with
 * [compose] or [view].
 */
public sealed class AdaptyCustomElementContent {

    public class Compose internal constructor(
        internal val content: @Composable (AdaptyCustomElementContext) -> Unit,
    ) : AdaptyCustomElementContent()

    public class View internal constructor(
        internal val factory: (Context, AdaptyCustomElementContext) -> android.view.View,
        internal val update: (android.view.View, AdaptyCustomElementContext) -> Unit,
    ) : AdaptyCustomElementContent()

    public companion object {

        /**
         * Composable content. It recomposes when a value it read through the context changes.
         */
        @JvmStatic
        public fun compose(content: @Composable (AdaptyCustomElementContext) -> Unit): Compose =
            Compose(content)

        /**
         * View content.
         *
         * [factory] must create a new view on every call: the same element can be on screen twice at
         * once (for example, during a screen transition), and it is created again each time it
         * reappears (a pager page, a re-entered screen). Keep expensive state outside the factory.
         *
         * [update] runs after [factory] and again whenever a value it read through the context changes.
         * The view itself is not recreated.
         */
        @JvmStatic
        @JvmOverloads
        public fun view(
            factory: (Context, AdaptyCustomElementContext) -> android.view.View,
            update: (android.view.View, AdaptyCustomElementContext) -> Unit = { _, _ -> },
        ): View =
            View(factory, update)
    }
}

/**
 * Gives the content of a `custom` element access to the element's assets, strings, bindings and
 * payload, to its enabled state, and to the flow script.
 *
 * Lookups take a key from the element's own `assets`, `strings` or `bindings` and return `null`
 * when the element has no such key. Reads are live: a composable or an `update` block that read
 * a value runs again when the value changes. Bindings are read-only.
 */
@Stable
public class AdaptyCustomElementContext internal constructor(
    private val element: CustomElement,
    private val stateAccessor: StateAccessor,
    private val screen: NavigationEntry,
    private val dispatch: (Message) -> Unit,
    private val sendAppMessage: (id: String, payload: Map<String, Any?>, source: Effect.SendSDKEvent.AppMessage.Source) -> Unit,
) {
    internal var assets: Map<String, AdaptyUiAsset> by mutableStateOf(emptyMap())
    internal var strings: Map<String, AnnotatedString> by mutableStateOf(emptyMap())
    internal var spannedStrings: Map<String, CharSequence> by mutableStateOf(emptyMap())
    internal var enabled: Boolean by mutableStateOf(true)
    internal var compositionContext: CompositionContext? = null
    internal var isDisposed: Boolean = false

    private val loggedBindingProblems = mutableSetOf<String>()

    /**
     * The asset under [id] in the element's `assets`, with the view's [AdaptyCustomAssets]
     * substitutes applied. `null` when the element has no such key or the asset is of a kind this
     * SDK version does not support.
     */
    public fun asset(id: String): AdaptyUiAsset? = assets[id]

    /**
     * The string under [id] in the element's `strings`, with tags replaced and styling kept.
     * Inline images are left out.
     */
    public fun annotatedString(id: String): AnnotatedString? = strings[id]

    /**
     * [annotatedString] as a `Spanned`, for views.
     */
    public fun text(id: String): CharSequence? = spannedStrings[id]

    /**
     * A composable that draws the string under [id] the way the SDK draws text: with the string's
     * own styling, aligned to the start. Attributes that a text element of the configuration adds
     * on top of a string (alignment, font size, line limit) do not apply, as the string is not tied
     * to a text element.
     */
    public fun textContent(id: String): (@Composable (Modifier) -> Unit)? {
        val stringId = element.strings?.get(id) ?: return null
        val textElement = TextElement(stringId, TextAlign.START, null, null, EMPTY_TEXT_ATTRIBUTES, BaseProps.EMPTY)
        return { modifier -> textElement.toComposable(dispatch, modifier)() }
    }

    /**
     * A composable that draws the image under [id] the way a built-in image element does: loaded
     * and cached by the SDK, with the preview of a remote image shown until it loads. The image is
     * stretched to the modifier's size, so give the modifier the size or aspect ratio it needs.
     */
    public fun imageContent(id: String): (@Composable (Modifier) -> Unit)? {
        val assetId = element.assets?.get(id) ?: return null
        val imageElement = ImageElement(assetId, AspectRatio.STRETCH, null, BaseProps.EMPTY)
        return { modifier -> imageElement.toComposable(dispatch, modifier)() }
    }

    /**
     * [textContent] as a view. Creates a new view on every call, so call it from the `factory` of
     * [AdaptyCustomElementContent.view], not from `update`. The view updates itself when the string
     * changes.
     */
    public fun textView(context: Context, id: String): android.view.View? =
        textContent(id)?.let { content -> composeView(context) { content(Modifier) } }

    /**
     * [imageContent] as a view. Creates a new view on every call, so call it from the `factory` of
     * [AdaptyCustomElementContent.view], not from `update`.
     */
    public fun imageView(context: Context, id: String): android.view.View? =
        imageContent(id)?.let { content -> composeView(context) { content(Modifier) } }

    /**
     * The current value of the binding under [key], converted to [T]: `Boolean`, `Int`, `Long`,
     * `Float`, `Double`, `String`, or a `List` of one of them. `null` when the element has no such
     * binding or the bound value is not set; also `null`, with an error in the log, when [T] is not
     * supported or the value cannot be converted.
     */
    public inline fun <reified T : Any> binding(key: String): T? =
        readBinding(key, typeOf<T>()) as? T

    /**
     * The Java-friendly form of [binding]. Primitive classes (`int.class`) work as their wrappers.
     * With `List.class` the items are returned as they are: integral numbers as `Long`, other
     * numbers as `Double`.
     */
    public fun <T : Any> binding(key: String, type: Class<T>): T? =
        type.kotlin.javaObjectType.cast(readBinding(key, type.kotlin, elementType = null))

    /**
     * The element's `payload` exactly as written in the configuration.
     */
    public val payloadString: String? get() = element.payload

    /**
     * [payloadString] encoded as UTF-8.
     */
    public val payloadBytes: ByteArray? get() = element.payload?.toByteArray(Charsets.UTF_8)

    /**
     * The element's `payload` parsed as a JSON object, or `null` when it is not one. It is parsed
     * again on every access. Integral numbers are returned as `Long`, other numbers as `Double`.
     */
    public val payloadMap: Map<String, Any?>?
        get() = element.payload?.let { payload ->
            @Suppress("UNCHECKED_CAST")
            runCatching {
                PAYLOAD_GSON.fromJson(payload, Map::class.java) as Map<String, Any?>?
            }.getOrNull()?.normalizeJsonNumbers()
        }

    /**
     * `false` while the element is disabled through `ui_enabled`. Taps do not reach the content
     * then, but accessibility services and keyboard focus still do, so set `enabled` on the
     * content's own controls.
     */
    public val isEnabled: Boolean get() = enabled

    /**
     * Sends [message] to the flow script on behalf of this element. Once the element has left the
     * screen, the message is dropped with a warning in the log.
     *
     * The script's `handleSDKEvent` receives `{ id, name: "customElementMsg", screen, element,
     * payload }`, where `screen` describes the screen showing the element (`instanceId`,
     * `navigatorId`, `type`, `contextPath`), `element` holds its `custom_id` and `custom_type`
     * (`id`, `type`), and `payload` is [message]. Values are converted as in
     * [AdaptyUI.FlowConfiguration.sendAppMessage].
     */
    public fun send(message: Map<String, Any?>) {
        if (isDisposed) {
            log(WARN) { "$LOG_PREFIX custom element ${element.customType}:${element.customId} sendMessage error: the element has left the screen, message dropped" }
            return
        }
        sendAppMessage(
            UUID.randomUUID().toString(),
            message,
            Effect.SendSDKEvent.AppMessage.Source.CustomElement(screen, element.customId, element.customType),
        )
    }

    @PublishedApi
    internal fun readBinding(key: String, type: KType): Any? {
        val classifier = type.classifier as? KClass<*>
            ?: return logBindingProblem(key, type.toString(), "unsupported type")
        val elementType = if (classifier == List::class) {
            type.arguments.firstOrNull()?.type?.classifier as? KClass<*>
                ?: return logBindingProblem(key, type.toString(), "unsupported type")
        } else null
        return readBinding(key, classifier, elementType)
    }

    private fun readBinding(key: String, type: KClass<*>, elementType: KClass<*>?): Any? {
        val typeName = type.simpleName + (elementType?.let { "<${it.simpleName}>" } ?: "")
        if (type != List::class && type !in SCALAR_BINDING_TYPES || elementType != null && elementType !in SCALAR_BINDING_TYPES)
            return logBindingProblem(key, typeName, "unsupported type")
        val binding = element.bindings?.get(key) ?: return null
        val value = stateAccessor.get(binding, screen.contextPath) ?: return null
        val converted = if (type == List::class) {
            (value as? List<*>)?.let { list ->
                if (elementType == null) list.takeIf { it.all(::isJsonScalar) }?.map { item -> item?.let(::normalizeJsonNumbers) }
                else list.map { item -> item.convertTo(elementType) ?: return@let null }
            }
        } else {
            value.convertTo(type)
        }
        return converted ?: logBindingProblem(key, typeName, "value $value does not convert")
    }

    private fun logBindingProblem(key: String, typeName: String, reason: String): Any? {
        if (loggedBindingProblems.add("$key|$typeName|$reason"))
            log(ERROR) { "$LOG_PREFIX custom element ${element.customType}:${element.customId} binding $key read error: $reason ($typeName)" }
        return null
    }

    private fun composeView(context: Context, content: @Composable () -> Unit) =
        ComposeView(context).apply {
            compositionContext?.let(::setParentCompositionContext)
            setContent(content)
        }

    private companion object {
        val PAYLOAD_GSON = Gson()

        val SCALAR_BINDING_TYPES = setOf(Boolean::class, Int::class, Long::class, Float::class, Double::class, String::class)

        val EMPTY_TEXT_ATTRIBUTES = BaseTextElement.Attributes(
            fontId = null,
            fontSize = null,
            strikethrough = false,
            underline = false,
            textColor = null,
            background = null,
            tint = null,
        )

        fun isJsonScalar(value: Any?) = value is Boolean || value is Number || value is String

        fun Any?.convertTo(type: KClass<*>): Any? = when (type) {
            Boolean::class -> this as? Boolean
            Int::class -> toJsInt()
            Long::class -> toJsLong()
            Float::class -> toJsFloat()
            Double::class -> when (this) {
                is Number -> toDouble()
                is Boolean -> if (this) 1.0 else 0.0
                else -> null
            }
            String::class -> this?.toBindingString()
            else -> null
        }
    }
}

internal object CustomElementsRegistry {
    var globalResolver: AdaptyCustomElementsResolver? by mutableStateOf(null)

    fun resolve(perViewResolver: AdaptyCustomElementsResolver?, type: String, id: String): AdaptyCustomElementContent? =
        (perViewResolver ?: globalResolver)?.resolve(type, id)
}
