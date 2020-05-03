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

package `in`.technowolf.nyx.core

import `in`.technowolf.nyx.utils.Extension.and
import `in`.technowolf.nyx.utils.Extension.shl
import `in`.technowolf.nyx.utils.Extension.shr
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.or

class Stego {

    private val binary = intArrayOf(16, 8, 0)
    private val andByte = byteArrayOf(0xC0.toByte(), 0x30, 0x0C, 0x03)
    private val toShift = intArrayOf(6, 4, 2, 0)

    /**
     * This method represent the core of LSB on 2 bit (Encoding).
     *
     * @param imageArray The **rgb** array.
     * @param imageWidth Image width.
     * @param imageHeight Image height.
     * @param messageEncodingStatus Message encoding model.
     * @return Encoded message image.
     */
    private fun encodeMessage(
        imageArray: IntArray, imageWidth: Int, imageHeight: Int,
        messageEncodingStatus: MessageEncodingStatus
    ): ByteArray {
        val channels = 3
        var shiftIndex = 4
        //Array.newInstance(Byte.class, imgRows * imgCols * channels);
        val result = ByteArray(imageHeight * imageWidth * channels)
        var resultIndex = 0
        for (row in 0 until imageHeight) {
            for (col in 0 until imageWidth) {
                val element = row * imageWidth + col
                var tmp: Byte
                for (channelIndex in 0 until channels) {
                    if (!messageEncodingStatus.isMessageEncoded) {
                        tmp =
                            (imageArray[element] shr binary[channelIndex] and 0xFF and 0xFC or
                                    (messageEncodingStatus.byteArrayMessage[messageEncodingStatus.currentMessageIndex]
                                            shr toShift[shiftIndex++ % toShift.size] and 0x3)).toByte()
                        if (shiftIndex % toShift.size == 0) {
                            //progress ongoing with increment
                            messageEncodingStatus.incrementMessageIndex()
                        }
                        if (messageEncodingStatus.currentMessageIndex == messageEncodingStatus.byteArrayMessage.size) {
                            //done
                            messageEncodingStatus.isMessageEncoded = true
                        }
                    } else {
                        tmp =
                            (imageArray[element] shr binary[channelIndex] and 0xFF).toByte()
                    }
                    result[resultIndex++] = tmp
                }
            }
        }
        return result
    }

    fun encodeMessage(
        imageList: List<Bitmap>,
        string: String
    ): List<Bitmap> {
        var str = string
        val result: MutableList<Bitmap> = ArrayList(imageList.size)
        str += END_MESSAGE_CONSTANT
        str = START_MESSAGE_CONSTANT + str
        val msg = str.toByteArray(Charset.forName("UTF-8"))
        val message = MessageEncodingStatus()
        message.message = str
        message.byteArrayMessage = msg
        message.currentMessageIndex = 0
        message.isMessageEncoded = false
        Log.i(javaClass.simpleName, "Message length " + msg.size)
        for (image in imageList) {
            if (!message.isMessageEncoded) {
                val width = image.width
                val height = image.height
                val oneD = IntArray(width * height)
                image.getPixels(oneD, 0, width, 0, 0, width, height)
                val density = image.density
                val encodedImage =
                    encodeMessage(oneD, width, height, message)
                val oneDMod = byteArrayToIntArray(encodedImage)
                val destBitmap = Bitmap.createBitmap(
                    width, height,
                    Bitmap.Config.ARGB_8888
                )
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
        Log.d(javaClass.simpleName, "Message current index " + message.currentMessageIndex)
        return result
    }

    fun decodeMessage(encodedImages: List<Bitmap>): String? {
        val messageDecodingStatus = MessageDecodingStatus()
        for (bit in encodedImages) {
            val pixels = IntArray(bit.width * bit.height)
            bit.getPixels(
                pixels, 0, bit.width, 0, 0, bit.width,
                bit.height
            )
            var b: ByteArray?
            b = convertArray(pixels)
            decodeMessage(b, messageDecodingStatus)
            if (messageDecodingStatus.isEnded) break
        }
        return messageDecodingStatus.message
    }

    /**
     * This is the decoding method of LSB on 2 bit.
     *
     * @param imageByteArray The byte array image.
     * @param messageDecodingStatus The decoded message.
     */
    private fun decodeMessage(
        imageByteArray: ByteArray?,
        messageDecodingStatus: MessageDecodingStatus
    ) {
        val v = Vector<Byte>()
        var shiftIndex = 4
        var tmp: Byte = 0x00
        for (i in imageByteArray!!.indices) {
            tmp = (tmp or
                    ((imageByteArray[i] shl toShift[shiftIndex % toShift.size]
                            and andByte[shiftIndex++ % toShift.size]).toByte()))
            if (shiftIndex % toShift.size == 0) {
                v.addElement(tmp)
                val nonso = byteArrayOf(v.elementAt(v.size - 1).toByte())
                val str = String(nonso, Charset.forName("UTF-8"))

                if (messageDecodingStatus.message!!.endsWith(END_MESSAGE_CONSTANT)) {
                    Log.i("TEST", "Decoding ended")
                    //fix utf-8 decoding
                    val temp = ByteArray(v.size)
                    for (index in temp.indices) temp[index] = v[index]
                    val stra =
                        String(temp, Charset.forName("UTF-8"))
                    messageDecodingStatus.message = stra.substring(0, stra.length - 1)
                    //end fix
                    messageDecodingStatus.isEnded = true
                    break
                } else {
                    messageDecodingStatus.message = messageDecodingStatus.message + str
                    if (messageDecodingStatus.message!!.length == START_MESSAGE_CONSTANT.length
                        && START_MESSAGE_CONSTANT != messageDecodingStatus.message
                    ) {
                        messageDecodingStatus.message = null
                        messageDecodingStatus.isEnded = true
                        break
                    }
                }
                tmp = 0x00
            }
        }
        if (messageDecodingStatus.message != null) {
            messageDecodingStatus.message = messageDecodingStatus.message!!
                .substring(
                    START_MESSAGE_CONSTANT.length,
                    messageDecodingStatus.message!!.length - END_MESSAGE_CONSTANT.length
                )
        }
    }

    private fun byteArrayToIntArray(b: ByteArray): IntArray {
        Log.v("Size byte array", b.size.toString() + "")
        val size = b.size / 3
        Log.v("Size Int array", size.toString() + "")
        System.runFinalization()
        System.gc()
        Log.v(
            "FreeMemory",
            Runtime.getRuntime().freeMemory().toString() + ""
        )
        val result = IntArray(size)
        var off = 0
        var index = 0
        while (off < b.size) {
            result[index++] = byteArrayToInt(b, off)
            off += 3
        }
        return result
    }

    private fun byteArrayToInt(b: ByteArray, offset: Int = 0): Int {
        var value = 0x00000000
        for (i in 0..2) {
            val shift = (3 - 1 - i) * 8
            value = value or (b[i + offset] and 0x000000FF shl shift)
        }
        value = value and 0x00FFFFFF
        return value
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


    private class MessageDecodingStatus {
        var message: String? = ""
        var isEnded = false

    }

    private class MessageEncodingStatus {
        var isMessageEncoded = false
        var currentMessageIndex = 0
        lateinit var byteArrayMessage: ByteArray
        var message: String? = null
        fun incrementMessageIndex() {
            currentMessageIndex++
        }

    }

    companion object {
        private const val START_MESSAGE_CONSTANT = "@!#"
        private const val END_MESSAGE_CONSTANT = "#!@"
    }
}