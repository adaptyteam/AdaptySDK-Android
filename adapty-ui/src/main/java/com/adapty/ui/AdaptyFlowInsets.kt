package com.adapty.ui

/**
 * @property[start] Additional left margin in pixels for LTR and right margin for RTL layouts
 * @property[top] Additional top margin in pixels. Useful when the status bar overlaps the flow screen
 * @property[end] Additional right margin in pixels for LTR and left margin for RTL layouts
 * @property[bottom] Additional bottom margin in pixels. Useful when the navigation bar overlaps the flow screen
 */
public class AdaptyFlowInsets private constructor(
    public val start: Int,
    public val top: Int,
    public val end: Int,
    public val bottom: Int,
) {
    public companion object {
        /**
         * @param[start] Additional left margin in pixels for LTR and right margin for RTL layouts
         * @param[top] Additional top margin in pixels. Useful when the status bar overlaps the flow screen
         * @param[end] Additional right margin in pixels for LTR and left margin for RTL layouts
         * @param[bottom] Additional bottom margin in pixels. Useful when the navigation bar overlaps the flow screen
         */
        @JvmStatic
        public fun of(start: Int, top: Int, end: Int, bottom: Int): AdaptyFlowInsets =
            AdaptyFlowInsets(start, top, end, bottom)

        /**
         * @param[top] Additional top margin in pixels. Useful when the status bar overlaps the flow screen
         * @param[bottom] Additional bottom margin in pixels. Useful when the navigation bar overlaps the flow screen
         */
        @JvmStatic
        public fun vertical(top: Int, bottom: Int): AdaptyFlowInsets =
            AdaptyFlowInsets(0, top, 0, bottom)

        /**
         * @param[start] Additional left margin in pixels for LTR and right margin for RTL layouts
         * @param[end] Additional right margin in pixels for LTR and left margin for RTL layouts
         */
        @JvmStatic
        public fun horizontal(start: Int, end: Int): AdaptyFlowInsets =
            AdaptyFlowInsets(start, 0, end, 0)

        /**
         * @param[all] Additional margins in pixels
         */
        @JvmStatic
        public fun of(all: Int): AdaptyFlowInsets = AdaptyFlowInsets(all, all, all, all)

        /**
         * You can use this field when none of the system bars overlap the flow screen
         */
        @JvmField
        public val None: AdaptyFlowInsets = of(0)

        /**
         * You can use this field in case the flow screen is edge-to-edge
         * and actual window insets should be applied
         */
        @JvmField
        public val Unspecified: AdaptyFlowInsets = of(-1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdaptyFlowInsets) return false

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
        return "AdaptyFlowInsets(start=$start, top=$top, end=$end, bottom=$bottom)"
    }
}