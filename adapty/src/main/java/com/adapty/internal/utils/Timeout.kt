package com.adapty.internal.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

internal fun <T> timeout(flow: Flow<T>, timeout: Int, scope: CoroutineScope = adaptyScope): Flow<T> =
    flow {
        val upstream = scope.async {
            flow.take(1).toList().ifEmpty { awaitCancellation() }
        }
        val value = try {
            withTimeout(timeout.toLong()) { upstream.await() }.single()
        } catch (e: TimeoutCancellationException) {
            throw TimeoutException()
        } finally {
            upstream.cancel()
        }
        emit(value)
    }

internal suspend fun <T> withTimeoutAbandoning(
    timeout: Int,
    scope: CoroutineScope = adaptyScope,
    call: suspend (onResponseReceived: () -> Unit) -> T,
): T {
    if (timeout == INF_PLACEMENT_TIMEOUT_MILLIS) return call {}

    val responseReceived = CompletableDeferred<Unit>()
    val upstream = scope.async { call { responseReceived.complete(Unit) } }
    try {
        withTimeoutOrNull(timeout.toLong()) {
            select<Unit> {
                upstream.onAwait {}
                responseReceived.onAwait {}
            }
        } ?: throw TimeoutException()
        return upstream.await()
    } finally {
        upstream.cancel()
    }
}

internal fun remainingPlacementTimeout(loadTimeout: Int, elapsedMillis: Long): Int =
    if (loadTimeout == INF_PLACEMENT_TIMEOUT_MILLIS) {
        INF_PLACEMENT_TIMEOUT_MILLIS
    } else {
        (loadTimeout - elapsedMillis).coerceAtLeast(PLACEMENT_TIMEOUT_MILLIS_SHIFT.toLong()).toInt()
    }

internal class PlacementTimeoutBudget(private val loadTimeout: Int) {
    private val startedAtNanos = System.nanoTime()

    fun remaining(): Int =
        remainingPlacementTimeout(loadTimeout, (System.nanoTime() - startedAtNanos) / 1_000_000)
}

internal class TimeoutException: Exception()
