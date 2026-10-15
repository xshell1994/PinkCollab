import java.io.File
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Signing values come from the environment only when all four are set (CI).
// Otherwise the build uses the machine-local key in ~/.pinkcollab-signing, which
// is not a Gradle cache and is not tracked. A partial environment does not hide
// that key: a leftover ANDROID_* variable must not disable a working install.
val signingDirectory = File(System.getProperty("user.home"), ".pinkcollab-signing")
val localKeystore = signingDirectory.resolve("pinkcollab-release.p12")
val localPropertiesFile = signingDirectory.resolve("pinkcollab-release.properties")
val localSigning: Map<String, String>? =
    if (localKeystore.isFile && localPropertiesFile.isFile) {
        val stored = Properties().apply { localPropertiesFile.inputStream().use { load(it) } }
        mapOf(
            "keystore" to localKeystore.absolutePath,
            "storePassword" to stored.getProperty("ANDROID_KEYSTORE_PASSWORD"),
            "keyAlias" to stored.getProperty("ANDROID_KEY_ALIAS"),
            "keyPassword" to stored.getProperty("ANDROID_KEY_PASSWORD"),
        ).takeIf { values -> values.values.none { it.isNullOrBlank() } }?.mapValues { it.value!! }
    } else {
        null
    }
val environment =
    mapOf(
        "keystore" to providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull,
        "storePassword" to providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull,
        "keyAlias" to providers.environmentVariable("ANDROID_KEY_ALIAS").orNull,
        "keyPassword" to providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull,
    ).mapValues { (_, value) -> value?.takeIf { it.isNotBlank() } }
val environmentComplete = environment.values.none { it.isNullOrBlank() }
val releaseSigning: Map<String, String>? = if (environmentComplete) {
    environment.mapValues { it.value!! }
} else {
    localSigning
}
val signingReady = releaseSigning != null
val signingFailure =
    if (environment.values.any { it != null } && !environmentComplete) {
        "Android release signing environment is partial. Set ANDROID_KEYSTORE_FILE, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD together, or unset them and install ~/.pinkcollab-signing (docs/npm-release.md)."
    } else if (localKeystore.isFile && localSigning == null) {
        "Found ${localKeystore.absolutePath} but ${localPropertiesFile.name} is missing ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, or ANDROID_KEY_PASSWORD."
    } else {
        "Refusing to build an unsigned release. Install ~/.pinkcollab-signing/pinkcollab-release.p12 and pinkcollab-release.properties (docs/npm-release.md)."
    }

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.pinkcollab"
    compileSdk = 36
    defaultConfig {
        // 改 applicationId 以与官方版共存（namespace/代码包名不变，无需改代码）
        applicationId = "dev.xin.pinkcollab"
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 版本号需递增（更新检查靠 versionCode 比较）。标签须为规范 vX.Y.Z。
        versionCode = 3002006
        versionName = "3.2.6"
    }
    testBuildType = providers.gradleProperty("testBuildType").getOrElse("debug")
    signingConfigs {
        releaseSigning?.let { values ->
            create("release") {
                storeFile = file(values.getValue("keystore"))
                storePassword = values.getValue("storePassword")
                keyAlias = values.getValue("keyAlias")
                keyPassword = values.getValue("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // image-coil 依赖老版 coil-base:0.13.0，R8 压缩时报 Missing class coil.Coil，
            // 故关闭 minify（APK 略大，但功能稳定）；保留资源压缩。
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        create("releaseTest") {
            initWith(getByName("release"))
            proguardFile("release-test-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".releaseTest"
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    // 全局排除 annotations-java5：prism4j 传递引入它，与 Compose 的 org.jetbrains:annotations 重复
    configurations.configureEach {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("io.noties.markwon:core:4.6.2")
    // Markdown 扩展：表格 / 删除线 / 任务列表 / 链接自动识别 / HTML
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tasklist:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    // 图片渲染（自带 Coil，AI 常输出图片 URL）
    // ⚠️ image-coil 依赖老版 Coil，运行时缺 coil.Coil 会 NoClassDefFoundError，暂不启用
    // implementation("io.noties.markwon:image-coil:4.6.2")
    // 代码语法高亮（Prism4j 老库 + kapt），暂不启用以确保稳定
    // implementation("io.noties.markwon:syntax-highlight:4.6.2")
    // implementation("io.noties:prism4j:2.0.0")
    // kapt("io.noties:prism4j-bundler:2.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // The unit tests exercise the wire parsing, and `android.jar` only ships an org.json stub.
    testImplementation("org.json:json:20250517")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestCompileOnly("com.google.errorprone:error_prone_annotations:2.28.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// An unsigned release artifact installs but cannot upgrade an installed one.
tasks.matching { it.name == "packageRelease" || it.name == "bundleRelease" }.configureEach {
    doFirst {
        check(signingReady) { signingFailure }
    }
}
