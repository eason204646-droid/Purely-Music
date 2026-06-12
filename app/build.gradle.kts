import org.jetbrains.kotlin.gradle.dsl.JvmTarget // 🚩 必须导入这个
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    // id("kotlin-kapt") // 🚩 如果只有 Room 用它，现在可以删掉这行
    id("com.google.devtools.ksp")
}

// 加载签名配置
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// 加载API配置
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.music.purelymusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.music.purelymusic"
        minSdk = 26
        targetSdk = 36
        versionCode = 27
        versionName = "2.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        vectorDrawables {
            useSupportLibrary = true
            lint {
                // 1. 禁用导致崩溃的特定检查（LiveData 相关）
                disable += "NullSafeMutableLiveData" // 使用 +=
            }
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            val storeFilePath = keystoreProperties.getProperty("storeFile", "")
            if (storeFilePath.isNotEmpty()) {
                create("release") {
                    storeFile = file(storeFilePath)
                    storePassword = keystoreProperties.getProperty("storePassword", "")
                    keyAlias = keystoreProperties.getProperty("keyAlias", "")
                    keyPassword = keystoreProperties.getProperty("keyPassword", "")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "MUSIC_API_KEY", "\"${localProperties.getProperty("MUSIC_API_KEY", "")}\"")
        }
        debug {
            buildConfigField("String", "MUSIC_API_KEY", "\"${localProperties.getProperty("MUSIC_API_KEY", "")}\"")
        }
    }

    // 修改 APK 文件名
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "purelymusic.apk"
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
        buildConfig = true
    }
}

dependencies {
    // 基础库（使用版本目录）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose 统一版本管理
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // 导航
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // 图片加载
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.ui.unit)

    // Media3 (代替过时的 ExoPlayer 2.x)
    val media3Version = "1.5.0"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    // FFmpeg 解码器（Jellyfin 发布版）
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.5.0+1")

    // Room 数据库
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.android.material:material:1.12.0")

    // Markdown rendering
    implementation("io.noties.markwon:core:4.6.2")
}
