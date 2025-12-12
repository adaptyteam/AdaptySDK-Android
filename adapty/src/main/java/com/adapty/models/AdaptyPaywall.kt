@file:OptIn(InternalAdaptyApi::class)

package com.adapty.models

import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.immutableWithInterop
import com.adapty.utils.ImmutableList

/**
 * @property[hasViewConfiguration] If `true`, it is possible to fetch the [AdaptyUI.LocalizedViewConfiguration][com.adapty.ui.AdaptyUI.LocalizedViewConfiguration] object and use it with [AdaptyUI](https://search.maven.org/artifact/io.adapty/android-ui) library.
 * @property[placement] A placement, configured in Adapty Dashboard.
 * @property[name] A paywall name.
 * @property[remoteConfig] A [AdaptyRemoteConfig] configured in Adapty Dashboard for this paywall
 * @property[variationId] An identifier of a variation, used to attribute purchases to this paywall.
 * @property[vendorProductIds] Array of related products ids.
 */
public class AdaptyPaywall internal constructor(
    public val name: String,
    public val variationId: String,
    public val remoteConfig: AdaptyRemoteConfig?,
    public val placement: AdaptyPlacement,
    @get:JvmSynthetic internal val products: List<BackendProduct>,
    @get:JvmSynthetic internal val id: String,
    @get:JvmSynthetic internal val viewConfig: Map<String, Any>?,
    @get:JvmSynthetic internal val webPurchaseUrl: String?,
    @get:JvmSynthetic internal val requestedLocale: String,
    @get:JvmSynthetic internal val snapshotAt: Long,
) {

    @get:JvmName("hasViewConfiguration")
    public val hasViewConfiguration: Boolean get() = viewConfig != null

    public val vendorProductIds: ImmutableList<String> get() =
        products.map { it.vendorProductId }.immutableWithInterop()

    @Deprecated(
        message = "Moved to placement",
        replaceWith = ReplaceWith("placement.id"),
        level = DeprecationLevel.ERROR,
    )
    public val placementId: String get() = placement.id
    @Deprecated(
        message = "Moved to placement",
        replaceWith = ReplaceWith("placement.abTestName"),
        level = DeprecationLevel.ERROR,
    )
    public val abTestName: String get() = placement.abTestName
    @Deprecated(
        message = "Moved to placement",
        replaceWith = ReplaceWith("placement.audienceName"),
        level = DeprecationLevel.ERROR,
    )
    public val audienceName: String get() = placement.audienceName
    @Deprecated(
        message = "Moved to placement",
        replaceWith = ReplaceWith("placement.revision"),
        level = DeprecationLevel.ERROR,
    )
    public val revision: Int get() = placement.revision

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyPaywall

        if (name != other.name) return false
        if (variationId != other.variationId) return false
        if (vendorProductIds != other.vendorProductIds) return false
        if (remoteConfig != other.remoteConfig) return false
        if (placement != other.placement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + variationId.hashCode()
        result = 31 * result + vendorProductIds.hashCode()
        result = 31 * result + (remoteConfig?.hashCode() ?: 0)
        result = 31 * result + placement.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyPaywall(name=$name, variationId=$variationId, vendorProductIds=$vendorProductIds, remoteConfig=$remoteConfig, placement=$placement)"
    }

}