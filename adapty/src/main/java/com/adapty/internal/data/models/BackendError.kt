package com.adapty.internal.data.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BackendError(
    val responseCode: Int,
    private val internalErrors: Set<InternalError>,
) {
    internal companion object {
        const val INCORRECT_SEGMENT_HASH_ERROR = "INCORRECT_SEGMENT_HASH_ERROR"
    }

    class InternalError(val code: String)

    fun containsErrorCode(code: String) =
        internalErrors.any { error -> error.code == code }
}