package com.slothiesmooth.nyx.client.data.codec

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.PixelImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val OPAQUE_ALPHA: Int = 0xFF shl 24
private const val PNG_QUALITY = 100

actual fun defaultImageCodec(): ImageCodec = AndroidImageCodec()

private class AndroidImageCodec : ImageCodec {

    override suspend fun decode(bytes: ByteArray): AppResult<PixelImage> = withContext(Dispatchers.Default) {
        runCatching {
            val options = BitmapFactory.Options().apply { inPremultiplied = false }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return@withContext AppResult.Err(AppError.Validation("Not a decodable image"))
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.recycle()
            for (index in pixels.indices) pixels[index] = pixels[index] or OPAQUE_ALPHA
            AppResult.Ok(PixelImage(width, height, pixels))
        }.getOrElse { failure -> AppResult.Err(AppError.Storage("Image decode failed", failure)) }
    }

    override suspend fun encodePng(image: PixelImage): AppResult<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = Bitmap.createBitmap(image.pixels, image.width, image.height, Bitmap.Config.ARGB_8888)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
            bitmap.recycle()
            AppResult.Ok(stream.toByteArray())
        }.getOrElse { failure -> AppResult.Err(AppError.Storage("Image encode failed", failure)) }
    }
}
