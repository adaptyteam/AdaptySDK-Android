package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.models.Variation
import com.adapty.internal.utils.DEFAULT_PLACEMENT_LOCALE
import com.adapty.internal.utils.orDefault

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PlacementLocalReader(
    private val cacheRepository: CacheRepository,
    private val drawer: VariationDrawer,
) {

    fun readVariationFromCache(placementId: String, locales: Set<String>, variationType: VariationType, maxAgeMillis: Long? = null): Variation? {
        val pinnedVariationId = cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.get(placementId)
        if (pinnedVariationId != null) {
            return readVariationByIdFromCache(placementId, locales, pinnedVariationId, variationType)
        }
        cacheRepository.getRawVariations(placementId, locales, variationType, maxAgeMillis)?.let { variations ->
            val profileId = cacheRepository.getProfileId()
            val locale = locales.firstOrNull() ?: DEFAULT_PLACEMENT_LOCALE
            return runCatching {
                drawer.extractSingleVariation(variations.data, profileId, placementId, locale, PlacementSource.Cache, variationType)
            }.getOrNull()
        }
        return readLegacyVariation(placementId, locales, variationType, maxAgeMillis)
    }

    private fun readLegacyVariation(placementId: String, locales: Set<String>, variationType: VariationType, maxAgeMillis: Long? = null): Variation? {
        if (variationType == VariationType.Flow) return null
        return cacheRepository.getVariation(placementId, locales, variationType, maxAgeMillis)
    }

    fun readVariationFromCache(placementId: String, locale: String, variationType: VariationType, maxAgeMillis: Long? = null): Variation? =
        readVariationFromCache(placementId, setOf(locale), variationType, maxAgeMillis)

    fun readVariationByIdFromCache(placementId: String, locales: Set<String>, variationId: String, variationType: VariationType, maxAgeMillis: Long? = null): Variation? {
        cacheRepository.getRawSingleVariation(variationId, locales, variationType, maxAgeMillis)
            ?.let { variation ->
                drawer.sendVariationAssignedEvent(variation, variationType)
                return variation
            }
        cacheRepository.getRawVariations(placementId, locales, variationType, maxAgeMillis)
            ?.data?.firstOrNull { variation -> variation.variationId == variationId }
            ?.let { variation ->
                drawer.sendVariationAssignedEvent(variation, variationType)
                return variation
            }
        return readLegacyVariation(placementId, locales, variationType, maxAgeMillis)
            ?.takeIf { variation -> variation.variationId == variationId }
    }

    fun getCachedVariationsSnapshotAt(placementId: String, locale: String, variationType: VariationType): Long? =
        cacheRepository.getRawVariationsSnapshotAt(placementId, variationType)
            ?: cacheRepository.getVariation(placementId, setOf(locale, DEFAULT_PLACEMENT_LOCALE), variationType)?.snapshotAt

    fun selectFromCacheOrFallbackFile(placementId: String, locale: String, variationType: VariationType, placementSource: PlacementSource): Variation? {
        val cachedVariation = readVariationFromCache(placementId, setOf(locale, DEFAULT_PLACEMENT_LOCALE), variationType)
            ?: return drawFromFallbackFileOrNull(placementId, locale, placementSource, variationType)

        if (cachedVariation.snapshotAt >= cacheRepository.getFallbackFileSnapshotAt().orDefault())
            return cachedVariation

        return drawFromFallbackFileOrNull(placementId, locale, placementSource, variationType)
            ?: cachedVariation
    }

    fun drawFromFallbackFileOrNull(placementId: String, locale: String, placementSource: PlacementSource, variationType: VariationType): Variation? {
        val fallbackVariations = getFallbackFileVariations(placementId) ?: return null
        val profileId = cacheRepository.getProfileId()
        return runCatching { drawer.extractSingleVariation(fallbackVariations, profileId, placementId, locale, placementSource, variationType) }.getOrNull()
    }

    fun getFallbackFileVariations(placementId: String): List<Variation>? {
        return cacheRepository.getFallbackFileVariations(placementId)?.data
    }
}
