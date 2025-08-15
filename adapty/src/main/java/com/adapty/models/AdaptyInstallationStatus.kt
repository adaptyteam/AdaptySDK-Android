package com.adapty.models

import com.adapty.utils.ImmutableMap

public sealed class AdaptyInstallationStatus {

    public sealed class Determined: AdaptyInstallationStatus() {
        public class Success(public val details: AdaptyInstallationDetails): Determined() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Success

                return details == other.details
            }

            override fun hashCode(): Int {
                return details.hashCode()
            }

            override fun toString(): String {
                return "AdaptyInstallationStatusSuccess(details=$details)"
            }
        }

        public object NotAvailable: Determined() {
            override fun toString(): String {
                return "AdaptyInstallationStatusNotAvailable"
            }
        }
    }

    public object NotDetermined: AdaptyInstallationStatus() {
        override fun toString(): String {
            return "AdaptyInstallationStatusNotDetermined"
        }
    }
}

public class AdaptyInstallationDetails(
    public val id: String,
    public val installedAt: String,
    public val appLaunchCount: Long,
    public val payload: Payload?,
) {
    public class Payload(
        public val jsonString: String,
        public val dataMap: ImmutableMap<String, Any>,
    ) {
        override fun toString(): String {
            return "Payload(jsonString='$jsonString', dataMap=$dataMap)"
        }
    }

    override fun toString(): String {
        return "AdaptyInstallationDetails(id='$id', installedAt='$installedAt', appLaunchCount=$appLaunchCount, payload=$payload)"
    }
}
