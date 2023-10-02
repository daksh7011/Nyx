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

package `in`.technowolf.nyx.ui.encryption

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import coil.Coil
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import com.google.android.material.transition.MaterialContainerTransform
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto
import com.unsplash.pickerandroid.photopicker.presentation.UnsplashPickerActivity
import `in`.technowolf.nyx.R
import `in`.technowolf.nyx.data.ImageDao
import `in`.technowolf.nyx.databinding.FragmentEncryptionBinding
import `in`.technowolf.nyx.ui.models.ImageModel
import `in`.technowolf.nyx.utils.Extension.getImageName
import `in`.technowolf.nyx.utils.Extension.getTimeStampForImage
import `in`.technowolf.nyx.utils.Extension.gone
import `in`.technowolf.nyx.utils.Extension.saveImage
import `in`.technowolf.nyx.utils.Extension.snackBar
import `in`.technowolf.nyx.utils.Extension.visible
import `in`.technowolf.nyx.utils.ImageHelper
import `in`.technowolf.nyx.utils.Logger
import `in`.technowolf.nyx.utils.themeColor
import `in`.technowolf.nyx.utils.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

@Suppress("detekt.TooManyFunctions")
class EncryptionFragment : Fragment(R.layout.fragment_encryption) {

    private val binding by viewBinding(FragmentEncryptionBinding::bind)

    private val encryptionViewModel: EncryptionViewModel by viewModels()

    private val imageDao: ImageDao by inject()

    private lateinit var popupMenu: PopupMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = 300.toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(com.google.android.material.R.attr.colorSurface))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    private fun init() {
        popupMenu = setupPopupMenu()
        attachObserver()
        setupButtons()
    }

    private fun attachObserver() {
        observeEncryption()
        observeEncryptedImages()
    }

    private fun observeEncryption() {
        encryptionViewModel.encryptedMessage.observe(
            viewLifecycleOwner,
            Observer {
                lifecycleScope.launch {
                    prepareImageEncryption(it)
                }
            },
        )
    }

    private fun observeEncryptedImages() {
        encryptionViewModel.encryptedImages.observe(
            viewLifecycleOwner,
            Observer {
                val imageModel = ImageModel(getImageName(), getTimeStampForImage())
                lifecycleScope.launch(Dispatchers.IO) {
                    imageDao.insertImage(imageModel.toImageEntity())
                }
                requireContext().saveImage(it.first(), imageModel.name)
                binding.root.snackBar(
                    "Image was encrypted with your secret message",
                    anchor = binding.fabEncryptImage,
                ) {}

                clearInputs()
            },
        )
    }

    private fun prepareImageEncryption(encryptedMessage: String?) {
        if (encryptedMessage != null) {
            encryptImage(encryptedMessage)
        } else {
            Logger.w(javaClass.simpleName, "Encrypted Message is empty")
        }
    }

    private fun encryptImage(encryptedMessage: String) {
        encryptionViewModel.prepareEncryptedImage(encryptedMessage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ImageHelper.IMAGE_PICKER_INTENT -> setupSelectedImage(data)
                ImageHelper.UNSPLASH_IMAGE_PICKER_INTENT -> {
                    val photoUrl =
                        data?.getParcelableArrayListExtra<UnsplashPhoto>(UnsplashPickerActivity.EXTRA_PHOTOS)
                            ?.first()?.urls?.regular
                    photoUrl?.let { setupImageFromUnsplash(it) }
                }
            }
        }
    }

    private fun setupSelectedImage(data: Intent?) {
        val imageLoader = Coil.imageLoader(requireContext())
        val request = ImageRequest.Builder(binding.ivImagePreview.context)
            .data(data?.data)
            .target {
                binding.pbImageLoading.gone()
                binding.ivImagePreview.setImageDrawable(it)
                encryptionViewModel.imageForEncryption = (it as BitmapDrawable).bitmap
            }
            .size(ViewSizeResolver(binding.ivImagePreview))
            .placeholder(R.drawable.ic_encryption_illustration)
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(true)
            .build()
        imageLoader.enqueue(request)
    }

    private fun setupImageFromUnsplash(url: String) {
        val imageLoader = Coil.imageLoader(requireContext())
        val request = ImageRequest.Builder(binding.ivImagePreview.context)
            .data(url)
            .target {
                binding.pbImageLoading.gone()
                binding.ivImagePreview.setImageDrawable(it)
                encryptionViewModel.imageForEncryption = (it as BitmapDrawable).bitmap
            }
            .size(ViewSizeResolver(binding.ivImagePreview))
            .placeholder(R.drawable.ic_encryption_illustration)
            .precision(Precision.EXACT)
            .scale(Scale.FILL)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .crossfade(true)
            .build()
        imageLoader.enqueue(request)
    }

    private fun isInputValid(): Boolean {
        if (binding.etEncryptionKey.text.toString().isEmpty() ||
            binding.etEncryptionKey.text.toString().isBlank()
        ) {
            binding.tilEncryptionKey.error = "Encryption key is a required field."
            return false
        }

        if (binding.etMessage.text.toString().isEmpty() ||
            binding.etMessage.text.toString().isBlank()
        ) {
            binding.tilMessage.error = "Message is a required field."
            return false
        }

        return true
    }

    private fun setupButtons() {
        binding.btnSelectImage.setOnClickListener {
            popupMenu.show()
        }
        setupFab()
    }

    private fun setupFab() {
        binding.fabEncryptImage.setOnClickListener {
            if (encryptionViewModel.imageForEncryption == null) {
                binding.root.snackBar(
                    resources.getString(R.string.select_image_warning),
                    anchor = binding.fabEncryptImage,
                ) {}
            } else {
                binding.apply {
                    if (isInputValid()) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                encryptionViewModel.encryptText(
                                    etMessage.text.toString(),
                                    etEncryptionKey.text.toString(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openImagePicker() {
        clearCurrentImage()
//        startActivityForResult(
//            ImageHelper.prepareImagePickerIntent(),
//            ImageHelper.IMAGE_PICKER_INTENT
//        )
    }

    private fun openUnsplashImagePicker() {
        clearCurrentImage()
        startActivityForResult(
            UnsplashPickerActivity.getStartingIntent(
                requireContext(),
                false,
            ),
            ImageHelper.UNSPLASH_IMAGE_PICKER_INTENT,
        )
    }

    private fun clearCurrentImage() {
        encryptionViewModel.imageForEncryption = null
        binding.ivImagePreview.setImageDrawable(null)
    }

    private fun setupPopupMenu(): PopupMenu {
        val popupMenu = PopupMenu(requireContext(), binding.btnSelectImage)
        popupMenu.menuInflater.inflate(R.menu.menu_select_image, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.nav_device -> {
                    openImagePicker()
                    binding.pbImageLoading.visible()
                    return@setOnMenuItemClickListener true
                }

                R.id.nav_unsplash -> {
                    openUnsplashImagePicker()
                    binding.pbImageLoading.visible()
                    return@setOnMenuItemClickListener true
                }

                else -> return@setOnMenuItemClickListener false
            }
        }
        return popupMenu
    }

    private fun clearInputs() {
        with(binding) {
            ivImagePreview.setImageResource(android.R.color.transparent)
            etEncryptionKey.setText("")
            etEncryptionKey.clearFocus()
            etMessage.setText("")
            etMessage.clearFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        if (encryptionViewModel.imageForEncryption != null) {
            BitmapDrawable(resources, encryptionViewModel.imageForEncryption).let {
                binding.ivImagePreview.setImageDrawable(it)
            }
        }
    }
}
