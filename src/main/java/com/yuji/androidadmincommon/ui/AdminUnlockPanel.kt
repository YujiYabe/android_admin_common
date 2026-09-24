package com.yuji.androidadmincommon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuji.androidadmincommon.model.AdminUnlockLabels

@Composable
fun AdminUnlockPanel(
    password: String,
    errorMessage: String?,
    onPasswordChange: (String) -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    labels: AdminUnlockLabels = AdminUnlockLabels(),
    maxPasswordLength: Int = 32,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AdminShapes.card,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = labels.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            AdminPinDisplay(
                password = password,
                label = labels.passwordLabel,
                isError = errorMessage != null,
                errorMessage = errorMessage,
            )
            AdminNumericKeypad(
                onDigit = { digit ->
                    if (password.length < maxPasswordLength) {
                        onPasswordChange(password + digit)
                    }
                },
                onBackspace = {
                    if (password.isNotEmpty()) {
                        onPasswordChange(password.dropLast(1))
                    }
                },
                onClear = { onPasswordChange("") },
            )
            Button(
                onClick = onUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AdminShapes.card,
            ) {
                Text(text = labels.unlockButton, fontWeight = FontWeight.Bold)
            }
        }
    }
}
