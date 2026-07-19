package com.slothiesmooth.nyx.client

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.slothiesmooth.nyx.client.data.codec.defaultImageCodec
import com.slothiesmooth.nyx.crypto.DefaultNyxCrypto
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.DecryptOutcome
import com.slothiesmooth.nyx.feature.decrypt.basic.domain.usecase.DecryptMessageUseCase
import com.slothiesmooth.nyx.feature.encrypt.basic.domain.usecase.EncryptMessageUseCase
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.source.PickedImage
import com.slothiesmooth.nyx.shared.testsupport.FakeImagePicker
import com.slothiesmooth.nyx.steganography.Steganography
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * Proves the full steganography round-trip on a real device: a cover supplied through the injectable
 * [FakeImagePicker] is encrypted, and the modified image — fed back through the same picker seam —
 * decrypts to the original message. This exercises the real Android [defaultImageCodec] (PNG
 * decode/encode via Bitmap), which host tests cannot, alongside the real crypto and stego engines.
 */
@RunWith(AndroidJUnit4::class)
class StegoRoundTripDeviceTest {

    private lateinit var imagePicker: FakeImagePicker
    private lateinit var encrypt: EncryptMessageUseCase
    private lateinit var decrypt: DecryptMessageUseCase

    @Before
    fun setup() {
        val crypto = DefaultNyxCrypto()
        val stego = Steganography()
        val codec = defaultImageCodec()
        encrypt = EncryptMessageUseCase(crypto, stego, codec)
        decrypt = DecryptMessageUseCase(crypto, stego, codec)
        imagePicker = FakeImagePicker(next = PickedImage(bytes = generateCoverPng(), suggestedName = "cover.png"))
    }

    @After
    fun teardown() {
        imagePicker.next = null
    }

    @Test
    fun hidesAndRecoversMessageThroughTheRealAndroidCodec() = runBlocking {
        // Encrypt path: the picker supplies the base cover, exactly as the wizard would.
        val cover = imagePicker.pickImage() ?: error("picker must supply the cover image")
        val stegoBytes = (encrypt(cover.bytes, MESSAGE, PASSWORD) as? AppResult.Ok)?.value
            ?: error("encrypting into a cover of $COVER_SIZE px should succeed")
        assertFalse("stego output must differ from the cover", stegoBytes.contentEquals(cover.bytes))

        // Decrypt path: feed the modified image back through the same picker seam.
        imagePicker.next = PickedImage(bytes = stegoBytes, suggestedName = "stego.png")
        val modified = imagePicker.pickImage() ?: error("picker must supply the modified image")
        assertEquals(DecryptOutcome.Success(MESSAGE), decrypt(modified.bytes, PASSWORD))

        // Reverse-stego guarantees: a wrong password is rejected, and a plain cover reveals nothing.
        assertEquals(DecryptOutcome.WrongPasswordOrTampered, decrypt(stegoBytes, WRONG_PASSWORD))
        assertEquals(DecryptOutcome.NoHiddenMessage, decrypt(cover.bytes, PASSWORD))
    }

    private fun generateCoverPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(COVER_SIZE, COVER_SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until COVER_SIZE) {
            for (x in 0 until COVER_SIZE) {
                bitmap.setPixel(x, y, Color.rgb(x, y, x + y))
            }
        }
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
            stream.toByteArray()
        }
    }

    private companion object {
        const val MESSAGE = "meet at the old bridge at midnight"
        const val PASSWORD = "correct horse battery staple"
        const val WRONG_PASSWORD = "not the password"
        const val COVER_SIZE = 128
        const val PNG_QUALITY = 100
    }
}
