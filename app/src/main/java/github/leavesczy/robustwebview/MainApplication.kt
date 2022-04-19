package github.leavesczy.robustwebview

import android.app.Application
import com.tencent.mmkv.MMKV
import github.leavesczy.robustwebview.base.WebViewInitTask
import github.leavesczy.robustwebview.utils.ContextHolder

/**
 * @Author : FFB
 * @Date : 2022/4/19
 * @Description :
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        ContextHolder.application = this
        WebViewInitTask.init(this)
    }
}