package dev.pinkcollab.ui;

import io.noties.prism4j.annotations.PrismBundle;

/**
 * Prism4j 语法定义打包入口：kapt 注解处理器据此生成 GrammarLocatorDef 类。
 *
 * 用 Java 而非 Kotlin 写，避免 SOURCE 级 Java 注解在 Kotlin 上的兼容问题。
 * include 必须用 Prism4j 支持的真实语言名（见 prism4j-bundler 文档）。
 * grammarLocatorClassName 以点开头 → 生成到本类同包（dev.pinkcollab.ui.GrammarLocatorDef）。
 */
@PrismBundle(
        include = {"clike", "c", "cpp", "java", "kotlin", "javascript", "json",
                "python", "sql", "yaml", "markup", "go", "css", "markdown"},
        grammarLocatorClassName = ".GrammarLocatorDef"
)
public final class PrismBundleMarker {
    private PrismBundleMarker() {
    }
}
