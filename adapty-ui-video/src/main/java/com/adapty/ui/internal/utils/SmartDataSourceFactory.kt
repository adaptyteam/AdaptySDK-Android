@file:androidx.annotation.OptIn(UnstableApi::class)

package com.adapty.ui.internal.utils

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.AssetDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource

internal class SmartDataSourceFactory(
    private val context: Context,
    cache: Cache,
) : DataSource.Factory {

    private val httpFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)

    private val cacheFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(httpFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    override fun createDataSource(): DataSource {
        return object : DataSource {

            private var actual: DataSource? = null
            private val transferListeners = mutableListOf<TransferListener>()

            override fun open(dataSpec: DataSpec): Long {
                val uri = dataSpec.uri
                actual = when (uri.scheme) {
                    "asset" -> AssetDataSource(context)
                    "android.resource" -> RawResourceDataSource(context)
                    "file" -> FileDataSource()
                    "http", "https" -> cacheFactory.createDataSource()
                    else -> cacheFactory.createDataSource()
                }

                transferListeners.forEach { listener ->
                    actual?.addTransferListener(listener)
                }

                return actual?.open(dataSpec) ?: -1
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                return actual?.read(buffer, offset, length) ?: -1
            }

            override fun getUri(): Uri? = actual?.uri

            override fun getResponseHeaders(): Map<String, List<String>> =
                actual?.responseHeaders ?: emptyMap()

            override fun close() {
                actual?.close()
            }

            override fun addTransferListener(transferListener: TransferListener) {
                transferListeners += transferListener
            }
        }
    }
}
