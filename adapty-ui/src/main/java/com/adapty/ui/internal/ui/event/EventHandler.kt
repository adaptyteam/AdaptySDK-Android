@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.event

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.element.Action

@InternalAdaptyApi
public data class EventHandler internal constructor(
    internal val triggers: List<Trigger>,
    internal val animations: List<Animation<*>>,
    internal val onAnimationsFinish: List<Action>?,
)

@InternalAdaptyApi
public data class Trigger internal constructor(
    internal val events: List<String>,
    internal val filter: TriggerFilter?,
    internal val transitions: List<String>?,
)

internal enum class TriggerFilter {
    FIRST,
    NOT_FIRST,
}

private val APPEAR_EVENTS = listOf("on_will_appear", "on_did_appear")

internal fun List<EventHandler>.appearAnimations(
    predictedFireCount: (eventId: String) -> Int = { 1 },
    currentTransitionId: String? = null,
): List<Animation<*>> =
    mapNotNull { handler ->
        val appearEvent = handler.triggers
            .mapNotNull { trigger ->
                trigger.events.firstOrNull { it in APPEAR_EVENTS }
                    ?.takeIf {
                        (trigger.transitions.isNullOrEmpty()
                                || (currentTransitionId != null && currentTransitionId in trigger.transitions))
                                && when (trigger.filter) {
                                    null -> true
                                    TriggerFilter.FIRST -> predictedFireCount(it) <= 1
                                    TriggerFilter.NOT_FIRST -> predictedFireCount(it) > 1
                                }
                    }
            }
            .minByOrNull { APPEAR_EVENTS.indexOf(it) }
            ?: return@mapNotNull null
        APPEAR_EVENTS.indexOf(appearEvent) to handler.animations
    }
        .flatMap { (priority, anims) -> anims.map { priority to it } }
        .sortedWith(compareBy({ it.first }, { it.second.startDelayMillis }))
        .map { it.second }
