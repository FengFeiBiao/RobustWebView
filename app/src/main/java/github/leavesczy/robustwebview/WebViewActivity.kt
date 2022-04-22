package github.leavesczy.robustwebview

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import github.leavesczy.robustwebview.base.RobustWebView
import github.leavesczy.robustwebview.base.WebViewCacheHolder
import github.leavesczy.robustwebview.base.WebViewListener
import github.leavesczy.robustwebview.databinding.ActivityWebViewBinding
import github.leavesczy.robustwebview.utils.showToast

/**
 * @Author : FFB
 * @Date : 2022/4/19
 * @Description :
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding

    private val url1 = "https://juejin.cn/user/1767670429521837"
    private val url2 = "https://www1.pcauto.com.cn/auto-c/front-end-projects/app-h5/index.html#/gold-coin-task-rule";//"https://www.bilibili.com/"
    private val url3 = " http://soft.imtt.qq.com/browser/tes/feedback.html"

    private lateinit var webView: RobustWebView

    private val webViewListener = object : WebViewListener {
        override fun onProgressChanged(webView: RobustWebView, progress: Int) {
            binding.tvProgress.text = progress.toString()
        }

        override fun onReceivedTitle(webView: RobustWebView, title: String) {
            binding.tvTitle.text = title
        }

        override fun onPageFinished(webView: RobustWebView, url: String) {
            //url加载结束
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        webView = WebViewCacheHolder.acquireWebViewInternal(this)
        webView.webViewListener = webViewListener
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.apply {
            webViewContainer.addView(webView, layoutParams)
            tvBack.setOnClickListener {
                onBackPressed()
            }
            btnOpenUrl1.setOnClickListener {
                webView.loadUrl(url1)
            }
            btnOpenUrl2.setOnClickListener {
                webView.loadUrl(url2)
            }
            btnOpenUrl3.setOnClickListener {
                webView.loadUrlWithCookie(url3, "")
            }
            btnReload.setOnClickListener {
                webView.reload()
            }
            btnOpenHtml.setOnClickListener {
                webView.loadUrl("""file:/android_asset/javascript.html""")
            }
            btnCallJsByAndroid.setOnClickListener {
                webView.evaluateJavascript("javascript:callJsByAndroid()") {
                    showToast("evaluateJavascript: $it")
                }
            }
            btnShowToastByAndroid.setOnClickListener {
                webView.loadUrl("javascript:showToastByAndroid()")
            }
            btnCallJsPrompt.setOnClickListener {
                webView.loadUrl("javascript:callJsPrompt()")
            }
        }
    }

    override fun onBackPressed() {
        if (webView.toGoBack()) {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebViewCacheHolder.prepareWebView()
    }

}