# 翻译功能说明

## 实现的功能

1. **歌词语言检测**：自动检测歌词是否为中文（包括简体和繁体）
2. **翻译按钮**：在歌词界面右上角显示翻译按钮（仅在非中文歌词时显示）
3. **翻译API调用**：使用uapis.cn的翻译API将歌词翻译成中文
4. **翻译显示**：翻译后的歌词显示在原文下方，字体更小

## API配置

翻译API服务配置在 `PlayerViewModel.kt` 中：
```kotlin
private val translateService: TranslateApiService by lazy {
    Retrofit.Builder()
        .baseUrl("https://uapis.cn/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TranslateApiService::class.java)
}
```

## 请求格式

根据常见的翻译API格式，请求格式为：
```json
{
  "text": "Hello World",
  "targetLang": "zh"
}
```

## 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "translatedText": "你好世界",
    "sourceLang": "en"
  }
}
```

## 注意事项

如果API的实际格式与上述格式不同，需要调整 `TranslateApiService.kt` 和 `TranslateRequest`/`TranslateResponse` 数据类。

建议在首次使用前，使用curl或其他工具测试API：
```bash
curl -X POST "https://uapis.cn/api/translate/text" \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello World","targetLang":"zh"}'
```

## 文件修改列表

1. **新建文件**:
   - `app/src/main/java/com/music/purelymusic/model/TranslateApiService.kt` - 翻译API接口
   - `app/src/main/java/com/music/purelymusic/utils/LanguageDetector.kt` - 语言检测工具

2. **修改文件**:
   - `app/src/main/java/com/music/purelymusic/model/LyricModels.kt` - 添加translation字段
   - `app/src/main/java/com/music/purelymusic/ui/LyricView.kt` - 添加翻译按钮和翻译显示
   - `app/src/main/java/com/music/purelymusic/viewmodel/PlayerViewModel.kt` - 添加翻译功能