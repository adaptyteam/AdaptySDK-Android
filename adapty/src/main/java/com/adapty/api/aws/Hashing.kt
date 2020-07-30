package com.adapty.api.aws

import java.nio.charset.Charset
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal const val MAC_ALGORITHM = "HmacSHA256"

@Throws(Exception::class)
internal fun hmacSha256(key: ByteArray, data: String): ByteArray {
    val sha256Hmac = Mac.getInstance(MAC_ALGORITHM)
    val secretKey = SecretKeySpec(key, MAC_ALGORITHM)
    sha256Hmac.init(secretKey)

    return sha256Hmac.doFinal(data.toByteArray(charset("UTF-8")))
}

@Throws(Exception::class)
internal fun hmacSha256(key: String, data: String) =
    hmacSha256(key.toByteArray(Charset.forName("utf-8")), data)

/**
 * Convert the byte array to a hex string
 */
internal fun ByteArray.toHexString() =
    fold("") { str, it -> str + "%02x".format(it) }
