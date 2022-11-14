package com.adapty.internal.utils

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.adapty.internal.data.cloud.CloudRepository

internal class LifecycleManager(
    private val cloudRepository: CloudRepository,
) : LifecycleObserver {

    interface StateCallback {
        fun onGoForeground()
        fun onGoBackground()
    }

    @JvmField
    @JvmSynthetic
    var stateCallback: StateCallback? = null

    private var isFirstStart = true

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
            cloudRepository.allowActivate()
        }
    }

    @JvmSynthetic
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        if (isFirstStart) {
            cloudRepository.allowActivate()
            isFirstStart = false
        }
        stateCallback?.onGoForeground()
    }

    @JvmSynthetic
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        stateCallback?.onGoBackground()
    }
}