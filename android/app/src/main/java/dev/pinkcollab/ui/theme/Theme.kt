package dev.pinkcollab.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
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
val Base0 = Color(0xFF07070A)
val Base1 = Color(0xFF101014)
val BrandPurple = Color(0xFF8360DD)
val BrandPink = Color(0xFFC069C9)
val Purple400 = BrandPurple
val Purple200 = Color(0xFFCFC3F7)
val Purple700 = Color(0xFF372A57)
val Violet400 = Color(0xFF9B87F5)
val Teal300 = Color(0xFF4DE0BE)
val Amber300 = Color(0xFFF2B84B)
val Red400 = Color(0xFFFF5470)
val Gray400 = Color(0xFF85858F)
val TextHigh = Color(0xFFF4F4F5)
val TextMid = Color(0xFFB3B3BD)

/** 默认（紫罗兰）配色。运行时可用 [accentScheme] 按偏好生成其它强调色。 */
val PinkCollabScheme = darkColorScheme(
    primary = Purple400,
    onPrimary = Color(0xFF160B24),
    primaryContainer = Purple700,
    onPrimaryContainer = Purple200,
    secondary = Violet400,
    onSecondary = Color(0xFF171126),
    secondaryContainer = Color(0xFF2D2540),
    onSecondaryContainer = Color(0xFFE9E3F5),
    tertiary = Teal300,
    onTertiary = Color(0xFF03211A),
    background = Base0,
    onBackground = TextHigh,
    surface = Base1,
    onSurface = TextHigh,
    surfaceVariant = Color(0xFF1A1A20),
    onSurfaceVariant = TextMid,
    surfaceContainerLowest = Base0,
    surfaceContainerLow = Color(0xFF0D0D11),
    surfaceContainer = Color(0xFF141418),
    surfaceContainerHigh = Color(0xFF1B1B21),
    surfaceContainerHighest = Color(0xFF23232A),
    inverseSurface = Color(0xFF23232A),
    inverseOnSurface = TextHigh,
    inversePrimary = Purple200,
    outline = Color(0xFF45434C),
    outlineVariant = Color(0xFF302F36),
    error = Red400,
    onError = Color(0xFF2B040C),
    errorContainer = Color(0xFF43101E),
    onErrorContainer = Color(0xFFFFC9D3),
)

/**
 * 按强调色主题生成配色：保留深色底与语义色，只替换 primary/secondary 系列。
 */
internal fun accentScheme(theme: AccentTheme) = PinkCollabScheme.copy(
    primary = Color(theme.primary),
    secondary = Color(theme.secondary),
    primaryContainer = Color(theme.primary).copy(alpha = 0.35f),
    onPrimaryContainer = Color(theme.primary).copy(alpha = 0.30f).compositeOver(Base0),
)

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
    colorScheme: androidx.compose.material3.ColorScheme = PinkCollabScheme,
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
