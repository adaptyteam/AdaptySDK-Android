package com.adapty.ui.internal.utils

internal fun String.decodeBase64Strict(): ByteArray? {
    if (length % 4 != 0) return null
    val padding = when {
        endsWith("==") -> 2
        endsWith("=") -> 1
        else -> 0
    }
    val bytes = ByteArray(length / 4 * 3 - padding)
    var byteIndex = 0
    for (groupStart in indices step 4) {
        var group = 0
        for (i in groupStart until groupStart + 4) {
            val sextet = if (i >= length - padding) 0 else this[i].base64Sextet()
            if (sextet < 0) return null
            group = (group shl 6) or sextet
        }
        for (shift in intArrayOf(16, 8, 0)) {
            if (byteIndex == bytes.size) break
            bytes[byteIndex++] = (group shr shift).toByte()
        }
    }
    return bytes
}

private fun Char.base64Sextet(): Int = when (this) {
    in 'A'..'Z' -> this - 'A'
    in 'a'..'z' -> this - 'a' + 26
    in '0'..'9' -> this - '0' + 52
    '+' -> 62
    '/' -> 63
    else -> -1
}
