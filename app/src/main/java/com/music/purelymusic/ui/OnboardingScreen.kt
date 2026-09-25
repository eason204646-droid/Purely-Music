package com.music.purelymusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.purelymusic.R
import com.music.purelymusic.ui.theme.AppleRed
import kotlinx.coroutines.launch

private data class IntroductionPage(
    val eyebrow: String,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(language: String, onComplete: () -> Unit) {
    val chinese = language == "zh"
    val dark = isSystemInDarkTheme()
    val pages = if (chinese) {
        listOf(
            IntroductionPage("纯粹聆听", "你的音乐，随时开听", "把喜欢的歌曲放进自己的曲库，打开应用，就能开始聆听。"),
            IntroductionPage("轻松导入", "几步，建立自己的曲库", "导入单曲或批量选择文件。联网时，还能自动获取封面与歌词。"),
            IntroductionPage("沉浸其中", "让每一次聆听更投入", "跟随同步歌词，也可用播放列表与均衡器整理、调整你的音乐。")
        )
    } else {
        listOf(
            IntroductionPage("PURE LISTENING", "Your music, ready to play", "Bring the songs you love into your own library and start listening right away."),
            IntroductionPage("EASY IMPORT", "Make the library yours", "Import one song or select several files. Artwork and lyrics can be fetched when online."),
            IntroductionPage("LOSE YOURSELF IN IT", "More in every moment", "Follow synced lyrics, organize playlists, and tune the equalizer to your taste.")
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == pages.lastIndex

    BackHandler {
        if (pagerState.currentPage == 0) onComplete()
        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.widthIn(max = 620.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 18.dp, top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).background(AppleRed, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Purely Music", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                TextButton(onClick = onComplete) {
                    Text(if (chinese) "跳过" else "Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val compact = maxHeight < 480.dp
                    val artworkHeight = if (compact) 270.dp else 330.dp
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(horizontal = 30.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        OnboardingArtwork(pageIndex, chinese, dark, artworkHeight)
                        Spacer(Modifier.height(if (compact) 18.dp else 28.dp))
                        Text(
                            text = pages[pageIndex].eyebrow,
                            color = AppleRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = pages[pageIndex].title,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 30.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = pages[pageIndex].description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            lineHeight = 23.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 10.dp, bottom = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "0${pagerState.currentPage + 1}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(11.dp))
                    pages.indices.forEach { index ->
                        Box(
                            Modifier.padding(end = 6.dp)
                                .size(width = if (index == pagerState.currentPage) 18.dp else 6.dp, height = 6.dp)
                                .background(
                                    if (index == pagerState.currentPage) AppleRed
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                    CircleShape
                                )
                        )
                    }
                }
                GlassPressable(
                    modifier = Modifier.width(if (chinese) 150.dp else 174.dp).height(54.dp),
                    onClick = {
                        if (lastPage) onComplete()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                ) {
                    LiquidGlass(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp), dark = dark)
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (lastPage) (if (chinese) "开始使用" else "Start listening")
                                else (if (chinese) "继续" else "Continue"),
                            color = AppleRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AppleRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingArtwork(pageIndex: Int, chinese: Boolean, dark: Boolean, height: Dp) {
    Box(modifier = Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(height * 0.98f).background(
                Brush.radialGradient(listOf(AppleRed.copy(alpha = if (dark) 0.24f else 0.13f), Color.Transparent)),
                CircleShape
            )
        )
        when (pageIndex) {
            0 -> ListeningArtwork(chinese, dark, height)
            1 -> LibraryArtwork(chinese, dark, height)
            else -> LyricsArtwork(chinese, height)
        }
    }
}

@Composable
private fun ListeningArtwork(chinese: Boolean, dark: Boolean, height: Dp) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.onboarding_album_art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.offset(y = (-24).dp).size(height * 0.67f)
                .shadow(15.dp, RoundedCornerShape(27.dp)).clip(RoundedCornerShape(27.dp))
        )
        LiquidGlass(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                .widthIn(max = 290.dp).fillMaxWidth().height(70.dp),
            shape = RoundedCornerShape(24.dp),
            dark = dark
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.onboarding_album_art),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (chinese) "正在播放" else "NOW PLAYING",
                        color = AppleRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                    Text(
                        if (chinese) "纯粹聆听" else "Pure listening",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(Modifier.size(34.dp).background(AppleRed, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryArtwork(chinese: Boolean, dark: Boolean, height: Dp) {
    LiquidGlass(
        modifier = Modifier.widthIn(max = 318.dp).fillMaxWidth()
            .height(if (height < 300.dp) 244.dp else height * 0.78f),
        shape = RoundedCornerShape(29.dp),
        dark = dark
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(21.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (chinese) "资料库" else "Library",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(Modifier.size(32.dp).background(AppleRed.copy(alpha = 0.13f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AppleRed, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            LibraryPreviewRow(
                icon = Icons.Default.MusicNote,
                title = if (chinese) "导入歌曲" else "Import songs",
                detail = if (chinese) "选择设备中的音乐" else "Choose files on your device"
            )
            Spacer(Modifier.height(10.dp))
            LibraryPreviewRow(
                icon = Icons.Default.LibraryMusic,
                title = if (chinese) "批量导入" else "Import multiple",
                detail = if (chinese) "一次添加多首歌曲" else "Add several songs at once"
            )
        }
    }
}

@Composable
private fun LibraryPreviewRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp)
            .background(AppleRed.copy(alpha = 0.075f), RoundedCornerShape(17.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(AppleRed.copy(alpha = 0.14f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = AppleRed, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LyricsArtwork(chinese: Boolean, height: Dp) {
    Box(
        modifier = Modifier.widthIn(max = 318.dp).fillMaxWidth()
            .height(if (height < 300.dp) 244.dp else height * 0.79f)
            .clip(RoundedCornerShape(29.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF29272C), Color(0xFF17171C))))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(29.dp))
    ) {
        Column(Modifier.fillMaxSize().padding(21.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.onboarding_album_art),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (chinese) "正在播放" else "NOW PLAYING", color = AppleRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Purely Music", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(23.dp))
            Text(
                if (chinese) "让旋律慢慢流动" else "Let the melody flow",
                color = Color.White.copy(alpha = 0.42f), fontSize = 13.sp
            )
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(24.dp).background(AppleRed, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (chinese) "此刻，认真听" else "Listen to this moment",
                    color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(13.dp))
            Text(
                if (chinese) "每一刻都有回响" else "Every moment resonates",
                color = Color.White.copy(alpha = 0.42f), fontSize = 13.sp
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(3.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) {
                    Box(Modifier.fillMaxWidth(0.43f).fillMaxHeight().background(AppleRed, CircleShape))
                }
                Spacer(Modifier.width(11.dp))
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
