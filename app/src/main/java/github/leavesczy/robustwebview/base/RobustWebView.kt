package github.leavesczy.robustwebview.base

import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.*
import github.leavesczy.robustwebview.JsInterface
import github.leavesczy.robustwebview.utils.log
import java.io.File

/**
 * @Author: leavesCZY
 * @Date: 2021/9/20 22:45
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
interface WebViewListener {

    fun onProgressChanged(webView: RobustWebView, progress: Int) {

    }

    fun onReceivedTitle(webView: RobustWebView, title: String) {

    }

    fun onPageFinished(webView: RobustWebView, url: String) {

    }

}

class RobustWebView(context: Context, attributeSet: AttributeSet? = null) :
    WebView(context, attributeSet) {

    private val baseCacheDir by lazy {
        File(context.cacheDir, "webView")
    }

    private val databaseCachePath by lazy {
        File(baseCacheDir, "databaseCache").absolutePath
    }

    private val appCachePath by lazy {
        File(baseCacheDir, "appCache").absolutePath
    }

    var hostLifecycleOwner: LifecycleOwner? = null

    var webViewListener: WebViewListener? = null

    private val mWebChromeClient = object : WebChromeClient() {

        // 接收当前页面的加载进度
        override fun onProgressChanged(webView: WebView, newProgress: Int) {
            super.onProgressChanged(webView, newProgress)
            log("onProgressChanged-$newProgress")
            webViewListener?.onProgressChanged(this@RobustWebView, newProgress)
        }

        // 接收标题
        override fun onReceivedTitle(webView: WebView, title: String?) {
            super.onReceivedTitle(webView, title)
            log("onReceivedTitle-$title")
            webViewListener?.onReceivedTitle(this@RobustWebView, title ?: "")
        }

        // 通过重载 WebChromeClient 的下列方法控制弹框的交互，比如替换系统默认的对话框或屏蔽这些对话框
        // 显示一个alert对话框
        override fun onJsAlert(
            webView: WebView,
            url: String?,
            message: String?,
            result: JsResult
        ): Boolean {
            log("onJsAlert: $webView $message")
            return super.onJsAlert(webView, url, message, result)
        }

        // 显示一个confirm对话框
        override fun onJsConfirm(
            webView: WebView,
            url: String?,
            message: String?,
            result: JsResult
        ): Boolean {
            log("onJsConfirm: $url $message")
            return super.onJsConfirm(webView, url, message, result)
        }

        // 显示一个prompt对话框
        override fun onJsPrompt(
            webView: WebView,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            log("onJsPrompt: $url $message $defaultValue")
            return super.onJsPrompt(webView, url, message, defaultValue, result)
        }
    }

    private val mWebViewClient = object : WebViewClient() {

        private var startTime = 0L

        //当即将在当前 WebView 中加载 URL 时，让宿主应用程序有机会进行控制
        override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
            webView.loadUrl(url)
            //false 用手机浏览器打开
            return true
        }

        //应用程序页面已开始加载
        override fun onPageStarted(webView: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(webView, url, favicon)
            startTime = System.currentTimeMillis()
        }

        //应用程序页面已完成加载
        override fun onPageFinished(webView: WebView, url: String?) {
            super.onPageFinished(webView, url)
            log("onPageFinished-$url")
            webViewListener?.onPageFinished(this@RobustWebView, url ?: "")
            log("onPageFinished duration： " + (System.currentTimeMillis() - startTime))
        }

        //程序加载资源时发生 SSL 错误
        override fun onReceivedSslError(
            webView: WebView,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            log("onReceivedSslError-$error")
            super.onReceivedSslError(webView, handler, error)
        }

        //此方法在 API 级别 21 中已弃用。请shouldInterceptRequest(WebView, WebResourceRequest)改用
        override fun shouldInterceptRequest(webView: WebView, url: String): WebResourceResponse? {
            return super.shouldInterceptRequest(webView, url)
        }

        //将资源请求通知宿主应用程序并允许应用程序返回数据
        override fun shouldInterceptRequest(
            webView: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            return WebViewInterceptRequestProxy.shouldInterceptRequest(request)
                ?: super.shouldInterceptRequest(webView, request)
        }
    }

    init {
        webViewClient = mWebViewClient
        webChromeClient = mWebChromeClient
        initWebViewSettings(this)
        initWebViewSettingsExtension(this)
        //向Web页面注入Java对象，页面Javascript脚本可直接引用该对象并调用该对象的方法。
        addJavascriptInterface(JsInterface(), "android")
        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            log(
                "setDownloadListener: $url \n" +
                        "$userAgent \n " +
                        " $contentDisposition \n" +
                        " $mimetype \n" +
                        " $contentLength"
            )
        }
    }

    /**
     * 加载url并带上cookie
     */
    fun loadUrlWithCookie(url: String, cookie: String) {
        val mCookieManager = CookieManager.getInstance()
        mCookieManager?.setCookie(url, cookie)
        mCookieManager?.flush()
        loadUrl(url)
    }

    fun toGoBack(): Boolean {
        if (canGoBack()) {
            goBack()
            return false
        }
        return true
    }

    private fun initWebViewSettings(webView: WebView) {
        val settings = webView.settings

//        settings.userAgentString = "android-leavesCZY"

        //js支持相关
        settings.javaScriptEnabled = true   //支持js
        settings.javaScriptCanOpenWindowsAutomatically = true //支持通过JS弹窗

        settings.pluginsEnabled = true      //支持插件

        //页面自动适配 自动根据手机分辨率缩放
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        //页面缩放支持
        settings.setSupportZoom(false)          //仅支持双击缩放，不支持触摸缩放（android4.0）
        settings.builtInZoomControls = false    //设置支持缩放。设置了此属性，setSupportZoom也默认设置为true
        settings.displayZoomControls = false    //隐藏webview缩放按钮

        settings.allowFileAccess = true         //设置在WebView内部是否允许访问文件 eg:webView.loadUrl("file:///data/data/com.xxx/index.html");
        settings.allowContentAccess = true      //设置在WebView内部是否允许在WebView中访问内容URL eg:webView.loadUrl("content://com.xxx.test_provider/index.html");

        //图片加载
        settings.loadsImagesAutomatically = true//支持自动加载图片

        //Android 8.0 及以上 安全策略
        settings.safeBrowsingEnabled = false    //是否开启安全模式

        //存储相关
        settings.domStorageEnabled = true       //DOM存储API是否可用，默认false,不开启可能导致一些页面空白或者缓存异常
        settings.databaseEnabled = true         //数据库存储API是否可用
        settings.databasePath = databaseCachePath //数据库存储路径
        settings.setAppCacheEnabled(true)       //应用缓存API是否可用
        settings.setAppCachePath(appCachePath)  //设置应用缓存文件的路径
        settings.cacheMode = WebSettings.LOAD_DEFAULT //缓存方式

        //Android 8.0以上，默认禁用了http,还需要在AndroidManifest.xml application中加上 android:usesCleartextTraffic="true" 属性
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }

    private fun initWebViewSettingsExtension(webView: WebView) {
        val settingsExtension = webView.settingsExtension ?: return
        //开启后, 前进后退将不再重新加载页面
        settingsExtension.setContentCacheEnable(true)
        //对于刘海屏机器如果WebView被遮挡会自动padding
        settingsExtension.setDisplayCutoutEnable(true)
        //夜间模式
        settingsExtension.setDayOrNight(true)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        log("onAttachedToWindow : $context")
        (hostLifecycleOwner ?: findLifecycleOwner(context))?.let {
            addHostLifecycleObserver(it)
        }
    }

    private fun findLifecycleOwner(context: Context): LifecycleOwner? {
        if (context is LifecycleOwner) {
            return context
        }
        if (context is MutableContextWrapper) {
            val baseContext = context.baseContext
            if (baseContext is LifecycleOwner) {
                return baseContext
            }
        }
        return null
    }

    private fun addHostLifecycleObserver(lifecycleOwner: LifecycleOwner) {
        log("addLifecycleObserver")
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                onHostResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                onHostPause()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                onHostDestroy()
            }
        })
    }

    private fun onHostResume() {
        log("onHostResume")
        onResume()
    }

    private fun onHostPause() {
        log("onHostPause")
        onPause()
    }

    private fun onHostDestroy() {
        log("onHostDestroy")
        release()
    }

    //释放资源
    private fun release() {
        hostLifecycleOwner = null
        webViewListener = null
        webChromeClient = null
        webViewClient = null
        (parent as? ViewGroup)?.removeView(this)
        destroy()
    }

}