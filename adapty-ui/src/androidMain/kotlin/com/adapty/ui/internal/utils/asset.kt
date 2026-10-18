@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.ui.resolveState

@InternalAdaptyApi
public class VisualValue(public val source: StringSource, public val orderedTypes: Set<Type>) {
    public enum class Type(public val condition: (String) -> Boolean) {
        ColorLiteral({ value -> value.matches(ColorRegex) }),
        AssetId({ true })
    }

    internal companion object {
        fun any(source: StringSource) = VisualValue(source, setOf(Type.ColorLiteral, Type.AssetId))
        fun assetId(source: StringSource) = VisualValue(source, setOf(Type.AssetId))
        fun assetId(assetId: String) = assetId(StringSource.Value(assetId))

        private val ColorRegex = Regex("^#([0-9A-Fa-f]{2}){3,4}\$")
    }
}

@InternalAdaptyApi
@Composable
internal inline fun <reified T: Asset.Filling> VisualValue.resolveAsset(): Asset.Composite<T>? {
    return source.resolve()?.let { value ->
        when (orderedTypes.firstOrNull { it.condition(value) }) {
            VisualValue.Type.ColorLiteral -> remember(value) {
                Asset.Composite(Asset.Color(value.parseColorInt())).castOrNull<T>()
            }
            VisualValue.Type.AssetId -> resolveAssets().getAsset<T>(value)
            else -> null
        }
    }
}

@Composable
internal fun VisualValue.resolveColorFilter(): ColorFilter? {
    return source.resolve()?.let { value ->
        when (orderedTypes.firstOrNull { it.condition(value) }) {
            VisualValue.Type.ColorLiteral -> remember(value) {
                ColorFilter.tint(Color(value.parseColorInt()))
            }
            VisualValue.Type.AssetId -> {
                val isSystemInDarkTheme = isSystemInDarkTheme()
                val tintAsset = resolveAsset<Asset.Color>()
                remember(isSystemInDarkTheme) {
                    tintAsset?.toComposeFill()?.color?.let { color ->
                        ColorFilter.tint(color)
                    }
                }
            }
            else -> null
        }
    }
}

@InternalAdaptyApi
@Composable
public fun StringSource.resolve(): String? {
    return when (this) {
        is StringSource.Value -> value
        is StringSource.Binding -> resolveState()[this]?.toBindingString()
    }
}

private fun Any.toBindingString(): String? = when (this) {
    is String -> this
    is Number -> {
        val d = toDouble()
        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
    }
    is Boolean -> toString()
    else -> null
}

@InternalAdaptyApi
public sealed class StringSource {
    public class Value internal constructor(public val value: String) : StringSource()
    public class Binding internal constructor(
        override val variable: String,
        override val scope: Scope,
        override val converter: String? = null,
        override val converterParams: Any? = null,
    ) : StringSource(), com.adapty.ui.internal.utils.Binding
}

@InternalAdaptyApi
public interface Binding {
    public val variable: String
    public val scope: Scope
    public val converter: String? get() = null
    public val converterParams: Any? get() = null
}
@InternalAdaptyApi
public class OneWayBinding(
    override val variable: String,
    override val scope: Scope,
    override val converter: String? = null,
    override val converterParams: Any? = null,
): Binding
@InternalAdaptyApi
public class TwoWayBinding(
    override val variable: String,
    override val scope: Scope,
    public val setter: String?,
    override val converter: String? = null,
    override val converterParams: Any? = null,
): Binding
@InternalAdaptyApi
public enum class Scope { Screen, Global }
