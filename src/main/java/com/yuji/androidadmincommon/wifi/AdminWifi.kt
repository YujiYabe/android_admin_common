package com.yuji.androidadmincommon.wifi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

private const val DEFAULT_WIFI_TAG = "AndroidAdminWifi"

fun Context.hasFineLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

@Suppress("DEPRECATION")
fun Context.currentWifiSsid(): String? {
    if (!hasFineLocationPermission()) return null
    val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    val ssid = wifiManager?.connectionInfo?.ssid
        ?.trim()
        ?.trim('"')
        .orEmpty()
    return ssid.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID }
}

@Suppress("DEPRECATION", "MissingPermission")
fun Context.availableWifiSsids(logTag: String = DEFAULT_WIFI_TAG): List<String> {
    if (!hasFineLocationPermission()) return emptyList()
    val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return emptyList()

    runCatching {
        Log.d(logTag, "Start WiFi scan for admin SSID picker")
        wifiManager.startScan()
    }.onFailure { throwable ->
        Log.d(logTag, "Failed to start WiFi scan", throwable)
    }

    return wifiManager.scanResults
        .map { it.SSID.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
}

fun requestAdminWifiConnection(
    context: Context,
    ssid: String,
    password: String,
    logTag: String = DEFAULT_WIFI_TAG,
    onStatus: (String) -> Unit,
    onConnected: (String) -> Unit,
): () -> Unit {
    if (ssid.isBlank()) {
        onStatus("SSIDを選択してください。")
        return {}
    }
    if (!context.hasFineLocationPermission()) {
        onStatus("WiFi接続には位置情報権限が必要です。")
        return {}
    }
    if (context.currentWifiSsid() == ssid) {
        Log.d(logTag, "Already connected to requested WiFi: $ssid")
        onConnected("接続OK: $ssid（端末は既にこのWiFiに接続中）")
        return {}
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        onStatus("この端末ではアプリからのWiFi接続リクエストに対応していません。")
        return {}
    }

    val connectivityManager = context.getSystemService(
        Context.CONNECTIVITY_SERVICE,
    ) as ConnectivityManager
    val mainHandler = Handler(Looper.getMainLooper())
    var registered = false
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(logTag, "Admin WiFi test available: $ssid")
            mainHandler.post {
                connectivityManager.bindProcessToNetwork(network)
                onConnected("接続OK: $ssid（このアプリのみ）")
            }
        }

        override fun onUnavailable() {
            Log.d(logTag, "Admin WiFi test unavailable: $ssid")
            mainHandler.post { onStatus("接続できませんでした: $ssid") }
        }

        override fun onLost(network: Network) {
            Log.d(logTag, "Admin WiFi test lost: $ssid")
            mainHandler.post { onStatus("接続が切れました: $ssid") }
        }
    }

    runCatching {
        Log.d(logTag, "Request admin WiFi test: $ssid")
        val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(ssid)
        if (password.isNotBlank()) {
            specifierBuilder.setWpa2Passphrase(password)
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifierBuilder.build())
            .build()
        connectivityManager.requestNetwork(request, callback)
        registered = true
    }.onFailure { throwable ->
        Log.d(logTag, "Failed to request admin WiFi test: $ssid", throwable)
        onStatus(throwable.message ?: "WiFi接続リクエストを開始できませんでした。")
    }

    return {
        Log.d(logTag, "Release admin WiFi test request: $ssid")
        connectivityManager.bindProcessToNetwork(null)
        if (registered) {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }
}
