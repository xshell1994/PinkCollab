package dev.pinkcollab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pinkcollab.ui.theme.*

/**
 * 设置界面：外观（强调色 / 消息字号 / 代码字号）+ 输入（隐藏输入框）。
 * 所有改动即时写入 [AppPreferences]，返回后立即生效。
 */
@Composable
internal fun SettingsScreen(prefs: AppPreferences) {
    // 显式读取语言状态，保证切换语言时本页重组。
    val lang = AppLanguage.zh
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SettingsSection(T("通用", "General")) {
            SettingsSwitch(
                label = T("中文界面", "Chinese UI"),
                description = T("关闭则使用英文", "Turn off for English"),
                checked = prefs.chinese,
                onCheckedChange = { prefs.applyChinese(it) },
            )
            Spacer(Modifier.height(12.dp))
            SettingsSwitch(
                label = T("启动时自动检查更新", "Check updates on launch"),
                description = T("关闭后仅在手动点击时检查", "Off: check only when tapped"),
                checked = prefs.autoCheckUpdates,
                onCheckedChange = { prefs.applyAutoCheckUpdates(it) },
            )
        }

        SettingsSection(T("外观", "Appearance")) {
            SettingsSwitch(
                label = T("深色模式", "Dark mode"),
                description = T("关闭则使用浅色主题", "Off uses the light theme"),
                checked = prefs.darkTheme,
                onCheckedChange = { prefs.applyDarkTheme(it) },
            )
            Spacer(Modifier.height(16.dp))
            SettingsLabel(T("强调色", "Accent"))
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccentTheme.entries.forEach { theme ->
                    AccentSwatch(
                        theme = theme,
                        selected = prefs.accentThemeKey == theme.key,
                        onClick = { prefs.applyAccentTheme(theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SettingsSlider(
                label = T("消息字号", "Message size"),
                value = prefs.messageFontSize,
                valueRange = AppPreferences.MinFontSize..AppPreferences.MaxFontSize,
                steps = (AppPreferences.MaxFontSize - AppPreferences.MinFontSize).toInt() - 1,
                display = "${prefs.messageFontSize.toInt()} sp",
                onChange = { prefs.applyMessageFontSize(it) },
            )
            Spacer(Modifier.height(8.dp))
            SettingsSlider(
                label = T("代码字号", "Code size"),
                value = prefs.codeFontSize,
                valueRange = AppPreferences.MinCodeFontSize..AppPreferences.MaxCodeFontSize,
                steps = (AppPreferences.MaxCodeFontSize - AppPreferences.MinCodeFontSize).toInt() - 1,
                display = "${prefs.codeFontSize.toInt()} sp",
                onChange = { prefs.applyCodeFontSize(it) },
            )
            Spacer(Modifier.height(12.dp))
            SettingsSwitch(
                label = T("紧凑消息密度", "Compact density"),
                description = T("减小消息间距，同屏显示更多内容", "Tighter spacing, more on screen"),
                checked = prefs.compactDensity,
                onCheckedChange = { prefs.applyCompactDensity(it) },
            )
        }

        SettingsSection(T("输入", "Input")) {
            SettingsSwitch(
                label = T("隐藏输入框", "Hide composer"),
                description = T("隐藏底部输入框，需要时用右下角按钮唤出", "Tap the bottom-right button to bring it back"),
                checked = prefs.composerHidden,
                onCheckedChange = { prefs.applyComposerHidden(it) },
            )
            Spacer(Modifier.height(12.dp))
            SettingsSlider(
                label = T("输入框最大行数", "Composer max lines"),
                value = prefs.composerMaxLines.toFloat(),
                valueRange = AppPreferences.MinComposerLines.toFloat()..AppPreferences.MaxComposerLines.toFloat(),
                steps = AppPreferences.MaxComposerLines - AppPreferences.MinComposerLines - 1,
                display = if (AppLanguage.zh) "${prefs.composerMaxLines} 行" else "${prefs.composerMaxLines} lines",
                onChange = { prefs.applyComposerMaxLines(it.toInt()) },
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            T("设置即时生效并自动保存。", "Settings apply instantly and save automatically."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun AccentSwatch(
    theme: AccentTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(theme.primaryArgb)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = T("已选择", "Selected"),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SettingsLabel(label)
            Text(
                display,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange,
            steps = steps.coerceAtLeast(0),
        )
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SettingsLabel(label)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
