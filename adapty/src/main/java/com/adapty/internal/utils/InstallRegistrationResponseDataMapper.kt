package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.InstallData
import com.adapty.internal.data.models.InstallRegistrationResponseData
import com.adapty.models.AdaptyInstallationDetails
import com.adapty.models.AdaptyInstallationStatus

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InstallRegistrationResponseDataMapper(
    private val installationPayloadMapper: InstallationPayloadMapper,
    private val metaInfoRetriever: MetaInfoRetriever,
) {

    fun mapStatus(data: InstallRegistrationResponseData, installData: InstallData, sessionCount: Long) =
        AdaptyInstallationStatus.Determined.Success(
            details = mapDetails(data, installData, sessionCount)
        )

    fun mapDetails(data: InstallRegistrationResponseData, installData: InstallData, sessionCount: Long) =
        AdaptyInstallationDetails(
            id = data.installId,
            installedAt = metaInfoRetriever.formatDateTimeGMT(installData.installTimestampMillis),
            appLaunchCount = sessionCount,
            payload = data.payload?.takeIf(String::isNotEmpty)?.let(installationPayloadMapper::map),
        )
}
