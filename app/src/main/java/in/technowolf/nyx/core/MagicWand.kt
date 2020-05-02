package `in`.technowolf.nyx.core

import android.util.Base64
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


object MagicWand {

    fun encrypt(plainText: String, passPhrase: String): String? {

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
        val encryptedTextBytes: ByteArray = cipher.doFinal(plainText.toByteArray(charset("UTF-8")))

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
        return Base64.encodeToString(buffer, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String?, passPhrase: String): String? {

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
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        }

        return decryptedTextBytes?.toString(Charsets.UTF_8)
    }

}