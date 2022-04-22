package github.leavesczy.robustwebview.base

import android.app.Application
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.MimeTypeMap
import github.leavesczy.robustwebview.utils.log
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * @Author : FFB
 * @Date : 2022/4/19
 * @Description :
 */
object WebViewInterceptRequestProxy {

    private lateinit var application: Application

    private val webViewResourceCacheDir by lazy {
        File(application.cacheDir, "RobustWebView")
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder().cache(Cache(webViewResourceCacheDir, 600L * 1024 * 1024))
            .followRedirects(false)//禁制OkHttp的重定向操作，我们自己处理重定向
            .followSslRedirects(false)//在 followRedirects这个开关打开的前提下，当你重定向的时候 发生了协议切换的时候，要不要依然重定向，比如 一开始进行了http请求重定向到了https 还要不要继续重定向
            .addNetworkInterceptor(getChuckerInterceptor(application = application))
            .addNetworkInterceptor(getWebViewCacheInterceptor())
            .build()
    }

    private fun getChuckerInterceptor(application: Application): Interceptor {
        return ChuckerInterceptor.Builder(application)
            .collector(ChuckerCollector(application))
            .maxContentLength(250000L)
            .alwaysReadResponseBody(true)
            .build()
    }

    private fun getWebViewCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            response.newBuilder()
                .removeHeader("pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", "max-age=" + (360L * 24 * 60 * 60))
                .build()
        }
    }

    fun init(application: Application) {
        this.application = application
    }

    fun shouldInterceptRequest(webResourceRequest: WebResourceRequest?): WebResourceResponse? {
        if (toProxy(webResourceRequest)) {
            return getHttpResource(webResourceRequest!!)
        }
        return null
    }

    //判断是否需要拦截
    private fun toProxy(webResourceRequest: WebResourceRequest?): Boolean {
        //isForMainFrame 获取当前的网络请求是否是为main frame创建的
        if (webResourceRequest == null || webResourceRequest.isForMainFrame) {
            return false
        }
        val url = webResourceRequest.url ?: return false
        //GET方法才能往下执行
        if (!webResourceRequest.method.equals("GET", true)) {
            return false
        }

        val extension = getExtensionFromUrl(url.toString())
        if ((url.scheme == "https" || url.scheme == "http") && (extension == "ico" || extension == "bmp" || extension == "gif"
                    || extension == "jpeg" || extension == "jpg" || extension == "png"
                    || extension == "svg" || extension == "webp" || extension == "css"
                    || extension == "js" || extension == "json" || extension == "eot"
                    || extension == "otf" || extension == "ttf" || extension == "woff")) {
            return true
        }
        return false
    }

    private fun getHttpResource(webResourceRequest: WebResourceRequest): WebResourceResponse? {
        try {
            val url = webResourceRequest.url.toString()
            val requestBuilder =
                Request.Builder().url(url).method(webResourceRequest.method, null)
            val requestHeaders = webResourceRequest.requestHeaders
            if (!requestHeaders.isNullOrEmpty()) {
                requestHeaders.forEach {
                    requestBuilder.addHeader(it.key, it.value)
                }
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val code = response.code
            if (code != 200) {
                return null
            }
            val body = response.body
            if (body != null) {
                val mimeType = response.header(
                    "content-type", body.contentType()?.type
                )
                val encoding = response.header(
                    "content-encoding",
                    "utf-8"
                )
                val responseHeaders = mutableMapOf<String, String>()
                for (header in response.headers) {
                    responseHeaders[header.first] = header.second
                }
                // 解决webView跨域问题
                responseHeaders["Access-Control-Allow-Origin"] = "*"
                responseHeaders["Access-Control-Allow-Headers"] = "X-Requested-With"
                responseHeaders["Access-Control-Allow-Methods"] = "POST, GET, OPTIONS, DELETE"
                responseHeaders["Access-Control-Allow-Credentials"] = "true"
                var message = response.message
                if (message.isBlank()) {
                    message = "OK"
                }
                val resourceResponse =
                    WebResourceResponse(mimeType, encoding, body.byteStream())
                resourceResponse.responseHeaders = responseHeaders
                resourceResponse.setStatusCodeAndReasonPhrase(code, message)
                return resourceResponse
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    //获取url的类型
    private fun getExtensionFromUrl(url: String): String {
        try {
            if (url.isNotBlank() && url != "null") {
                return MimeTypeMap.getFileExtensionFromUrl(url)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }


    private fun getAssetsImage(url: String): WebResourceResponse? {
        if (url.contains(".jpg")) {
            try {
                val inputStream = application.assets.open("ic_launcher.webp")
                return WebResourceResponse(
                    "image/webp",
                    "utf-8", inputStream
                )
            } catch (e: Throwable) {
                log("Throwable: $e")
            }
        }
        return null
    }

}