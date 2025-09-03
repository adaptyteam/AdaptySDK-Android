package com.adapty.models

import com.adapty.internal.data.models.OnboardingBuilder

/**
 * @property[placement] A placement, configured in Adapty Dashboard.
 * @property[name] An onboarding name.
 * @property[remoteConfig] A [AdaptyRemoteConfig] configured in Adapty Dashboard for this onboarding
 * @property[variationId] An identifier of a variation, used to attribute purchases to this onboarding.
 */
public class AdaptyOnboarding internal constructor(
    public val name: String,
    public val variationId: String,
    public val remoteConfig: AdaptyRemoteConfig?,
    public val placement: AdaptyPlacement,
    @get:JvmSynthetic internal val id: String,
    @get:JvmSynthetic internal val viewConfig: OnboardingBuilder,
    @get:JvmSynthetic internal val requestedLocale: String,
    @get:JvmSynthetic internal val snapshotAt: Long,
) {

    @get:JvmName("hasViewConfiguration")
    public val hasViewConfiguration: Boolean get() = viewConfig != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyOnboarding

        if (name != other.name) return false
        if (variationId != other.variationId) return false
        if (remoteConfig != other.remoteConfig) return false
        if (placement != other.placement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + variationId.hashCode()
        result = 31 * result + (remoteConfig?.hashCode() ?: 0)
        result = 31 * result + placement.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyOnboarding(name=$name, variationId=$variationId, remoteConfig=$remoteConfig, placement=$placement)"
    }
}