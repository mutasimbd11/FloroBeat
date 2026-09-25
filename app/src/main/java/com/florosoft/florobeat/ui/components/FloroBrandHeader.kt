package com.florosoft.florobeat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.florosoft.florobeat.R
import com.florosoft.florobeat.data.model.Account
import com.florosoft.florobeat.ui.icons.FloroBeatIcons
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroDarkSurfaceVariant
import com.florosoft.florobeat.ui.theme.FloroDarkCardBorder

/**
 * Top branding header matching the reference FloroBeat visual identity.
 *
 * Left: FloroBeat brand mark (FloroBeat_foreground.png) + "Floro" (white) "Beat" (mint) wordmark.
 * Right: Notification bell with notification indicator badge, and user profile avatar.
 */
@Composable
fun FloroBrandHeader(
    account: Account?,
    onNotificationClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo & Wordmark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        ) {
            Image(
                painter = painterResource(R.drawable.florobeat_foreground),
                contentDescription = "FloroBeat",
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Floro",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = "Beat",
                    color = FloroMint,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
            }
        }

        // Right Actions: Notification Bell + Profile Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Notification bell with badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onNotificationClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FloroBeatIcons.NotificationBell,
                    contentDescription = "Notifications",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp),
                )
                // Small indicator dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .clip(CircleShape)
                        .background(FloroMint),
                )
            }

            // User Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.2.dp, FloroDarkCardBorder, CircleShape)
                    .clickable(onClick = onAccountClick),
                contentAlignment = Alignment.Center,
            ) {
                if (account?.thumbnailUrl != null) {
                    AsyncImage(
                        model = account.thumbnailUrl,
                        contentDescription = account.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                    )
                } else if (account != null && (account.name.isNotBlank() || account.email.isNotBlank())) {
                    val initial = account.name.trim().firstOrNull()?.uppercaseChar()
                        ?: account.email.trim().firstOrNull()?.uppercaseChar()
                        ?: '?'
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FloroDarkSurfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial.toString(),
                            color = FloroMint,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FloroDarkSurfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FloroBeatIcons.AccountCircle,
                            contentDescription = "Account",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
