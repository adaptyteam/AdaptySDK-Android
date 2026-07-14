@file:OptIn(InternalAdaptyApi::class)

package com.adapty.models

import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.immutableWithInterop
import com.adapty.utils.ImmutableList

public class AdaptyFlowPaywall internal constructor(
    public val id: String,
    public val variationId: String,
    public val name: String,
    public val placement: AdaptyPlacement,
    @get:JvmSynthetic internal val products: List<BackendProduct>,
    @get:JvmSynthetic internal val webPurchaseUrl: String?,
) {

    public val vendorProductIds: ImmutableList<String> get() =
        products.map { it.vendorProductId }.immutableWithInterop()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyFlowPaywall

        if (id != other.id) return false
        if (variationId != other.variationId) return false
        if (name != other.name) return false
        if (vendorProductIds != other.vendorProductIds) return false
        if (placement != other.placement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + variationId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + vendorProductIds.hashCode()
        result = 31 * result + placement.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyFlowPaywall(id=$id, variationId=$variationId, name=$name, vendorProductIds=$vendorProductIds, placement=$placement)"
    }
}
