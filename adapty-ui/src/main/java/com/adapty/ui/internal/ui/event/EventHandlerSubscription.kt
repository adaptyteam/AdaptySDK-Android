@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.attributes.AnimationTarget
import com.adapty.ui.internal.ui.attributes.target
import com.adapty.ui.internal.ui.element.BaseProps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class AnimationPlayback(
    val animations: List<Animation<*>>,
    val eventSequence: Long,
    val sequenceByTarget: Map<AnimationTarget, Long>,
)

internal fun AnimationPlayback?.mergedWith(matched: List<Animation<*>>, sequence: Long): AnimationPlayback {
    val matchedTargets = matched.mapTo(mutableSetOf()) { it.target }
    val kept = this?.animations?.filter { it.target !in matchedTargets }.orEmpty()
    val sequenceByTarget = buildMap {
        this@mergedWith?.sequenceByTarget?.forEach { (target, seq) ->
            if (target !in matchedTargets) put(target, seq)
        }
        matchedTargets.forEach { target -> put(target, sequence) }
    }
    return AnimationPlayback(kept + matched, sequence, sequenceByTarget)
}

@Composable
internal fun rememberActiveAnimations(
    baseProps: BaseProps,
    dispatch: (Message) -> Unit,
): State<AnimationPlayback?> {
    val handlers = baseProps.eventHandlers
    val active = remember { mutableStateOf<AnimationPlayback?>(null) }
    if (handlers.isNullOrEmpty()) return active

    val dispatcher = LocalEventDispatcher.current
    val entry = LocalScreenInstance.current
    val ownInstanceId = entry.screenInstanceId
    val ownEpoch = entry.epoch

    val processedSequences = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver<MutableSet<Long>, Long>(
            save = { it.toList() },
            restore = { it.toMutableSet() },
        ),
    ) { mutableSetOf<Long>() }

    LaunchedEffect(handlers, dispatcher, ownInstanceId, ownEpoch) {
        fun processEvent(event: DispatchedEvent) {
            if (!event.matchesScope(ownInstanceId, ownEpoch)) return
            val isReplay = event.sequence in processedSequences
            if (isReplay && active.value?.eventSequence == event.sequence) return
            val matched = mutableListOf<Animation<*>>()
            handlers.forEach { handler ->
                if (!handler.matches(event, dispatcher)) return@forEach
                matched.addAll(handler.animations)
                val finishActions = handler.onAnimationsFinish
                val totalMs = if (!finishActions.isNullOrEmpty() && !isReplay) {
                    handler.totalAnimationDurationMillis()
                } else null
                if (totalMs != null) {
                    launch {
                        if (totalMs > 0L) delay(totalMs)
                        dispatch(Message.ActionsRequested(finishActions!!, entry))
                    }
                }
            }
            if (matched.isNotEmpty()) {
                active.value = active.value.mergedWith(matched, event.sequence)
                processedSequences.add(event.sequence)
            }
        }

        dispatcher.lifecycleHistoryFor(ownInstanceId, ownEpoch).forEach(::processEvent)

        dispatcher.events.collect(::processEvent)
    }
    return active
}

private fun EventHandler.matches(
    event: DispatchedEvent,
    dispatcher: EventDispatcher,
): Boolean = triggers.any { trigger -> trigger.matches(event, dispatcher) }

private fun Trigger.matches(
    event: DispatchedEvent,
    dispatcher: EventDispatcher,
): Boolean {
    if (!events.contains(event.eventKey)) return false

    val transitionFilter = transitions
    if (!transitionFilter.isNullOrEmpty()) {
        val incoming = event.transitionId ?: run {
            return false
        }
        if (incoming !in transitionFilter) {
            return false
        }
    }

    val f = filter
    if (f != null) {
        val count = dispatcher.fireCount(event.screenInstanceId, event.eventKey)
        when (f) {
            TriggerFilter.FIRST -> if (count > 1) return false
            TriggerFilter.NOT_FIRST -> if (count <= 1) return false
        }
    }

    return true
}

private fun DispatchedEvent.matchesScope(ownInstanceId: String, ownEpoch: Long): Boolean = when (this) {
    is DispatchedEvent.Lifecycle -> screenInstanceId == ownInstanceId && epoch == ownEpoch
    is DispatchedEvent.Custom ->
        screenInstanceId == null || (screenInstanceId == ownInstanceId && epoch == ownEpoch)
}

private fun EventHandler.totalAnimationDurationMillis(): Long? {
    if (animations.isEmpty()) return 0L
    val hasInfiniteLoop = animations.any { it.repeatMode != null && it.repeatMaxCount == Int.MAX_VALUE }
    if (hasInfiniteLoop) return null
    return animations.maxOf { anim ->
        val base = anim.startDelayMillis + anim.durationMillis.toLong()
        val loopExtra = when (anim.repeatMode) {
            null -> 0L
            Animation.RepeatMode.Normal ->
                (anim.durationMillis.toLong() + anim.repeatDelayMillis) * anim.repeatMaxCount
            Animation.RepeatMode.PingPong ->
                (anim.durationMillis.toLong() * 2 + anim.pingPongDelayMillis) * anim.repeatMaxCount
        }
        base + loopExtra
    }
}
