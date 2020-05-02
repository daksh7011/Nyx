package `in`.technowolf.nyx

import `in`.technowolf.nyx.databinding.ActivityMainBinding
import `in`.technowolf.nyx.utils.binding
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val binding by binding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}
