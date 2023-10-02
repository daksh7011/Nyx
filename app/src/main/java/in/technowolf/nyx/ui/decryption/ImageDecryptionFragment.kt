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

package `in`.technowolf.nyx.ui.decryption

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import `in`.technowolf.nyx.R
import `in`.technowolf.nyx.databinding.FragmentImageDescriptionBinding
import `in`.technowolf.nyx.ui.dashboard.DashboardViewModel
import `in`.technowolf.nyx.utils.Extension.action
import `in`.technowolf.nyx.utils.Extension.gone
import `in`.technowolf.nyx.utils.Extension.retrieveImage
import `in`.technowolf.nyx.utils.Extension.snackBar
import `in`.technowolf.nyx.utils.Extension.visible
import `in`.technowolf.nyx.utils.viewBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ImageDecryptionFragment : Fragment(R.layout.fragment_image_description) {

    private val binding by viewBinding(FragmentImageDescriptionBinding::bind)
    private val dashboardViewModel: DashboardViewModel by sharedViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        setupImageView()
        setupEditText()
        setupFab()
        setupObservers()
    }

    private fun setupObservers() {
        dashboardViewModel.decryptedText.observe(viewLifecycleOwner) { decryptedMessage ->
            if (decryptedMessage.isNullOrEmpty()) {
                binding.root.snackBar(
                    "Wrong passphrase! Try again",
                    anchor = binding.fabDecryptMessage
                ) {}
                binding.tvDecryptedMessage.gone()
            } else {
                binding.root.snackBar("Message Decrypted", anchor = binding.fabDecryptMessage) {
                    action("Copy to Clipboard") {
                        val clipboard: ClipboardManager? =
                            ContextCompat.getSystemService(
                                requireContext(),
                                ClipboardManager::class.java
                            )
                        val clipData = ClipData.newPlainText("Decrypted Message", decryptedMessage)
                        clipboard?.setPrimaryClip(clipData)
                    }
                }
                binding.apply {
                    tvDecryptedMessage.visible()
                    tvDecryptedMessage.text = decryptedMessage
                }
            }

        }
    }

    private fun setupEditText() {
        binding.apply {
            etPassphrase.doAfterTextChanged { tilPassphrase.error = null }
        }
    }

    private fun setupFab() {
        binding.apply {
            fabDecryptMessage.setOnClickListener {
                if (isFormValidated(etPassphrase.text.toString())) {
                    dashboardViewModel.decryptImage(
                        getBitmapFromLocal(),
                        etPassphrase.text.toString()
                    )
                } else {
                    validateForm()
                }
            }
        }
    }

    @Suppress("detekt.MagicNumber")
    private fun setupImageView() {
        binding.apply {
            ivEncryptedImagePreview.shapeAppearanceModel =
                ivEncryptedImagePreview.shapeAppearanceModel
                    .toBuilder()
                    .setBottomLeftCornerSize(16f)
                    .setBottomRightCornerSize(16f)
                    .build()
        }
        val bitmap = getBitmapFromLocal()
        val bitmapDrawable = BitmapDrawable(resources, bitmap)
        binding.ivEncryptedImagePreview.setImageDrawable(bitmapDrawable)
    }

    private fun getBitmapFromLocal() =
        requireContext().retrieveImage(dashboardViewModel.selectedImageModel?.name.orEmpty())

    private fun isFormValidated(passphrase: String) = passphrase.isNotEmpty()

    private fun validateForm() {
        binding.tilPassphrase.error = "Passphrase is required."
    }

}
