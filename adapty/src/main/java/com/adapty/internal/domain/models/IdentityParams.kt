package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class IdentityParams(
    val customerUserId: String?,
    val obfuscatedAccountId: String?,
) {
    companion object Companion {
        fun from(
            customerUserId: String?,
            obfuscatedAccountId: String?,
        ): IdentityParams? {
            if (customerUserId.isNullOrBlank() && obfuscatedAccountId == null)
                return null
            return IdentityParams(customerUserId, obfuscatedAccountId)
        }
    }
}