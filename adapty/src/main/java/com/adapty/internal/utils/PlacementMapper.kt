package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.Placement
import com.adapty.models.AdaptyPlacement

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PlacementMapper {

    fun map(placement: Placement) = AdaptyPlacement(
        id = placement.id,
        abTestName = placement.abTestName,
        audienceName = placement.audienceName,
        revision = placement.revision,
        isTrackingPurchases = placement.isTrackingPurchases ?: false,
        audienceVersionId = placement.placementAudienceVersionId,
    )
}