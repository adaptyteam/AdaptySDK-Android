package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import java.nio.charset.Charset
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class HashingHelper {

    private companion object {
        private const val MAC_ALGORITHM = "HmacSHA256"
    }

    fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val sha256Hmac = Mac.getInstance(MAC_ALGORITHM)
        val secretKey = SecretKeySpec(key, MAC_ALGORITHM)
        sha256Hmac.init(secretKey)

        return sha256Hmac.doFinal(data.toByteArray(charset("UTF-8")))
    }

    fun hmacSha256(key: String, data: String) =
        hmacSha256(key.toByteArray(Charset.forName("utf-8")), data)

    fun toHexString(byteArray: ByteArray) =
        byteArray.fold(StringBuilder()) { sb, it -> sb.append("%02x".format(it)) }.toString()

    fun sha256(input: String): String {
        return hashString(input, "SHA-256")
    }

    fun md5(input: String): String {
        return hashString(input, "MD5")
    }

    private fun hashString(input: String, algorithm: String, charset: Charset = Charsets.UTF_8): String {
        return toHexString(
            MessageDigest
                .getInstance(algorithm)
                .digest(input.toByteArray(charset))
        )
    }
}