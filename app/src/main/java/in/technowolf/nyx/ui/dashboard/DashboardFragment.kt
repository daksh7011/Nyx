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

import `in`.technowolf.nyx.R
import `in`.technowolf.nyx.databinding.FragmentDashboardBinding
import `in`.technowolf.nyx.utils.Extension.isDarkMode
import `in`.technowolf.nyx.utils.Extension.setStatusBarColor
import `in`.technowolf.nyx.utils.Extension.snackBar
import `in`.technowolf.nyx.utils.Logger
import `in`.technowolf.nyx.utils.viewBinding
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val binding by viewBinding(FragmentDashboardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    private fun init() {
        if (requireContext().isDarkMode()) {
            setStatusBarColor(R.color.black_800)
        } else {
            setStatusBarColor(R.color.colorPrimaryLight)
        }
        setupEncrypt()
        setupDecrypt()
        setupInfo()
        setupSettings()
    }

    private fun setupSettings() {
        binding.ivSettings.setOnClickListener {
            binding.root.snackBar("Settings screen will be added in future releases.") {}
        }
    }

    private fun setupEncrypt() {
        binding.cvEncrypt.setOnClickListener {
            Logger.i("Navigating from Dashboard to Encryption fragment.")
            val encryptionCardTransitionName = "BLAH"
            val extras = FragmentNavigatorExtras(it to encryptionCardTransitionName)
            val directions = DashboardFragmentDirections.actionToEncryptionFragment()
            findNavController().navigate(directions,extras)
        }
    }

    private fun setupDecrypt() {
        binding.cvDecrypt.setOnClickListener {
            Logger.i("Navigating from Dashboard to Decryption fragment.")
            findNavController().navigate(R.id.actionToDecryptionFragment)
        }
    }

    private fun setupInfo() {
        binding.cvInfo.setOnClickListener {
            Logger.i("Navigating from Dashboard to FAQ fragment.")
            binding.root.snackBar("FAQ section will be implemented in future releases.") {}
        }
    }

}
