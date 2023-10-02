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

package `in`.technowolf.nyx.steganography

import android.graphics.Bitmap
import android.graphics.Color
import `in`.technowolf.nyx.utils.NLog
import `in`.technowolf.nyx.utils.TagConstants
import `in`.technowolf.nyx.utils.shl
import `in`.technowolf.nyx.utils.shr
import `in`.technowolf.nyx.utils.and
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.or

class Steganography(
    private val startMessageConstant: String = "@!#",
    private val endMessageConstant: String = "#!@"
) {

    private val binary = intArrayOf(16, 8, 0)
    private val andByte = byteArrayOf(0xC0.toByte(), 0x30, 0x0C, 0x03)
    private val toShift = intArrayOf(6, 4, 2, 0)

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + job)

    /**
     * This method represent the core of LSB on 2 bit (Encoding).
     *
     * @param imageArray The rgb array.
     * @param imageWidth Image width.
     * @param imageHeight Image height.
     * @param encodingState Message encoding model.
     * @return Encoded message image.
     */
    private suspend fun encodeMessage(
        imageArray: IntArray, imageWidth: Int, imageHeight: Int,
        encodingState: EncodingState
    ): ByteArray {
        val channels = 3
        var shiftIndex = 4
        val result = ByteArray(imageHeight * imageWidth * channels)
        var resultIndex = 0
        return withContext(coroutineScope.coroutineContext + Dispatchers.IO) {
            for (row in 0 until imageHeight) {
                for (col in 0 until imageWidth) {
                    val element = row * imageWidth + col
                    var tmp: Byte
                    for (channelIndex in 0 until channels) {
                        if (!encodingState.isMessageEncoded) {
                            tmp =
                                (imageArray[element] shr binary[channelIndex] and 0xFF and 0xFC or
                                        (encodingState.byteArrayMessage[encodingState.currentMessageIndex]
                                                shr toShift[shiftIndex++ % toShift.size] and 0x3)).toByte()
                            if (shiftIndex % toShift.size == 0) {
                                //progress ongoing with increment
                                encodingState.incrementMessageIndex()
                            }
                            if (encodingState.currentMessageIndex == encodingState.byteArrayMessage.size) {
                                //done
                                encodingState.isMessageEncoded = true
                            }
                        } else {
                            tmp =
                                (imageArray[element] shr binary[channelIndex] and 0xFF).toByte()
                        }
                        result[resultIndex++] = tmp
                    }
                }
            }
            result
        }
    }

    suspend fun encodeMessage(
        imageList: List<Bitmap>,
        encryptedMessage: String
    ): List<Bitmap> {
        var encryptedMessageWithConstant = encryptedMessage
        val result: MutableList<Bitmap> = ArrayList(imageList.size)
        encryptedMessageWithConstant += endMessageConstant
        encryptedMessageWithConstant = startMessageConstant + encryptedMessageWithConstant
        val byteArrayMessage = encryptedMessageWithConstant.toByteArray(Charset.forName("UTF-8"))
        val messageEncodingStatus = EncodingState()
        messageEncodingStatus.message = encryptedMessageWithConstant
        messageEncodingStatus.byteArrayMessage = byteArrayMessage
        messageEncodingStatus.currentMessageIndex = 0
        messageEncodingStatus.isMessageEncoded = false
        NLog.i(TagConstants.STEGANOGRAPHY, "Message length " + byteArrayMessage.size)
        return withContext(coroutineScope.coroutineContext + Dispatchers.IO) {
            for (image in imageList) {
                if (!messageEncodingStatus.isMessageEncoded) {
                    val width = image.width
                    val height = image.height
                    val oneD = IntArray(width * height)
                    image.getPixels(oneD, 0, width, 0, 0, width, height)
                    val density = image.density
                    val encodedImage =
                        encodeMessage(oneD, width, height, messageEncodingStatus)
                    val oneDMod = byteArrayToIntArray(encodedImage)
                    val destBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    destBitmap.density = density
                    var masterIndex = 0
                    for (j in 0 until height) for (i in 0 until width) {
                        destBitmap.setPixel(
                            i, j, Color.argb(
                                0xFF,
                                oneDMod[masterIndex] shr 16 and 0xFF,
                                oneDMod[masterIndex] shr 8 and 0xFF,
                                oneDMod[masterIndex++] and 0xFF
                            )
                        )
                    }
                    result.add(destBitmap)
                } else result.add(image.copy(image.config, false))
            }
            result
        }
    }

    suspend fun decodeMessage(encodedImages: List<Bitmap>): String? {
        val messageDecodingStatus = DecodingState()
        return withContext(coroutineScope.coroutineContext + Dispatchers.IO) {
            for (bitmapImage in encodedImages) {
                val pixelArray = IntArray(bitmapImage.width * bitmapImage.height)
                bitmapImage.getPixels(
                    pixelArray, 0, bitmapImage.width, 0, 0, bitmapImage.width,
                    bitmapImage.height
                )
                val imageByteArray: ByteArray = convertArray(pixelArray)
                decodeMessage(imageByteArray, messageDecodingStatus)
                if (messageDecodingStatus.isEnded) break
            }
            messageDecodingStatus.message
        }
    }

    /**
     * This is the decoding method of LSB on 2 bit.
     *
     * @param imageByteArray The byte array image.
     * @param decodingState The decoded message.
     */
    private suspend fun decodeMessage(
        imageByteArray: ByteArray,
        decodingState: DecodingState
    ) {
        val vector = Vector<Byte>()
        var shiftIndex = 4
        var initialArray: Byte = 0x00
        withContext(coroutineScope.coroutineContext + Dispatchers.IO) {
            for (i in imageByteArray.indices) {
                initialArray =
                    initialArray or (imageByteArray[i] shl toShift[shiftIndex % toShift.size]
                            and andByte[shiftIndex++ % toShift.size]).toByte()
                if (shiftIndex % toShift.size == 0) {
                    vector.addElement(initialArray)
                    val finalByteArray = byteArrayOf(vector.elementAt(vector.size - 1).toByte())
                    val str = String(finalByteArray, Charset.forName("UTF-8"))

                    if (decodingState.message != null) {
                        if (decodingState.message!!.endsWith(endMessageConstant)) {
                            NLog.i(TagConstants.STEGANOGRAPHY, "Decoding ended")
                            //fix utf-8 decoding
                            val temp = ByteArray(vector.size)
                            for (index in temp.indices) temp[index] = vector[index]
                            val string =
                                String(temp, Charset.forName("UTF-8"))
                            decodingState.message = string.substring(0, string.length - 1)
                            //end fix
                            decodingState.isEnded = true
                            break
                        } else {
                            decodingState.message = decodingState.message + str
                            if (decodingState.message!!.length == startMessageConstant.length
                                && startMessageConstant != decodingState.message
                            ) {
                                decodingState.message = null
                                decodingState.isEnded = true
                                break
                            }
                        }
                    }
                    initialArray = 0x00
                }
            }
            if (decodingState.message != null) {
                decodingState.message = decodingState.message!!
                    .substring(
                        startMessageConstant.length,
                        decodingState.message!!.length - endMessageConstant.length
                    )
            }
        }
    }

    private fun byteArrayToIntArray(b: ByteArray): IntArray {
        NLog.i(TagConstants.STEGANOGRAPHY, b.size.toString())
        val size = b.size / 3
        NLog.i(TagConstants.STEGANOGRAPHY, size.toString())
        System.runFinalization()
        System.gc()
        NLog.i(TagConstants.STEGANOGRAPHY, Runtime.getRuntime().freeMemory().toString())
        val result = IntArray(size)
        var byteOffset = 0
        var index = 0
        while (byteOffset < b.size) {
            result[index++] = byteArrayToInt(b, byteOffset)
            byteOffset += 3
        }
        return result
    }

    private fun byteArrayToInt(byteArray: ByteArray, offset: Int = 0): Int {
        var finalInt = 0x00000000
        for (i in 0..2) {
            val shift = (3 - 1 - i) * 8
            finalInt = finalInt or (byteArray[i + offset] and 0x000000FF shl shift)
        }
        finalInt = finalInt and 0x00FFFFFF
        return finalInt
    }

    private fun convertArray(array: IntArray): ByteArray {
        val newArray = ByteArray(array.size * 3)
        for (i in array.indices) {
            newArray[i * 3] = (array[i] shr 16 and 0xFF).toByte()
            newArray[i * 3 + 1] = (array[i] shr 8 and 0xFF).toByte()
            newArray[i * 3 + 2] = (array[i] and 0xFF).toByte()
        }
        return newArray
    }

    private class DecodingState {
        var message: String? = ""
        var isEnded = false
    }

    private class EncodingState {
        var isMessageEncoded = false
        var currentMessageIndex = 0
        lateinit var byteArrayMessage: ByteArray
        var message: String? = null
        fun incrementMessageIndex() {
            currentMessageIndex++
        }
    }

}