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

package `in`.technowolf.nyx.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto
import com.unsplash.pickerandroid.photopicker.presentation.UnsplashPickerActivity
import `in`.technowolf.nyx.R
import `in`.technowolf.nyx.databinding.FragmentHomeBinding
import `in`.technowolf.nyx.ui.dashboard.BottomSheetAppbar
import `in`.technowolf.nyx.ui.dashboard.DashboardViewModel
import `in`.technowolf.nyx.ui.dashboard.EncryptDialogFragment
import `in`.technowolf.nyx.ui.dashboard.EncryptedImagesAdapter
import `in`.technowolf.nyx.ui.dashboard.ImageSourceBottomSheet
import `in`.technowolf.nyx.utils.ImageHelper
import `in`.technowolf.nyx.utils.alert
import `in`.technowolf.nyx.utils.viewBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.File


class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val dashboardViewModel: DashboardViewModel by sharedViewModel()
    private val encryptedImagesAdapter = EncryptedImagesAdapter()
    private val homeBottomSheet = BottomSheetAppbar()
    private var imageSourceBottomSheet: ImageSourceBottomSheet = ImageSourceBottomSheet()
    private var encryptDialogFragment: EncryptDialogFragment? = EncryptDialogFragment()
    private var imageHelper: ImageHelper? = null
    private var imagePickerLauncher: ActivityResultLauncher<Intent>? = null
    private var unsplashPickerLauncher: ActivityResultLauncher<Intent>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        setupImagePickerLauncher()
        setupUnsplashLauncher()
        setupHomeBottomSheet()
        setupImageSourceBottomSheet()
        setupBottomBar()
        setupObservers()
        setupFab()
        setupRecyclerView()
        imageHelper = ImageHelper(requireContext())
    }

    private fun setupHomeBottomSheet() {
        homeBottomSheet.apply {
            onSelectDecryptionImage = {
                showTbdToast()
            }
            onFaq = {
                showTbdToast()
            }
            onDonate = {
                showTbdToast()
            }
            onAbout = {
                showTbdToast()
            }
        }
    }

    private fun setupImageSourceBottomSheet() {
        imageSourceBottomSheet.apply {
            onGallerySelected = {
                imagePickerLauncher?.launch(imageHelper?.getPickImageIntent())
                dismiss()
            }
            onUnsplashSelected = {
                val intent = UnsplashPickerActivity.getStartingIntent(requireContext(), false)
                unsplashPickerLauncher?.launch(intent)
                dismiss()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvEncryptedImages.apply {
            adapter = encryptedImagesAdapter
        }
        encryptedImagesAdapter.onImageClicked = {
            dashboardViewModel.selectedImageModel = it
            findNavController().navigate(R.id.actionToImageDecryptionFragment)
        }
    }

    private fun setupFab() {
        binding.fabEncryptImage.setOnClickListener {
            imageSourceBottomSheet.show(childFragmentManager, imageSourceBottomSheet.tag)
        }
    }

    private fun setupBottomBar() {
        binding.bottomAppBar.setNavigationOnClickListener {
            homeBottomSheet.show(childFragmentManager, BottomSheetAppbar.BOTTOM_SHEET_TAG)
        }
    }

    private fun setupUnsplashLauncher() {
        unsplashPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data

                    val photoUrl =
                        data?.getParcelableArrayListExtra<UnsplashPhoto>(UnsplashPickerActivity.EXTRA_PHOTOS)
                            ?.first()?.urls?.regular
                    val request = ImageRequest.Builder(requireContext())
                        .data(photoUrl)
                        .bitmapConfig(Bitmap.Config.ARGB_8888)
                        .target {
                            dashboardViewModel.selectedImage = (it as BitmapDrawable).bitmap
                            encryptDialogFragment?.show(
                                childFragmentManager,
                                EncryptDialogFragment.TAG
                            )
                        }
                        .build()
                    requireContext().imageLoader.enqueue(request)
                }
            }
    }

    private fun setupImagePickerLauncher() {
        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data

                    if (data?.data != null) {
                        // from gallery
                        val request = ImageRequest.Builder(requireContext())
                            .data(data.data)
                            .bitmapConfig(Bitmap.Config.ARGB_8888)
                            .target {
                                dashboardViewModel.selectedImage = (it as BitmapDrawable).bitmap
                                encryptDialogFragment?.show(
                                    childFragmentManager,
                                    EncryptDialogFragment.TAG
                                )
                            }
                            .build()
                        requireContext().imageLoader.enqueue(request)
                    } else {
                        // from camera
                        val request = ImageRequest.Builder(requireContext())
                            .data(File(imageHelper?.handleImageRequest(data).orEmpty()))
                            .bitmapConfig(Bitmap.Config.ARGB_8888)
                            .target {
                                dashboardViewModel.selectedImage = (it as BitmapDrawable).bitmap
                                encryptDialogFragment?.show(
                                    childFragmentManager,
                                    EncryptDialogFragment.TAG
                                )
                            }
                            .build()
                        requireContext().imageLoader.enqueue(request)
                    }

                }
            }
    }

    private fun setupObservers() {
        dashboardViewModel.encryptedImages.observe(viewLifecycleOwner, {
            encryptedImagesAdapter.submitList(it)
        })
        dashboardViewModel.wasImageEncrypted.observe(viewLifecycleOwner, { wasEncrypted ->
            if (wasEncrypted) dashboardViewModel.getEncryptedImages()
        })
        dashboardViewModel.decryptedText.observe(viewLifecycleOwner, {
            val alertMessage: String
            var positiveButtonText = ""
            if (it.isNullOrEmpty()) {
                alertMessage = "Wrong passphrase, Please try again!"
            } else {
                alertMessage = it
                positiveButtonText = "Copy to clipboard"
            }
            alert("Decrypted Message", alertMessage) {
                positiveButton(positiveButtonText) {
                    val clipboard: ClipboardManager? =
                        ContextCompat.getSystemService(
                            requireContext(),
                            ClipboardManager::class.java
                        )
                    val clip = ClipData.newPlainText("Decrypted Message", alertMessage)
                    clipboard?.setPrimaryClip(clip)
                }
                negativeButton("Close")
            }.show()
        })
    }

    private fun showTbdToast() {
        Toast.makeText(
            requireContext(),
            "Will be updated in next version.",
            Toast.LENGTH_SHORT
        ).show()
    }

}