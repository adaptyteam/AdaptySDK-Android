@file:OptIn(InternalAdaptyApi::class)

package com.adapty.models

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.utils.ImmutableList

public class AdaptyFlow internal constructor(
    public val id: String,
    public val variationId: String,
    public val name: String,
    public val remoteConfigs: ImmutableList<AdaptyRemoteConfig>,
    public val placement: AdaptyPlacement,
    public val paywalls: ImmutableList<AdaptyFlowPaywall>,
    @get:JvmSynthetic internal val viewConfigurationId: String?,
    @get:JvmSynthetic internal val viewConfig: Map<String, Any>?,
    @get:JvmSynthetic internal val requestedLocale: String,
    @get:JvmSynthetic internal val snapshotAt: Long,
) {

    @get:JvmName("hasViewConfiguration")
    public val hasViewConfiguration: Boolean get() = viewConfigurationId != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyFlow

        if (id != other.id) return false
        if (variationId != other.variationId) return false
        if (name != other.name) return false
        if (remoteConfigs != other.remoteConfigs) return false
        if (placement != other.placement) return false
        if (paywalls != other.paywalls) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + variationId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + remoteConfigs.hashCode()
        result = 31 * result + placement.hashCode()
        result = 31 * result + paywalls.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyFlow(id='$id', variationId='$variationId', name='$name', remoteConfigs=$remoteConfigs, placement=$placement, paywalls=$paywalls)"
    }
}
