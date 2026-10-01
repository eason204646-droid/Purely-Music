# Purely Music

[![Android CI](https://github.com/eason204646-droid/Purely-Music/actions/workflows/android.yml/badge.svg)](https://github.com/eason204646-droid/Purely-Music/actions/workflows/android.yml)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/oreo)
[![License: Mulan PSL v2](https://img.shields.io/badge/License-Mulan%20PSL%20v2-blue.svg)](LICENSE)

Purely Music 是一款开源 Android 本地音乐播放器。导入设备中的音乐，整理专辑与播放列表，跟随同步歌词聆听；也可以按自己的喜好调整播放衔接和声音效果。

## 功能

- **本地音乐库**：导入单曲或多个音频文件；按歌曲、专辑和自建播放列表浏览与管理。导入的音频、封面和歌词保存在应用私有存储中。
- **播放器与系统控制**：后台播放、播放队列、最近播放、通知栏和锁屏媒体控制，以及可设置停止时间的睡眠定时器。
- **歌词**：解析并同步显示 LRC 歌词，点击歌词跳转到对应进度；支持单行/多行样式、敏感词过滤和非中文歌词翻译。
- **自动补全**：联网获取歌曲封面与歌词，可在设置中选择获取源。网络补全需要配置 `MUSIC_API_KEY`；本地播放不依赖网络。
- **播放与声音设置**：智能混音（AutoMix）分析曲目片段并尝试匹配节拍与调性；也可选择固定时长交叉渐变。另有均衡器、环绕音效和歌词显示选项。
- **个性化**：中文和 English 界面，并根据系统外观适配明暗主题。

智能混音与固定时长交叉渐变是两种可选的自动切歌方式。若歌曲不适合分析或设备无法处理，应用会继续普通播放。

## 获取应用

前往 [GitHub Releases](https://github.com/eason204646-droid/Purely-Music/releases) 下载已发布的安装包。应用要求 **Android 8.0（API 26）或更高版本**。

## 从源码构建

### 环境要求

- JDK 17
- Android SDK 36
- Git

### 构建 Debug APK

```bash
git clone https://github.com/eason204646-droid/Purely-Music.git
cd Purely-Music
```

macOS / Linux：

```bash
./gradlew assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
```

APK 输出到 `app/build/outputs/apk/debug/purelymusic.apk`。也可以用 Android Studio 打开仓库并运行 `app` 配置。

### 可选：配置联网服务

歌曲信息补全和歌词翻译使用 `MUSIC_API_KEY`。在项目根目录的 `local.properties` 中添加你自己的服务密钥，然后重新构建：

```properties
MUSIC_API_KEY=your_api_key
```

没有配置密钥时，应用仍可用于本地播放。请勿将密钥提交到 Git，也不要在公开分发的 APK 中复用个人密钥：该值会被打包进客户端，无法作为服务端机密保管。

## 项目结构

```text
app/src/main/java/com/music/purelymusic/
├── data/       Room 数据库、文件存储与在线元数据获取
├── playback/   后台播放、音频分析与过渡渲染
├── ui/         Jetpack Compose 界面
├── utils/      歌词解析、语言检测与偏好设置
└── viewmodel/  界面状态和应用逻辑
```

## 开发

项目使用 Kotlin、Jetpack Compose、Material 3、AndroidX Media3、Room、Retrofit 和 Gradle Kotlin DSL。GitHub Actions 会在推送到 `main` 或创建 Pull Request 时运行单元测试、Android Lint 和 Debug 构建，并编译 Android 测试。

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Windows PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

## 文档

- [使用说明](help/使用说明.md)
- [功能特性](help/功能特性.md)
- [疑难解答](help/疑难解答.md)
- [歌词翻译说明](help/歌词翻译说明.md)

## 许可证

本项目采用 [木兰宽松许可证，第 2 版（Mulan PSL v2）](LICENSE)。
