package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.domain.models.BackendProduct

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductPicker {

    @JvmSynthetic
    fun pick(
        source1: List<BackendProduct>,
        source2: List<BackendProduct>,
        requiredIds: Set<String>,
    ): List<BackendProduct> {
        val source1Map = source1.associateBy { product -> product.vendorProductId }
        val source2Map = source2.associateBy { product -> product.vendorProductId }
        return requiredIds.mapNotNull { productId ->
            val product1 = source1Map[productId]
            val product2 = source2Map[productId]

            when {
                product1 == null && product2 == null -> null
                product1 != null && (product2 == null || product1.timestamp >= product2.timestamp) -> product1
                else -> product2
            }
        }
    }
}