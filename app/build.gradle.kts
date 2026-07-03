import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
            isMinifyEnabled = true
            isDebuggable = false
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)

    implementation("androidx.navigation:navigation-compose:${libs.versions.navigationCompose.get()}")
    implementation("io.coil-kt:coil-compose:${libs.versions.coilCompose.get()}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${libs.versions.lifecycleViewmodelCompose.get()}")

    val media3 = libs.versions.media3.get()
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.5.0+1")

    val room = libs.versions.room.get()
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("com.squareup.retrofit2:retrofit:${libs.versions.retrofit.get()}")
    implementation("com.squareup.retrofit2:converter-gson:${libs.versions.retrofit.get()}")
    implementation("com.google.android.material:material:${libs.versions.material.get()}")

    implementation("io.noties.markwon:core:${libs.versions.markwon.get()}")
}
