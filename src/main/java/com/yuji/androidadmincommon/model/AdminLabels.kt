package com.yuji.androidadmincommon.model

data class AdminLoginLabels(
    val title: String = "管理画面ログイン",
    val passwordLabel: String = "管理パスワード",
    val loginButton: String = "ログイン",
    val backButton: String = "戻る",
)

data class AdminUnlockLabels(
    val title: String = "パスワードを入力してください",
    val passwordLabel: String = "パスワード",
    val unlockButton: String = "開く",
)

