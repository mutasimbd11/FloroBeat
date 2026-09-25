package com.florosoft.florobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.florosoft.florobeat.R
import com.florosoft.florobeat.data.model.HEADER_ART_PX
import com.florosoft.florobeat.data.model.ShelfItem
import com.florosoft.florobeat.data.model.artworkAt
import com.florosoft.florobeat.ui.icons.FloroBeatIcons
import com.florosoft.florobeat.ui.theme.FloroDarkCard
import com.florosoft.florobeat.ui.theme.FloroDarkCardBorder
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroTextMuted
import com.florosoft.florobeat.ui.theme.FloroTextSecondary

/**
 * Large featured music hero card matching the reference screenshot.
 *
 * Immersive glassmorphic container:
 * - Square album artwork on left
 * - "FEATURED" pill + song title + artist + equalizer duration in center
 * - Prominent circular mint play button on right
 */
@Composable
fun FeaturedHeroCard(
    item: ShelfItem?,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item == null) return

    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        FloroDarkCard,
                        Color(0xFF0F1822),
                    ),
                ),
            )
            .border(1.dp, FloroDarkCardBorder, cardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album Artwork
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A2634)),
            ) {
                AsyncImage(
                    model = item.thumbnailUrl?.artworkAt(HEADER_ART_PX),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }

            Spacer(Modifier.width(14.dp))

            // Metadata Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.featured),
                    color = FloroMint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.subtitle.ifBlank { "Featured Artist" },
                    color = FloroTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                // Duration with equalizer icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = FloroBeatIcons.Equalizer,
                        contentDescription = null,
                        tint = FloroMint,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "3:42",
                        color = FloroTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Large Circular Mint Play Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(FloroMint)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
