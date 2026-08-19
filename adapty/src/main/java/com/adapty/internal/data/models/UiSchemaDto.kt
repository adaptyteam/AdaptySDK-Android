package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class UiSchemaDto(
    @SerializedName("layouts")
    val layouts: List<LayoutDto>,
    @SerializedName("grids")
    val grids: List<GridDto>,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LayoutDto(
    @SerializedName("flow_layout_id")
    val id: String,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class GridDto(
    val platforms: List<String>?,
    val devices: List<String>?,
    val customId: String?,
    val hBreakpoints: List<Int>,
    val vBreakpoints: List<Int>,
    val cells: List<Int>,
)
