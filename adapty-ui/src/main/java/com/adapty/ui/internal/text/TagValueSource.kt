@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import androidx.compose.runtime.Composable
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.utils.Scope

internal sealed class TagValueSource {
    abstract val defaultConverter: ConverterSpec?

    class Literal(
        val value: Any,
        override val defaultConverter: ConverterSpec? = null,
    ) : TagValueSource()

    class Binding internal constructor(
        override val variable: String,
        override val scope: Scope,
        override val converter: String? = null,
        override val converterParams: Any? = null,
    ) : TagValueSource(), com.adapty.ui.internal.utils.Binding {
        override val defaultConverter: ConverterSpec? = null
    }
}

@Composable
internal fun TagValueSource.resolve(): Any? = when (this) {
    is TagValueSource.Literal -> value
    is TagValueSource.Binding -> resolveState()[this]
}
