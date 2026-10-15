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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SettingsSection("外观") {
            // 深色/浅色
            SettingsSwitch(
                label = "深色模式",
                description = "关闭则使用浅色主题",
                checked = prefs.darkTheme,
                onCheckedChange = { prefs.applyDarkTheme(it) },
            )
            Spacer(Modifier.height(16.dp))
            // 强调色
            SettingsLabel("强调色")
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
            // 消息字号
            SettingsSlider(
                label = "消息字号",
                value = prefs.messageFontSize,
                valueRange = AppPreferences.MinFontSize..AppPreferences.MaxFontSize,
                steps = (AppPreferences.MaxFontSize - AppPreferences.MinFontSize).toInt() - 1,
                display = "${prefs.messageFontSize.toInt()} sp",
                onChange = { prefs.applyMessageFontSize(it) },
            )
            Spacer(Modifier.height(8.dp))
            // 代码字号
            SettingsSlider(
                label = "代码字号",
                value = prefs.codeFontSize,
                valueRange = AppPreferences.MinCodeFontSize..AppPreferences.MaxCodeFontSize,
                steps = (AppPreferences.MaxCodeFontSize - AppPreferences.MinCodeFontSize).toInt() - 1,
                display = "${prefs.codeFontSize.toInt()} sp",
                onChange = { prefs.applyCodeFontSize(it) },
            )
            Spacer(Modifier.height(12.dp))
            // 消息密度
            SettingsSwitch(
                label = "紧凑消息密度",
                description = "减小消息间距，同屏显示更多内容",
                checked = prefs.compactDensity,
                onCheckedChange = { prefs.applyCompactDensity(it) },
            )
        }

        SettingsSection("输入") {
            SettingsSwitch(
                label = "隐藏输入框",
                description = "隐藏底部输入框，需要时用顶栏按钮唤出",
                checked = prefs.composerHidden,
                onCheckedChange = { prefs.applyComposerHidden(it) },
            )
            Spacer(Modifier.height(12.dp))
            SettingsSlider(
                label = "输入框最大行数",
                value = prefs.composerMaxLines.toFloat(),
                valueRange = AppPreferences.MinComposerLines.toFloat()..AppPreferences.MaxComposerLines.toFloat(),
                steps = AppPreferences.MaxComposerLines - AppPreferences.MinComposerLines - 1,
                display = "${prefs.composerMaxLines} 行",
                onChange = { prefs.applyComposerMaxLines(it.toInt()) },
            )
        }

        SettingsSection("行为") {
            SettingsSwitch(
                label = "启动时自动检查更新",
                description = "关闭后仅在手动点击时检查",
                checked = prefs.autoCheckUpdates,
                onCheckedChange = { prefs.applyAutoCheckUpdates(it) },
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "设置即时生效并自动保存。",
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
                    contentDescription = "已选择",
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
