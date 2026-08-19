@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.data.cache

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.utils.HashingHelper
import com.adapty.internal.utils.InternalAdaptyApi
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FileCache(
    appContext: Context,
    private val gson: Gson,
    private val hashingHelper: HashingHelper,
) {

    internal enum class ItemType(val dirName: String, val schemaVersion: Int) {
        FLOW_VARIANTS("flow_variants", 1),
        ONBOARDING_VARIANTS("onboarding_variants", 1),
        FLOW("flow", 1),
        ONBOARDING("onboarding", 1),
        FLOW_LAYOUT("flow_layout", 2),
    }

    internal class ItemKey(
        val profileId: String?,
        val itemType: ItemType,
        val itemId: String,
    )

    internal class Meta(
        @SerializedName("profile")
        val profileId: String?,
        @SerializedName("type")
        val itemType: String,
        @SerializedName("id")
        val itemId: String,
        @SerializedName("format")
        val schemaVersion: Int,
        @SerializedName("size")
        val size: Long,
        @SerializedName("locale")
        val locale: String?,
        @SerializedName("segment_id")
        val segmentId: String?,
        @SerializedName("snapshot_at")
        val snapshotAt: Long,
        @SerializedName("stored_at")
        val storedAt: Long,
        @SerializedName("last_accessed_at")
        val lastAccessedAt: Long,
    )

    private val root = File(appContext.cacheDir, ROOT_DIR_NAME)

    private var totalBytesUpperBound: Long? = null

    private var nextEvictionScanAllowedAt: Long? = null

    @Synchronized
    fun write(key: ItemKey, data: String, locale: String?, segmentId: String?, snapshotAt: Long) {
        try {
            val directory = directoryFor(key)
            if (!directory.exists() && !directory.mkdirs()) return
            val dataFile = dataFile(key)
            val oldDataSize = if (dataFile.exists()) dataFile.length() else 0L
            val bytes = data.toByteArray(Charsets.UTF_8)
            if (!writeAtomically(dataFile, bytes)) return
            val now = System.currentTimeMillis()
            val meta = Meta(
                profileId = key.profileId,
                itemType = key.itemType.dirName,
                itemId = key.itemId,
                schemaVersion = key.itemType.schemaVersion,
                size = bytes.size.toLong(),
                locale = locale,
                segmentId = segmentId,
                snapshotAt = snapshotAt,
                storedAt = now,
                lastAccessedAt = now,
            )
            if (!writeAtomically(metaFile(key), gson.toJson(meta).toByteArray(Charsets.UTF_8))) {
                dataFile.delete()
                return
            }
            totalBytesUpperBound = totalBytesUpperBound?.plus(bytes.size - oldDataSize)
            enforceSizeLimit()
        } catch (e: Exception) {
        }
    }

    @Synchronized
    fun read(key: ItemKey): Pair<String, Meta>? {
        return try {
            val meta = readValidatedMeta(key) ?: return null
            val data = dataFile(key).readText(Charsets.UTF_8)
            val touched = Meta(
                meta.profileId, meta.itemType, meta.itemId, meta.schemaVersion, meta.size,
                meta.locale, meta.segmentId, meta.snapshotAt, meta.storedAt,
                lastAccessedAt = System.currentTimeMillis(),
            )
            runCatching { writeAtomically(metaFile(key), gson.toJson(touched).toByteArray(Charsets.UTF_8)) }
            data to meta
        } catch (e: Exception) {
            remove(key)
            null
        }
    }

    @Synchronized
    fun readMeta(key: ItemKey): Meta? =
        try {
            readValidatedMeta(key)
        } catch (e: Exception) {
            null
        }

    @Synchronized
    fun remove(key: ItemKey) {
        runCatching { metaFile(key).delete() }
        runCatching { dataFile(key).delete() }
    }

    @Synchronized
    fun removeAll() {
        runCatching { root.deleteRecursively() }
        totalBytesUpperBound = 0L
        nextEvictionScanAllowedAt = null
    }

    @Synchronized
    fun removeOtherProfiles(currentProfileId: String) {
        val keep = setOf(SHARED_DIR_NAME, hashingHelper.sha256(currentProfileId))
        runCatching {
            root.listFiles()?.forEach { dir ->
                if (dir.name !in keep) dir.deleteRecursively()
            }
        }
    }

    private fun readValidatedMeta(key: ItemKey): Meta? {
        val metaFile = metaFile(key)
        if (!metaFile.exists()) return null
        val meta = try {
            gson.fromJson(metaFile.readText(Charsets.UTF_8), Meta::class.java)
        } catch (e: Exception) {
            null
        }
        val valid = meta != null
                && meta.schemaVersion == key.itemType.schemaVersion
                && meta.itemId == key.itemId
                && meta.itemType == key.itemType.dirName
                && dataFile(key).exists()
        if (!valid) {
            remove(key)
            return null
        }
        return meta
    }

    private fun enforceSizeLimit() {
        val upperBound = totalBytesUpperBound
        if (upperBound != null && upperBound <= MAX_BYTES) return
        val now = System.currentTimeMillis()
        nextEvictionScanAllowedAt?.let { allowedAt -> if (now < allowedAt) return }

        val entries = scanEntries(now)
        val totalSize = entries.sumOf { entry -> entry.size }
        if (totalSize <= MAX_BYTES) {
            totalBytesUpperBound = totalSize
            nextEvictionScanAllowedAt = null
            return
        }

        val toFree = totalSize - MAX_BYTES
        var freed = 0L
        val (evictable, graced) = entries.partition { entry -> now - entry.storedAt >= GRACE_PERIOD_MILLIS }
        for (entry in evictable.sortedBy { it.lastAccessedAt }) {
            if (freed >= toFree) break
            entry.metaFile.delete()
            entry.dataFile.delete()
            freed += entry.size
        }
        totalBytesUpperBound = totalSize - freed
        nextEvictionScanAllowedAt =
            if (freed >= toFree) null
            else graced.minOfOrNull { entry -> entry.storedAt + GRACE_PERIOD_MILLIS }
    }

    private class Entry(
        val metaFile: File,
        val dataFile: File,
        val size: Long,
        val storedAt: Long,
        val lastAccessedAt: Long,
    )

    private fun scanEntries(now: Long): List<Entry> {
        val entries = mutableListOf<Entry>()
        root.listFiles()?.forEach { profileDir ->
            profileDir.listFiles()?.forEach { typeDir ->
                typeDir.listFiles()?.forEach { file ->
                    when (file.extension) {
                        META_EXTENSION -> {
                            val dataFile = File(file.parentFile, "${file.nameWithoutExtension}.$DATA_EXTENSION")
                            val meta = runCatching {
                                gson.fromJson(file.readText(Charsets.UTF_8), Meta::class.java)
                            }.getOrNull()
                            if (meta == null || !dataFile.exists()) {
                                file.delete()
                                dataFile.delete()
                            } else {
                                val size = if (meta.size > 0) meta.size else dataFile.length()
                                entries.add(Entry(file, dataFile, size, meta.storedAt, meta.lastAccessedAt))
                            }
                        }
                        DATA_EXTENSION -> {
                            val metaFile = File(file.parentFile, "${file.nameWithoutExtension}.$META_EXTENSION")
                            if (!metaFile.exists() && now - file.lastModified() >= GRACE_PERIOD_MILLIS)
                                file.delete()
                        }
                        TMP_EXTENSION -> {
                            if (now - file.lastModified() >= GRACE_PERIOD_MILLIS)
                                file.delete()
                        }
                    }
                }
            }
        }
        return entries
    }

    private fun writeAtomically(target: File, bytes: ByteArray): Boolean {
        val tmp = File(target.parentFile, "${target.name}.$TMP_EXTENSION")
        return try {
            tmp.writeBytes(bytes)
            if (tmp.renameTo(target)) {
                true
            } else {
                tmp.delete()
                false
            }
        } catch (e: Exception) {
            runCatching { tmp.delete() }
            false
        }
    }

    private fun directoryFor(key: ItemKey) =
        File(File(root, profileDirName(key.profileId)), key.itemType.dirName)

    private fun profileDirName(profileId: String?) =
        profileId?.let(hashingHelper::sha256) ?: SHARED_DIR_NAME

    private fun dataFile(key: ItemKey) =
        File(directoryFor(key), "${hashingHelper.sha256(key.itemId)}.$DATA_EXTENSION")

    private fun metaFile(key: ItemKey) =
        File(directoryFor(key), "${hashingHelper.sha256(key.itemId)}.$META_EXTENSION")

    private companion object {
        const val ROOT_DIR_NAME = "sdk.adapty.io"
        const val SHARED_DIR_NAME = "shared"
        const val DATA_EXTENSION = "data"
        const val META_EXTENSION = "meta"
        const val TMP_EXTENSION = "tmp"
        const val MAX_BYTES = 20L * 1024 * 1024
        const val GRACE_PERIOD_MILLIS = 15L * 60 * 1000
    }
}
