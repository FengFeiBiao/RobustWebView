package github.leavesczy.robustwebview.base

import android.app.Application
import android.content.Context
import com.tencent.mmkv.MMKV
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsDownloader
import com.tencent.smtt.sdk.TbsListener
import github.leavesczy.robustwebview.utils.log
import github.leavesczy.robustwebview.utils.showToast

/**
 * @Author : FFB
 * @Date : 2022/4/19
 * @Description :
 */
object WebViewInitTask {

    fun init(application: Application) {
        this.application = application
        initWebView(application)
        WebViewCacheHolder.init(application)
        WebViewInterceptRequestProxy.init(application)
    }

    private lateinit var application: Application
    private val mmkv = MMKV.defaultMMKV()
    private const val X5_INIT_KEY = "init_key"

    private var isInit = false  //内核初始化是否成功
    private var time = 0        //内核下载重试次数

    /**
     * TBS腾讯浏览服务：https://x5.tencent.com/docs/access.html
     */
    private fun initWebView(context: Context) {
        initQbSdk()
        QbSdk.setTbsListener(object : TbsListener {
            override fun onDownloadFinish(code: Int) {
                log("QbSdk onDownloadFinish -->下载X5内核code=：$code")
                if (code != 100 && time < 3) {
                    time++
                    resetQbSdk()
                }
            }

            override fun onInstallFinish(p0: Int) {
                isInit = true
                log("QbSdk onInstallFinish -->安装X5内核进度：$p0")
            }

            override fun onDownloadProgress(p0: Int) {
                log("QbSdk onDownloadProgress -->下载X5内核进度：$p0")
            }
        })
        val cb: QbSdk.PreInitCallback = object : QbSdk.PreInitCallback {
            override fun onViewInitFinished(isX5: Boolean) {
                //x5內核初始化完成的回调，true表X5内核加载成功，否则表加载失败，会自动切换到系统内核。
                showToast("onViewInitFinished: $isX5")
                log("QbSdk x5內核初始化完成的回调是否成功: $isX5")
                isInit = isX5
                if (!isInit && TbsDownloader.needDownload(context, false) && !TbsDownloader.isDownloading()) {
                    resetQbSdk()
                }
                mmkv.encode(X5_INIT_KEY, isInit)
            }

            override fun onCoreInitFinished() {
                log("onCoreInitFinished")
            }
        }
        //x5内核初始化接口
        QbSdk.initX5Environment(context, cb)
    }

    fun resetQbSdk(): Boolean {
        log("是否可以加载X5内核=" + QbSdk.canLoadX5(application))
        if (mmkv.decodeBool(X5_INIT_KEY, false) && QbSdk.canLoadX5(application)) {
            log("是否已经加载下载过X5内核且可用=" + QbSdk.canLoadX5(application))
            return isInit
        }
        if (!isInit && !TbsDownloader.isDownloading()) {
            initQbSdk(true)
        }
        return isInit
    }

    private fun initQbSdk(reset: Boolean = false) {
        if (reset) QbSdk.reset(application)
        //（可选）为了提高内核占比，在初始化前可配置允许移动网络下载内核（大小 40-50 MB）。默认移动网络不下载
        QbSdk.setDownloadWithoutWifi(true)
        val map = mutableMapOf<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_PRIVATE_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        QbSdk.initTbsSettings(map)
        if (reset) TbsDownloader.startDownload(application)
    }

}