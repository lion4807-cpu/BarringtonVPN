package com.barringtonvpn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Введите токен подписки (sub_id) и нажми \"Проверить\".") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BarringtonVPN", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Токен подписки") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                loading = true
                status = "Запрашиваю..."
                scope.launch {
                    val result = AccountApi.fetch(token)
                    loading = false
                    status = result.fold(
                        onSuccess = { info ->
                            buildString {
                                appendLine("Пользователь: ${info.username}")
                                appendLine("Ключ: ${info.keyName}")
                                appendLine("Истекает: ${info.expiresAt}")
                                appendLine("Осталось дней: ${info.daysLeft}")
                                appendLine("Трафик: ${info.trafficUsedGb} / ${info.trafficLimitGb} ГБ")
                                appendLine("Устройства: ${info.devicesActive} / ${info.devicesLimit}")
                                appendLine("Бан: ${info.isBanned}, истёк: ${info.isExpired}")
                            }
                        },
                        onFailure = { "Ошибка: ${it.message}" }
                    )
                }
            },
            enabled = !loading && token.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Загрузка..." else "Проверить")
        }

        Spacer(Modifier.height(24.dp))
        Text(status)

        Spacer(Modifier.height(32.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Text("Подключение (VPN)", style = MaterialTheme.typography.titleMedium)
        Text("Появится на следующем этапе: выбор протокола (VLESS/Reality, Hysteria2, ...) и само подключение.")
    }
}
