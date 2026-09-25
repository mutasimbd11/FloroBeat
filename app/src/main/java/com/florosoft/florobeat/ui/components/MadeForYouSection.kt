package com.florosoft.florobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.florosoft.florobeat.R
import com.florosoft.florobeat.data.model.CARD_ART_PX
import com.florosoft.florobeat.data.model.ShelfItem
import com.florosoft.florobeat.data.model.artworkAt
import com.florosoft.florobeat.ui.icons.FloroBeatIcons
import com.florosoft.florobeat.ui.theme.FloroDarkCard
import com.florosoft.florobeat.ui.theme.FloroDarkCardBorder
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroMintLight
import com.florosoft.florobeat.ui.theme.FloroTextMuted
import com.florosoft.florobeat.ui.theme.FloroTextSecondary

/**
 * Made for You section matching the reference design.
 *
 * Left: Large featured mix card with landscape artwork and quick play button.
 * Right: Vertical stack of 3 pill-shaped mix cards with dedicated colored badges.
 */
@Composable
fun MadeForYouSection(
    featuredMixItem: ShelfItem?,
    onFeaturedMixClick: (ShelfItem) -> Unit,
    onVibeClick: (title: String) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FloroSectionHeader(
            title = stringResource(R.string.made_for_you),
            actionLabel = stringResource(R.string.personalized_mixes) + " ›",
            onActionClick = onSeeAllClick,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER)
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Left: Large Featured Mix Card
            val mixShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .clip(mixShape)
                    .background(FloroDarkCard)
                    .border(1.dp, FloroDarkCardBorder, mixShape)
                    .clickable {
                        featuredMixItem?.let { onFeaturedMixClick(it) } ?: onVibeClick("Daily Mix")
                    },
            ) {
                // Background artwork / gradient
                if (featuredMixItem?.thumbnailUrl != null) {
                    AsyncImage(
                        model = featuredMixItem.thumbnailUrl.artworkAt(CARD_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1B323F),
                                        Color(0xFF0F1B24),
                                    ),
                                ),
                            ),
                    )
                }

                // Dark gradient scrim for legible text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF070A0E).copy(alpha = 0.85f),
                                ),
                                startY = 40f,
                            ),
                        ),
                )

                // Bottom Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp, end = 52.dp),
                ) {
                    Text(
                        text = featuredMixItem?.title?.ifBlank { null } ?: stringResource(R.string.daily_mix),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = featuredMixItem?.subtitle?.ifBlank { null } ?: stringResource(R.string.your_vibe_every_day),
                        color = FloroMintLight.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Quick Play Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FloroMint)
                        .clickable {
                            featuredMixItem?.let { onFeaturedMixClick(it) } ?: onVibeClick("Daily Mix")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Right: Column of 3 Recommendation Pills
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                RecommendationPill(
                    icon = FloroBeatIcons.Leaf,
                    iconBg = Color(0xFF10B981).copy(alpha = 0.20f),
                    iconTint = Color(0xFF10B981),
                    title = "Chill Vibes",
                    subtitle = "Relax • Focus • Flow",
                    onClick = { onVibeClick("Chill Vibes") },
                )
                RecommendationPill(
                    icon = FloroBeatIcons.Equalizer,
                    iconBg = Color(0xFFA855F7).copy(alpha = 0.20f),
                    iconTint = Color(0xFFA855F7),
                    title = "Bangla Hits",
                    subtitle = "Trending Now",
                    onClick = { onVibeClick("Bangla Hits") },
                )
                RecommendationPill(
                    icon = FloroBeatIcons.Thunder,
                    iconBg = Color(0xFFF59E0B).copy(alpha = 0.20f),
                    iconTint = Color(0xFFF59E0B),
                    title = "Workout Energy",
                    subtitle = "Move • Feel • Repeat",
                    onClick = { onVibeClick("Workout Energy") },
                )
            }
        }
    }
}

@Composable
private fun RecommendationPill(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(pillShape)
            .background(FloroDarkCard)
            .border(1.dp, FloroDarkCardBorder, pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = FloroTextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
