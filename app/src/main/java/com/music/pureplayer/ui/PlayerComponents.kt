package com.music.PurelyPlayer.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.PurelyPlayer.R
import com.music.PurelyPlayer.viewmodel.PlayerViewModel

/**
 * 底部播放悬浮条 (参考图二底部样式)
 * 衔接资料库与全屏播放页
 */
@Composable
fun BottomPlayerBar(viewModel: PlayerViewModel, onClick: () -> Unit) {
    val song = viewModel.currentSong ?: return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 歌曲封面 - 小圆角
            AsyncImage(
                model = song.coverUri ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.default_cover),
                placeholder = painterResource(R.drawable.default_cover)
            )

            // 歌曲信息 - 自动截断防止挤压按钮
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            // 播放/暂停快捷按钮
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(
                    imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * 导入歌曲信息填写对话框
 * 当用户选择文件后自动弹出
 */
@Composable
fun ImportInfoDialog(
    onConfirm: (String, String, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { coverUri = it }

    AlertDialog(
        onDismissRequest = { /* 强制处理 */ },
        title = { Text("完善歌曲信息", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("歌曲名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("歌手名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 封面选择按钮
                Button(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (coverUri == null)
                            MaterialTheme.colorScheme.primaryContainer
                        else Color(0xFF4CAF50) // 选好后变绿
                    )
                ) {
                    Text(if (coverUri == null) "选择歌曲封面" else "封面已选定 ✓")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, artist, coverUri) },
                enabled = title.isNotBlank() // 至少要填个歌名
            ) {
                Text("保存至资料库")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("跳过", color = Color.Gray)
            }
        }
    )
}