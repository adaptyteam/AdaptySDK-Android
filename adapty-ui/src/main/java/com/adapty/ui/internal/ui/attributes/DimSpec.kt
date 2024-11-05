package com.adapty.ui.internal.ui.attributes

import com.adapty.internal.utils.InternalAdaptyApi

@InternalAdaptyApi
public sealed class DimSpec(internal val axis: Axis) {
    internal enum class Axis { X, Y }

    public class Min internal constructor(internal val value: DimUnit, axis: Axis): DimSpec(axis)
    public class FillMax internal constructor(axis: Axis): DimSpec(axis)
    public class Specified internal constructor(internal val value: DimUnit, axis: Axis): DimSpec(axis)
    public class Shrink internal constructor(internal val min: DimUnit, axis: Axis): DimSpec(axis)
}