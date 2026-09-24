package com.music.purelymusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.purelymusic.R
import com.music.purelymusic.ui.theme.AppleRed

/** Calm, compact header shared by album and playlist detail screens. */
@Composable
fun CollectionHeader(
    cover: Any?,
    eyebrow: String,
    title: String,
    subtitle: String,
    playLabel: String,
    shuffleLabel: String,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = cover ?: R.drawable.default_cover,
                contentDescription = null,
                modifier = Modifier.size(132.dp).clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(start = 18.dp)) {
                Text(eyebrow.uppercase(), color = AppleRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text(title, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassPressable(modifier = Modifier.size(52.dp), onClick = onPlay) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(AppleRed), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(27.dp))
                }
            }
            Text(playLabel, modifier = Modifier.padding(start = 12.dp).weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            GlassPressable(onClick = onShuffle) {
                LiquidGlass(
                    modifier = Modifier.height(42.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    opacity = 0.78f,
                    highlightAlpha = 0.30f,
                    edgeAlpha = 0.34f
                ) {
                    Text(shuffleLabel, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** A deliberately quiet cover-derived backdrop for collection detail pages. */
@Composable
fun CollectionAtmosphere(
    cover: Any?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = cover ?: R.drawable.default_cover,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(38.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF9F9FC).copy(alpha = 0.79f),
                            Color(0xFFFCFCFE).copy(alpha = 0.91f),
                            Color(0xFFFDFDFF).copy(alpha = 0.96f)
                        )
                    )
                )
        )
    }
}
