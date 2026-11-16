package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val DEFAULT_BANNER_URL = "https://zumdewisk.github.io/NYNYNY/banner.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebBannerAd(
    modifier: Modifier = Modifier,
    url: String = DEFAULT_BANNER_URL
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("web_banner_ad"),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUri = request?.url ?: return false
                        val targetStr = targetUri.toString()
                        if (request.isForMainFrame && !targetStr.startsWith(url)) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                            return true
                        }
                        return false
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url && webView.originalUrl != url) {
                webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        }
    )
}
