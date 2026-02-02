# PurelyPlayer 🎵

一款基于 Jetpack Compose 构建的极简风格本地音乐播放器。专注于丝滑的交互体验与沉浸式的歌词展示。

![License](https://img.shields.io/badge/License-MulanPSL--2.0-blue.svg)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)

---

## ✨ 功能特性

* **沉浸式 UI**：采用 Jetpack Compose 全新构建，支持动态模糊背景。
* **智能歌词**：
    * 支持 `.lrc` 文件解析。
    * 歌词自动滚动，锁定屏幕上方 1/4 黄金视线处。
    * 当前播放歌词支持动态发光与字号缩放动画。
* **系统级交互**：
    * 完整支持系统通知栏、锁屏及控制中心操作。
    * 支持蓝牙设备切换与进度条远程拖动。
* **媒体库管理**：
    * 自动扫描本地音频。
    * 支持自定义编辑歌曲封面、歌名、歌手及关联歌词。
    * 基于 Room 数据库的本地持久化存储。
* **高性能架构**：采用 ViewModel + Coroutines 驱动，保证 UI 零卡顿。

---

## 📸 界面预览

| 播放界面 | 资料库 | 歌词模式 |
| :--- | :--- | :--- |
| ![Playback](https://via.placeholder.com/200x400?text=Playback+UI) | ![Library](https://via.placeholder.com/200x400?text=Library+UI) | ![Lyrics](https://via.placeholder.com/200x400?text=Lyrics+UI) |

---

## 🚀 快速上手

### 开发环境要求
* Android Studio Ladybug (2024.2.1) 或更高版本。
* JDK 17。
* Android SDK 24+ (Android 7.0+)。

### 编译运行
1.  克隆仓库：
    ```bash
    git clone [https://github.com/your-username/PurelyPlayer.git](https://github.com/your-username/PurelyPlayer.git)
    ```
2.  使用 Android Studio 打开项目。
3.  同步 Gradle 并直接运行 `app` 模块。

---

## 🛠 技术栈

* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Media**: [Media3 / MediaSession](https://developer.android.com/guide/topics/media/media3) (支持系统控制)
* **Database**: [Room](https://developer.android.com/training/data-storage/room)
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
* **Architecture**: MVVM + Kotlin Coroutines

---

## ⚖️ 开源协议

本项目采用 **木兰宽松许可证, 第2版 (Mulan PSL v2)** 协议授权。详细内容请参阅 [LICENSE](LICENSE) 文件。

---

## 🤝 贡献与反馈

如果你有任何建议或发现了 Bug，欢迎提交 Issue 或 Pull Request。
