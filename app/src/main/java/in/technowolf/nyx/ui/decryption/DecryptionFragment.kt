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

import `in`.technowolf.nyx.R
import `in`.technowolf.nyx.data.ImageDao
import `in`.technowolf.nyx.databinding.FragmentDecryptionBinding
import `in`.technowolf.nyx.ui.models.ImageModel
import `in`.technowolf.nyx.utils.Extension.deleteImage
import `in`.technowolf.nyx.utils.Extension.gone
import `in`.technowolf.nyx.utils.Extension.retrieveImage
import `in`.technowolf.nyx.utils.Extension.snackBar
import `in`.technowolf.nyx.utils.Extension.visible
import `in`.technowolf.nyx.utils.Logger
import `in`.technowolf.nyx.utils.alert
import `in`.technowolf.nyx.utils.viewBinding
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject


class DecryptionFragment : Fragment(R.layout.fragment_decryption) {

    private val binding by viewBinding(FragmentDecryptionBinding::bind)

    private val decryptionViewModel: DecryptionViewModel by viewModels()

    private val imageGalleryAdapter = ImageGalleryAdapter()

    private val imageDao: ImageDao by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    private fun init() {
        setupRecyclerView()
        populateImages()
        setupOnImageDecryptAction()
        setupOnImageDeleteAction()
        observeDecryptedText()
    }

    private fun observeDecryptedText() {
        decryptionViewModel.decryptedText.observe(viewLifecycleOwner, Observer {
            setupDecryptionAlert(it)
        })
    }

    private fun setupDecryptionAlert(decryptedText: String?) {
        val alertMessage: String
        var positiveButtonText = ""
        if (decryptedText.isNullOrEmpty()) {
            alertMessage = "Wrong passphrase, Please try again!"
        } else {
            alertMessage = decryptedText
            positiveButtonText = "Copy to clipboard"
        }
        alert("Decrypted Message", alertMessage) {
            positiveButton(positiveButtonText) {
                val clipboard: ClipboardManager? =
                    getSystemService(requireContext(), ClipboardManager::class.java)
                val clip = ClipData.newPlainText("Decrypted Message", alertMessage)
                clipboard?.setPrimaryClip(clip)
            }
            negativeButton("Close")
        }.show()
    }

    private fun setupOnImageDeleteAction() {
        imageGalleryAdapter.onDelete = { imageModel: ImageModel ->
            lifecycleScope.launch(Dispatchers.IO) { imageDao.delete(imageModel.name) }
            requireContext().deleteImage(imageModel.name)
            decryptionViewModel.imageList.remove(imageModel)
            imageGalleryAdapter.submitList(decryptionViewModel.imageList)
            imageGalleryAdapter.notifyDataSetChanged()
        }
    }

    private fun setupOnImageDecryptAction() {
        imageGalleryAdapter.onDecrypt = {
            alert(
                getString(R.string.alert_decryption_title),
                getString(R.string.alert_decryption_message),
                true
            ) {
                cancelable = false
                positiveButton(getString(R.string.ok)) {
                    if (etPassphrase.text.toString().isNotEmpty()) {
                        Logger.i("Starting decryption from image")
                        decryptionViewModel.decryptImage(
                            requireContext().retrieveImage(it.name),
                            etPassphrase.text.toString()
                        )
                    } else {
                        binding.root.snackBar(getString(R.string.error_enter_passphrase)) {}
                    }
                }
                negativeButton(getString(R.string.cancel))
            }.show()
        }
    }

    private fun setupRecyclerView() {
        binding.rvEncryptedImages.apply {
            adapter = imageGalleryAdapter
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }
    }

    private fun populateImages() {
        lifecycleScope.launch(Dispatchers.IO) {
            imageDao.getAllImages().let {
                it.map { imageEntity ->
                    imageEntity.toImageModel()
                }.also { imageModelList ->
                    requireActivity().runOnUiThread {
                        if (imageModelList.isEmpty()) {
                            binding.noResultAnimation.visible()
                            binding.tvNoImageFound.visible()
                            binding.rvEncryptedImages.gone()
                        } else {
                            decryptionViewModel.imageList.addAll(imageModelList)
                            imageGalleryAdapter.submitList(imageModelList)
                        }
                    }
                }
            }
        }
    }
}
