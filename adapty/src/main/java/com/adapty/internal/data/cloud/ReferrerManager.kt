@file:OptIn(ExperimentalCoroutinesApi::class)

package com.adapty.internal.data.cloud

import android.content.Context
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.releaseQuietly
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore

internal class ReferrerManager(
    context: Context,
) {

    private val referrerClient = InstallReferrerClient.newBuilder(context).build()

    fun getData(): Flow<ReferrerDetails?> =
        flow {
            emit(referrerClient.getDataSync())
        }
            .take(1)

    private val startConnectionSemaphore = Semaphore(1)

    private suspend fun InstallReferrerClient.getDataSync(): ReferrerDetails? {
        startConnectionSemaphore.acquire()
        return suspendCancellableCoroutine { continuation ->
            startConnection(
                object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        if (continuation.isActive) {
                            runCatching {
                                when (responseCode) {
                                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                                        continuation.resume(referrerClient.installReferrer) {}
                                    }
                                    else -> {
                                        Logger.log(WARN) { "Referrer setup finished with error code: $responseCode" }
                                        continuation.resume(null) {}
                                    }
                                }
                            }
                        }
                        startConnectionSemaphore.releaseQuietly()
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        if (continuation.isActive) {
                            runCatching {
                                continuation.resume(null) {}
                            }
                        }
                        startConnectionSemaphore.releaseQuietly()
                    }
                }
            )
        }
    }
}