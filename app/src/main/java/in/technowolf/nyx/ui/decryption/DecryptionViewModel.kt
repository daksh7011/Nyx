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

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.technowolf.nyx.core.MagicWand
import `in`.technowolf.nyx.core.Steganography
import `in`.technowolf.nyx.ui.models.ImageModel
import kotlinx.coroutines.launch

class DecryptionViewModel : ViewModel() {

    private val steganography = Steganography()

    val imageList: MutableList<ImageModel> = mutableListOf()

    private var _decryptedText = MutableLiveData<String?>()
    val decryptedText: LiveData<String?> = _decryptedText

    fun decryptImage(bitmap: Bitmap, passPhrase: String) {
        viewModelScope.launch {
            val bitmapText = steganography.decodeMessage(listOf(bitmap))
            _decryptedText.value = MagicWand.decrypt(bitmapText, passPhrase)
        }
    }
}
