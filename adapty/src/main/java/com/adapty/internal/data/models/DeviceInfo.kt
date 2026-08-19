package com.adapty.internal.data.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class DeviceKind(val value: String) {
    PHONE("phone"),
    TAB("tab"),
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DeviceInfo(
    val kind: DeviceKind,
    val horizontal: Int,
    val vertical: Int,
)
