/*
 * MIT License
 *
 * Copyright (c) 2021 TechnoWolf FOSS
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

package `in`.technowolf.nyx.utils

import `in`.technowolf.nyx.BuildConfig
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

object ImageHelper {

    const val IMAGE_PICKER_INTENT = 69
    const val UNSPLASH_IMAGE_PICKER_INTENT = 96

    val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + BuildConfig.AppName

    fun prepareImagePickerIntent(): Intent? {
        val imagePickerIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        return Intent.createChooser(imagePickerIntent, "Please Select Image")
    }

    fun Context.saveImage(bitmap: Bitmap, imageName: String): String {
        this.openFileOutput(imageName, Context.MODE_PRIVATE).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return this.filesDir.absolutePath
    }

    fun Context.retrieveImage(imageName: String): Bitmap =
        BitmapFactory.decodeStream(FileInputStream(File(filesDir, imageName)))

    fun Context.deleteImage(imageName: String): Boolean {
        return File(filesDir, imageName).delete()
    }

}