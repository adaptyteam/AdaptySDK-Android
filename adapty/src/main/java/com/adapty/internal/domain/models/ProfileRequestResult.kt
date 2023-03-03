package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class ProfileRequestResult {
    object ProfileIdSame: ProfileRequestResult()
    object ProfileIdChanged: ProfileRequestResult()
    class ProfileNotCreated(val error: Throwable): ProfileRequestResult()
}