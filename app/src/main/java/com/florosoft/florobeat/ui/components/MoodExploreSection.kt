package com.florosoft.florobeat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.florosoft.florobeat.R
import com.florosoft.florobeat.ui.icons.FloroBeatIcons
import com.florosoft.florobeat.ui.theme.FloroDarkCardBorder
import com.florosoft.florobeat.ui.theme.MoodChill
import com.florosoft.florobeat.ui.theme.MoodFocus
import com.florosoft.florobeat.ui.theme.MoodParty
import com.florosoft.florobeat.ui.theme.MoodRomantic
import com.florosoft.florobeat.ui.theme.MoodSad
import com.florosoft.florobeat.ui.theme.MoodSleep

data class MoodItem(
    val title: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val query: String = title,
)

private val DefaultMoods = listOf(
    MoodItem(
        title = "Chill",
        icon = FloroBeatIcons.Leaf,
        gradientColors = listOf(Color(0xFF0F382B), Color(0xFF092019)),
    ),
    MoodItem(
        title = "Romantic",
        icon = FloroBeatIcons.Heart,
        gradientColors = listOf(Color(0xFF4C1D2C), Color(0xFF260D15)),
    ),
    MoodItem(
        title = "Sad",
        icon = FloroBeatIcons.Rain,
        gradientColors = listOf(Color(0xFF162D4A), Color(0xFF0A1728)),
    ),
    MoodItem(
        title = "Party",
        icon = FloroBeatIcons.Sparkles,
        gradientColors = listOf(Color(0xFF381A4E), Color(0xFF1E0B2D)),
    ),
    MoodItem(
        title = "Focus",
        icon = FloroBeatIcons.Target,
        gradientColors = listOf(Color(0xFF143B3B), Color(0xFF0B2121)),
    ),
    MoodItem(
        title = "Sleep",
        icon = FloroBeatIcons.Moon,
        gradientColors = listOf(Color(0xFF1A2244), Color(0xFF0B0F24)),
    ),
)

/**
 * Explore by Mood horizontal card carousel matching the reference design.
 */
@Composable
fun MoodExploreSection(
    onMoodClick: (title: String) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FloroSectionHeader(
            title = stringResource(R.string.explore_by_mood),
            actionLabel = stringResource(R.string.see_all) + " ›",
            onActionClick = onSeeAllClick,
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = DefaultMoods,
                key = { it.title },
            ) { mood ->
                MoodCard(
                    mood = mood,
                    onClick = { onMoodClick(mood.title) },
                )
            }
        }
    }
}

@Composable
private fun MoodCard(
    mood: MoodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .width(100.dp)
            .height(66.dp)
            .clip(cardShape)
            .background(Brush.verticalGradient(mood.gradientColors))
            .border(1.dp, FloroDarkCardBorder, cardShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = mood.icon,
                contentDescription = mood.title,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = mood.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
