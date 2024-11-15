package com.adapty.internal.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.adapty.internal.data.cache.CacheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take

internal class LifecycleManager(private val app: Application, cacheRepository: CacheRepository) : DefaultLifecycleObserver {

    interface StateCallback {
        fun onGoForeground()
        fun onGoBackground()
    }

    @JvmField
    @JvmSynthetic
    var stateCallback: StateCallback? = null

    private var isFirstStart = true

    private val isActivateAllowed = MutableStateFlow(!cacheRepository.hasLocalProfile())

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
        app.registerActivityLifecycleCallbacks(object: ActivityCallbacks() {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                allowActivateOnce()
            }
        })

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            allowActivateOnce()
        }
    }

    @JvmSynthetic
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        stateCallback?.onGoForeground()
    }

    @JvmSynthetic
    override fun onStop(owner: LifecycleOwner) {
        stateCallback?.onGoBackground()
        super.onStop(owner)
    }

    @JvmSynthetic
    fun onActivateAllowed() =
        isActivateAllowed
            .filter { it }
            .take(1)

    private fun allowActivateOnce() {
        if (isFirstStart) {
            allowActivate()
            isFirstStart = false
        }
    }

    private fun allowActivate() {
        isActivateAllowed.value = true
    }

    private open class ActivityCallbacks: Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }
}