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

    val accentTheme: AccentTheme get() = AccentTheme.fromKey(accentThemeKey)

    fun setMessageFontSize(value: Float) {
        messageFontSize = value.coerceIn(MinFontSize, MaxFontSize)
        prefs.edit().putFloat("message-font-size", messageFontSize).apply()
    }

    fun setCodeFontSize(value: Float) {
        codeFontSize = value.coerceIn(MinCodeFontSize, MaxCodeFontSize)
        prefs.edit().putFloat("code-font-size", codeFontSize).apply()
    }

    fun setComposerHidden(value: Boolean) {
        composerHidden = value
        prefs.edit().putBoolean("composer-hidden", value).apply()
    }

    fun setAccentTheme(theme: AccentTheme) {
        accentThemeKey = theme.key
        prefs.edit().putString("accent-theme", theme.key).apply()
    }

    companion object {
        const val MinFontSize = 12f
        const val MaxFontSize = 20f
        const val MinCodeFontSize = 10f
        const val MaxCodeFontSize = 18f
    }
}

/** 强调色主题预设：保留深色底，只换强调色，避免改动整个配色层。 */
internal enum class AccentTheme(
    val key: String,
    val label: String,
    val primary: Long,
    val secondary: Long,
) {
    Purple("purple", "紫罗兰", 0xFF8360DD, 0xFFC069C9),
    Teal("teal", "青碧", 0xFF4DE0BE, 0xFF52A8E0),
    Amber("amber", "琥珀", 0xFFF2B84B, 0xFFE08A4D),
    Green("green", "翠绿", 0xFF5BD37A, 0xFF4DB8A8),
    Pink("pink", "樱粉", 0xFFE06BA8, 0xFFC069C9);

    companion object {
        fun fromKey(key: String): AccentTheme = entries.firstOrNull { it.key == key } ?: Purple
    }
}
