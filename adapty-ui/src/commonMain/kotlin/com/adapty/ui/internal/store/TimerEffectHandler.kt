@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.currentTimeMillis
import com.adapty.ui.internal.ui.TimerValueStore
import com.adapty.ui.internal.platform.time

internal class TimerEffectHandler(
    private val timerStore: TimerValueStore,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        if (effect !is Effect.ResolveTimerCommand) return
        val now = currentTimeMillis() / 1000L
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
        val cached = timerStore.getLong(key, isPersisted)
        if (cached != null && cached > now) return cached
        val newEnd = now + durationSeconds
        timerStore.setLong(key, newEnd, isPersisted)
        return newEnd
    }
}
