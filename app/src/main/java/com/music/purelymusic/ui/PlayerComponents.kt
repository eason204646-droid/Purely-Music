//Copyright (c) [2026] [eason204646]
//[purelymusic] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//Mulan Permissive Software License，Version 2
//
//Mulan Permissive Software License，Version 2 (Mulan PSL v2)
//
//January 2020 http://license.coscl.org.cn/MulanPSL2
package com.music.purelymusic.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
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
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.viewmodel.PlayerViewModel

/**
 * 底部播放悬浮条 (参考图二底部样式)
 * 衔接资料库与全屏播放页
 */
@OptIn(UnstableApi::class)
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
                    .size(56.dp)
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