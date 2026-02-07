# Purely Player

一款开源的 Android 音乐播放器，采用现代化的 UI 设计，支持本地音乐播放、歌单管理、歌词显示等功能。

## 特性

### 核心功能
- 🎵 **本地音乐播放** - 支持导入和管理本地音乐文件
- 📝 **歌词显示** - 支持 LRC 格式歌词同步显示，可切换封面/歌词视图
- 📚 **歌单管理** - 创建、编辑和管理自定义歌单
- 🔍 **歌曲编辑** - 支持编辑歌曲信息（标题、歌手、封面、歌词）
- 🎨 **精美 UI** - 采用红色主题，简洁大气的设计风格
- 🌙 **深色模式** - 自动适配系统深色模式
- 🔔 **媒体通知** - 完整的媒体通知栏支持，包括进度条拖动
- 📻 **后台播放** - 支持后台持续播放
- 🎭 **封面虚化** - 播放界面背景使用歌曲封面虚化效果

### 界面特点
- **资料库页** - 展示所有歌单和歌曲，支持网格视图
- **播放列表详情** - 美观的封面展示，支持顺序/随机播放
- **播放界面** - 沉浸式设计，支持下滑返回手势
- **歌词界面** - 歌词高亮显示，自动滚动定位
- **底部播放条** - MiniPlayer 悬浮播放控制

### 交互优化
- 长按歌曲显示操作菜单（收藏、编辑、删除）
- 长按歌单显示操作菜单（删除）
- 直接在歌单详情页添加/删除歌曲
- 流畅的页面切换和动画效果

## 截图预览

### 🎵 播放界面
![播放界面](https://github.com/user-attachments/assets/f14a75b6-d5ea-4d98-a13b-7fbbe07865da)
沉浸式播放体验，背景采用歌曲封面虚化效果，支持下滑返回手势

### 📝 歌词界面
![歌词界面](https://github.com/user-attachments/assets/bd9a13c9-70e1-4d0f-8d75-1d4b3abddfa9)
支持 LRC 格式歌词同步显示，歌词高亮显示并自动滚动定位，可切换封面/歌词视图

### 🏠 主页
![主页](https://github.com/user-attachments/assets/571ae786-f333-4708-b31c-81092470c8a9)
简洁大气的主页设计，底部 MiniPlayer 悬浮播放控制，方便快捷

### 📚 资料库
![资料库](https://github.com/user-attachments/assets/24167232-4ab1-4eee-8cd8-4c59b3f992c9)
展示所有歌单和歌曲，支持网格视图，美观的封面展示

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **设计系统**: Material 3
- **播放引擎**: Media3 (ExoPlayer)
- **数据库**: Room
- **图片加载**: Coil
- **依赖管理**: Gradle (Kotlin DSL)

## 开发环境

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17 或更高版本
- Android SDK 35 (compileSdkVersion 35)
- 最低支持 Android 8.0 (API 26)

## 构建

```bash
# 克隆项目
git clone https://github.com/eason204646-droid/Purely-Music.git

# 进入项目目录
cd Purely-Music

# 构建项目
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

## 许可证

本项目采用 [Mulan PSL v2](http://license.coscl.org.cn/MulanPSL2) 开源许可证。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- GitHub: [@eason204646-droid](https://github.com/eason204646-droid)

---

**享受音乐，享受生活！** 🎵