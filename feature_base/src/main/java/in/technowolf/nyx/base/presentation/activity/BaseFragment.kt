package `in`.technowolf.nyx.base.presentation.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import `in`.technowolf.nyx.utils.NLog
import `in`.technowolf.nyx.utils.TagConstants.FEATURE_BASE

class BaseFragment: Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NLog.d(FEATURE_BASE,"onCreate ${javaClass.simpleName}")
    }
}
