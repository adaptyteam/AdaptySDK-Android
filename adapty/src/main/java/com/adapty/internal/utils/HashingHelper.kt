package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import java.nio.charset.Charset
import java.security.MessageDigest

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalAdaptyApi
public class HashingHelper {

    internal companion object {
        const val SHA_256 = "SHA-256"
        const val MD5 = "MD5"
    }

    public fun sha256(input: String): String {
        return hashString(input, SHA_256)
    }

    public fun md5(input: String): String {
        return hashString(input, MD5)
    }

    private fun hashString(input: String, algorithm: String, charset: Charset = Charsets.UTF_8): String {
        return toHexString(
            hashBytes(input, algorithm, charset)
        )
    }

    internal fun toHexString(byteArray: ByteArray) =
        byteArray.fold(StringBuilder()) { sb, it -> sb.append("%02x".format(it)) }.toString()

    internal fun hashBytes(input: String, algorithm: String, charset: Charset = Charsets.UTF_8): ByteArray {
        return MessageDigest
            .getInstance(algorithm)
            .digest(input.toByteArray(charset))
    }
}