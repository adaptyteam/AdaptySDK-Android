@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.store

import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.utils.InternalAdaptyApi

internal class TimerEffectHandler(
    private val cacheRepository: CacheRepository,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        if (effect !is Effect.ResolveTimerCommand) return
        val now = System.currentTimeMillis() / 1000L
        val key = "${effect.placementId}_timer_${effect.timerId}_end"
        val endAtSeconds: Long = when (effect.behavior) {
            "restart" -> now + effect.durationSeconds
            "continue" -> resolveReusable(key, isPersisted = false, now, effect.durationSeconds)
            "persisted" -> resolveReusable(key, isPersisted = true, now, effect.durationSeconds)
            "custom" -> effect.timerResolver.timerEndAtDate(effect.timerId).time / 1000L
            else -> now + effect.durationSeconds
        }
        dispatch(Message.TimerCommandResolved(effect.timerId, endAtSeconds))
    }

    private fun resolveReusable(
        key: String,
        isPersisted: Boolean,
        now: Long,
        durationSeconds: Long,
    ): Long {
        val cached = cacheRepository.getLongValue(key, isPersisted)
        if (cached != null && cached > now) return cached
        val newEnd = now + durationSeconds
        cacheRepository.setLongValue(key, newEnd, isPersisted)
        return newEnd
    }
}
