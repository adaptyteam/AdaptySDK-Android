@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import kotlinx.coroutines.suspendCancellableCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ConnectivityHelper(
    private val connectivityManager: ConnectivityManager,
) {

    fun hasInternetConnectivity(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities?.isInternetAvailable() == true
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            activeNetworkInfo?.isConnected == true
        }
    }

    suspend fun waitForInternetConnectivity() =
        suspendCancellableCoroutine { continuation ->
            if (hasInternetConnectivity()) {
                continuation.resume(Unit) {}
                return@suspendCancellableCoroutine
            }

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    connectivityManager.unregisterNetworkCallbackQuietly(this)
                    if (continuation.isActive) {
                        continuation.resume(Unit) {}
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val internetAvailable = networkCapabilities.isInternetAvailable()

                        if (internetAvailable) {
                            connectivityManager.unregisterNetworkCallbackQuietly(this)
                            if (continuation.isActive) {
                                continuation.resume(Unit) {}
                            }
                        }
                    } else {
                        val networkInfo = connectivityManager.getNetworkInfo(network)
                        if (networkInfo?.isConnected == true) {
                            connectivityManager.unregisterNetworkCallbackQuietly(this)
                            if (continuation.isActive) {
                                continuation.resume(Unit) {}
                            }
                        }
                    }
                }
            }

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    else
                        this
                }
                .build()

            runCatching {
                connectivityManager.registerNetworkCallback(networkRequest, callback)
            }.onFailure {
                continuation.resume(Unit) {}
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                connectivityManager.unregisterNetworkCallbackQuietly(callback)
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun NetworkCapabilities.isInternetAvailable() =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    private fun ConnectivityManager.unregisterNetworkCallbackQuietly(callback: ConnectivityManager.NetworkCallback) {
        runCatching { unregisterNetworkCallback(callback) }
    }
}