package com.adapty.internal.domain

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal suspend fun completeUnsyncedValidate(
    responseProfileId: String,
    currentProfileId: String,
    removeUnsyncedData: () -> Unit,
    saveProfile: suspend () -> Unit,
) {
    removeUnsyncedData()
    if (responseProfileId == currentProfileId)
        saveProfile()
}
