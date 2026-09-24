package com.yuji.androidadmincommon.model

data class YoutubeWifiStatus(
    val message: String,
    val isReady: Boolean,
)

data class YoutubePlayerLabels(
    val back: String = "戻る",
    val remainingPrefix: String = "残り",
    val preparing: String = "YouTubeを開く準備をしています",
    val wifiConnectionAlert: String = "このアプリ内にある管理画面でWiFiの接続をしてください",
    val ok: String = "OK",
)

