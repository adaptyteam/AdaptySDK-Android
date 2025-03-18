package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
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
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return {
            renderSection(resolveState) { currentIndex ->
                content[currentIndex].run {
                    render(
                        resolveAssets,
                        resolveText,
                        resolveState,
                        eventCallback,
                    )
                }
            }
        }
    }

    override fun ColumnScope.toComposableInColumn(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier
    ): @Composable () -> Unit {
        return {
            renderSection(resolveState) { currentIndex ->
                content[currentIndex].run {
                    render(
                        this@toComposableInColumn.toComposableInColumn(
                            resolveAssets,
                            resolveText,
                            resolveState,
                            eventCallback,
                            fillModifierWithScopedParams(
                                this,
                                Modifier.fillWithBaseParams(this, resolveAssets),
                            ),
                        )
                    )
                }
            }
        }
    }

    override fun RowScope.toComposableInRow(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return {
            renderSection(resolveState) { currentIndex ->
                content[currentIndex].run {
                    render(
                        this@toComposableInRow.toComposableInRow(
                            resolveAssets,
                            resolveText,
                            resolveState,
                            eventCallback,
                            fillModifierWithScopedParams(
                                this,
                                Modifier.fillWithBaseParams(this, resolveAssets),
                            ),
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun renderSection(
        resolveState: ResolveState,
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