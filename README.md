# Purely Music

[GitHub 仓库](https://github.com/eason204646-droid/purely-music)

Purely Music 是一款开源 Android 本地音乐播放器，使用 Kotlin、Jetpack Compose 和 Material 3 构建，围绕本地导入、自动补全信息、歌词体验和播放质感持续打磨。

## 当前功能概览

### 1. 本地音乐导入

- 支持单曲导入
- 支持批量导入音频文件
- 导入后会将音乐、封面、歌词复制到应用私有目录，避免系统清理或权限失效后丢失
- 如果音频元数据不完整，会弹出补充信息界面手动完善

### 2. 自动获取信息

这是当前版本最突出的能力之一。

- 可根据音频元数据中的歌名、歌手自动联网补全信息
- 可自动获取歌曲封面
- 可自动获取歌词文件并关联到歌曲
- 设置页可切换自动获取源：
  - `网易云`：默认方案，优先稳定性
  - `混合`：用于曲库覆盖补充
- 设置页可开启或关闭“从元数据自动获取封面和歌词”
- 在单曲导入和资料库导入入口中都接入了自动获取流程
- 手动保存歌曲时，如果歌名和歌手完整，也会优先尝试自动获取封面与歌词

### 3. 播放与播放器体验

- 使用 Media3 / ExoPlayer 播放本地音频
- 支持播放 / 暂停 / 上一首 / 下一首
- 支持顺序播放与单曲循环
- 支持系统媒体会话控制
- 支持通知栏 / 系统侧的播放控制与进度拖动
- Mini Player 常驻底部，支持快速进入播放器
- 播放详情页支持下滑返回
- 播放页支持封面视图、歌词视图、播放队列视图切换
- 背景会根据封面生成模糊沉浸效果

### 4. 歌词能力

- 支持 LRC 歌词解析与同步滚动
- 支持点击歌词跳转播放进度
- 支持多行歌词样式
- 支持单行歌词样式
- 支持当前歌词发光效果开关
- 支持歌词敏感词过滤
- 非中文歌词支持翻译功能
- 提供翻译日志查看入口

### 5. 资料库与内容管理

- 首页展示最近播放与全部歌曲
- 资料库页展示歌曲、播放列表、专辑
- 支持创建自定义播放列表
- 支持为播放列表设置封面
- 支持在播放列表详情页添加歌曲
- 支持从播放列表中删除歌曲
- 支持拖拽排序播放列表内歌曲
- 支持专辑详情页查看与整张播放
- 导入歌曲时会根据专辑信息自动归档专辑

### 6. 设置与个性化

- 支持中文 / English 双语言切换
- 支持自动获取源切换
- 支持自动获取开关持久化保存
- 支持歌词样式、歌词发光、歌词过滤等偏好持久化保存
- 内置帮助文档查看

## 界面特性

- 红色主视觉主题
- 深浅主题适配
- Material 3 风格界面
- 封面大卡片与沉浸式播放器布局
- 资料库歌单 / 专辑横向卡片展示
- 更现代的底部导航与 Mini Player 组合

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose
- **设计系统**：Material 3
- **播放引擎**：Media3 / ExoPlayer
- **数据库**：Room
- **图片加载**：Coil
- **网络请求**：Retrofit
- **Markdown 渲染**：Markwon
- **构建系统**：Gradle Kotlin DSL

## 当前版本信息

- **applicationId**：`com.music.purelymusic`
- **minSdk**：26
- **targetSdk / compileSdk**：36
- **versionName**：`2.5`
- **versionCode**：26

## 构建

```bash
git clone https://github.com/eason204646-droid/purely-music.git
cd purely-music
./gradlew assembleDebug
```

## 下载

- 仓库主页：https://github.com/eason204646-droid/purely-music
- Releases：https://github.com/eason204646-droid/purely-music/releases

## 许可证

本项目采用 [Mulan PSL v2](http://license.coscl.org.cn/MulanPSL2) 开源许可证。
