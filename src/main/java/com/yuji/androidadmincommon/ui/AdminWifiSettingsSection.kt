package com.yuji.androidadmincommon.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuji.androidadmincommon.model.AdminWifiLabels
import com.yuji.androidadmincommon.model.AdminWifiNetwork
import com.yuji.androidadmincommon.wifi.availableWifiSsids
import com.yuji.androidadmincommon.wifi.currentWifiSsid
import com.yuji.androidadmincommon.wifi.hasFineLocationPermission
import com.yuji.androidadmincommon.wifi.requestAdminWifiConnection

@Composable
fun AdminWifiSettingsSection(
    selectedSsid: String,
    selectedPassword: String,
    savedNetworks: List<AdminWifiNetwork>,
    onSelectedNetworkChange: (AdminWifiNetwork) -> Unit,
    onSaveNetwork: (AdminWifiNetwork) -> Unit,
    onDeleteNetwork: (AdminWifiNetwork) -> Unit,
    modifier: Modifier = Modifier,
    labels: AdminWifiLabels = AdminWifiLabels(),
    logTag: String = "AndroidAdminWifi",
    useCardContainer: Boolean = true,
    saveButtonText: String = labels.save,
    showSavedCountWhenEmpty: Boolean = false,
    passwordMaxLength: Int = 64,
) {
    AdminWifiSettingsContent(
        selectedSsid = selectedSsid,
        selectedPassword = selectedPassword,
        savedNetworks = savedNetworks,
        onSelectedNetworkChange = onSelectedNetworkChange,
        onSaveNetwork = onSaveNetwork,
        onDeleteNetwork = onDeleteNetwork,
        modifier = modifier,
        labels = labels,
        logTag = logTag,
        useCardContainer = useCardContainer,
        saveButtonText = saveButtonText,
        showSavedCountWhenEmpty = showSavedCountWhenEmpty,
        passwordMaxLength = passwordMaxLength,
    )
}

@Composable
private fun AdminWifiSettingsContent(
    selectedSsid: String,
    selectedPassword: String,
    savedNetworks: List<AdminWifiNetwork>,
    onSelectedNetworkChange: (AdminWifiNetwork) -> Unit,
    onSaveNetwork: (AdminWifiNetwork) -> Unit,
    onDeleteNetwork: (AdminWifiNetwork) -> Unit,
    modifier: Modifier,
    labels: AdminWifiLabels,
    logTag: String,
    useCardContainer: Boolean,
    saveButtonText: String,
    showSavedCountWhenEmpty: Boolean,
    passwordMaxLength: Int,
) {
    val context = LocalContext.current
    var hasWifiPermission by remember {
        mutableStateOf(context.hasFineLocationPermission())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasWifiPermission = granted
    }
    val connectedWifiSsid = remember(hasWifiPermission) {
        if (hasWifiPermission) context.currentWifiSsid().orEmpty() else ""
    }
    val selectedSsidIsSaved = savedNetworks.any { it.ssid == selectedSsid }
    var wifiSsid by remember(selectedSsid, connectedWifiSsid, savedNetworks) {
        val initialSsid = if (selectedSsidIsSaved) {
            ""
        } else {
            selectedSsid.ifBlank { connectedWifiSsid }
        }
        mutableStateOf(initialSsid)
    }
    var wifiPassword by remember(selectedSsid, selectedPassword, savedNetworks) {
        mutableStateOf(if (selectedSsidIsSaved) "" else selectedPassword)
    }
    var wifiSsidOptions by remember { mutableStateOf(emptyList<String>()) }
    var showWifiSsidDialog by remember { mutableStateOf(false) }
    var wifiSettingsMessage by remember { mutableStateOf<String?>(null) }
    var wifiPasswordConfirmed by remember { mutableStateOf(false) }
    var wifiTestTarget by remember { mutableStateOf<AdminWifiNetwork?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminWifiNetwork?>(null) }

    fun updateSelected(network: AdminWifiNetwork, message: String? = null) {
        wifiSsid = network.ssid
        wifiPassword = network.password
        wifiPasswordConfirmed = false
        onSelectedNetworkChange(network)
        if (message != null) wifiSettingsMessage = message
    }

    fun selectSavedNetwork(network: AdminWifiNetwork, message: String? = null) {
        wifiSsid = ""
        wifiPassword = ""
        wifiPasswordConfirmed = false
        onSelectedNetworkChange(network)
        if (message != null) wifiSettingsMessage = message
    }

    LaunchedEffect(hasWifiPermission, connectedWifiSsid) {
        if (!hasWifiPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (selectedSsid.isBlank() && connectedWifiSsid.isNotBlank()) {
            val saved = savedNetworks.firstOrNull { it.ssid == connectedWifiSsid }
            if (saved == null) {
                updateSelected(AdminWifiNetwork(connectedWifiSsid, selectedPassword))
            } else {
                selectSavedNetwork(saved)
            }
        }
    }

    wifiTestTarget?.let { target ->
        DisposableEffect(target) {
            wifiSettingsMessage = "接続確認中: ${target.ssid}"
            val cancel = requestAdminWifiConnection(
                context = context,
                ssid = target.ssid,
                password = target.password,
                logTag = logTag,
                onStatus = { message ->
                    wifiSettingsMessage = message
                    wifiPasswordConfirmed = false
                    wifiTestTarget = null
                },
                onConnected = { message ->
                    wifiSettingsMessage = message
                    wifiPasswordConfirmed = true
                    onSaveNetwork(target)
                    wifiSsid = ""
                    wifiPassword = ""
                    wifiTestTarget = null
                },
            )
            onDispose { cancel() }
        }
    }

    if (showWifiSsidDialog) {
        AlertDialog(
            onDismissRequest = { showWifiSsidDialog = false },
            title = { Text(text = "SSIDを選択") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (wifiSsidOptions.isEmpty()) {
                        item { Text(text = "SSIDが見つかりませんでした。") }
                    } else {
                        items(wifiSsidOptions) { ssid ->
                            OutlinedButton(
                                onClick = {
                                    val saved = savedNetworks.firstOrNull { it.ssid == ssid }
                                    if (saved == null) {
                                        updateSelected(AdminWifiNetwork(ssid, wifiPassword))
                                    } else {
                                        selectSavedNetwork(saved, "WiFi設定を選択しました: ${saved.ssid}")
                                    }
                                    showWifiSsidDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AdminShapes.card,
                            ) {
                                Text(
                                    text = ssid,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWifiSsidDialog = false }) {
                    Text(text = "閉じる")
                }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(text = "保存済みWiFiを削除") },
            text = {
                Text(text = "${target.ssid} を削除しますか？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNetwork(target)
                        if (wifiSsid == target.ssid || selectedSsid == target.ssid) {
                            selectSavedNetwork(AdminWifiNetwork("", ""))
                        }
                        wifiSettingsMessage = "保存済みWiFiを削除しました: ${target.ssid}"
                        deleteTarget = null
                    },
                    shape = AdminShapes.card,
                ) {
                    Text(text = "削除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(text = "キャンセル")
                }
            },
        )
    }

    val content: @Composable () -> Unit = {
        Column(
            modifier = if (useCardContainer) Modifier.padding(24.dp) else Modifier,
            verticalArrangement = Arrangement.spacedBy(if (useCardContainer) 20.dp else 12.dp),
        ) {
            Text(
                text = labels.title,
                style = if (useCardContainer) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = if (useCardContainer) FontWeight.SemiBold else FontWeight.Black,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AdminShapes.card,
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (useCardContainer) 12.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (wifiSsid.isBlank()) labels.noSsid else wifiSsid,
                            modifier = Modifier.weight(0.9f),
                            color = if (wifiSsid.isBlank()) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        OutlinedButton(
                            onClick = {
                                if (!hasWifiPermission) {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    wifiSettingsMessage = "SSID取得には位置情報権限が必要です。"
                                } else {
                                    wifiSsidOptions = context.availableWifiSsids(logTag)
                                    showWifiSsidDialog = true
                                    wifiSettingsMessage = if (wifiSsidOptions.isEmpty()) {
                                        "SSIDを取得できませんでした。端末のWiFi設定と位置情報を確認してください。"
                                    } else {
                                        "${wifiSsidOptions.size}件のSSIDを取得しました。"
                                    }
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = AdminShapes.card,
                        ) {
                            Text(text = labels.fetchSsid, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = {
                                wifiPassword = it.take(passwordMaxLength)
                                wifiPasswordConfirmed = false
                                onSelectedNetworkChange(AdminWifiNetwork(wifiSsid, wifiPassword))
                            },
                            modifier = Modifier.weight(if (useCardContainer) 1.15f else 1f),
                            singleLine = true,
                            label = { Text(if (wifiPasswordConfirmed) labels.passwordOk else labels.password) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        OutlinedButton(
                            onClick = {
                                if (wifiSsid.isBlank()) {
                                    wifiSettingsMessage = "SSIDを選択してください。"
                                } else if (!hasWifiPermission) {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    wifiSettingsMessage = "接続確認には位置情報権限が必要です。"
                                } else {
                                    wifiPasswordConfirmed = false
                                    wifiTestTarget = AdminWifiNetwork(wifiSsid, wifiPassword)
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = AdminShapes.card,
                        ) {
                            Text(text = labels.test, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (wifiSsid.isBlank()) {
                                    wifiSettingsMessage = "SSIDを選択してください。"
                                } else {
                                    val network = AdminWifiNetwork(wifiSsid.trim(), wifiPassword)
                                    onSaveNetwork(network)
                                    wifiSsid = ""
                                    wifiPassword = ""
                                    wifiSettingsMessage = "WiFi設定を保存しました。"
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = AdminShapes.card,
                        ) {
                            Text(text = saveButtonText, fontWeight = FontWeight.Bold)
                        }
                    }
                    wifiSettingsMessage?.let { message ->
                        Text(
                            text = message,
                            color = if (wifiPasswordConfirmed) Color(0xFF047857) else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    SavedWifiNetworks(
                        selectedSsid = selectedSsid.ifBlank { wifiSsid },
                        savedNetworks = savedNetworks,
                        labels = labels,
                        showSavedCountWhenEmpty = showSavedCountWhenEmpty,
                        onSelect = { network ->
                            selectSavedNetwork(network, "WiFi設定を選択しました: ${network.ssid}")
                        },
                        onDelete = { network ->
                            deleteTarget = network
                        },
                    )
                }
            }
        }
    }

    if (useCardContainer) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = AdminShapes.card,
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F1F3)),
        ) {
            content()
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SavedWifiNetworks(
    selectedSsid: String,
    savedNetworks: List<AdminWifiNetwork>,
    labels: AdminWifiLabels,
    showSavedCountWhenEmpty: Boolean,
    onSelect: (AdminWifiNetwork) -> Unit,
    onDelete: (AdminWifiNetwork) -> Unit,
) {
    if (savedNetworks.isEmpty()) {
        if (showSavedCountWhenEmpty) {
            Text(
                text = labels.noSavedNetworks,
                color = Color(0xFF4B5563),
                fontWeight = FontWeight.Bold,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${labels.savedNetworks} ${savedNetworks.size}件",
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
        )
        savedNetworks.forEach { network ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { onSelect(network) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = AdminShapes.card,
                ) {
                    Text(
                        text = network.ssid,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (network.ssid == selectedSsid) labels.selected else "",
                    modifier = Modifier.width(64.dp),
                    color = Color(0xFF047857),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(
                    onClick = { onDelete(network) },
                    modifier = Modifier.height(48.dp),
                    shape = AdminShapes.card,
                ) {
                    Text(text = labels.delete, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
