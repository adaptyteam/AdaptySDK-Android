package com.adapty.internal.data.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FallbackVariations(
    val placementId: String,
    val data: List<PaywallDto>,
)