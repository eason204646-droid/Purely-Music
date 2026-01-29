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