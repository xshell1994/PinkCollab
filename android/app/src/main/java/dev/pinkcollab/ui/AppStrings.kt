package dev.pinkcollab.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 运行时中英文切换（方案 B：轻量字典，不依赖 Android 资源体系）。
 *
 * 全局 [AppLanguage] 决定当前语言；`T` 提供双语文本。
 * 关键界面用 `T("中文", "English")` 取词，切换语言即全局重组。
 */
internal object AppLanguage {
    /** true=中文，false=English。 */
    var zh by mutableStateOf(true)
}

/** 按当前语言取文案：zh 为中文、en 为英文。 */
fun T(zh: String, en: String): String = if (AppLanguage.zh) zh else en

/**
 * 全局沉浸模式状态：滑动消息区时置 true，隐藏顶栏/输入框，全屏只显示消息区。
 * （系统状态栏不隐藏——不做全屏 window flags。）
 */
internal object AppImmersive {
    var active by mutableStateOf(false)
}
