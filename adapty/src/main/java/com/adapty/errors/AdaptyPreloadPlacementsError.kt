package com.adapty.errors

/**
 * Aggregated error of `Adapty.preloadFlows`/`Adapty.preloadOnboardings` (and their
 * `ForDefaultAudience` variants): contains the failure reason for each placement
 * in the batch that could not be preloaded, keyed by placement id.
 */
public class AdaptyPreloadPlacementsError internal constructor(
    public val preloadErrors: Map<String, AdaptyError>,
) : AdaptyError(
    message = "Failed to preload ${preloadErrors.size} placement(s): ${preloadErrors.map { (placementId, error) -> "$placementId: ${error.message}" }}",
    adaptyErrorCode = AdaptyErrorCode.REQUEST_FAILED,
)
