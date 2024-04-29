package com.adapty.internal.data.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BackendError(
    val responseCode: Int,
    val errorBody: String,
)