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

package `in`.technowolf.nyx

import `in`.technowolf.nyx.core.MagicWand
import `in`.technowolf.nyx.core.Stego
import `in`.technowolf.nyx.databinding.ActivityMainBinding
import `in`.technowolf.nyx.utils.binding
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private val binding by binding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val asd = MagicWand.encrypt(binding.editText.text.toString(), "12345")
        val bm = (binding.raw.drawable as BitmapDrawable).bitmap
        val stego = Stego().encodeMessage(listOf(bm), asd!!)
        binding.finished.setImageBitmap(stego[0])
        binding.btnDecrypt.setOnClickListener {
            val decodedImage = (binding.finished.drawable as BitmapDrawable).bitmap
            val decode = Stego().decodeMessage(listOf(decodedImage))
            Log.e(javaClass.simpleName, MagicWand.decrypt(decode, "12345"))
        }
    }
}
