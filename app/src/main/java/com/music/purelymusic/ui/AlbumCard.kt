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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.model.Album
import com.music.purelymusic.ui.utils.AppDimensions

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(AppDimensions.homeRecentCardWidth())
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = album.coverUri ?: R.drawable.default_cover,
            contentDescription = null,
            modifier = Modifier
                .size(AppDimensions.homeRecentCardWidth())
                .clip(RoundedCornerShape(AppDimensions.cornerRadiusM())),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(AppDimensions.spacingS()))
        Text(
            text = album.name,
            color = Color.Black,
            fontSize = AppDimensions.textM().value.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            color = Color.Gray,
            fontSize = AppDimensions.textS().value.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}