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

import timber.log.Timber

object NLog {

    private val isDebug: Boolean = BuildConfig.DEBUG
    init {
        if(isDebug) Timber.plant(Timber.DebugTree())
    }

    fun v(tag: String, message: String) {
        if (isDebug) {
            Timber.v(tag, message)
        }
    }

    fun d(tag: String, message: String) {
        if (isDebug) {
            Timber.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (isDebug) {
            Timber.i(tag, message)
        }
    }

    fun w(tag: String, message: String) {
        if (isDebug) {
            Timber.w(tag, message)
        }
    }

    fun e(tag: String, message: String) {
        if (isDebug) {
            Timber.e(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        if (isDebug) {
            Timber.e(tag, message, throwable)
        }
    }

    fun wtf(tag: String, message: String) {
        if (isDebug) {
            Timber.wtf(tag, message)
        }
    }
}
