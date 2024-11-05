package com.adapty.ui.internal.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.adapty.ui.AdaptyPaywallInsets

internal sealed class InsetWrapper {
    abstract fun getTop(density: Density): Int
    abstract fun getBottom(density: Density): Int
    abstract fun getLeft(density: Density, layoutDirection: LayoutDirection): Int
    abstract fun getRight(density: Density, layoutDirection: LayoutDirection): Int

    val isCustom get() = this is Custom

    class System(internal val insets: WindowInsets): InsetWrapper() {
        override fun getBottom(density: Density): Int {
            return insets.getBottom(density)
        }

        override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
            return insets.getLeft(density, layoutDirection)
        }

        override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
            return insets.getRight(density, layoutDirection)
        }

        override fun getTop(density: Density): Int {
            return insets.getTop(density)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as System

            return insets == other.insets
        }

        override fun hashCode(): Int {
            return insets.hashCode()
        }
    }

    class Custom(internal val insets: AdaptyPaywallInsets): InsetWrapper() {
        override fun getBottom(density: Density): Int {
            return insets.bottom
        }

        override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
            return if (layoutDirection == LayoutDirection.Rtl) insets.end else insets.start
        }

        override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
            return if (layoutDirection == LayoutDirection.Rtl) insets.start else insets.end
        }

        override fun getTop(density: Density): Int {
            return insets.top
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Custom

            return insets == other.insets
        }

        override fun hashCode(): Int {
            return insets.hashCode()
        }
    }
}

internal fun WindowInsets.wrap() = InsetWrapper.System(this)

internal fun AdaptyPaywallInsets.wrap() = InsetWrapper.Custom(this)

internal val LocalCustomInsets = staticCompositionLocalOf<InsetWrapper.Custom> {
    AdaptyPaywallInsets.UNSPECIFIED.wrap()
}

@Composable
internal fun getInsets() = LocalCustomInsets.current.takeIf { it.insets != AdaptyPaywallInsets.UNSPECIFIED }
    ?: WindowInsets.safeContent.wrap()