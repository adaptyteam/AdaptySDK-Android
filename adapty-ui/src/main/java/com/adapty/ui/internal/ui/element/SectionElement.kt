package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class SectionElement internal constructor(
    internal val id: String,
    internal val index: Int,
    override var content: List<UIElement>,
): UIElement, MultiContainer {
    override val baseProps: BaseProps = BaseProps.EMPTY

    internal val key get() = getKey(id)

    internal companion object {
        fun getKey(sectionId: String) = "section_$sectionId"
    }

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return {
            renderSection(resolveState) { currentIndex ->
                content[currentIndex].run {
                    toComposable(
                        resolveAssets,
                        resolveText,
                        resolveState,
                        eventCallback,
                        Modifier.fillWithBaseParams(this, resolveAssets)
                    ).invoke()
                }
            }
        }
    }

    override fun ColumnScope.toComposableInColumn(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier
    ): @Composable () -> Unit {
        return {
            renderSection(resolveState) { currentIndex ->
                content[currentIndex].run {
                    this@toComposableInColumn.toComposableInColumn(
                        resolveAssets,
                        resolveText,
                        resolveState,
                        eventCallback,
                        fillModifierWithScopedParams(
                            this,
                            Modifier.fillWithBaseParams(this, resolveAssets),
                        ),
                    ).invoke()
                }
            }
        }
    }

    override fun RowScope.toComposableInRow(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return {
            renderSection(resolveState) { currentIndex ->
                content[currentIndex].run {
                    this@toComposableInRow.toComposableInRow(
                        resolveAssets,
                        resolveText,
                        resolveState,
                        eventCallback,
                        fillModifierWithScopedParams(
                            this,
                            Modifier.fillWithBaseParams(this, resolveAssets),
                        ),
                    ).invoke()
                }
            }
        }
    }

    @Composable
    private fun renderSection(
        resolveState: () -> Map<String, Any>,
        renderChild: @Composable (currentIndex: Int) -> Unit,
    ) {
        val state = resolveState()
        val currentIndex by remember {
            derivedStateOf { (state[key] as? Int) ?: index }
        }
        if (currentIndex in content.indices)
            renderChild(currentIndex)
    }
}