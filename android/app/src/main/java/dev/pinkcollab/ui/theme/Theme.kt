package dev.pinkcollab.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.pinkcollab.R
import dev.pinkcollab.data.SessionStatus
import dev.pinkcollab.ui.AccentTheme
// ── Palette: neutral graphite surfaces with restrained violet accents ────────
// 品牌/语义色：深浅模式通用，保持常量。
val BrandPurple = Color(0xFF8360DD)
val BrandPink = Color(0xFFC069C9)
val Purple400 = BrandPurple
val Purple700 = Color(0xFF372A57)
val Violet400 = Color(0xFF9B87F5)
val Teal300 = Color(0xFF4DE0BE)
val Amber300 = Color(0xFFF2B84B)
val Red400 = Color(0xFFFF5470)

/**
 * 全局外观状态：由 App 根节点按用户偏好写入。
 *
 * 用全局 [mutableStateOf]（而非 CompositionLocal）是为了让**非 @Composable 上下文**
 * （Modifier 扩展、顶层 val）也能读取当前主题；@Composable 读取这些颜色属性时会
 * 自动订阅本状态，切换主题即触发重组。
 */
internal object AppAppearance {
    /** true=深色，false=浅色。 */
    var dark by mutableStateOf(true)
}

// ── 明暗相关色：getter 按当前外观返回（调用点写法不变）────────────────────────
val Base0: Color get() = if (AppAppearance.dark) Color(0xFF07070A) else Color(0xFFF4F4F7)
val Base1: Color get() = if (AppAppearance.dark) Color(0xFF101014) else Color(0xFFFFFFFF)
val Purple200: Color get() = if (AppAppearance.dark) Color(0xFFCFC3F7) else Color(0xFF5B3FA8)
val Gray400: Color get() = if (AppAppearance.dark) Color(0xFF85858F) else Color(0xFF7C7C88)
val TextHigh: Color get() = if (AppAppearance.dark) Color(0xFFF4F4F5) else Color(0xFF16161C)
val TextMid: Color get() = if (AppAppearance.dark) Color(0xFFB3B3BD) else Color(0xFF55555F)

/** 分隔线（深色=白，浅色=黑）。 */
val HairlineBase: Color get() = if (AppAppearance.dark) Color.White else Color.Black
/** 半透明微填充（卡片/胶囊底）。 */
val FaintFill: Color get() = if (AppAppearance.dark) Color.White else Color.Black
/** 遮罩/渐隐（基于底色）。 */
val ScrimBase: Color get() = if (AppAppearance.dark) Color.Black else Color(0xFFF4F4F7)

/** 深色配色（默认紫罗兰）。配色随强调色主题变化见 [accentScheme]。 */
private fun darkPinkCollabScheme() = darkColorScheme(
    primary = Purple400,
    onPrimary = Color(0xFF160B24),
    primaryContainer = Purple700,
    onPrimaryContainer = Color(0xFFCFC3F7),
    secondary = Violet400,
    onSecondary = Color(0xFF171126),
    secondaryContainer = Color(0xFF2D2540),
    onSecondaryContainer = Color(0xFFE9E3F5),
    tertiary = Teal300,
    onTertiary = Color(0xFF03211A),
    background = Color(0xFF07070A),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF101014),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF1A1A20),
    onSurfaceVariant = Color(0xFFB3B3BD),
    surfaceContainerLowest = Color(0xFF07070A),
    surfaceContainerLow = Color(0xFF0D0D11),
    surfaceContainer = Color(0xFF141418),
    surfaceContainerHigh = Color(0xFF1B1B21),
    surfaceContainerHighest = Color(0xFF23232A),
    inverseSurface = Color(0xFF23232A),
    inverseOnSurface = Color(0xFFF4F4F5),
    inversePrimary = Color(0xFFCFC3F7),
    outline = Color(0xFF45434C),
    outlineVariant = Color(0xFF302F36),
    error = Red400,
    onError = Color(0xFF2B040C),
    errorContainer = Color(0xFF43101E),
    onErrorContainer = Color(0xFFFFC9D3),
)

/** 浅色配色。 */
private fun lightPinkCollabScheme() = lightColorScheme(
    primary = Purple400,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7DFFB),
    onPrimaryContainer = Color(0xFF2A1C52),
    secondary = Violet400,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE7FB),
    onSecondaryContainer = Color(0xFF241A45),
    tertiary = Color(0xFF1FA98C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F4F7),
    onBackground = Color(0xFF16161C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16161C),
    surfaceVariant = Color(0xFFECECF1),
    onSurfaceVariant = Color(0xFF55555F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainer = Color(0xFFF1F1F5),
    surfaceContainerHigh = Color(0xFFEAEAEF),
    surfaceContainerHighest = Color(0xFFE3E3E9),
    inverseSurface = Color(0xFF2A2A32),
    inverseOnSurface = Color(0xFFF4F4F5),
    inversePrimary = Color(0xFFCFC3F7),
    outline = Color(0xFFB6B6C0),
    outlineVariant = Color(0xFFDCDCE3),
    error = Color(0xFFD32F4A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDADF),
    onErrorContainer = Color(0xFF4A0416),
)

/** 当前外观对应的基础配色。 */
fun pinkCollabScheme(): androidx.compose.material3.ColorScheme =
    if (AppAppearance.dark) darkPinkCollabScheme() else lightPinkCollabScheme()

/**
 * 按强调色主题生成配色：只替换 primary/secondary 系列，明暗自适。
 */
internal fun accentScheme(theme: AccentTheme): androidx.compose.material3.ColorScheme {
    val base = pinkCollabScheme()
    val accent = Color(theme.primaryArgb)
    val secondary = Color(theme.secondaryArgb)
    val onPrimary = if (AppAppearance.dark) Color(0xFF160B24) else Color(0xFFFFFFFF)
    return base.copy(
        primary = accent,
        onPrimary = onPrimary,
        secondary = secondary,
    )
}

private val MapleMono = FontFamily(
    Font(R.font.maple_mono_cn_regular, FontWeight.Normal),
)

private fun TextStyle.sessionFont(sizeSp: Float = fontSize.value - 1f) = copy(fontFamily = MapleMono, fontSize = sizeSp.sp)

private fun Typography.sessionFont(bodySize: Float) = copy(
    displayLarge = displayLarge.sessionFont(),
    displayMedium = displayMedium.sessionFont(),
    displaySmall = displaySmall.sessionFont(),
    headlineLarge = headlineLarge.sessionFont(),
    headlineMedium = headlineMedium.sessionFont(),
    headlineSmall = headlineSmall.sessionFont(),
    titleLarge = titleLarge.sessionFont(),
    titleMedium = titleMedium.sessionFont(),
    titleSmall = titleSmall.sessionFont(),
    bodyLarge = bodyLarge.sessionFont(bodySize + 3f),
    bodyMedium = bodyMedium.sessionFont(bodySize),
    bodySmall = bodySmall.sessionFont(bodySize),
    labelLarge = labelLarge.sessionFont(),
    labelMedium = labelMedium.sessionFont(),
    labelSmall = labelSmall.sessionFont(),
)

private val DefaultTypography = Typography()

/** Preserve Material 3's label rhythm with a slightly stronger optical size on dark surfaces. */
val PinkCollabTypography = Typography(
    labelLarge = DefaultTypography.labelLarge.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

/** Message list only. Heavier weights are synthesized from the bundled regular face. */
internal val SessionTypography = PinkCollabTypography.sessionFont(12f)

/** 按偏好字号生成消息列表字体。 */
internal fun sessionTypographyFor(fontSize: Float) = PinkCollabTypography.sessionFont(fontSize)

@Composable
fun PinkCollabTheme(
    colorScheme: androidx.compose.material3.ColorScheme = pinkCollabScheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PinkCollabTypography,
        content = content,
    )
}

// ── Status semantics ─────────────────────────────────────────────────────────
/** Semantic session-status colors, kept separate from decorative brand accents. */
fun statusColor(status: SessionStatus): Color = when (status) {
    SessionStatus.NeedsInput -> Amber300
    SessionStatus.Starting, SessionStatus.Running, SessionStatus.Stopping -> Violet400
    SessionStatus.Idle -> Gray400
}

fun statusLabel(status: SessionStatus): String = when (status) {
    SessionStatus.Starting -> "Starting"
    SessionStatus.Stopping -> "Stopping"
    SessionStatus.Running -> "Running"
    SessionStatus.NeedsInput -> "Needs you"
    SessionStatus.Idle -> "Paused"
}
