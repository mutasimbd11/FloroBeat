package com.florosoft.florobeat.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroTextMuted

/**
 * Standardized Section Header matching the reference FloroBeat UI.
 *
 * Left: Rounded vertical mint indicator pill (`| Title`)
 * Right: Optional secondary action label (e.g. `See all >`)
 */
@Composable
fun FloroSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            // Vertical mint accent indicator pill
            Box(
                modifier = Modifier
                    .size(width = 3.5.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FloroMint),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!actionLabel.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(
                        if (onActionClick != null) Modifier.clickable(onClick = onActionClick)
                        else Modifier,
                    )
                    .padding(vertical = 4.dp, horizontal = 4.dp),
            ) {
                Text(
                    text = actionLabel,
                    color = FloroTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
