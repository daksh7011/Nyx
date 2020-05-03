package `in`.technowolf.nyx

import `in`.technowolf.nyx.core.MagicWand
import `in`.technowolf.nyx.core.Stego
import `in`.technowolf.nyx.databinding.ActivityMainBinding
import `in`.technowolf.nyx.utils.binding
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private val binding by binding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val asd = MagicWand.encrypt(binding.editText.text.toString(), "12345")
        val bm = (binding.raw.drawable as BitmapDrawable).bitmap
        val stego = Stego().encodeMessage(listOf(bm), asd!!)
        binding.finished.setImageBitmap(stego[0])
        binding.btnDecrypt.setOnClickListener {
            val decodedImage = (binding.finished.drawable as BitmapDrawable).bitmap
            val decode = Stego().decodeMessage(listOf(decodedImage))
            Log.e(javaClass.simpleName, MagicWand.decrypt(decode, "12345"))
        }
    }
}
