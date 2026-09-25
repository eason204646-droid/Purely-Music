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
package com.music.purelymusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6B86),
    onPrimary = Color(0xFF390914),
    primaryContainer = Color(0xFF58202F),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFE8ABB5),
    onSecondary = Color(0xFF43252B),
    tertiary = Color(0xFFFFB3AF),
    background = Color(0xFF101115),
    onBackground = Color(0xFFF4F3F7),
    surface = Color(0xFF1B1C22),
    onSurface = Color(0xFFF4F3F7),
    surfaceVariant = Color(0xFF292B33),
    onSurfaceVariant = Color(0xFFBABBC6),
    outline = Color(0xFF888A96),
    outlineVariant = Color(0xFF41434E),
    surfaceContainer = Color(0xFF22242B),
    surfaceContainerHigh = Color(0xFF2B2D35),
    inverseSurface = Color(0xFFF4F3F7),
    inverseOnSurface = Color(0xFF202127),
)

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    secondary = Red40,
    tertiary = Red60,
    background = GlassBackground,
    surface = GlassSurface,
    surfaceVariant = GlassSurfaceVariant,
    onBackground = Color(0xFF141418),
    onSurface = Color(0xFF141418),
    onSurfaceVariant = Color(0xFF575A66),
    outline = Color(0xFF737784),
    outlineVariant = Color(0xFFD2D4DD),
    surfaceContainer = Color(0xFFF3F3F7),
    surfaceContainerHigh = Color(0xFFEDEDF3),
)

@Composable
fun AMPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
