@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

internal class TimerCallbackScheduler(
    private val scope: CoroutineScope,
) : EffectHandler {
    private val activeJobs = ConcurrentHashMap<String, Job>()

    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        if (effect !is Effect.ScheduleTimerCallback) return
        activeJobs.remove(effect.timerId)?.cancel()
        activeJobs[effect.timerId] = scope.launch {
            delay(effect.delayMs)
            activeJobs.remove(effect.timerId)
            dispatch(Message.TimerCallbackExpired(effect.timerId))
        }
    }
}
