package com.music.purelymusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

internal class GlassMenuHostState {
    internal data class Menu(
        val key: Any,
        val anchor: Offset,
        val offset: IntOffset,
        val onDismissRequest: () -> Unit,
        val content: @Composable ColumnScope.() -> Unit
    )

    var menu by mutableStateOf<Menu?>(null)
        private set

    fun show(menu: Menu) {
        if (this.menu?.key != menu.key) this.menu?.onDismissRequest?.invoke()
        this.menu = menu
    }

    fun move(key: Any, anchor: Offset) {
        val current = menu ?: return
        if (current.key == key && current.anchor != anchor) menu = current.copy(anchor = anchor)
    }

    fun hide(key: Any) {
        if (menu?.key == key) menu = null
    }
}

internal val LocalGlassMenuHost = compositionLocalOf<GlassMenuHostState?> { null }

/** The menu and its backdrop stay in the activity window, where Haze can sample the live page. */
@Composable
internal fun GlassMenuHost(state: GlassMenuHostState, modifier: Modifier = Modifier) {
    val menu = state.menu
    var origin by remember { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val margin = with(density) { 12.dp.roundToPx() }
    BackHandler(enabled = menu != null) { state.menu?.onDismissRequest?.invoke() }

    Box(
        modifier = modifier.fillMaxSize().onGloballyPositioned {
            origin = it.positionInWindow()
            bounds = it.size
        }
    ) {
        if (menu != null) {
            Box(
                Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = menu.onDismissRequest
                )
            )
            val anchorX = (menu.anchor.x - origin.x).roundToInt()
            val anchorY = (menu.anchor.y - origin.y).roundToInt()
            val desiredX = anchorX + menu.offset.x
            val desiredY = anchorY + menu.offset.y
            val x = desiredX.coerceIn(margin, (bounds.width - menuSize.width - margin).coerceAtLeast(margin))
            val y = if (desiredY + menuSize.height + margin <= bounds.height) {
                desiredY
            } else {
                anchorY - menuSize.height - menu.offset.y
            }.coerceIn(margin, (bounds.height - menuSize.height - margin).coerceAtLeast(margin))
            GlassInlineMenu(
                expanded = true,
                modifier = Modifier.offset { IntOffset(x, y) }.onGloballyPositioned { menuSize = it.size },
                content = menu.content
            )
        }
    }
}
