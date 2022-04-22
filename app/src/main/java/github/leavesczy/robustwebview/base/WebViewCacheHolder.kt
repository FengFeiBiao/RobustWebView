package github.leavesczy.robustwebview.base

import android.app.Application
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Looper
import android.view.ViewGroup
import github.leavesczy.robustwebview.utils.log
import java.util.*

/**
 * @Author : FFB
 * @Date : 2022/4/19
 * @Description :
 */
object WebViewCacheHolder {

    private val webViewCacheStack = Stack<RobustWebView>()

    private const val CACHED_WEB_VIEW_MAX_NUM = 2

    private lateinit var application: Application

    fun init(application: Application) {
        this.application = application
        prepareWebView()
    }

    fun prepareWebView() {
        if (webViewCacheStack.size < CACHED_WEB_VIEW_MAX_NUM) {
            Looper.myQueue().addIdleHandler {
                log("WebViewCacheStack Size: " + webViewCacheStack.size)
                if (webViewCacheStack.size < CACHED_WEB_VIEW_MAX_NUM) {
                    webViewCacheStack.push(createWebView(MutableContextWrapper(application)))
                }
                false
            }
        }
    }

    fun acquireWebViewInternal(context: Context): RobustWebView {
        if (webViewCacheStack.isEmpty()) {
            return createWebView(context)
        }
        val webView = webViewCacheStack.pop()
        recycleWebView(webView)
        val contextWrapper = webView.context as MutableContextWrapper
        contextWrapper.baseContext = context
        return webView
    }

    private fun createWebView(context: Context): RobustWebView {
        return RobustWebView(context)
    }

    /**
     * 回收WebView
     *
     * @param webView
     */
    private fun recycleWebView(webView: RobustWebView) {
        try {
            if (webView.parent != null && webView.parent is ViewGroup) {
                (webView.parent as ViewGroup).removeView(webView)
                log( "removeParent: remove")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            log("recycleWebView Exception:-->$e")
        }
    }

}