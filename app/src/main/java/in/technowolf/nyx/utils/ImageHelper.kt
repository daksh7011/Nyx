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

package `in`.technowolf.nyx.utils

import `in`.technowolf.nyx.BuildConfig
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

class ImageHelper(private val context: Context) {

    val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + BuildConfig.APPLICATION_ID

    private var imgPath: String = ""
    private var imageUri: Uri? = null
    private var queryImageUrl: String = ""
    private val permissions = arrayOf(Manifest.permission.CAMERA)

    fun prepareImagePickerIntent(): Intent? {
        val imagePickerIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        return Intent.createChooser(imagePickerIntent, "Please Select an Image")
    }

    fun prepareImageCaptureIntent(): Intent? {
        val imagePickerIntent =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return Intent.createChooser(imagePickerIntent, "Please Capture an Image")
    }

    fun getPickImageIntent(): Intent? {
        var chooserIntent: Intent? = null

        var intentList: MutableList<Intent> = ArrayList()

        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri())

        intentList = addIntentsToList(context, intentList, pickIntent)
        intentList = addIntentsToList(context, intentList, takePhotoIntent)

        if (intentList.size > 0) {
            chooserIntent = Intent.createChooser(
                intentList.removeAt(intentList.size - 1),
                "Please Select an Image"
            )
            chooserIntent!!.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                intentList.toTypedArray<Parcelable>()
            )
        }

        return chooserIntent
    }

    fun handleImageRequest(data: Intent?): String {
        if (data?.data != null) {     //Photo from gallery
            imageUri = data.data
            queryImageUrl = imageUri?.path!!
        } else {
            queryImageUrl = imgPath
        }
        imageUri = Uri.fromFile(File(queryImageUrl))
        return queryImageUrl
    }

    private fun setImageUri(): Uri {
        val folder = File("${context.getExternalFilesDir(Environment.DIRECTORY_DCIM)}")
        folder.mkdirs()

        val file = File(folder, "Image_Tmp.jpg")
        if (file.exists())
            file.delete()
        file.createNewFile()
        imageUri = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".fileProvider",
            file
        )
        imgPath = file.absolutePath
        return imageUri!!
    }

    private fun addIntentsToList(
        context: Context,
        list: MutableList<Intent>,
        intent: Intent
    ): MutableList<Intent> {
        val resInfo = context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resInfo) {
            val packageName = resolveInfo.activityInfo.packageName
            val targetedIntent = Intent(intent)
            targetedIntent.setPackage(packageName)
            list.add(targetedIntent)
        }
        return list
    }

    companion object {
        const val IMAGE_PICKER_INTENT = 69
        const val UNSPLASH_IMAGE_PICKER_INTENT = 96
    }

}