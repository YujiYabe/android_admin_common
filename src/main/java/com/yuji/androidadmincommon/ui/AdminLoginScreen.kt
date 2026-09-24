package com.yuji.androidadmincommon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuji.androidadmincommon.model.AdminLoginLabels

@Composable
fun AdminLoginScreen(
    password: String,
    errorMessage: String?,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    labels: AdminLoginLabels = AdminLoginLabels(),
    maxPasswordLength: Int = 32,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AdminShapes.card,
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = labels.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                AdminPinDisplay(
                    password = password,
                    label = labels.passwordLabel,
                    isError = errorMessage != null,
                    errorMessage = errorMessage,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onLogin) {
                        Text(labels.loginButton)
                    }
                    OutlinedButton(onClick = onBack) {
                        Text(labels.backButton)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
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
    }
}
