package com.adapty.utils

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Random
import java.util.UUID

object UUIDTimeBased {

    private val lock = Any()

    private var lastTime: Long = 0
    private var clockSequence: Long = 0
    private val hostIdentifier = hostId

    private val hostId: Long
        get() {
            var macAddressAsLong: Long = 0
            try {
                val random = Random()
                val address = InetAddress.getLocalHost()
                val ni = NetworkInterface.getByInetAddress(address)
                if (ni != null) {
                    val mac = ni.hardwareAddress ?: return macAddressAsLong

                    random.nextBytes(mac)

                    for (i in mac.indices) {
                        macAddressAsLong = macAddressAsLong shl 8
                        macAddressAsLong = macAddressAsLong xor (mac[i].toLong() and 0xFF)
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return macAddressAsLong
        }

    fun generateId(): UUID {
        return generateIdFromTimestamp(System.currentTimeMillis())
    }

    private fun generateIdFromTimestamp(currentTimeMillis: Long): UUID {

        synchronized(lock) {
            if (currentTimeMillis > lastTime) {
                lastTime = currentTimeMillis
                clockSequence = 0
            } else {
                ++clockSequence
            }
        }

        // low Time
        var time = currentTimeMillis shl 32

        // mid Time
        time = time or (currentTimeMillis and 0xFFFF00000000L shr 16)

        // hi Time
        time = time or ((0x1000 or ((currentTimeMillis shr 48 and 0x0FFF).toInt())).toLong())

        var clockSequenceHi = clockSequence

        clockSequenceHi = clockSequenceHi shl 48

        val lsb = clockSequenceHi or hostIdentifier

        return UUID(time, lsb)
    }
}
