/*
 * MIT License
 * Copyright (c) 2021.  TechnoWolf FOSS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package `in`.technowolf.nyx.utils

import androidx.annotation.MainThread
import androidx.annotation.Nullable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SingleEvent MutableLiveData which will be observed only once.
 * It can be used for one shot events like showing snack bar, dismissing dialog etc.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    /**
     * Atomic Boolean because we can not use compareAndSet() on volatile
     * booleans. Also we do not need synchronous block to access atomic booleans.
     * One more reason to use atomic boolean is that no other thread but the caller
     * thread can access the atomic boolean. Just to make it more thread safe.
     */
    private val isPending: AtomicBoolean = AtomicBoolean(false)

    /**
     * Do this activity on main thread only because livedata can not be observed on
     * any thread other than Main Thread.
     */
    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T?>) {
        if (hasActiveObservers()) {
            Logger.w("Multiple observers registered but only one will be notified of changes.")
        }

        /**
         * Observe the internal MutableLiveData and handle changes
         */
        super.observe(owner, { t ->
            if (isPending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    /**
     * Do this activity only on main thread since we can not access the livedata from
     * other threads.
     */
    @MainThread
    override fun setValue(@Nullable t: T?) {
        isPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Unit, to make calls cleaner and concise.
     */
    @MainThread
    fun call() {
        value = null
    }
}