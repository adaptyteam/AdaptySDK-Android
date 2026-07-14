@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toAnimation
import com.adapty.ui.internal.ui.event.EventHandler
import com.adapty.ui.internal.ui.event.Trigger
import com.adapty.ui.internal.ui.event.TriggerFilter

internal fun Map<*, *>.extractEventHandlers(): List<EventHandler>? {
    val handlers = this["event_handlers"] as? List<*> ?: return null
    return handlers
        .mapNotNull { it as? Map<*, *> }
        .mapNotNull { handlerMap ->
            val triggers = (handlerMap["triggers"] as? List<*>)
                ?.mapNotNull { (it as? Map<*, *>)?.toTrigger() }
                ?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null

            val animations = (handlerMap["animations"] as? List<*>)
                ?.mapNotNull { (it as? Map<*, *>)?.toAnimation() }
                .orEmpty()

            val onAnimationsFinish = handlerMap["on_animations_finish"].toActionListOrNull()

            if (animations.isEmpty() && onAnimationsFinish.isNullOrEmpty()) return@mapNotNull null

            EventHandler(triggers, animations, onAnimationsFinish)
        }
        .takeIf { it.isNotEmpty() }
}

private fun Map<*, *>.toTrigger(): Trigger? {
    val events = (this["events"] as? List<*>)
        ?.mapNotNull { it as? String }
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    val filter = (this["filter"] as? String)?.let {
        when (it) {
            "first" -> TriggerFilter.FIRST
            "not_first" -> TriggerFilter.NOT_FIRST
            else -> null
        }
    }

    val transitions = (this["transitions"] as? List<*>)
        ?.mapNotNull { it as? String }
        ?.takeIf { it.isNotEmpty() }

    return Trigger(events, filter, transitions)
}

private fun Any?.toActionListOrNull(): List<com.adapty.ui.internal.ui.element.Action>? = when (this) {
    is Map<*, *> -> listOf(toAction())
    is Iterable<*> -> mapNotNull { (it as? Map<*, *>)?.toAction() }.takeIf { it.isNotEmpty() }
    else -> null
}
