package com.music.PurelyPlayer.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp

@Composable
fun ImportInfoDialog(
    onConfirm: (String, String, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    // 使用 remember 确保弹窗内的输入文字不会因为刷新而消失
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "完善歌曲信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("歌名") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("歌手") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, artist, null)
                    }
                },
                enabled = title.isNotBlank() // 只有输入了歌名才能点确定
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}