package com.florosoft.florobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.florosoft.florobeat.R
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroDeepTeal
import com.florosoft.florobeat.ui.theme.FloroCardMediumShape

/**
 * The pill-shaped search field used at the top of the search page — a
 * magnifier that doubles as the submit button, a hint that gives way to typed
 * text, and a clear button once there is something to clear. Reused wherever
 * else the app needs the same "search this" affordance, such as filtering the
 * settings list.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    /** What the empty field says it is for — the one part that changes per screen. */
    placeholder: String = stringResource(R.string.search_hint),
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    // Both ways of saying "search this" do the same two things, so they're
    // written once here rather than twice.
    val submit = {
        onSubmit()
        focusManager.clearFocus()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(FloroCardMediumShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        if (query.isNotEmpty()) FloroMint.copy(alpha = 0.5f) else FloroMint.copy(alpha = 0.2f),
                        FloroDeepTeal.copy(alpha = 0.15f),
                    ),
                ),
                FloroCardMediumShape,
            )
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = stringResource(R.string.search),
            tint = if (query.isNotBlank()) FloroMint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(enabled = query.isNotBlank(), onClick = submit)
                .padding(6.dp),
        )
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(FloroMint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        // Emptying the field is also how the recent searches are got back to,
        // so it needs to be one tap rather than a held backspace.
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable {
                        onQueryChange("")
                        focusManager.clearFocus()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
