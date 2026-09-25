package com.florosoft.florobeat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Centralized Geometry, Spacing, and Elevation scale for FloroBeat.
 *
 * Replaces scattered hardcoded dimensions across screens with a cohesive design system.
 */
object FloroDesignSystem {

    // Radius / Shapes
    val RadiusPill = RoundedCornerShape(999.dp)
    val RadiusCardLarge = RoundedCornerShape(20.dp)
    val RadiusCardMedium = RoundedCornerShape(16.dp)
    val RadiusCardSmall = RoundedCornerShape(12.dp)
    val RadiusThumbnail = RoundedCornerShape(10.dp)
    val RadiusBadge = RoundedCornerShape(6.dp)

    // Spacing
    val PageGutter = 18.dp
    val SectionSpacing = 26.dp
    val ItemSpacing = 14.dp
    val RowSpacing = 10.dp
    val ContentPadding = 16.dp

    // Standardized Card Dimensions (Precomputed for zero-lag scrolling)
    val ShelfCardWidth = 144.dp
    val HeroCardWidth = 260.dp
    val HeroCardAspectRatio = 16f / 9.5f
    val RowThumbnailSize = 48.dp
    val MiniPlayerHeight = 60.dp
    val FloatingBarMaxWidth = 540.dp
    val MoodCardWidth = 106.dp
    val MoodCardHeight = 64.dp
    val MadeForYouHeroWidth = 220.dp
    val MadeForYouPillHeight = 48.dp

    // Touch Targets
    val MinTouchTarget = 48.dp
    val ActionIconSize = 24.dp
}

// Top-level aliases for direct, ergonomic imports
val FloroCardLargeShape = FloroDesignSystem.RadiusCardLarge
val FloroCardMediumShape = FloroDesignSystem.RadiusCardMedium
val FloroCardSmallShape = FloroDesignSystem.RadiusCardSmall
const val HeroCardAspectRatio = 16f / 9.5f

