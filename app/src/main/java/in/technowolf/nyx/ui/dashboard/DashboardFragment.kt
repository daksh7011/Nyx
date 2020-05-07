/*
 * MIT License
 *
 * Copyright (c)  2020.  TechnoWolf FOSS
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

package `in`.technowolf.nyx.ui.dashboard

import `in`.technowolf.nyx.R
import `in`.technowolf.nyx.databinding.FragmentDashboardBinding
import `in`.technowolf.nyx.utils.Extension.setStatusBarColor
import `in`.technowolf.nyx.utils.binding
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val binding by binding(FragmentDashboardBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    private fun init() {
        setStatusBarColor()
        setupEncrypt()
        setupDecrypt()
        setupInfo()
    }

    private fun setupEncrypt() {
        binding.cvEncrypt.setOnClickListener {

        }
    }

    private fun setupDecrypt() {
        binding.cvDecrypt.setOnClickListener {

        }
    }

    private fun setupInfo() {
        binding.cvInfo.setOnClickListener {

        }
    }

}
