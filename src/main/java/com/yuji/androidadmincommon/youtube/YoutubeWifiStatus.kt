package com.yuji.androidadmincommon.youtube

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yuji.androidadmincommon.model.AdminWifiNetwork
import com.yuji.androidadmincommon.model.YoutubeWifiStatus
import com.yuji.androidadmincommon.wifi.currentWifiSsid

@Composable
fun rememberYoutubeWifiStatus(
    wifiCandidates: List<AdminWifiNetwork>,
    hasWifiPermission: Boolean,
    logTag: String = "AndroidAdminWifi",
    failureMessage: String = "保存済みWiFiに接続できませんでした。",
): YoutubeWifiStatus {
    val context = LocalContext.current
    val normalizedCandidates = remember(wifiCandidates) {
        wifiCandidates
            .filter { it.ssid.isNotBlank() }
            .distinctBy { it.ssid }
    }
    var status by remember(normalizedCandidates) {
        mutableStateOf(
            if (normalizedCandidates.isEmpty()) {
                YoutubeWifiStatus("YouTube WiFiが未設定です。現在のネットワークで開きます。", isReady = true)
            } else {
                YoutubeWifiStatus("このアプリ用WiFi接続リクエスト中: ${normalizedCandidates.first().ssid}", isReady = false)
            },
        )
    }

    DisposableEffect(normalizedCandidates, hasWifiPermission) {
        if (normalizedCandidates.isEmpty()) {
            status = YoutubeWifiStatus("YouTube WiFiが未設定です。現在のネットワークで開きます。", isReady = true)
            onDispose { }
        } else if (!hasWifiPermission) {
            status = YoutubeWifiStatus("WiFi接続には位置情報権限が必要です。", isReady = false)
            onDispose { }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            status = YoutubeWifiStatus("この端末ではアプリからのWiFi接続リクエストに対応していません。", isReady = false)
            onDispose { }
        } else {
            val connectivityManager = context.getSystemService(
                Context.CONNECTIVITY_SERVICE,
            ) as ConnectivityManager
            val mainHandler = Handler(Looper.getMainLooper())
            val registeredCallbacks = mutableListOf<ConnectivityManager.NetworkCallback>()
            var disposed = false

            fun unregister(callback: ConnectivityManager.NetworkCallback) {
                if (registeredCallbacks.remove(callback)) {
                    runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                }
            }

            fun tryWifi(index: Int) {
                if (disposed) return
                val candidate = normalizedCandidates.getOrNull(index)
                if (candidate == null) {
                    Log.d(logTag, "All YouTube WiFi candidates unavailable")
                    connectivityManager.bindProcessToNetwork(null)
                    status = YoutubeWifiStatus(failureMessage, isReady = false)
                    return
                }

                val ssid = candidate.ssid
                val attemptText = if (normalizedCandidates.size == 1) {
                    ssid
                } else {
                    "$ssid (${index + 1}/${normalizedCandidates.size})"
                }

                if (context.currentWifiSsid() == ssid) {
                    Log.d(logTag, "Already connected to YouTube WiFi: $ssid")
                    status = YoutubeWifiStatus("WiFi接続中: $ssid（端末は既にこのWiFiに接続中）", isReady = true)
                    return
                }

                status = YoutubeWifiStatus("このアプリ用WiFi接続リクエスト中: $attemptText", isReady = false)
                lateinit var callback: ConnectivityManager.NetworkCallback
                callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(logTag, "YouTube WiFi available: $ssid")
                        connectivityManager.bindProcessToNetwork(network)
                        mainHandler.post {
                            status = YoutubeWifiStatus("WiFi接続中: $ssid（このアプリのみ）", isReady = true)
                        }
                    }

                    override fun onUnavailable() {
                        Log.d(logTag, "YouTube WiFi unavailable: $ssid")
                        mainHandler.post {
                            unregister(callback)
                            status = YoutubeWifiStatus("接続できませんでした: $ssid。次のWiFiを試します。", isReady = false)
                            tryWifi(index + 1)
                        }
                    }

                    override fun onLost(network: Network) {
                        Log.d(logTag, "YouTube WiFi lost: $ssid")
                        mainHandler.post {
                            unregister(callback)
                            connectivityManager.bindProcessToNetwork(null)
                            status = YoutubeWifiStatus("接続が切れました: $ssid。次のWiFiを試します。", isReady = false)
                            tryWifi(index + 1)
                        }
                    }
                }

                runCatching {
                    Log.d(logTag, "Request YouTube WiFi: $ssid")
                    val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(ssid)
                    if (candidate.password.isNotBlank()) {
                        specifierBuilder.setWpa2Passphrase(candidate.password)
                    }
                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifierBuilder.build())
                        .build()
                    connectivityManager.requestNetwork(request, callback)
                    registeredCallbacks.add(callback)
                }.onFailure { throwable ->
                    Log.d(logTag, "Failed to request YouTube WiFi: $ssid", throwable)
                    status = YoutubeWifiStatus(
                        "接続リクエストに失敗しました: $ssid。次のWiFiを試します。",
                        isReady = false,
                    )
                    tryWifi(index + 1)
                }
            }

            tryWifi(0)

            onDispose {
                disposed = true
                Log.d(logTag, "Release YouTube WiFi requests")
                connectivityManager.bindProcessToNetwork(null)
                registeredCallbacks.toList().forEach(::unregister)
            }
        }
    }

    return status
}
