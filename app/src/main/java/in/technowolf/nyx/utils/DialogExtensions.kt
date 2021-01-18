/*
 * MIT License
 *
 * Copyright (c) 2021 TechnoWolf FOSS
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

import `in`.technowolf.nyx.databinding.DialogInfoBinding
import `in`.technowolf.nyx.utils.Extension.setVisibility
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

inline fun AppCompatActivity.alert(
    title: CharSequence? = null,
    message: CharSequence? = null,
    shouldPassphrase: Boolean = false,
    func: AlertDialogHelper.() -> Unit
): AlertDialog {
    return AlertDialogHelper(this, title, message, shouldPassphrase).apply {
        func()
    }.create()
}

inline fun AppCompatActivity.alert(
    titleResource: Int = 0,
    messageResource: Int = 0,
    shouldPassphrase: Boolean = false,
    func: AlertDialogHelper.() -> Unit
): AlertDialog {
    val title = if (titleResource == 0) null else getString(titleResource)
    val message = if (messageResource == 0) null else getString(messageResource)
    return AlertDialogHelper(this, title, message, shouldPassphrase).apply {
        func()
    }.create()
}

inline fun Fragment.alert(
    title: CharSequence? = null,
    message: CharSequence? = null,
    shouldPassphrase: Boolean = false,
    func: AlertDialogHelper.() -> Unit
): AlertDialog {
    return AlertDialogHelper(requireContext(), title, message, shouldPassphrase).apply {
        func()
    }.create()
}

inline fun Fragment.alert(
    titleResource: Int = 0,
    messageResource: Int = 0,
    shouldPassphrase: Boolean = false,
    func: AlertDialogHelper.() -> Unit
): AlertDialog {
    val title = if (titleResource == 0) null else getString(titleResource)
    val message = if (messageResource == 0) null else getString(messageResource)
    return AlertDialogHelper(requireContext(), title, message, shouldPassphrase).apply {
        func()
    }.create()
}

@SuppressLint("InflateParams")
class AlertDialogHelper(
    context: Context,
    title: CharSequence?,
    message: CharSequence?,
    shouldPassphrase: Boolean = false
) {
    private var binding: DialogInfoBinding = DialogInfoBinding.inflate(LayoutInflater.from(context))


    private val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        .setView(binding.root)

    private var dialog: AlertDialog? = null

    var cancelable: Boolean = true

    val etPassphrase = binding.etPassphrase

    init {
        binding.tvTitle.text = title
        binding.tvMessage.text = message
        binding.tilPassphrase.setVisibility(shouldPassphrase)
    }

    fun positiveButton(@StringRes textResource: Int, func: (() -> Unit)? = null) {
        with(binding.btnOk) {
            text = builder.context.getString(textResource)
            setClickListenerToDialogButton(func)
        }
    }

    fun positiveButton(text: CharSequence, func: (() -> Unit)? = null) {
        with(binding.btnOk) {
            this.text = text
            setClickListenerToDialogButton(func)
        }
    }

    fun negativeButton(@StringRes textResource: Int, func: (() -> Unit)? = null) {
        with(binding.btnCancel) {
            text = builder.context.getString(textResource)
            setClickListenerToDialogButton(func)
        }
    }

    fun negativeButton(text: CharSequence, func: (() -> Unit)? = null) {
        with(binding.btnCancel) {
            this.text = text
            setClickListenerToDialogButton(func)
        }
    }

    fun onCancel(func: () -> Unit) {
        builder.setOnCancelListener { func() }
    }

    fun create(): AlertDialog {
        binding.tvTitle.goneIfTextEmpty()
        binding.tvMessage.goneIfTextEmpty()
        binding.btnOk.goneIfTextEmpty()
        binding.btnCancel.goneIfTextEmpty()

        dialog = builder
            .setCancelable(cancelable)
            .create()
        return dialog!!
    }

    private fun TextView.goneIfTextEmpty() {
        this.setVisibility(!text.isNullOrEmpty())
    }

    private fun Button.setClickListenerToDialogButton(func: (() -> Unit)?) {
        setOnClickListener {
            func?.invoke()
            dialog?.dismiss()
        }
    }

}