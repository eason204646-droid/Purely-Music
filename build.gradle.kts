// 直接在 plugins 块中定义，不使用 libs 别名
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    // 启用 Kotlin 2.0 的 Compose 编译器插件
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}