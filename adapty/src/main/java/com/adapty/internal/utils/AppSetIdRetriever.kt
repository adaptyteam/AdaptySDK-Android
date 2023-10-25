package com.adapty.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import com.google.android.gms.appset.AppSet
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AppSetIdRetriever(
    private val appContext: Context,
) {

    private val semaphore = Semaphore(1)

    @Volatile
    private var cachedAppSetId: String? = null

    init {
        execute { getAppSetIdIfAvailable().catch { }.collect() }
    }

    fun getAppSetIdIfAvailable(): Flow<String> =
        flow {
            cachedAppSetId?.let { appSetId ->
                emit(appSetId)
                return@flow
            }

            semaphore.acquire()
            cachedAppSetId?.let { appSetId ->
                semaphore.release()
                emit(appSetId)
                return@flow
            }

            val appSetId = try {
                Tasks.await(AppSet.getClient(appContext).appSetIdInfo).id
            } catch (e: Exception) {
                null
            }

            cachedAppSetId = appSetId
            semaphore.release()
            emit(appSetId.orEmpty())
        }.flowOnIO()
}