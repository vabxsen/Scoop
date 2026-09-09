package com.scoop.app.ui.screen.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.scoop.app.R
import com.scoop.app.extractor.InstagramSession
import com.scoop.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled") // Instagram's own authentication page requires JavaScript. No JS bridge is installed.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstagramSignInScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasSession by remember { mutableStateOf(InstagramSession.hasSession()) }
    var showBrowser by remember { mutableStateOf(!hasSession) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pageOrigin by remember { mutableStateOf("https://www.instagram.com") }
    var progress by remember { mutableFloatStateOf(0f) }
    var browserGeneration by remember { mutableIntStateOf(0) }
    var browser by remember { mutableStateOf<WebView?>(null) }
    val blockedMessage = stringResource(R.string.instagram_navigation_blocked)
    val loadError = stringResource(R.string.instagram_load_error)

    fun checkSession() { hasSession = InstagramSession.hasSession() }
    fun goBack() {
        if (showBrowser && browser?.canGoBack() == true) browser?.goBack() else onBack()
    }
    BackHandler(onBack = ::goBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.instagram_title)) },
                navigationIcon = {
                    IconButton(onClick = ::goBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Text(
                stringResource(R.string.instagram_session_explanation),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Spacing.md),
            )
            if (showBrowser) {
                // Native origin label remains visible above the page, including redirects.
                Text(pageOrigin, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = Spacing.md))
                if (progress < 1f) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                key(browserGeneration) {
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onReset = null,
                    onRelease = { view ->
                        if (browser === view) {
                            view.stopLoading()
                            view.destroy()
                            browser = null
                        }
                    },
                    factory = {
                        WebView(context).apply {
                            browser = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, value: Int) { progress = value / 100f }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    if (!request.isForMainFrame) return false
                                    if (InstagramSession.isInstagramPage(request.url.toString())) return false
                                    error = blockedMessage
                                    return true
                                }
                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    error = null
                                    if (url != null && InstagramSession.isInstagramPage(url)) pageOrigin = "https://${android.net.Uri.parse(url).host}"
                                    progress = 0f
                                }
                                override fun onPageFinished(view: WebView, url: String?) { checkSession() }
                                override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                                    if (request.isForMainFrame) error = loadError
                                }
                            }
                            // HTTPS/TLS errors retain WebView's default cancellation behavior.
                            loadUrl(InstagramSession.LOGIN_URL)
                        }
                    },
                )
                }
            } else {
                Text(stringResource(R.string.instagram_session_saved), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(Spacing.md))
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Spacing.md).semantics { liveRegion = LiveRegionMode.Polite })
                TextButton(onClick = { error = null; browser?.loadUrl(InstagramSession.LOGIN_URL) }) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            Row(Modifier.fillMaxWidth().padding(Spacing.md)) {
                if (hasSession) {
                    TextButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            try {
                                browser?.stopLoading()
                                browser?.clearCache(true)
                                browser?.destroy()
                                browser = null
                                showBrowser = false
                                browserGeneration++
                                InstagramSession.disconnect()
                                hasSession = false
                                showBrowser = true
                            } finally { busy = false }
                        }
                    }) { Text(stringResource(R.string.instagram_disconnect)) }
                }
                Button(modifier = Modifier.weight(1f), enabled = !busy, onClick = {
                    checkSession()
                    if (hasSession) {
                        busy = true
                        scope.launch {
                            try { InstagramSession.persist(); onDone() } finally { busy = false }
                        }
                    } else {
                        error = context.getString(R.string.instagram_finish_sign_in)
                    }
                }) { Text(stringResource(R.string.instagram_use_session)) }
            }
        }
    }
}
