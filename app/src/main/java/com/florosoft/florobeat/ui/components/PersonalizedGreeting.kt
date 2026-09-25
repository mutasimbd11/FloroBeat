package com.florosoft.florobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.florosoft.florobeat.R
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroTextMuted
import java.util.Calendar

/**
 * Personalized greeting area matching the reference design.
 *
 * Structure:
 * - Time-based greeting (GOOD MORNING / AFTERNOON / EVENING / NIGHT)
 * - Dynamic user name with vibrant mint underline pill accent
 * - Inspiring tagline ("Good music makes a better tomorrow.")
 */
@Composable
fun PersonalizedGreeting(
    userName: String?,
    modifier: Modifier = Modifier,
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> R.string.good_morning
            in 12..16 -> R.string.good_afternoon
            in 17..21 -> R.string.good_evening
            else -> R.string.good_night
        }
    }

    val displayName = remember(userName) {
        if (!userName.isNullOrBlank()) {
            userName.trim().split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: userName.trim()
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
    ) {
        // Soft cinematic atmospheric glow on the right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(180.dp)
                .height(110.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            FloroMint.copy(alpha = 0.08f),
                            Color(0xFF024E40).copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column {
            Text(
                text = stringResource(greeting),
                color = FloroTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            if (displayName != null) {
                Spacer(Modifier.height(3.dp))
                Column {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    // Vibrant mint accent underline pill
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(FloroMint),
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = stringResource(R.string.good_music_quote),
                color = FloroTextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 17.sp,
            )
        }
    }
}
