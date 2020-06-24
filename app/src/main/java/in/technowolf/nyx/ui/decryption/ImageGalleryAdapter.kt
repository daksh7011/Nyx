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

package `in`.technowolf.nyx.ui.decryption

import `in`.technowolf.nyx.databinding.CellDecryptionImageBinding
import `in`.technowolf.nyx.ui.models.ImageModel
import `in`.technowolf.nyx.utils.ImageHelper.retrieveImage
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load


class ImageGalleryAdapter : ListAdapter<ImageModel, ImageGalleryAdapter.ImageViewHolder>(CALLBACK) {

    var onDecrypt: ((ImageModel) -> Unit)? = null
    var onDelete: ((ImageModel, position: Int) -> Unit)? = null

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

        val set = ConstraintSet()

        fun bind(imageModel: ImageModel) {
            setCorners()
            setupImage(imageModel.name)

            binding.btnDecrypt.setOnClickListener {
                onDecrypt?.invoke(imageModel)
            }
            binding.btnDelete.setOnClickListener {
                onDelete?.invoke(imageModel, adapterPosition)
            }
        }

        private fun setupImage(imageName: String) {
            try {
                binding.ivDecryptionImage.load(binding.root.context.retrieveImage(imageName))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun setCorners() {
            val curveRadius = 8F
            binding.ivDecryptionImage.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(
                        0,
                        0,
                        view.width,
                        (view.height + curveRadius).toInt(),
                        curveRadius
                    )
                }
            }
            binding.ivDecryptionImage.clipToOutline = true
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