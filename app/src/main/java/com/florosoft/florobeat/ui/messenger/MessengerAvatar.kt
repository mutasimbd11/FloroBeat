package com.florosoft.florobeat.ui.messenger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.florosoft.florobeat.data.messenger.MessengerUser
import com.florosoft.florobeat.ui.theme.FloroDeepTeal
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroObsidian
import dev.chrisbanes.haze.HazeState
import java.util.Locale

@Composable
fun MessengerAvatar(
    user: MessengerUser,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showOnlineIndicator: Boolean = true,
    hazeState: HazeState? = null,
) {
    val indicatorSize = (size.value * 0.28f).coerceIn(10f, 18f).dp
    val fontSize = (size.value * 0.4f).sp

    Box(modifier = modifier) {
        if (!user.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
            )
        } else {
            val initial = user.displayName.trim().take(1).ifBlank {
                user.username.trim().take(1)
            }.uppercase(Locale.ROOT).ifBlank { "?" }

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(FloroDeepTeal, FloroMint))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    color = FloroObsidian,
                    fontSize = fontSize,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        if (showOnlineIndicator && user.isOnline) {
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .align(Alignment.BottomEnd),
            )
        }
    }
}
