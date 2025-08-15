package com.adapty.ui.internal.ui.attributes

internal data class Point(
    val y: Float,
    val x: Float,
) {
    constructor(value: Float): this(value, value)

    companion object {
        val Zero = Point(0f, 0f)
        val One = Point(1f, 1f)
        val NormalizedCenter = Point(0.5f, 0.5f)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        if (y != other.y) return false
        if (x != other.x) return false

        return true
    }

    override fun hashCode(): Int {
        var result = y.hashCode()
        result = 31 * result + x.hashCode()
        return result
    }
}