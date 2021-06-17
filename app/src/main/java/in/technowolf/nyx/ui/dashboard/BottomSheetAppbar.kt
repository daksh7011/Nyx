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

package `in`.technowolf.nyx.ui.dashboard

import `in`.technowolf.nyx.databinding.BottomsheetAppbarBinding
import `in`.technowolf.nyx.utils.Extension.isDarkMode
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetAppbar : BottomSheetDialogFragment() {

    private var _binding: BottomsheetAppbarBinding? = null
    private val binding get() = _binding!!

    var onSelectDecryptionImage: (() -> Unit)? = null
    var onFaq: (() -> Unit)? = null
    var onDonate: (() -> Unit)? = null
    var onAbout: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAppbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.selectDecryptionImage.setOnClickListener { onSelectDecryptionImage?.invoke() }
        binding.tvFaq.setOnClickListener { onFaq?.invoke() }
        binding.tvDonation.setOnClickListener { onDonate?.invoke() }
        binding.switchDarkMode.isChecked = requireContext().isDarkMode()
        binding.switchDarkMode.setOnCheckedChangeListener { buttonView, isChecked ->
            val mode =
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
        binding.tvInfo.setOnClickListener { onAbout?.invoke() }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val BOTTOM_SHEET_TAG = "BOTTOM_SHEET_TAG"
    }

}