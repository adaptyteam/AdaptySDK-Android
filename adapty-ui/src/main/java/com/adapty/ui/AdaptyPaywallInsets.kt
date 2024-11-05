package com.adapty.ui

/**
 * @property[start] Additional left margin in pixels for LTR and right margin for RTL layouts
 * @property[top] Additional top margin in pixels. Useful when the status bar overlaps the paywall screen
 * @property[end] Additional right margin in pixels for LTR and left margin for RTL layouts
 * @property[bottom] Additional bottom margin in pixels. Useful when the navigation bar overlaps the paywall screen
 */
public class AdaptyPaywallInsets private constructor(
    public val start: Int,
    public val top: Int,
    public val end: Int,
    public val bottom: Int,
) {
    public companion object {
        /**
         * @param[start] Additional left margin in pixels for LTR and right margin for RTL layouts
         * @param[top] Additional top margin in pixels. Useful when the status bar overlaps the paywall screen
         * @param[end] Additional right margin in pixels for LTR and left margin for RTL layouts
         * @param[bottom] Additional bottom margin in pixels. Useful when the navigation bar overlaps the paywall screen
         */
        @JvmStatic
        public fun of(start: Int, top: Int, end: Int, bottom: Int): AdaptyPaywallInsets =
            AdaptyPaywallInsets(start, top, end, bottom)

        /**
         * @param[top] Additional top margin in pixels. Useful when the status bar overlaps the paywall screen
         * @param[bottom] Additional bottom margin in pixels. Useful when the navigation bar overlaps the paywall screen
         */
        @JvmStatic
        public fun vertical(top: Int, bottom: Int): AdaptyPaywallInsets =
            AdaptyPaywallInsets(0, top, 0, bottom)

        /**
         * @param[start] Additional left margin in pixels for LTR and right margin for RTL layouts
         * @param[end] Additional right margin in pixels for LTR and left margin for RTL layouts
         */
        @JvmStatic
        public fun horizontal(start: Int, end: Int): AdaptyPaywallInsets =
            AdaptyPaywallInsets(start, 0, end, 0)

        /**
         * @param[all] Additional margins in pixels
         */
        @JvmStatic
        public fun of(all: Int): AdaptyPaywallInsets = AdaptyPaywallInsets(all, all, all, all)

        /**
         * You can use this field when none of the system bars overlap the paywall screen
         */
        @JvmField
        public val NONE: AdaptyPaywallInsets = of(0)

        /**
         * You can use this field in case the paywall screen is edge-to-edge
         * and actual window insets should be applied
         */
        @JvmField
        public val UNSPECIFIED: AdaptyPaywallInsets = of(-1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdaptyPaywallInsets) return false

        if (start != other.start) return false
        if (top != other.top) return false
        if (end != other.end) return false
        if (bottom != other.bottom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + top
        result = 31 * result + end
        result = 31 * result + bottom
        return result
    }

    override fun toString(): String {
        return "AdaptyPaywallInsets(start=$start, top=$top, end=$end, bottom=$bottom)"
    }
}