package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cloud.CloudRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class IPv4Retriever(
    val disabled: Boolean,
    private val cloudRepository: CloudRepository,
    private val connectivityHelper: ConnectivityHelper,
) {

    @Volatile
    var value: String? = null
        private set(value) {
            field = value
            if (value != null)
                onValueReceived?.invoke(value)
        }

    var onValueReceived: ((String) -> Unit)? = null

    init {
        if (!disabled)
            execute {
                getIPv4().retryWhen { _, _ ->
                    delay(1000L)
                    connectivityHelper.waitForInternetConnectivity()
                    true
                }.catch { }.collect()
            }
    }

    private fun getIPv4(): Flow<String?> =
        flow {
            val ip = cloudRepository.getIPv4Request().data.value
            value = ip
            emit(ip)
        }
}