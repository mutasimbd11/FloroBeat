package com.florosoft.florobeat.ui.components

import com.florosoft.florobeat.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroMintLight
import com.florosoft.florobeat.ui.theme.FloroDeepTeal
import com.florosoft.florobeat.ui.theme.FloroDarkCard
import com.florosoft.florobeat.ui.icons.FloroBeatIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.florosoft.florobeat.data.model.ROW_ART_PX
import com.florosoft.florobeat.data.model.Song
import com.florosoft.florobeat.data.model.artworkAt
import com.florosoft.florobeat.data.settings.AppSettings
import com.florosoft.florobeat.ui.components.thumbnailBorder
import com.florosoft.florobeat.ui.haptics.Haptic
import com.florosoft.florobeat.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * The transport buttons' touch target. Material's default 48dp is what a bar
 * this slim is really made of, so it sets the height on its own.
 */
private val GLYPH_SLOT = 40.dp

/**
 * The play and skip glyphs themselves.
 *
 * Deliberately grown inside [GLYPH_SLOT] rather than by growing the slot: the
 * slot is level with the 40dp artwork opposite it, and it is the taller of the
 * two that sets the row's height — so a bigger slot would make the whole bar
 * taller, which is not what a bigger glyph is being asked for. At 32 there is
 * still 4dp of clearance to the slot's edge on every side.
 */
private val GLYPH_SIZE = 32.dp

/** The spinner that stands in for the play glyph, kept in proportion to it. */
private val SPINNER_SIZE = 22.dp

/**
 * The gap between the two transport controls.
 *
 * Material asks for at least 8dp between adjacent touch targets, and these had
 * none: two [GLYPH_SLOT] boxes sharing an edge, so the boundary between "pause"
 * and "skip" was a line with nothing either side of it. What space there looked
 * to be was only the margin each glyph keeps inside its own slot, and a thumb
 * lands on a target's edge far more often than it lands on a glyph's.
 *
 * Taken from the title's width rather than the bar's height, so nothing above
 * or below it moves.
 */
private val TRANSPORT_GAP = 8.dp

/**
 * Vertical padding, which with the 40dp artwork sets the bar's height at 56dp
 * and so its pill radius at 28.
 */
private val ROW_PADDING_VERTICAL = 8.dp

/**
 * Horizontal padding, deliberately larger than the vertical.
 *
 * A pill's ends are semicircles, so the edge nearest the artwork is not the
 * one beside it but the one curving away above and below it. At the artwork's
 * top corner that edge has already come 8.4dp in from the left — level with
 * where square corners would have put the whole side. Padding the ends by the
 * vertical figure would leave the artwork touching the curve; 12 clears it
 * with room, and reads as centred rather than jammed into the round.
 */
private val ROW_PADDING_HORIZONTAL = 12.dp

/**
 * The artwork's corner, on the 8dp every other thumbnail in the app carries.
 *
 * It used to be 7, picked so the bar's corner could sit concentric with it.
 * A pill has no corner to be concentric with — its radius is whatever half the
 * height happens to be — so that constraint is gone and the artwork can go
 * back to matching [SongRow].
 */
private val ART_CORNER = 8.dp

/** Distance that makes a horizontal drag an intentional track change. */
private val TRACK_SWIPE_THRESHOLD = 72.dp

/**
 * Shared gesture for both mini-player materials. A left swipe advances through
 * the queue; a right swipe goes back, matching the full player's artwork
 * gesture. Waiting until drag end prevents one long gesture from skipping more
 * than one item.
 */
@Composable
internal fun Modifier.miniPlayerTrackSwipe(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier {
    // Playback state updates can recompose the bar while a finger is down.
    // Keep the gesture coroutine alive through those updates while still
    // dispatching to the latest controller callbacks when the drag finishes.
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    return pointerInput(Unit) {
        val threshold = TRACK_SWIPE_THRESHOLD.toPx()
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onDragCancel = { totalDrag = 0f },
            onDragEnd = {
                when {
                    totalDrag <= -threshold -> currentOnNext()
                    totalDrag >= threshold -> currentOnPrevious()
                }
                totalDrag = 0f
            },
            onHorizontalDrag = { change, amount ->
                change.consume()
                totalDrag += amount
            },
        )
    }
}

/** Frosted mini player that rides just above the floating tab bar matching the FloroBeat reference design. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    hazeState: HazeState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    playbackPosition: com.florosoft.florobeat.playback.PlaybackPosition? = null,
    durationMs: Long = 0L,
    playbackProgress: Float = 0f,
    onQueueClick: (() -> Unit)? = null,
    hasPrevious: Boolean = true,
    hasNext: Boolean = true,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .padding(horizontal = PAGE_GUTTER)
            .clip(shape)
            .then(
                if (reduceDynamicBlur) {
                    Modifier.background(Color(0xFF0D141C))
                } else {
                    Modifier
                        .background(Color(0xFF0D141C).copy(alpha = 0.92f))
                        .optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.thin(Color(0xFF0D141C).copy(alpha = 0.85f)),
                        )
                },
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                shape,
            )
            .clickable(onClick = onExpand)
            .miniPlayerTrackSwipe(
                onNext = {
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
                onPrevious = {
                    haptics.play(Haptic.SkipPrevious)
                    onPrevious()
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art thumbnail
            AsyncImage(
                model = song.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(10.dp))
            // Titles
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                ExplicitSongTitle(
                    song = song,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.florosoft.florobeat.ui.theme.FloroTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Transport controls: [Previous] [Play/Pause] [Next]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Previous button: restart track if elapsed, or skip to previous queue item
                val canPrev = hasPrevious || isPlaying || (playbackPosition?.positionMs ?: 0L) > 3000L
                val prevAlpha = if (canPrev) 1f else 0.35f
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canPrev) {
                            haptics.play(Haptic.SkipPrevious)
                            onPrevious()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.previous_track),
                        tint = Color.White.copy(alpha = 0.90f * prevAlpha),
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Play/Pause circular button (with buffering spinner)
                if (isLoading) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = FloroMint,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                            .clickable {
                                haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                                onPlayPause()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Next button
                val nextAlpha = if (hasNext) 1f else 0.35f
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasNext) {
                            haptics.play(Haptic.SkipNext)
                            onNext()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next_track),
                        tint = Color.White.copy(alpha = 0.90f * nextAlpha),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // Slim playback progress bar line running along the bottom
        MiniPlayerProgressBar(
            playbackPosition = playbackPosition,
            durationMs = durationMs,
            playbackProgress = playbackProgress,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/**
 * Isolated progress bar composable so reading [playbackPosition.positionMs] ticks
 * only recomposes this 2dp bar and never invalidates the rest of [MiniPlayer].
 */
@Composable
private fun MiniPlayerProgressBar(
    playbackPosition: com.florosoft.florobeat.playback.PlaybackPosition?,
    durationMs: Long,
    playbackProgress: Float,
    modifier: Modifier = Modifier,
) {
    val progress = if (playbackPosition != null && durationMs > 0L) {
        (playbackPosition.positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        playbackProgress.coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(FloroMint),
        )
    }
}

