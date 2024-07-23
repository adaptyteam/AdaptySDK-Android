package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cloud.StoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class StoreCountryRetriever(
    private val storeManager: StoreManager,
) {

    private val semaphore = Semaphore(1)

    @Volatile
    private var cachedStoreCountry: String? = null

    init {
        execute { getStoreCountryIfAvailable(true).collect() }
    }

    fun getStoreCountryIfAvailable(forceUpdate: Boolean): Flow<String> =
        flow {
            if (forceUpdate) {
                semaphore.acquire()
                emitAll(
                    storeManager.getStoreCountry()
                        .map { it.orEmpty() }
                        .catch { emit("") }
                        .onEach { country -> cachedStoreCountry = country; semaphore.release() }
                )
            } else {
                cachedStoreCountry?.let { country ->
                    emit(country)
                    return@flow
                }

                semaphore.acquire()
                cachedStoreCountry?.let { country ->
                    semaphore.release()
                    emit(country)
                    return@flow
                }

                emitAll(
                    storeManager.getStoreCountry()
                        .map { it.orEmpty() }
                        .catch { emit("") }
                        .onEach { country -> cachedStoreCountry = country; semaphore.release() }
                )
            }
        }
}