package com.music.purelymusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.purelymusic.ui.theme.RedLight
import com.music.purelymusic.ui.theme.RedPrimary
import com.music.purelymusic.ui.utils.AppDimensions
import com.music.purelymusic.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val bandLevels = viewModel.equalizerBandLevels
    val bandRanges = viewModel.equalizerBandFrequencies
    val levelRange = viewModel.equalizerLevelRange

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0D0D0D),
                        Color(0xFF000000)
                    )
                )
            )
    ) {
        val blurredBackground = viewModel.blurredBackground
        if (blurredBackground != null) {
            Image(
                bitmap = blurredBackground.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimensions.paddingScreen(), vertical = AppDimensions.spacingM()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (viewModel.currentLanguage == "zh") "返回" else "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (viewModel.currentLanguage == "zh") "均衡器" else "Equalizer",
                    color = Color.White,
                    fontSize = AppDimensions.textXL().value.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (bandLevels.isNotEmpty()) {
                    IconButton(onClick = viewModel::resetEqualizerBands) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (viewModel.currentLanguage == "zh") "重置" else "Reset",
                            tint = RedPrimary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimensions.paddingScreen()),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingM())
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimensions.cornerRadiusL()),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimensions.paddingCard()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = RedLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (viewModel.currentLanguage == "zh") "应用内均衡器" else "In-App Equalizer",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = AppDimensions.textL().value.sp
                            )
                            Text(
                                text = if (viewModel.currentLanguage == "zh") "直接在播放器内调整各频段增益" else "Adjust band gain directly inside the player",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = AppDimensions.textS().value.sp
                            )
                        }
                    }
                }

                if (bandLevels.isEmpty() || bandRanges.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimensions.cornerRadiusL()),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
                    ) {
                        Text(
                            text = if (viewModel.currentLanguage == "zh") {
                                "请先开始播放歌曲，均衡器准备好后这里会显示频段滑块。"
                            } else {
                                "Start playback first. Band sliders will appear here once the equalizer is ready."
                            },
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = AppDimensions.textM().value.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimensions.paddingCard())
                        )
                    }
                } else {
                    bandLevels.forEachIndexed { index, level ->
                        val frequencies = bandRanges.getOrNull(index)
                        val title = frequencies?.let { formatBandTitle(it.first, it.second, viewModel.currentLanguage) }
                            ?: if (viewModel.currentLanguage == "zh") "频段 ${index + 1}" else "Band ${index + 1}"
                        val subtitle = frequencies?.let { formatBandSubtitle(it.first, it.second, viewModel.currentLanguage) }
                        val levelDb = level / 100f

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimensions.cornerRadiusL()),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(AppDimensions.paddingCard()),
                                verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingS())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            color = Color.White,
                                            fontSize = AppDimensions.textM().value.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (subtitle != null) {
                                            Text(
                                                text = subtitle,
                                                color = Color.White.copy(alpha = 0.68f),
                                                fontSize = AppDimensions.textS().value.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = formatDbLabel(levelDb),
                                        color = RedPrimary,
                                        fontSize = AppDimensions.textS().value.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = level.toFloat(),
                                    onValueChange = { viewModel.updateEqualizerBandLevel(index, it.roundToInt().toShort()) },
                                    valueRange = levelRange.first.toFloat()..levelRange.second.toFloat()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(1.dp))
            }
        }
    }
}

private fun formatBandTitle(startHz: Int, endHz: Int, language: String): String {
    val centerHz = (startHz + endHz) / 2f
    val isZh = language == "zh"
    return when {
        centerHz < 90f -> if (isZh) "低频鼓点" else "Bass Drums"
        centerHz < 180f -> if (isZh) "贝斯" else "Bass Guitar"
        centerHz < 420f -> if (isZh) "男声厚度" else "Male Vocals"
        centerHz < 1000f -> if (isZh) "钢琴 / 吉他" else "Piano / Guitar"
        centerHz < 2500f -> if (isZh) "人声清晰度" else "Vocal Clarity"
        centerHz < 6000f -> if (isZh) "弦乐 / 亮度" else "Strings / Presence"
        else -> if (isZh) "空气感 / 镲片" else "Air / Cymbals"
    }
}

private fun formatBandSubtitle(startHz: Int, endHz: Int, language: String): String {
    val centerHz = (startHz + endHz) / 2f
    val isZh = language == "zh"
    return when {
        centerHz < 90f -> if (isZh) "鼓点下潜、震感" else "Depth and thump"
        centerHz < 180f -> if (isZh) "贝斯线条、低频量感" else "Bass body and groove"
        centerHz < 420f -> if (isZh) "男声厚度、温暖感" else "Warmth and body"
        centerHz < 1000f -> if (isZh) "乐器主体、和声层次" else "Instrument body and mix"
        centerHz < 2500f -> if (isZh) "咬字、人声靠前感" else "Speech detail and focus"
        centerHz < 6000f -> if (isZh) "通透感、拨弦与擦弦细节" else "Presence and articulation"
        else -> if (isZh) "空气感、镲片闪耀感" else "Sparkle and air"
    }
}

private fun formatDbLabel(db: Float): String {
    return if (db > 0) {
        String.format(java.util.Locale.getDefault(), "+%.1f dB", db)
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f dB", db)
    }
}
