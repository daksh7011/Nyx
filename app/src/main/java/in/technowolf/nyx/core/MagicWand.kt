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

import `in`.technowolf.nyx.utils.Logger
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object MagicWand {

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + job)

    suspend fun encrypt(plainText: String, passPhrase: String): String? {

        return withContext(coroutineScope.coroutineContext + Dispatchers.IO) {

            val random = SecureRandom()
            val bytes = ByteArray(20)
            random.nextBytes(bytes)

            // Deriving the key
            val secretKeyFactory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val pbeKeySpec =
                PBEKeySpec(passPhrase.toCharArray(), bytes, 65556, 256)
            val secretKey: SecretKey = secretKeyFactory.generateSecret(pbeKeySpec)
            val secretKeySpec = SecretKeySpec(secretKey.encoded, "AES")

            // Encrypting the plain text
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val params: AlgorithmParameters = cipher.parameters
            val ivBytes = params.getParameterSpec(IvParameterSpec::class.java).iv
            val encryptedTextBytes: ByteArray =
                cipher.doFinal(plainText.toByteArray(charset("UTF-8")))

            // prepending salt and initialization vector
            val buffer = ByteArray(bytes.size + ivBytes.size + encryptedTextBytes.size)
            System.arraycopy(bytes, 0, buffer, 0, bytes.size)
            System.arraycopy(ivBytes, 0, buffer, bytes.size, ivBytes.size)
            System.arraycopy(
                encryptedTextBytes,
                0,
                buffer,
                bytes.size + ivBytes.size,
                encryptedTextBytes.size
            )

            // Profit!
            Base64.encodeToString(buffer, Base64.DEFAULT)
        }
    }

    suspend fun decrypt(encryptedText: String?, passPhrase: String): String? {

        return withContext(coroutineScope.coroutineContext + Dispatchers.IO) {

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

            // brush off the salt and initialization vector
            val buffer = ByteBuffer.wrap(Base64.decode(encryptedText, Base64.DEFAULT))

            val saltBytes = ByteArray(20)
            buffer[saltBytes, 0, saltBytes.size]

            val ivBytes = ByteArray(cipher.blockSize)
            buffer[ivBytes, 0, ivBytes.size]

            // encrypted text with salt and initialization vector removed
            val encryptedTextBytes = ByteArray(buffer.capacity() - saltBytes.size - ivBytes.size)
            buffer.get(encryptedTextBytes)

            // deriving the key for decryption
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passPhrase.toCharArray(), saltBytes, 65556, 256)
            val secretKey = factory.generateSecret(spec)
            val secret = SecretKeySpec(secretKey.encoded, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(ivBytes))


            var decryptedTextBytes: ByteArray? = null
            try {
                decryptedTextBytes = cipher.doFinal(encryptedTextBytes)
            } catch (e: IllegalBlockSizeException) {
                Logger.e("Illegal block size while decrypting!", e)
            } catch (e: BadPaddingException) {
                Logger.e("Bad Padding while decrypting!", e)
            }

            decryptedTextBytes?.toString(Charsets.UTF_8)
        }
    }

}