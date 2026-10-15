package dev.pinkcollab.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 本地外观/行为偏好（SharedPreferences 持久化）。
 *
 * 通过 [rememberAppPreferences] 在 Compose 树顶层创建单例，子界面读取其属性即可；
 * 所有属性都是 Compose 可观察状态（mutableStateOf），改值会触发重组。
 */
internal class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("app-preferences", Context.MODE_PRIVATE)

    /** 消息列表字号（sp），12~20。 */
    var messageFontSize by mutableStateOf(prefs.getFloat("message-font-size", 12f))
        private set

    /** 代码块字号（sp），10~18。 */
    var codeFontSize by mutableStateOf(prefs.getFloat("code-font-size", 12f))
        private set

    /** 输入框是否隐藏（隐藏后可通过顶栏按钮唤出）。 */
    var composerHidden by mutableStateOf(prefs.getBoolean("composer-hidden", false))
        private set

    /** 强调色主题 key，见 [AccentTheme]。 */
    var accentThemeKey by mutableStateOf(prefs.getString("accent-theme", AccentTheme.Purple.key) ?: AccentTheme.Purple.key)
        private set

    /** 消息列表密度：紧凑 / 舒适。 */
    var compactDensity by mutableStateOf(prefs.getBoolean("compact-density", false))
        private set

    /** 输入框最大行数，3~10。 */
    var composerMaxLines by mutableStateOf(prefs.getInt("composer-max-lines", 5))
        private set

    /** 是否在启动时自动检查更新。 */
    var autoCheckUpdates by mutableStateOf(prefs.getBoolean("auto-check-updates", true))
        private set

    /** 是否深色模式（false=浅色）。 */
    var darkTheme by mutableStateOf(prefs.getBoolean("dark-theme", true))
        private set

    /** 界面语言：true=中文，false=English。 */
    var chinese by mutableStateOf(prefs.getBoolean("chinese", true))
        private set

    val accentTheme: AccentTheme get() = AccentTheme.fromKey(accentThemeKey)

    fun applyMessageFontSize(value: Float) {
        messageFontSize = value.coerceIn(MinFontSize, MaxFontSize)
        prefs.edit().putFloat("message-font-size", messageFontSize).apply()
    }

    fun applyCodeFontSize(value: Float) {
        codeFontSize = value.coerceIn(MinCodeFontSize, MaxCodeFontSize)
        prefs.edit().putFloat("code-font-size", codeFontSize).apply()
    }

    fun applyComposerHidden(value: Boolean) {
        composerHidden = value
        prefs.edit().putBoolean("composer-hidden", value).apply()
    }

    fun applyAccentTheme(theme: AccentTheme) {
        accentThemeKey = theme.key
        prefs.edit().putString("accent-theme", theme.key).apply()
    }

    fun applyCompactDensity(value: Boolean) {
        compactDensity = value
        prefs.edit().putBoolean("compact-density", value).apply()
    }

    fun applyComposerMaxLines(value: Int) {
        composerMaxLines = value.coerceIn(MinComposerLines, MaxComposerLines)
        prefs.edit().putInt("composer-max-lines", composerMaxLines).apply()
    }

    fun applyAutoCheckUpdates(value: Boolean) {
        autoCheckUpdates = value
        prefs.edit().putBoolean("auto-check-updates", value).apply()
    }

    fun applyDarkTheme(value: Boolean) {
        darkTheme = value
        prefs.edit().putBoolean("dark-theme", value).apply()
    }

    fun applyChinese(value: Boolean) {
        chinese = value
        prefs.edit().putBoolean("chinese", value).apply()
    }

    companion object {
        const val MinFontSize = 8f
        const val MaxFontSize = 32f
        const val MinCodeFontSize = 8f
        const val MaxCodeFontSize = 28f
        const val MinComposerLines = 3
        const val MaxComposerLines = 10
    }
}

/** 强调色主题预设：保留深色底，只换强调色，避免改动整个配色层。 */
internal enum class AccentTheme(
    val key: String,
    val labelZh: String,
    val labelEn: String,
    val primary: Long,
    val secondary: Long,
) {
    Purple("purple", "紫罗兰", "Violet", 0xFF8360DD, 0xFFC069C9),
    Teal("teal", "青碧", "Teal", 0xFF4DE0BE, 0xFF52A8E0),
    Amber("amber", "琥珀", "Amber", 0xFFF2B84B, 0xFFE08A4D),
    Green("green", "翠绿", "Green", 0xFF5BD37A, 0xFF4DB8A8),
    Pink("pink", "樱粉", "Pink", 0xFFE06BA8, 0xFFC069C9);

    /** 当前语言下的显示名。 */
    val label: String get() = if (AppLanguage.zh) labelZh else labelEn

    /** ARGB 颜色的 Int 形式（供 Compose Color(Int) 使用）。 */
    val primaryArgb: Int get() = primary.toInt()
    val secondaryArgb: Int get() = secondary.toInt()

    companion object {
        fun fromKey(key: String): AccentTheme = entries.firstOrNull { it.key == key } ?: Purple
    }
}
