//Copyright (c) [2026] [eason204646]
//[purelyplayer] is licensed under Mulan PSL v2.
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
//
//Your reproduction, use, modification and distribution of the Software shall
//be subject to Mulan PSL v2 (this License) with the following terms and
//conditions:
//
//0. Definition
//
//Software means the program and related documents which are licensed under
//this License and comprise all Contribution(s).
//
//Contribution means the copyrightable work licensed by a particular
//Contributor under this License.
//
//Contributor means the Individual or Legal Entity who licenses its
//copyrightable work under this License.
//
//Legal Entity means the entity making a Contribution and all its
//Affiliates.
//
//Affiliates means entities that control, are controlled by, or are under
//common control with the acting entity under this License, ‘control’ means
//direct or indirect ownership of at least fifty percent (50%) of the voting
//power, capital or other securities of controlled or commonly controlled
//entity.
//
//1. Grant of Copyright License
//
//Subject to the terms and conditions of this License, each Contributor hereby
//grants to you a perpetual, worldwide, royalty-free, non-exclusive,
//irrevocable copyright license to reproduce, use, modify, or distribute its
//Contribution, with modification or not.
//
//2. Grant of Patent License
//
//Subject to the terms and conditions of this License, each Contributor hereby
//grants to you a perpetual, worldwide, royalty-free, non-exclusive,
//irrevocable (except for revocation under this Section) patent license to
//make, have made, use, offer for sale, sell, import or otherwise transfer its
//Contribution, where such patent license is only limited to the patent claims
//owned or controlled by such Contributor now or in future which will be
//necessarily infringed by its Contribution alone, or by combination of the
//Contribution with the Software to which the Contribution was contributed.
//The patent license shall not apply to any modification of the Contribution,
//and any other combination which includes the Contribution. If you or your
//Affiliates directly or indirectly institute patent litigation (including a
//cross claim or counterclaim in a litigation) or other patent enforcement
//activities against any individual or entity by alleging that the Software or
//any Contribution in it infringes patents, then any patent license granted to
//you under this License for the Software shall terminate as of the date such
//litigation or activity is filed or taken.
//
//3. No Trademark License
//
//No trademark license is granted to use the trade names, trademarks, service
//marks, or product names of Contributor, except as required to fulfill notice
//requirements in section 4.
//
//4. Distribution Restriction
//
//You may distribute the Software in any medium with or without modification,
//whether in source or executable forms, provided that you provide recipients
//with a copy of this License and retain copyright, patent, trademark and
//disclaimer statements in the Software.
//
//5. Disclaimer of Warranty and Limitation of Liability
//
//THE SOFTWARE AND CONTRIBUTION IN IT ARE PROVIDED WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED. IN NO EVENT SHALL ANY CONTRIBUTOR OR
//COPYRIGHT HOLDER BE LIABLE TO YOU FOR ANY DAMAGES, INCLUDING, BUT NOT
//LIMITED TO ANY DIRECT, OR INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING
//FROM YOUR USE OR INABILITY TO USE THE SOFTWARE OR THE CONTRIBUTION IN IT, NO
//MATTER HOW IT’S CAUSED OR BASED ON WHICH LEGAL THEORY, EVEN IF ADVISED OF
//THE POSSIBILITY OF SUCH DAMAGES.
//
//6. Language
//
//THIS LICENSE IS WRITTEN IN BOTH CHINESE AND ENGLISH, AND THE CHINESE VERSION
//AND ENGLISH VERSION SHALL HAVE THE SAME LEGAL EFFECT. IN THE CASE OF
//DIVERGENCE BETWEEN THE CHINESE AND ENGLISH VERSIONS, THE CHINESE VERSION
//SHALL PREVAIL.
//
//END OF THE TERMS AND CONDITIONS
//
//How to Apply the Mulan Permissive Software License，Version 2
//(Mulan PSL v2) to Your Software
//
//To apply the Mulan PSL v2 to your work, for easy identification by
//recipients, you are suggested to complete following three steps:
//
//i. Fill in the blanks in following statement, including insert your software
//name, the year of the first publication of your software, and your name
//identified as the copyright owner;
//
//ii. Create a file named "LICENSE" which contains the whole context of this
//License in the first directory of your software package;
//
//iii. Attach the statement to the appropriate annotated syntax at the
//beginning of each source file.
//
//Copyright (c) [Year] [name of copyright holder]
//[Software Name] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
package com.music.PurelyPlayer.ui

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
// 导入对应的包
import com.music.PurelyPlayer.viewmodel.PlayerViewModel
import com.music.PurelyPlayer.model.LrcLine
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow

@OptIn(UnstableApi::class)
@Composable
fun LyricView(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val lyrics = viewModel.lyricLines
    val currentIndex = viewModel.currentLyricIndex
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.TopStart
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val containerHeightDp = maxHeight

        // 🚩 目标位置：屏幕上方 1/4 (25%) 处
        val targetLinePx = containerHeightPx * 0.25f
        val targetLineDp = containerHeightDp * 0.25f

        // 估算单行行高（包含文字和间距）用于平滑偏移
        val lineHeightPx = with(density) { 56.dp.toPx() }

        LaunchedEffect(currentIndex) {
            if (lyrics.isNotEmpty() && currentIndex in lyrics.indices) {
                listState.animateScrollToItem(
                    index = currentIndex,
                    // 🚩 偏移计算：将当前项的顶部对齐到 1/4 处，并向上修正半行高度实现垂直居中于该线
                    scrollOffset = (-targetLinePx + (lineHeightPx / 2)).toInt()
                )
            }
        }

        if (lyrics.isEmpty()) {
            Text(
                text = "暂无歌词",
                color = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start, // 靠左对齐
                // 🚩 重点：顶部留 1/4，底部留 3/4。
                contentPadding = PaddingValues(
                    top = targetLineDp,
                    bottom = containerHeightDp - targetLineDp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                itemsIndexed(
                    items = lyrics,
                    key = { index, line -> "${line.time}_$index" }
                ) { index, line ->
                    val isCurrent = index == currentIndex

                    val fontSize by animateFloatAsState(targetValue = if (isCurrent) 24f else 18f, label = "fontSize")
                    val textAlpha by animateFloatAsState(targetValue = if (isCurrent) 1f else 0.4f, label = "textAlpha")

                    // 🚩 定义一个发光模糊半径的动画，当前行会有 12f 的光晕，非当前行无光晕
                    val shadowBlur by animateFloatAsState(
                        targetValue = if (isCurrent) 12f else 0f,
                        label = "shadowBlur"
                    )

                    Text(
                        text = line.content,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.4f).sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White,
                        textAlign = TextAlign.Start,
                        style = LocalTextStyle.current.copy(
                            // 🚩 核心：添加发光阴影效果
                            shadow = if (isCurrent) Shadow(
                                color = Color.White.copy(alpha = 0.6f), // 发光颜色：带透明度的白
                                offset = Offset(0f, 0f),                // 偏移为0，光晕均匀向四周扩散
                                blurRadius = shadowBlur                 // 模糊半径
                            ) else null
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(textAlpha)
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}