# Purely Music

[项目地址](https://github.com/eason204646-droid/Purely-Music)

一款开源的 Android 音乐播放器，采用现代化的 UI 设计，支持本地音乐播放、歌单管理、歌词显示等功能。

## 更新日志

### v1.6&v1.7

#### 🆕 新增功能

- ✨ **自动获取封面，歌词和专辑** - 导入歌曲时，可以根据歌名和歌手自动从网易云音乐获取封面图片(apikey建议使用自己的，目前使用数据来自妖狐数据提供的https://api.yaohud.cn/api/music/wy)
- 🎨 **创建播放列表界面优化** - 创建播放列表时的封面和歌单名称区域采用浅红色主题设计

#### 🐛 修复

- 🔧 修复封面保存问题 - 自动获取的封面现在能正确保存并显示在资料库和主页中
- 🔧 优化封面下载逻辑 - 封面图片会自动下载到本地，确保离线也能正常显示

---

### v1.5

## 特性

### 核心功能

*   🎵 **本地音乐播放** - 支持导入和管理本地音乐文件
*   📝 **歌词显示** - 支持 LRC 格式歌词同步显示，可切换封面/歌词视图
*   📚 **歌单管理** - 创建、编辑和管理自定义歌单
*   🔍 **歌曲编辑** - 支持编辑歌曲信息（标题、歌手、封面、歌词）
*   🎨 **精美 UI** - 采用红色主题，简洁大气的设计风格
*   🌙 **深色模式** - 自动适配系统深色模式
*   🔔 **媒体通知** - 完整的媒体通知栏支持，包括进度条拖动
*   📻 **后台播放** - 支持后台持续播放
*   🎭 **封面虚化** - 播放界面背景使用歌曲封面虚化效果

### 界面特点

*   **资料库页** - 展示所有歌单和歌曲，支持网格视图
*   **播放列表详情** - 美观的封面展示，支持顺序/随机播放
*   **播放界面** - 沉浸式设计，支持下滑返回手势
*   **歌词界面** - 歌词高亮显示，自动滚动定位
*   **底部播放条** - MiniPlayer 悬浮播放控制

### 交互优化

*   长按歌曲显示操作菜单（收藏、编辑、删除）
*   长按歌单显示操作菜单（删除）
*   直接在歌单详情页添加/删除歌曲
*   流畅的页面切换和动画效果

## 截图预览

### 🎵 播放界面

<img width="393" height="699" alt="MuMuPlayer_lh9U1kT32c" src="https://p6-xtjj-sign.byteimg.com/tos-cn-i-73owjymdk6/3e5a00227d94469895678325e6dcf456~tplv-73owjymdk6-jj-mark-v1:0:0:0:0:5o6Y6YeR5oqA5pyv56S-5Yy6IEAg55So5oi3NDYxMDExNTkxMzc=:q75.awebp?rk3s=f64ab15b&x-expires=1771201864&x-signature=ez9fZn7B%2Bvpovl3GKTeTo39OugA%3D">

沉浸式播放体验，背景采用歌曲封面虚化效果，支持下滑返回手势

### 📝 歌词界面

<img width="393" height="699" alt="MuMuPlayer_MpZjTnjalZ" src="https://p6-xtjj-sign.byteimg.com/tos-cn-i-73owjymdk6/1f6d8b6b63e24118ab51302d10b84610~tplv-73owjymdk6-jj-mark-v1:0:0:0:0:5o6Y6YeR5oqA5pyv56S-5Yy6IEAg55So5oi3NDYxMDExNTkxMzc=:q75.awebp?rk3s=f64ab15b&x-expires=1771201864&x-signature=TEx%2FoGjocYxSMYXVJryeKLUyBCM%3D">

支持 LRC 格式歌词同步显示，歌词高亮显示并自动滚动定位，可切换封面/歌词视图

### 🏠 主页

<img width="393" height="699" alt="MuMuPlayer_lh9U1kT32c" src="https://p6-xtjj-sign.byteimg.com/tos-cn-i-73owjymdk6/59ae8f68cd0d455a9afa45f1704e9adf~tplv-73owjymdk6-jj-mark-v1:0:0:0:0:5o6Y6YeR5oqA5pyv56S-5Yy6IEAg55So5oi3NDYxMDExNTkxMzc=:q75.awebp?rk3s=f64ab15b&x-expires=1771201864&x-signature=baNLDYlniRQEP68Ncn2N%2FfbAAhU%3D">

简洁大气的主页设计，底部 MiniPlayer 悬浮播放控制，方便快捷

### 📚 资料库

<img width="393" height="699" alt="MuMuPlayer_prqatopbzX" src="https://p6-xtjj-sign.byteimg.com/tos-cn-i-73owjymdk6/efb791b57022425b8b7a940696083607~tplv-73owjymdk6-jj-mark-v1:0:0:0:0:5o6Y6YeR5oqA5pyv56S-5Yy6IEAg55So5oi3NDYxMDExNTkxMzc=:q75.awebp?rk3s=f64ab15b&x-expires=1771201864&x-signature=KSN34oC9KR8gKAsxYd%2Blljf0F7A%3D">

展示所有歌单和歌曲，支持网格视图，美观的封面展示

## 技术栈

*   **语言**: Kotlin
*   **UI 框架**: Jetpack Compose
*   **设计系统**: Material 3
*   **播放引擎**: Media3 (ExoPlayer)
*   **数据库**: Room
*   **图片加载**: Coil
*   **依赖管理**: Gradle (Kotlin DSL)

## 开发环境

*   Android Studio Hedgehog | 2023.1.1 或更高版本
*   JDK 17 或更高版本
*   Android SDK 35 (compileSdkVersion 35)
*   最低支持 Android 8.0 (API 26)

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

*   GitHub: [@eason204646-droid](https://github.com/eason204646-droid)

***

**享受音乐，享受生活！** 🎵
