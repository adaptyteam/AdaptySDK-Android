package com.adapty.internal.utils

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take

internal class LifecycleManager : LifecycleObserver {

    interface StateCallback {
        fun onGoForeground()
        fun onGoBackground()
    }

    @JvmField
    @JvmSynthetic
    var stateCallback: StateCallback? = null

    private var isFirstStart = true

    private val isActivateAllowed = MutableStateFlow(false)

    @JvmSynthetic
    fun init() {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            initInternal()
        } else {
            Handler(Looper.getMainLooper()).post {
                initInternal()
            }
        }
    }

    private fun initInternal() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            allowActivate()
        }
    }

    @JvmSynthetic
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (isFirstStart) {
            allowActivate()
            isFirstStart = false
        }
        stateCallback?.onGoForeground()
    }

    @JvmSynthetic
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        stateCallback?.onGoBackground()
    }

    @JvmSynthetic
    fun onActivateAllowed() =
        isActivateAllowed
            .filter { it }
            .take(1)

    private fun allowActivate() {
        isActivateAllowed.value = true
    }
}