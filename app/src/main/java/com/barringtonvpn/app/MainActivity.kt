package com.barringtonvpn.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

object AppColors {
    val bg = Color(0xFF090112)
    val accent = Color(0xFF9D4EDD)
    val accent2 = Color(0xFF7B2CBF)
    val textLight = Color(0xFFE0AAFF)
    val textMuted = Color(0xFF8E8E93)
    val cardBg = Color(0x14FFFFFF)
    val cardBorder = Color(0x1FFFFFFF)
    val success = Color(0xFF30D158)
    val info = Color(0xFF22D3EE)
    val danger = Color(0xFFFF453A)
}

object TokenStore {
    private const val PREFS = "barringtonvpn_prefs"
    private const val KEY_TOKEN = "sub_token"
    fun get(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)
    fun set(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TOKEN, token).apply()
    }
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_TOKEN).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = AppColors.accent,
                background = AppColors.bg,
                surface = AppColors.bg
            )) {
                Surface(modifier = Modifier.fillMaxSize(), color = AppColors.bg) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    var token by remember { mutableStateOf(TokenStore.get(context)) }

    if (token.isNullOrBlank()) {
        PairScreen(onSaved = { t ->
            TokenStore.set(context, t)
            token = t
        })
    } else {
        MainShell(token = token!!, onLogout = {
            TokenStore.clear(context)
            token = null
        })
    }
}

@Composable
fun PairScreen(onSaved: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogo()
        Spacer(Modifier.height(28.dp))
        Text("Введите токен подписки, чтобы привязать приложение", color = AppColors.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Токен подписки") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        if (error != null) {
            Text(error!!, color = AppColors.danger, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                loading = true; error = null
                scope.launch {
                    val result = AccountApi.fetch(input)
                    loading = false
                    result.fold(
                        onSuccess = { onSaved(input.trim()) },
                        onFailure = { error = "Не удалось проверить токен: ${it.message}" }
                    )
                }
            },
            enabled = !loading && input.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Проверка..." else "Войти")
        }
    }
}

@Composable
fun AppLogo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A0A2E))
                .border(1.dp, AppColors.info, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("BV", color = AppColors.accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text("BARRINGTON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("VPN", color = AppColors.info, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun AppHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(AppColors.cardBg)
                .border(1.dp, AppColors.cardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("\uD83C\uDFA7", fontSize = 14.sp) }

        Box(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(AppColors.cardBg)
                .border(1.dp, AppColors.cardBorder, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) { Text("\uD83C\uDDF7\uD83C\uDDFA RU", fontSize = 11.sp, color = Color.White) }
    }
    Spacer(Modifier.height(6.dp))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AppLogo() }
}

@Composable
fun SectionChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.accent.copy(alpha = 0.12f))
            .border(1.dp, AppColors.accent.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(AppColors.accent))
            Spacer(Modifier.width(6.dp))
            Text(text.uppercase(), color = AppColors.textLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlowCard(glowColor: Color?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.cardBg)
            .border(
                BorderStroke(1.5.dp, glowColor?.copy(alpha = 0.55f) ?: AppColors.cardBorder),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) { content() }
}

@Composable
fun BottomTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("\u23FB" to "Подключение", "\uD83C\uDF10" to "Серверы", "\uD83D\uDC64" to "Аккаунт")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.bg)
            .border(BorderStroke(1.dp, AppColors.cardBorder))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val active = i == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(i) }
            ) {
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 9.sp, color = if (active) AppColors.textLight else AppColors.textMuted,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun TrafficRing(percent: Int) {
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = AppColors.cardBorder,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = 5.dp.toPx()), size = Size(size.width, size.height)
            )
            drawArc(
                color = AppColors.info,
                startAngle = -90f, sweepAngle = 360f * (percent / 100f), useCenter = false,
                style = Stroke(width = 5.dp.toPx()), size = Size(size.width, size.height)
            )
        }
        Text("$percent%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun MainShell(token: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var account by remember { mutableStateOf<AccountInfo?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var servers by remember { mutableStateOf<List<ServerProfile>>(emptyList()) }
    var selectedServer by remember { mutableStateOf(0) }
    var connected by remember { mutableStateOf(true) }

    LaunchedEffect(token) {
        AccountApi.fetch(token).onSuccess { account = it }.onFailure { accountError = it.message }
        SubscriptionApi.fetch(token).onSuccess { servers = it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp).padding(top = 12.dp)) {
            AppHeader()
            Spacer(Modifier.height(14.dp))
            when (tab) {
                0 -> ConnectTab(
                    account = account,
                    error = accountError,
                    server = servers.getOrNull(selectedServer),
                    connected = connected,
                    onToggleConnect = {
                        connected = !connected
                        Toast.makeText(
                            context,
                            if (connected) "Это предпросмотр интерфейса — настоящее VPN-подключение добавим на следующем этапе"
                            else "Отключено (предпросмотр)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                1 -> ServersTab(servers, selectedServer) { selectedServer = it; tab = 0 }
                2 -> AccountTab(account, onLogout)
            }
        }
        BottomTabBar(tab) { tab = it }
    }
}

@Composable
fun ConnectTab(account: AccountInfo?, error: String?, server: ServerProfile?, connected: Boolean, onToggleConnect: () -> Unit) {
    SectionChip("Статус подключения")
    Spacer(Modifier.height(8.dp))
    GlowCard(glowColor = if (connected) AppColors.success else null) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape)
                        .border(2.dp, if (connected) AppColors.success else AppColors.textMuted, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u26A1", fontSize = 24.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (connected) "Подключено" else "Отключено",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                Text(
                    server?.let { "${it.flag} ${it.label} · ${it.protocol}" } ?: "Сервер не выбран",
                    color = AppColors.textMuted, fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.04f)).padding(10.dp)
                    ) {
                        Column {
                            Text("ТРАФИК", fontSize = 9.sp, color = AppColors.textMuted)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                account?.let { "${it.trafficUsedGb} / ${it.trafficLimitGb} ГБ" } ?: "—",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.04f)).padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val pct = account?.let {
                            if (it.trafficLimitGb > 0) ((it.trafficUsedGb / it.trafficLimitGb) * 100).toInt() else 0
                        } ?: 0
                        TrafficRing(pct.coerceIn(0, 100))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onToggleConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connected) Color(0xFF2E7D32) else AppColors.accent
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (connected) "Отключить" else "Подключить", fontWeight = FontWeight.Bold)
                }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text("Ошибка загрузки аккаунта: $error", color = AppColors.danger, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ServerRow(server: ServerProfile, selected: Boolean, onClick: () -> Unit) {
    GlowCard(
        glowColor = if (selected) AppColors.accent else null,
        modifier = Modifier.clickable { onClick() }.padding(bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(server.flag, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${server.label} · ${server.protocol.uppercase()}",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                Text("${server.security} / ${server.network}", color = AppColors.textMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ServersTab(servers: List<ServerProfile>, selected: Int, onSelect: (Int) -> Unit) {
    SectionChip("Выбор сервера")
    Spacer(Modifier.height(8.dp))
    if (servers.isEmpty()) {
        Text("Загрузка списка серверов...", color = AppColors.textMuted, fontSize = 13.sp)
    } else {
        servers.forEach { s ->
            ServerRow(server = s, selected = s.index == selected, onClick = { onSelect(s.index) })
        }
    }
}

@Composable
fun AccountTab(account: AccountInfo?, onLogout: () -> Unit) {
    GlowCard(glowColor = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(AppColors.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (account?.username?.firstOrNull()?.uppercase() ?: "?"),
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ЛИЧНЫЙ КАБИНЕТ", color = AppColors.textMuted, fontSize = 9.sp)
                Text(account?.username ?: "—", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)).padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(account?.let { "${it.devicesActive}/${it.devicesLimit}" } ?: "—/—", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("устройств", fontSize = 8.sp, color = AppColors.textMuted)
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    SectionChip("Моя подписка")
    Spacer(Modifier.height(8.dp))
    val isGoodStanding = account != null && !account.isBanned && !account.isExpired
    GlowCard(glowColor = if (isGoodStanding) AppColors.success else AppColors.danger) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("\uD83D\uDEE1 ${account?.keyName ?: "—"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                if (account?.isExpired == true) "ИСТЕКЛА" else "АКТИВНА",
                color = if (account?.isExpired == true) AppColors.danger else AppColors.success,
                fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.04f)).padding(10.dp)) {
                Column {
                    Text("ДЕЙСТВУЕТ ДО", fontSize = 9.sp, color = AppColors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(account?.expiresAt?.substringBefore(" ") ?: "—", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.04f)).padding(10.dp)) {
                Column {
                    Text("ТРАФИК", fontSize = 9.sp, color = AppColors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(account?.let { "${it.trafficUsedGb} / ${it.trafficLimitGb} ГБ" } ?: "—", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onLogout,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) { Text("Выйти / сменить токен", color = AppColors.textMuted) }
}
