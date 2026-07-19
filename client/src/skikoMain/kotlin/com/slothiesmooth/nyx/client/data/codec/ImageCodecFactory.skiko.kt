package com.slothiesmooth.nyx.client.data.codec

import com.slothiesmooth.nyx.shared.data.result.AppError
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.ImageCodec
import com.slothiesmooth.nyx.steganography.PixelImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

private const val BYTES_PER_PIXEL = 4
private const val BLUE_INDEX = 0
private const val GREEN_INDEX = 1
private const val RED_INDEX = 2
private const val ALPHA_INDEX = 3
private const val BYTE_MASK = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val ALPHA_SHIFT = 24
private const val OPAQUE_ALPHA: Int = BYTE_MASK shl ALPHA_SHIFT

actual fun defaultImageCodec(): ImageCodec = SkikoImageCodec()

private class SkikoImageCodec : ImageCodec {

    override suspend fun decode(bytes: ByteArray): AppResult<PixelImage> = withContext(Dispatchers.Default) {
        runCatching {
            val image = Image.makeFromEncoded(bytes)
            val width = image.width
            val height = image.height
            val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
            val bitmap = Bitmap().apply { allocPixels(info) }
            image.readPixels(bitmap, 0, 0)
            image.close()
            val bgra = bitmap.readPixels(info, width * BYTES_PER_PIXEL, 0, 0)
            bitmap.close()
            if (bgra == null) {
                AppResult.Err(AppError.Validation("Could not read image pixels"))
            } else {
                AppResult.Ok(PixelImage(width, height, bgraToArgb(bgra, width * height)))
            }
        }.getOrElse { failure -> AppResult.Err(AppError.Storage("Image decode failed", failure)) }
    }

    override suspend fun encodePng(image: PixelImage): AppResult<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val info = ImageInfo(image.width, image.height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
            val raster = Image.makeRaster(info, argbToBgra(image.pixels), image.width * BYTES_PER_PIXEL)
            val data = raster.encodeToData(EncodedImageFormat.PNG)
            raster.close()
            if (data == null) {
                AppResult.Err(AppError.Storage("PNG encode returned no data"))
            } else {
                AppResult.Ok(data.bytes)
            }
        }.getOrElse { failure -> AppResult.Err(AppError.Storage("Image encode failed", failure)) }
    }

    private fun bgraToArgb(bgra: ByteArray, pixelCount: Int): IntArray {
        val argb = IntArray(pixelCount)
        var offset = 0
        for (index in 0 until pixelCount) {
            val blue = bgra[offset + BLUE_INDEX].toInt() and BYTE_MASK
            val green = bgra[offset + GREEN_INDEX].toInt() and BYTE_MASK
            val red = bgra[offset + RED_INDEX].toInt() and BYTE_MASK
            argb[index] = OPAQUE_ALPHA or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue
            offset += BYTES_PER_PIXEL
        }
        return argb
    }

    private fun argbToBgra(argb: IntArray): ByteArray {
        val bgra = ByteArray(argb.size * BYTES_PER_PIXEL)
        var offset = 0
        for (pixel in argb) {
            bgra[offset + BLUE_INDEX] = (pixel and BYTE_MASK).toByte()
            bgra[offset + GREEN_INDEX] = ((pixel shr GREEN_SHIFT) and BYTE_MASK).toByte()
            bgra[offset + RED_INDEX] = ((pixel shr RED_SHIFT) and BYTE_MASK).toByte()
            bgra[offset + ALPHA_INDEX] = BYTE_MASK.toByte()
            offset += BYTES_PER_PIXEL
        }
        return bgra
    }
}
