/*
 * MIT License
 *
 * Copyright (c) 2020. TechnoWolf FOSS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
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

import `in`.technowolf.nyx.R
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

object Extension {

    infix fun Byte.shl(that: Int): Int = this.toInt().shl(that)
    infix fun Byte.shr(that: Int): Int = this.toInt().shr(that)


    infix fun Byte.and(that: Int): Int = this.toInt().and(that)
    infix fun Int.and(that: Byte): Int = this.and(that.toInt())

    fun View.gone() {
        this.visibility = View.GONE
    }

    fun View.visible() {
        this.visibility = View.VISIBLE
    }

    fun View.setVisibility(isVisible: Boolean) = if (isVisible) this.visible() else this.gone()

    fun View.enable() {
        this.isEnabled = true
    }

    fun View.disable() {
        this.isEnabled = false
    }

    fun Fragment.setStatusBarColor() {
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight)
    }

    inline fun View.snackBar(
        message: String,
        anchor: View? = null,
        length: Int = Snackbar.LENGTH_LONG,
        f: Snackbar.() -> Unit
    ) {
        val snackBar = Snackbar.make(this, message, length)
        snackBar.f()
        anchor?.let { snackBar.anchorView = it }
        snackBar.show()
    }

    fun Snackbar.action(action: String, color: Int? = null, listener: (View) -> Unit) {
        setAction(action, listener)
        color?.let { setActionTextColor(color) }
    }

    inline fun View.snackBar(
        @StringRes messageRes: Int,
        length: Int = Snackbar.LENGTH_LONG,
        anchor: View? = null,
        f: Snackbar.() -> Unit
    ) {
        snackBar(resources.getString(messageRes), anchor, length, f)
    }
}