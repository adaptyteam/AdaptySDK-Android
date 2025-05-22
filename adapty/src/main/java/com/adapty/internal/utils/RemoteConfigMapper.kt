package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.RemoteConfigDto
import com.adapty.models.AdaptyRemoteConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RemoteConfigMapper {

    fun map(remoteConfig: RemoteConfigDto) = AdaptyRemoteConfig(
        locale = remoteConfig.lang,
        jsonString = remoteConfig.data,
        dataMap = remoteConfig.dataMap.immutableWithInterop(),
    )
}