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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import coil.load
import `in`.technowolf.nyx.R
import `in`.technowolf.nyx.databinding.DialogFragmentEncryptBinding
import `in`.technowolf.nyx.ui.models.ImageModel
import `in`.technowolf.nyx.utils.Extension.getImageName
import `in`.technowolf.nyx.utils.Extension.getTimeStampForImage
import `in`.technowolf.nyx.utils.Extension.saveImage
import `in`.technowolf.nyx.utils.Extension.snackBar
import `in`.technowolf.nyx.utils.viewBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class EncryptDialogFragment : DialogFragment() {

    private val binding by viewBinding(DialogFragmentEncryptBinding::bind)

    private val dashboardViewModel: DashboardViewModel by sharedViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Nyx_FullScreenDialog)
    }

    override fun onStart() {
        super.onStart()
        requireDialog().apply {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
            window?.setWindowAnimations(R.style.Nyx_DialogFragment_Slide)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_fragment_encrypt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        setupImageView()
        setupToolbarBackButton()
        setupToolbarMenu()
        setupEditTexts()
        setupObservers()
    }

    private fun setupObservers() {
        dashboardViewModel.processedImages.observe(viewLifecycleOwner, {
            val imageModel = ImageModel(getImageName(), getTimeStampForImage())
            dashboardViewModel.saveImageEntity(imageModel)
            requireContext().saveImage(it.first(), imageModel.name)
            binding.root.snackBar("Image was encrypted with your secret message") {}
            lifecycleScope.launch {
                delay(750)
                dashboardViewModel.imageEncrypted(true)
                dismiss()
            }
        })
    }

    private fun setupEditTexts() {
        binding.etMessage.doAfterTextChanged { binding.tilMessage.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
    }

    private fun setupToolbarMenu() {
        binding.toolbarEncryptImage.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_action_encrypt -> {
                    if (areFieldsValidated()) encryptImage() else validateFields()
                    true
                }

                else -> true
            }
        }
    }

    private fun setupToolbarBackButton() {
        binding.toolbarEncryptImage.setNavigationOnClickListener {
            dismiss()
        }
    }

    private fun setupImageView() {
        binding.ivImagePreview.load(dashboardViewModel.selectedImage)
        binding.ivImagePreview.shapeAppearanceModel =
            binding.ivImagePreview.shapeAppearanceModel
                .toBuilder()
                .setAllCornerSizes(16f)
                .build()
    }

    private fun encryptImage() {
        dashboardViewModel.prepareEncryptedImage(
            binding.etMessage.text.toString(),
            binding.etPassword.text.toString()
        )
    }

    private fun areFieldsValidated(): Boolean =
        binding.etPassword.text.toString().isNotEmpty()
                && binding.etMessage.text.toString().isNotEmpty()

    private fun validateFields() {
        binding.apply {
            etPassword.text.toString().let {
                if (it.isEmpty()) tilPassword.error = getString(R.string.password_validation)
            }
            etMessage.text.toString().let {
                if (it.isEmpty()) tilMessage.error = getString(R.string.message_validation)
            }
        }
    }

    companion object {
        const val TAG = "EncryptDialogFragment"
    }
}