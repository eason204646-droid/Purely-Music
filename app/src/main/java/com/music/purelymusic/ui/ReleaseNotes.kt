package com.music.purelymusic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.purelymusic.ui.theme.AppleRed

/** Keep this object as the sole editable source for the first-launch update sheet. */
object ReleaseNotes {
    const val version = "3.0"
    val zhHighlights = listOf(
        Triple(Icons.Default.AutoAwesome, "Liquid Glass 视觉系统", "导航、悬浮播放条和核心控件采用更轻盈的玻璃材质与触感反馈。"),
        Triple(Icons.Default.Translate, "更可靠的歌词翻译", "分批保留行标识，并会自动补翻遗漏句子。"),
        Triple(Icons.Default.GraphicEq, "均衡器升级", "新增场景预设与更直观的频段控制。")
    )
    val enHighlights = listOf(
        Triple(Icons.Default.AutoAwesome, "Liquid Glass", "A lighter material system for navigation, now playing, and controls."),
        Triple(Icons.Default.Translate, "More reliable lyrics", "Batch markers and automatic retries prevent missing translated lines."),
        Triple(Icons.Default.GraphicEq, "Equalizer refined", "New listening presets and clearer band controls.")
    )
}

@Composable
fun WhatsNewSheet(language: String, onDismiss: () -> Unit) {
    val items = if (language == "zh") ReleaseNotes.zhHighlights else ReleaseNotes.enHighlights
    LiquidGlass(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)
    ) {
        Column {
            Text(if (language == "zh") "Purely Music ${ReleaseNotes.version}" else "Purely Music ${ReleaseNotes.version}", color = AppleRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text(if (language == "zh") "听感，焕然一新。" else "A more fluid way to listen.", fontWeight = FontWeight.Bold, fontSize = 27.sp)
            Spacer(Modifier.height(20.dp))
            items.forEach { (icon, title, detail) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LiquidGlass(modifier = Modifier.size(40.dp), shape = CircleShape) {
                        Icon(icon, null, tint = AppleRed, modifier = Modifier.align(Alignment.Center).size(20.dp))
                    }
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            GlassPressable(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
                LiquidGlass(modifier = Modifier.fillMaxWidth().height(50.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                Text(if (language == "zh") "开始体验" else "Explore now", color = AppleRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}
