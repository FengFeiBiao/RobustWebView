package github.leavesczy.robustwebview

import android.webkit.JavascriptInterface
import github.leavesczy.robustwebview.utils.log
import github.leavesczy.robustwebview.utils.showToast

/**
 * @Author : FFB
 * @Date : 2022/4/19
 * @Description :
 */
class JsInterface {

    @JavascriptInterface
    fun showToastByAndroid(log: String) {
        log("showToastByAndroid:$log")
        showToast(log)
    }

}