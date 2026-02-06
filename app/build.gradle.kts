import org.jetbrains.kotlin.gradle.dsl.JvmTarget // 🚩 必须导入这个

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    // id("kotlin-kapt") // 🚩 如果只有 Room 用它，现在可以删掉这行
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.music.PurelyPlayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.music.PurelyPlayer"
        minSdk = 26
        targetSdk = 34 // 🚩 同步建议修改为 35
        versionCode = 7
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
            lint {
                // 1. 禁用导致崩溃的特定检查（LiveData 相关）
                disable += "NullSafeMutableLiveData" // 使用 +=
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 修改 APK 文件名
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "PurelyPlayer.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 🚩 终极修正位置：使用新的 compilerOptions DSL
    // 解决 Assignment type mismatch 报错
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // 基础库
    implementation("androidx.core:core-ktx:1.15.0") // 🚩 建议升级到支持 SDK 35 的版本
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose 统一版本管理
    implementation(platform("androidx.compose:compose-bom:2024.12.01")) // 🚩 升级 BOM 解决版本冲突
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // 导航
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // 图片加载
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(libs.androidx.compose.foundation)

    // Media3 (代替过时的 ExoPlayer 2.x)
    val media3Version = "1.5.0" // 🚩 升级到 1.5.0 更好支持 SDK 35
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // Room 数据库
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.android.material:material:1.12.0")

    // 🚩 核心：这是 Compose 使用的 Material 3 库
    implementation("androidx.compose.material3:material3:1.4.0")

}