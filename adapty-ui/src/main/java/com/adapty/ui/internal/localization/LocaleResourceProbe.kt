package com.adapty.ui.internal.localization

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.io.File
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RequiresApi(Build.VERSION_CODES.N)
internal abstract class LocaleResourceProbe : Closeable {

    abstract fun ask(locale: Locale): Int

    protected fun Resources.answer(): Int =
        getString(LocaleResourceTable.RES_ID).let { if (it == LocaleResourceTable.NO_MATCH) -1 else it.toInt() }

    @SuppressLint("AppBundleLocaleChanges")
    protected fun Context.configurationFor(locale: Locale): Configuration =
        Configuration(resources.configuration).apply { setLocales(LocaleList(locale)) }

    companion object {
        fun create(context: Context, locales: List<Locale?>): LocaleResourceProbe {
            val table = LocaleResourceTable.build(context.packageName, locales)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                LoaderProbe(context, table)
            } else {
                AssetManagerProbe(context, table)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private class LoaderProbe(private val context: Context, table: ByteArray) : LocaleResourceProbe() {

    private val file = tempFile(context, ".arsc").apply { writeBytes(table) }
    private val provider = try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            ResourcesProvider.loadFromTable(descriptor, null)
        }
    } catch (e: Throwable) {
        file.delete()
        throw e
    }
    private val loader = ResourcesLoader().apply { addProvider(provider) }

    override fun ask(locale: Locale): Int {
        val resources = context.createConfigurationContext(context.configurationFor(locale)).resources
        resources.addLoaders(loader)
        try {
            return resources.answer()
        } finally {
            resources.removeLoaders(loader)
        }
    }

    override fun close() {
        loader.clearProviders()
        provider.close()
        file.delete()
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private class AssetManagerProbe(private val context: Context, table: ByteArray) : LocaleResourceProbe() {

    private val file = tempFile(context, ".zip")
    private val assets: AssetManager

    init {
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putStored("resources.arsc", table)
                zip.putStored("AndroidManifest.xml", ByteArray(4))
            }
            assets = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val cookie = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
                .invoke(assets, file.path) as Int
            if (cookie == 0) {
                assets.close()
                error("addAssetPath failed")
            }
        } catch (e: Throwable) {
            file.delete()
            throw e
        }
    }

    override fun ask(locale: Locale): Int {
        @Suppress("DEPRECATION")
        val resources = Resources(assets, context.resources.displayMetrics, context.configurationFor(locale))
        return resources.answer()
    }

    override fun close() {
        assets.close()
        file.delete()
    }

    private fun ZipOutputStream.putStored(name: String, bytes: ByteArray) {
        val crc = CRC32().apply { update(bytes) }
        putNextEntry(
            ZipEntry(name).apply {
                method = ZipEntry.STORED
                size = bytes.size.toLong()
                compressedSize = bytes.size.toLong()
                this.crc = crc.value
            },
        )
        write(bytes)
        closeEntry()
    }
}

private fun tempFile(context: Context, suffix: String): File =
    File.createTempFile("adapty_flow_locales", suffix, context.cacheDir)
