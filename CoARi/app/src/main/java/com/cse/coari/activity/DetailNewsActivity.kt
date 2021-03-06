package com.cse.coari.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.ViewGroup
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.cse.coari.R
import com.cse.coari.helper.WebViewClientClass
import kotlinx.android.synthetic.main.activity_detail_news.*

@Suppress("DEPRECATION")
class DetailNewsActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_news)

        news_webview.apply {
            webViewClient = WebViewClientClass(context, news_webview, news_progressbar )

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                    val newWebView = WebView(this@DetailNewsActivity).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                    }

                    val dialog = Dialog(this@DetailNewsActivity).apply{
                        setContentView(newWebView)
                        window!!.attributes.width   = ViewGroup.LayoutParams.MATCH_PARENT
                        window!!.attributes.height  = ViewGroup.LayoutParams.MATCH_PARENT
                        show()
                    }

                    newWebView.webChromeClient = object : WebChromeClient() {
                        override fun onCloseWindow(window: WebView?) {
                            dialog.dismiss()
                        }
                    }

                    (resultMsg?.obj as WebView.WebViewTransport).webView = newWebView
                    resultMsg.sendToTarget()
                    return true
                }
            }

            settings.javaScriptEnabled = true
            settings.setSupportMultipleWindows(true)    // ?????? ?????????
            settings.javaScriptCanOpenWindowsAutomatically = true   // ?????????????????? ??????????????????
            settings.loadWithOverviewMode = true    // ???????????? ????????????
            settings.useWideViewPort = true // ?????? ????????? ????????? ????????????
            settings.setSupportZoom(true)   // ?????? ??? ??????
            settings.builtInZoomControls = true // ?????? ?????? ?????? ????????????
            settings.cacheMode = WebSettings.LOAD_NO_CACHE  // ???????????? ?????? ????????????
            settings.domStorageEnabled = true   // ?????? ????????? ????????????
            settings.displayZoomControls = true

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                settings.safeBrowsingEnabled = true // api 26
            }
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowContentAccess = true
            settings.setGeolocationEnabled(true)
            settings.allowUniversalAccessFromFileURLs = true
            settings.allowFileAccess = true
            fitsSystemWindows = true
        }

        val url = intent.getSerializableExtra("data") as String
        news_webview.loadUrl(url)
    }

}