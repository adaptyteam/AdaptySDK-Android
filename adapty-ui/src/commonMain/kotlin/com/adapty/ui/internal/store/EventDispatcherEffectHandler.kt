@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.store

import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.ui.event.EventDispatcher

internal class EventDispatcherEffectHandler(
    private val dispatcher: EventDispatcher,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.PublishCustomEvents -> {
                effect.eventIds.forEach { id ->
                    dispatcher.publishCustom(id, effect.instanceId, effect.epoch)
                }
            }
            else -> Unit
        }
    }
}
