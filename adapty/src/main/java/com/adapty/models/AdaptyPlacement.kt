package com.adapty.models

/**
 * @property[abTestName] Parent A/B test name.
 * @property[audienceName] A name of an audience to which the placement belongs.
 * @property[id] An identifier of a placement, configured in Adapty Dashboard.
 * @property[revision] Current revision (version) of a placement. Every change within a placement creates a new revision.
 */
public class AdaptyPlacement internal constructor(
    public val id: String,
    public val abTestName: String,
    public val audienceName: String,
    public val revision: Int,
    @get:JvmSynthetic internal val isTrackingPurchases: Boolean,
    @get:JvmSynthetic internal val audienceVersionId: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyPlacement

        if (id != other.id) return false
        if (abTestName != other.abTestName) return false
        if (audienceName != other.audienceName) return false
        if (revision != other.revision) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + abTestName.hashCode()
        result = 31 * result + audienceName.hashCode()
        result = 31 * result + revision
        return result
    }

    override fun toString(): String {
        return "AdaptyPlacement(id=$id, abTestName=$abTestName, audienceName=$audienceName, revision=$revision)"
    }
}