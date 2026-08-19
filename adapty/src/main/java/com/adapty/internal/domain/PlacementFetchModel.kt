package com.adapty.internal.domain

import java.util.concurrent.locks.ReentrantReadWriteLock

internal enum class VariationType {
    Onboarding, Flow
}

internal sealed class PlacementSource {
    class Regular(val placementRequestId: String): PlacementSource()
    object Cache: PlacementSource()
    sealed class Fallback: PlacementSource() {
        object Remote: Fallback()
        object Local: Fallback()
    }
    object Untargeted: PlacementSource()
}

internal sealed class CheckPoint {
    object Unspecified: CheckPoint()
    class VariationAssigned(val variationId: String): CheckPoint()
    object TimeOut: CheckPoint()
}

internal class FetchCheckpointHolder {
    private val checkpoints = HashMap<String, CheckPoint>()

    private val lock = ReentrantReadWriteLock()

    fun getAndUpdate(requestId: String, checkPoint: CheckPoint): CheckPoint {
        return try {
            lock.writeLock().lock()
            val prevCheckpoint = checkpoints[requestId] ?: CheckPoint.Unspecified
            when (checkPoint) {
                is CheckPoint.Unspecified -> checkpoints.remove(requestId)
                is CheckPoint.TimeOut -> checkpoints[requestId] = checkPoint
                is CheckPoint.VariationAssigned -> checkpoints[requestId] = checkPoint
            }
            prevCheckpoint
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun get(requestId: String): CheckPoint {
        return try {
            lock.readLock().lock()
            checkpoints[requestId] ?: CheckPoint.Unspecified
        } finally {
            lock.readLock().unlock()
        }
    }
}
