package com.aezora.music.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import com.aezora.music.domain.model.*
import com.aezora.music.ui.theme.AezoraColors

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    downloadQuality: AudioQuality,
    streamQuality: AudioQuality,
    persistSpeed: Boolean,
    onSetTheme: (AppTheme) -> Unit,
    onSetDownloadQuality: (AudioQuality) -> Unit,
    onSetStreamQuality: (AudioQuality) -> Unit,
    onSetYandexToken: (String) -> Unit,
    onSetPersistSpeed: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTokenDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AezoraColors.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Настройки",
            color = AezoraColors.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            modifier = Modifier.padding(16.dp)
        )

        // ── Appearance ────────────────────────────────────────────────────────
        SettingsSection("ВНЕШНИЙ ВИД") {
            // Theme selector
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Тема приложения", color = AezoraColors.onSurface.copy(0.6f), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTheme.values().forEach { theme ->
                        val (bg, label) = when (theme) {
                            AppTheme.BLUE_PURPLE -> Color(0xFF6C63FF) to "🔵"
                            AppTheme.YELLOW_GREEN -> Color(0xFFB8E04A) to "🟢"
                            AppTheme.BLACK_WHITE -> Color(0xFFAAAAAA) to "⚪"
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (currentTheme == theme) 2.dp else 0.dp,
                                    color = if (currentTheme == theme) AezoraColors.primary else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .background(AezoraColors.container)
                                .clickable { onSetTheme(theme) }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(theme.label, color = AezoraColors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Yandex Music ──────────────────────────────────────────────────────
        SettingsSection("ЯНДЕКС МУЗЫКА") {
            SettingsItem(
                icon = Icons.Filled.Key,
                title = "Токен авторизации",
                subtitle = "Введите токен для доступа к Яндекс Музыке",
                onClick = { showTokenDialog = true }
            )
        }

        // ── Quality ───────────────────────────────────────────────────────────
        SettingsSection("КАЧЕСТВО") {
            Text(
                "Прослушивание",
                color = AezoraColors.onSurface.copy(0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioQuality.values().forEach { q ->
                    QualityChip(
                        label = q.label,
                        selected = streamQuality == q,
                        onClick = { onSetStreamQuality(q) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Скачивание",
                color = AezoraColors.onSurface.copy(0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioQuality.values().forEach { q ->
                    QualityChip(
                        label = q.label,
                        selected = downloadQuality == q,
                        onClick = { onSetDownloadQuality(q) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Playback ──────────────────────────────────────────────────────────
        SettingsSection("ВОСПРОИЗВЕДЕНИЕ") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Speed, null, tint = AezoraColors.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Запоминать Speed/Slowed", color = AezoraColors.onSurface, fontSize = 15.sp)
                    Text("Применять к каждому треку", color = AezoraColors.onSurface.copy(0.5f), fontSize = 12.sp)
                }
                Switch(
                    checked = persistSpeed,
                    onCheckedChange = onSetPersistSpeed,
                    colors = SwitchDefaults.colors(checkedThumbColor = AezoraColors.primary, checkedTrackColor = AezoraColors.primary.copy(0.3f))
                )
            }
        }

        // ── Support ───────────────────────────────────────────────────────────
        SettingsSection("ПОДДЕРЖКА АВТОРА") {
            SettingsItem(
                icon = Icons.Filled.Favorite,
                title = "Поддержать через CryptoBot",
                subtitle = "Отправить донат через Telegram",
                iconTint = Color(0xFFFFB800),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://t.me/send?start=IVzCeY4eliGd"))
                    context.startActivity(intent)
                }
            )
        }

        // ── About ─────────────────────────────────────────────────────────────
        SettingsSection("О ПРИЛОЖЕНИИ") {
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "Aezora",
                subtitle = "Версия 1.0.0 · Музыка без границ",
                onClick = {}
            )
            SettingsItem(
                icon = Icons.Filled.Shield,
                title = "Лицензии",
                subtitle = "SoundCloud, Яндекс Музыка",
                onClick = {}
            )
        }

        Spacer(Modifier.height(120.dp))
    }

    if (showTokenDialog) {
        YandexTokenDialog(
            onConfirm = { token ->
                onSetYandexToken(token)
                showTokenDialog = false
            },
            onDismiss = { showTokenDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text(
        title,
        color = AezoraColors.onSurface.copy(0.4f),
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AezoraColors.surface)
    ) {
        Column { content() }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = AezoraColors.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = AezoraColors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = AezoraColors.onSurface.copy(0.5f), fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = AezoraColors.onSurface.copy(0.3f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun QualityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) AezoraColors.primary.copy(alpha = 0.2f) else AezoraColors.container
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) AezoraColors.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) AezoraColors.primary else AezoraColors.onSurface.copy(0.6f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun YandexTokenDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AezoraColors.surface,
        title = {
            Column {
                Text("Яндекс Музыка", color = AezoraColors.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Токен авторизации", color = AezoraColors.onSurface.copy(0.5f), fontSize = 13.sp)
            }
        },
        text = {
            Column {
                Text(
                    "Получить токен можно через Яндекс OAuth.\n" +
                            "Ваш токен хранится только на устройстве.",
                    color = AezoraColors.onSurface.copy(0.6f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("OAuth токен", color = AezoraColors.onSurface.copy(0.5f)) },
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null, tint = AezoraColors.onSurface.copy(0.5f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AezoraColors.onSurface,
                        unfocusedTextColor = AezoraColors.onSurface,
                        focusedBorderColor = AezoraColors.primary,
                        unfocusedBorderColor = AezoraColors.container,
                        cursorColor = AezoraColors.primary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (token.isNotBlank()) onConfirm(token.trim()) }) {
                Text("Сохранить", color = AezoraColors.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = AezoraColors.onSurface.copy(0.5f))
            }
        }
    )
}
