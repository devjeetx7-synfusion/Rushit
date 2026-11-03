package com.rushit.app.data.auth.google

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

/** Application-owned, weak foreground host binding; never retains a destroyed Activity. */
class ForegroundActivityTracker {
    private var reference = WeakReference<Activity>(null)

    val activity: Activity?
        get() = reference.get()?.takeIf {
            !it.isFinishing && !it.isDestroyed &&
                (it as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) == true
        }

    /** Idempotent for an Activity recreation; each Activity gets its own observer. */
    fun bind(activity: Activity) {
        val owner = activity as? LifecycleOwner ?: return
        // Immediately record if already started/resumed
        if (owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            reference = WeakReference(activity)
        }
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                reference = WeakReference(activity)
            }

            override fun onResume(owner: LifecycleOwner) {
                reference = WeakReference(activity)
            }

            override fun onStop(owner: LifecycleOwner) {
                // If stopped and this is still the tracked activity, clear it
                clear(activity)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                clear(activity)
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    private fun clear(activity: Activity) {
        if (reference.get() === activity) reference = WeakReference(null)
    }
}
