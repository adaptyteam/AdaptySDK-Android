package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FlowLayout(
    val versionId: String,
    val id: String,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LayoutsConfiguration(
    val versionId: String,
    val layouts: List<LayoutDto>,
    val grids: List<GridDto>,
) {

    fun getLayout(deviceInfo: DeviceInfo, customId: String?): FlowLayout? {
        val grid = if (customId != null) {
            getGrid(customId) ?: return null
        } else {
            getGrid(deviceInfo.kind)
        }

        val index = grid.getIndex(deviceInfo.horizontal, deviceInfo.vertical)
        val layout = layouts.getOrNull(index) ?: throw noViewConfiguration()

        return FlowLayout(versionId = versionId, id = layout.id)
    }

    private fun getGrid(customId: String): GridDto? =
        grids.firstOrNull { it.customId == customId }

    private fun getGrid(kind: DeviceKind): GridDto =
        grids.firstOrNull { grid ->
            (grid.platforms?.contains(CURRENT_PLATFORM) ?: true) &&
                (grid.devices?.contains(kind.value) ?: true)
        } ?: throw noViewConfiguration()

    private fun GridDto.getIndex(horizontal: Int, vertical: Int): Int {
        val col = hBreakpoints.takeWhile { horizontal >= it }.count()
        val row = vBreakpoints.takeWhile { vertical >= it }.count()
        val index = row * (hBreakpoints.size + 1) + col
        return cells.getOrNull(index) ?: throw noViewConfiguration()
    }

    private fun noViewConfiguration() = AdaptyError(
        message = "View configuration has not been found for the requested flow",
        adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
    )

    internal companion object {
        private const val CURRENT_PLATFORM = "android"

        fun from(versionId: String?, uiSchema: UiSchemaDto?): LayoutsConfiguration? {
            if (versionId == null || uiSchema == null) return null
            return LayoutsConfiguration(versionId, uiSchema.layouts, uiSchema.grids)
        }
    }
}
