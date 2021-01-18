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

package `in`.technowolf.nyx.ui.decryption

import `in`.technowolf.nyx.databinding.CellDecryptionImageBinding
import `in`.technowolf.nyx.ui.models.ImageModel
import `in`.technowolf.nyx.utils.Logger
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import kotlinx.coroutines.Dispatchers
import java.io.File

class ImageGalleryAdapter : ListAdapter<ImageModel, ImageGalleryAdapter.ImageViewHolder>(CALLBACK) {

    var onDecrypt: ((ImageModel) -> Unit)? = null
    var onDelete: ((ImageModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder =
        ImageViewHolder(
            CellDecryptionImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(private val binding: CellDecryptionImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imageModel: ImageModel) {
            binding.tvImageName.text = imageModel.date
            setupImage(imageModel.name)

            binding.root.setOnClickListener {
                onDecrypt?.invoke(imageModel)
            }
            binding.btnDelete.setOnClickListener {
                onDelete?.invoke(imageModel)
            }
        }

        private fun setupImage(imageName: String) {
            try {
                binding.ivDecryptionImage.load(File(binding.root.context.filesDir, imageName)) {
                    crossfade(true)
                    dispatcher(Dispatchers.IO)
                    size(ViewSizeResolver(binding.ivDecryptionImage))
                    precision(Precision.EXACT)
                    scale(Scale.FILL)
                }
            } catch (e: Exception) {
                Logger.e("Image loading failed!", e)
            }
        }

    }

    companion object {
        val CALLBACK = object : DiffUtil.ItemCallback<ImageModel>() {
            override fun areItemsTheSame(oldItem: ImageModel, newItem: ImageModel): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ImageModel, newItem: ImageModel): Boolean =
                oldItem == newItem

        }
    }

}