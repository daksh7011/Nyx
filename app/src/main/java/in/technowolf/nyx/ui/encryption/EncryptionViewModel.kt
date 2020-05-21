/*
 * MIT License
 *
 * Copyright (c) 2020. TechnoWolf FOSS
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

package `in`.technowolf.nyx.ui.encryption

import `in`.technowolf.nyx.core.MagicWand
import `in`.technowolf.nyx.core.Steganography
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class EncryptionViewModel : ViewModel() {

    private val steganography = Steganography()

    private var _encryptedMessage = MutableLiveData<String?>()
    val encryptedMessage: LiveData<String?> = _encryptedMessage
    fun encryptText(message: String, passPhrase: String) {
        viewModelScope.launch { _encryptedMessage.value = MagicWand.encrypt(message, passPhrase) }
    }

    private var _encryptedImages = MutableLiveData<List<Bitmap>>()
    val encryptedImages: LiveData<List<Bitmap>> = _encryptedImages
    fun prepareEncryptedImage(imageList: List<Bitmap>, encryptedMessage: String) {
        _encryptedImages.value = steganography.encodeMessage(imageList, encryptedMessage)
    }

}