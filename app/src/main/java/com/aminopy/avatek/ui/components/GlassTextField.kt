package com.aminopy.avatek.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aminopy.avatek.ui.theme.LocalPalette
import com.aminopy.avatek.ui.theme.bounceClick
import com.aminopy.avatek.ui.theme.glass

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    imeAction: ImeAction = ImeAction.Search,
    onImeAction: () -> Unit = {},
    focusRequester: FocusRequester? = null
) {
    val p = LocalPalette.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) p.accent else p.accent.copy(alpha = 0f), tween(260), label = "tfBorder")
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier.fillMaxWidth().glass(shape, strength = if (focused) 0.9f else 0.6f).border(1.5.dp, border, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) Icon(leading, null, tint = if (focused) p.accent else p.muted, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f).padding(horizontal = if (leading != null) 12.dp else 0.dp)) {
            if (value.isEmpty()) Text(placeholder, color = p.muted, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = source,
                textStyle = TextStyle(color = p.text, fontSize = 15.sp),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                modifier = Modifier.fillMaxWidth().let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close, "پاک‌کردن", tint = p.muted,
                modifier = Modifier.size(22.dp).bounceClick(pressedScale = 0.8f) { onValueChange("") }
            )
        }
    }
}
