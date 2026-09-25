package com.music.purelymusic.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import com.music.purelymusic.ui.theme.AppleRed

/** The activity's full-page source is separate from sources used by individual controls. */
val LocalGlassDialogHazeState = compositionLocalOf<HazeState?> { null }

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    containerColor: Color = Color(0xFFF9F9FC),
    shape: Shape = RoundedCornerShape(28.dp),
    properties: DialogProperties = DialogProperties()
) {
    val hazeState = LocalGlassDialogHazeState.current
    val glassModifier = if (hazeState == null) {
        modifier
    } else {
        modifier.clip(shape).hazeEffect(hazeState) {
            blurRadius = 18.dp
            noiseFactor = 0.02f
            backgroundColor = containerColor.copy(alpha = 1f)
        }
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = glassModifier.border(1.dp, Color(0xFF87919D).copy(alpha = 0.32f), shape),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor.copy(alpha = if (hazeState == null) 0.94f else 0.70f),
        tonalElevation = 0.dp,
        properties = properties
    )
}

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val hazeState = LocalGlassDialogHazeState.current
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        CompositionLocalProvider(LocalLiquidGlassHazeState provides hazeState, content = content)
    }
}

/** Dialog fields share the dialog's glass surface without blurring the page a second time. */
@Composable
fun GlassDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color(0xFF17171B),
            unfocusedTextColor = Color(0xFF17171B),
            disabledTextColor = Color(0xFF17171B).copy(alpha = 0.55f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = AppleRed,
            unfocusedIndicatorColor = Color(0xFF87919D).copy(alpha = 0.45f),
            disabledIndicatorColor = Color(0xFF87919D).copy(alpha = 0.25f),
            errorIndicatorColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = AppleRed,
            unfocusedLabelColor = Color(0xFF3F4650),
            disabledLabelColor = Color(0xFF3F4650).copy(alpha = 0.55f),
            cursorColor = AppleRed,
            errorCursorColor = MaterialTheme.colorScheme.error
        )
    )
}
