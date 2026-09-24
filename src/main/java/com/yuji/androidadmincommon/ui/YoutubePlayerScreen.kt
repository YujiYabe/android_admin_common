package com.yuji.androidadmincommon.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yuji.androidadmincommon.model.AdminWifiNetwork
import com.yuji.androidadmincommon.model.YoutubePlayerLabels
import com.yuji.androidadmincommon.wifi.hasFineLocationPermission
import com.yuji.androidadmincommon.youtube.rememberYoutubeWifiStatus
import kotlinx.coroutines.delay

private const val YOUTUBE_PROGRESS_BRIDGE_NAME = "AndroidAdminYoutubeProgress"
private const val YOUTUBE_WIFI_CONNECTION_TIMEOUT_MILLIS = 10_000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubePlayerScreen(
    youtubeUrl: String,
    remainingText: String,
    wifiCandidates: List<AdminWifiNetwork>,
    onBack: () -> Unit,
    onChargeableChanged: (Boolean) -> Unit,
    onPlaybackProgress: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    labels: YoutubePlayerLabels = YoutubePlayerLabels(),
    logTag: String = "AndroidAdminWifi",
    showWifiStatusInHeader: Boolean = false,
    closeOnLifecycleStop: Boolean = true,
    headerBackground: Color = Color.White,
    remainingColor: Color = Color(0xFFFF0033),
    preparingTextColor: Color = MaterialTheme.colorScheme.secondary,
    failureMessage: String = "保存済みWiFiに接続できませんでした。",
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var hasWifiPermission by remember(wifiCandidates) {
        mutableStateOf(context.hasFineLocationPermission())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasWifiPermission = granted
    }
    val hasConfiguredWifi = wifiCandidates.any { it.ssid.isNotBlank() }

    LaunchedEffect(wifiCandidates, hasWifiPermission) {
        if (
            hasConfiguredWifi &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasWifiPermission
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val wifiStatus = rememberYoutubeWifiStatus(
        wifiCandidates = wifiCandidates,
        hasWifiPermission = hasWifiPermission,
        logTag = logTag,
        failureMessage = failureMessage,
    )
    var isYoutubePageLoaded by remember { mutableStateOf(false) }
    var showWifiConnectionAlert by remember { mutableStateOf(false) }

    LaunchedEffect(wifiStatus.isReady) {
        isYoutubePageLoaded = false
        showWifiConnectionAlert = false
        if (!wifiStatus.isReady) {
            onChargeableChanged(false)
        }
    }
    LaunchedEffect(hasConfiguredWifi, wifiStatus.isReady) {
        if (hasConfiguredWifi && !wifiStatus.isReady) {
            delay(YOUTUBE_WIFI_CONNECTION_TIMEOUT_MILLIS)
            showWifiConnectionAlert = true
        }
    }
    LaunchedEffect(wifiStatus.isReady, isYoutubePageLoaded) {
        onChargeableChanged(wifiStatus.isReady && isYoutubePageLoaded)
    }
    DisposableEffect(Unit) {
        onDispose {
            onChargeableChanged(false)
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
    DisposableEffect(lifecycleOwner, closeOnLifecycleStop) {
        if (!closeOnLifecycleStop) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    onBack()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    if (showWifiConnectionAlert) {
        AlertDialog(
            onDismissRequest = { showWifiConnectionAlert = false },
            text = { Text(text = labels.wifiConnectionAlert) },
            confirmButton = {
                TextButton(onClick = { showWifiConnectionAlert = false }) {
                    Text(text = labels.ok)
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBackground)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = AdminShapes.card,
            ) {
                Text(text = labels.back, fontWeight = FontWeight.Bold)
            }
            if (showWifiStatusInHeader) {
                Text(
                    text = wifiStatus.message,
                    modifier = Modifier.weight(1f),
                    color = if (wifiStatus.isReady) Color(0xFF065F46) else Color(0xFF9A3412),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            Text(
                text = "${labels.remainingPrefix} $remainingText",
                color = remainingColor,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
            )
        }

        if (wifiStatus.isReady) {
            AndroidView(
                factory = { androidContext ->
                    WebView(androidContext).apply {
                        val mainHandler = Handler(Looper.getMainLooper())
                        addJavascriptInterface(
                            YoutubePlaybackProgressBridge(
                                mainHandler = mainHandler,
                                onProgress = onPlaybackProgress,
                            ),
                            YOUTUBE_PROGRESS_BRIDGE_NAME,
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isYoutubePageLoaded = true
                                view?.injectYoutubeProgressReporter()
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        loadUrl(youtubeUrl)
                        webView = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labels.preparing,
                    color = preparingTextColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private class YoutubePlaybackProgressBridge(
    private val mainHandler: Handler,
    private val onProgress: (String, Int) -> Unit,
) {
    @JavascriptInterface
    fun save(url: String?, seconds: Double) {
        val safeUrl = url.orEmpty()
        val safeSeconds = seconds.toInt().coerceAtLeast(0)
        if (safeUrl.isBlank()) return
        mainHandler.post {
            onProgress(safeUrl, safeSeconds)
        }
    }
}

private fun WebView.injectYoutubeProgressReporter() {
    evaluateJavascript(
        """
        (function() {
          if (window.__androidAdminYoutubeProgressInstalled) return;
          window.__androidAdminYoutubeProgressInstalled = true;
          function reportProgress() {
            try {
              var video = document.querySelector('video');
              if (!video || !isFinite(video.currentTime)) return;
              $YOUTUBE_PROGRESS_BRIDGE_NAME.save(window.location.href, Math.floor(video.currentTime));
            } catch (e) {}
          }
          setInterval(reportProgress, 2000);
          window.addEventListener('pagehide', reportProgress);
          document.addEventListener('visibilitychange', reportProgress);
          reportProgress();
        })();
        """.trimIndent(),
        null,
    )
}
