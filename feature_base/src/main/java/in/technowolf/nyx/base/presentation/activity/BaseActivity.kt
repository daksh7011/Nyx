package `in`.technowolf.nyx.base.presentation.activity

import android.os.Bundle
import android.os.PersistableBundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import `in`.technowolf.nyx.utils.NLog
import `in`.technowolf.nyx.utils.TagConstants
import `in`.technowolf.nyx.utils.TagConstants.FEATURE_BASE
import timber.log.Timber

class BaseActivity(@LayoutRes contentLayoutRes: Int): AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NLog.d(FEATURE_BASE,"onCreate ${javaClass.simpleName}")
    }

    companion object{
        val Fragment.mainActivity: BaseActivity get() = activity as BaseActivity
    }
}
