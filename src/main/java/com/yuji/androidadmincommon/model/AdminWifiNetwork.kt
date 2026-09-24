package com.yuji.androidadmincommon.model

data class AdminWifiNetwork(
    val ssid: String,
    val password: String,
)

data class AdminWifiLabels(
    val title: String = "WiFi設定",
    val noSsid: String = "SSID未選択",
    val fetchSsid: String = "SSID取得",
    val password: String = "WiFiパスワード",
    val passwordOk: String = "WiFiパスワード OK",
    val test: String = "確認",
    val save: String = "保存",
    val saveOrUpdate: String = "保存/更新",
    val savedNetworks: String = "保存済みWiFi",
    val noSavedNetworks: String = "保存済みWiFiはありません",
    val selected: String = "選択中",
    val delete: String = "削除",
)
