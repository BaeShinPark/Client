package com.cse.coari.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import com.cse.coari.R
import kotlinx.android.synthetic.main.activity_curriculum.*

@Suppress("DEPRECATION")
class CurriculumActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_curriculum)

        curr_webview.apply {
            webViewClient = WebViewClientClass()

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                    val newWebView = WebView(this@CurriculumActivity).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                    }

                    val dialog = Dialog(this@CurriculumActivity).apply{
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
            settings.javaScriptCanOpenWindowsAutomatically = true   // ?????????????????? ???????????????
            settings.loadWithOverviewMode = true    // ???????????? ????????????
            settings.useWideViewPort = true // ?????? ????????? ????????? ????????????
            settings.setSupportZoom(true)   // ?????? ??? ??????
            settings.builtInZoomControls = true // ?????? ?????? ?????? ????????????
            settings.cacheMode = WebSettings.LOAD_NO_CACHE  // ???????????? ?????? ????????????
            settings.domStorageEnabled = true   // ?????? ????????? ????????????
            settings.displayZoomControls = true // pdf??? ??? ??????
            settings.allowContentAccess = true  // pdf ??? ????????? ?????? ??????
            settings.setGeolocationEnabled(true)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                settings.safeBrowsingEnabled = true // api 26
            }
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowUniversalAccessFromFileURLs = true
            settings.allowFileAccess = true
            fitsSystemWindows = true
        }


        curr_webview.loadUrl(URL)
    }

    // ??? ????????? ??????????????? ????????? ???, ??????????????? ??????????????? ?????? ??????.
    inner class WebViewClientClass: WebViewClient(){
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if(url != null)     view?.loadUrl(url)
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            curr_progressbar.visibility = ProgressBar.VISIBLE
            curr_webview.visibility = View.GONE
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            curr_progressbar.visibility = ProgressBar.GONE
            curr_webview.visibility = View.VISIBLE
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this@CurriculumActivity)
            var message = "SSL Certificate error"
            when(error?.primaryError) {
                SslError.SSL_UNTRUSTED  -> message = "The certificate authority is not trusted."
                SslError.SSL_EXPIRED    -> message = "The certificate has expired."
                SslError.SSL_IDMISMATCH -> message = "The certificate Hostname mismatch."
                SslError.SSL_NOTYETVALID-> message = "The certificate is not yet valid"
            }
            message += " Do you want to continue anyway?"
            builder.setTitle("SSL Certificate Error")
            builder.setMessage(message)
            builder.setPositiveButton("continue"
            ) { _, _ ->
                handler?.proceed()
            }
            builder.setNegativeButton("cancel"
            ) { _, _ ->
                handler?.cancel()
            }
            val dialog: android.app.AlertDialog? = builder.create()
            dialog?.show()
        }
    }

    companion object{
        const val URL = "https://cse.deu.ac.kr/2_6_abeek.php"
        const val TAG = "CurriculumActivity"
    }
}