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

import `in`.technowolf.nyx.core.MagicWand
import `in`.technowolf.nyx.core.Steganography
import `in`.technowolf.nyx.data.ImageDao
import `in`.technowolf.nyx.ui.models.ImageModel
import `in`.technowolf.nyx.utils.Extension.readOnly
import `in`.technowolf.nyx.utils.SingleLiveEvent
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardViewModel(private val imageDao: ImageDao) : ViewModel() {

    private val steganography = Steganography()

    private val _encryptedImages = MutableLiveData<List<ImageModel>>()
    val encryptedImages = _encryptedImages.readOnly()

    private var _decryptedText = SingleLiveEvent<String?>()
    val decryptedText = _decryptedText.readOnly()

    private var _processedImages = MutableLiveData<List<Bitmap>>()
    val processedImages = _processedImages.readOnly()

    private var _wasImageEncrypted = SingleLiveEvent<Boolean>()
    val wasImageEncrypted = _wasImageEncrypted.readOnly()

    var selectedImage: Bitmap? = null

    var selectedImageModel: ImageModel? = null

    init {
        getEncryptedImages()
    }

    fun prepareEncryptedImage(message: String, passPhrase: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val encryptedMessage = MagicWand.encrypt(message, passPhrase)
            if (selectedImage != null) {
                _processedImages.value =
                    steganography.encodeMessage(listOf(selectedImage!!), encryptedMessage.orEmpty())
            }
        }
    }

    fun getEncryptedImages() {
        viewModelScope.launch {
            _encryptedImages.value = imageDao.getAllImages().map { it.toImageModel() }
        }
    }

    fun decryptImage(bitmap: Bitmap, passPhrase: String) {
        viewModelScope.launch {
            val bitmapText = steganography.decodeMessage(listOf(bitmap))
            _decryptedText.value = MagicWand.decrypt(bitmapText, passPhrase)
        }
    }

    fun saveImageEntity(imageModel: ImageModel) {
        viewModelScope.launch {
            imageDao.insertImage(imageModel.toImageEntity())
        }
    }

    fun imageEncrypted(wasEncrypted: Boolean) {
        _wasImageEncrypted.value = wasEncrypted
    }
}
