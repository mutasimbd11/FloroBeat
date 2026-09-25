package com.florosoft.florobeat.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.florosoft.florobeat.R
import com.florosoft.florobeat.data.model.Account
import com.florosoft.florobeat.data.model.CARD_ART_PX
import com.florosoft.florobeat.data.model.HEADER_ART_PX
import com.florosoft.florobeat.data.model.HomeShelf
import com.florosoft.florobeat.data.model.ShelfItem
import com.florosoft.florobeat.data.model.UiState
import com.florosoft.florobeat.data.model.artworkAt
import com.florosoft.florobeat.data.settings.AppSettings
import com.florosoft.florobeat.data.settings.LibraryViewType
import com.florosoft.florobeat.ui.components.ConditionalShimmerScope
import com.florosoft.florobeat.ui.components.FeaturedHeroCard
import com.florosoft.florobeat.ui.components.FloroBrandHeader
import com.florosoft.florobeat.ui.components.FloroSectionHeader
import com.florosoft.florobeat.ui.components.MadeForYouSection
import com.florosoft.florobeat.ui.components.MessageState
import com.florosoft.florobeat.ui.components.MoodExploreSection
import com.florosoft.florobeat.ui.components.PAGE_GUTTER
import com.florosoft.florobeat.ui.components.PullToRefresh
import com.florosoft.florobeat.ui.components.SHELF_CARD_WIDTH
import com.florosoft.florobeat.ui.components.SignInBanner
import com.florosoft.florobeat.ui.components.feedMoreSkeleton
import com.florosoft.florobeat.ui.components.feedSkeleton
import com.florosoft.florobeat.ui.components.heroCardWidth
import com.florosoft.florobeat.ui.components.recentlyPlayedSkeleton
import com.florosoft.florobeat.ui.components.thumbnailBorder
import com.florosoft.florobeat.ui.icons.FloroBeatIcons
import com.florosoft.florobeat.ui.theme.FloroCardMediumShape
import com.florosoft.florobeat.ui.theme.FloroDeepTeal
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroMintLight
import com.florosoft.florobeat.ui.theme.FloroObsidian
import com.florosoft.florobeat.ui.theme.FloroTextMuted
import com.florosoft.florobeat.ui.theme.FloroTextSecondary
import java.util.Locale

private const val RECENTS_TITLE = "Recents"

private data class HomeProcessedShelves(
    val recentShelf: HomeShelf?,
    val featuredItem: ShelfItem?,
    val dailyMixItem: ShelfItem?,
    val discoverShelf: HomeShelf?,
    val remainingShelves: List<HomeShelf>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    account: Account? = null,
    onNotificationClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    /** The tapped item and the recommendation shelf it came from. */
    onItemClick: (ShelfItem, String) -> Unit,
    onPlayTrack: (ShelfItem, String) -> Unit = { item, shelf -> onItemClick(item, shelf) },
    onMoodClick: (String) -> Unit = {},
    onSeeAllRecents: () -> Unit = {},
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    title: String = "",
    signedIn: Boolean = true,
    onSignIn: (() -> Unit)? = null,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    recentlyPlayedLoading: Boolean = false,
    currentlyPlayingId: String? = null,
) {
    val recentsViewType by AppSettings.homeRecentsViewType.collectAsStateWithLifecycle()
    val isShimmering = state is UiState.Loading || loadingMore || recentlyPlayedLoading

    val processed = remember(state) {
        val shelves = (state as? UiState.Success)?.data.orEmpty()
        if (shelves.isEmpty()) {
            null
        } else {
            val recent = shelves.firstOrNull {
                it.title.equals(RECENTS_TITLE, ignoreCase = true) ||
                    it.title.equals("Recently played", ignoreCase = true) ||
                    it.title.equals("Listen again", ignoreCase = true)
            } ?: shelves.firstOrNull()

            val featured = shelves.firstOrNull {
                it.title.contains("Pick", ignoreCase = true) ||
                    it.title.contains("For you", ignoreCase = true) ||
                    it.title.contains("Mixed", ignoreCase = true)
            }?.items?.firstOrNull() ?: shelves.firstOrNull()?.items?.firstOrNull()

            val daily = shelves.flatMap { it.items }.firstOrNull {
                it.title.contains("Mix", ignoreCase = true) || it.title.contains("Vibe", ignoreCase = true)
            }

            val discover = shelves.firstOrNull {
                it.title.contains("New", ignoreCase = true) ||
                    it.title.contains("Release", ignoreCase = true) ||
                    it.title.contains("Explore", ignoreCase = true)
            }

            val remaining = shelves.filterNot {
                it == recent || it == discover
            }

            HomeProcessedShelves(recent, featured, daily, discover, remaining)
        }
    }

    ConditionalShimmerScope(loading = isShimmering) {
        Box(modifier = modifier) {
            PullToRefresh(
                refreshing = refreshing,
                onRefresh = onRefresh,
                state = pullState,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                ) {
                    if (!signedIn && onSignIn != null) {
                        item(key = "home_signin_banner") {
                            SignInBanner(onSignIn = onSignIn, modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }

                    when (state) {
                        is UiState.Loading -> {
                            if (recentlyPlayedLoading) {
                                recentlyPlayedSkeleton(listLayout = recentsViewType == LibraryViewType.LIST)
                                feedSkeleton(firstIsHero = false)
                            } else {
                                feedSkeleton()
                            }
                        }
                        is UiState.Error -> item(key = "home_error_state") {
                            MessageState(state.message, actionLabel = stringResource(R.string.retry), onAction = onRetry)
                        }
                        is UiState.Success -> {
                            val shelves = state.data
                            if (processed != null) {

                            // 1. Featured Hero Card (Prominently leads the Home screen)
                            if (processed.featuredItem != null) {
                                item(key = "floro_featured_hero_card") {
                                    FeaturedHeroCard(
                                        item = processed.featuredItem,
                                        onClick = { onItemClick(processed.featuredItem, "Featured") },
                                        onPlayClick = { onPlayTrack(processed.featuredItem, "Featured") },
                                    )
                                }
                            }

                            // 2. Recently Played Horizontal Carousel
                            val displayRecents = processed.recentShelf ?: shelves.firstOrNull()
                            if (displayRecents != null && displayRecents.items.isNotEmpty()) {
                                item(key = "floro_recently_played_shelf") {
                                    Column(Modifier.padding(top = 4.dp, bottom = 14.dp)) {
                                        FloroSectionHeader(
                                            title = stringResource(R.string.recently_played),
                                            actionLabel = stringResource(R.string.see_all) + " ›",
                                            onActionClick = onSeeAllRecents,
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            items(
                                                items = displayRecents.items,
                                                key = { item -> "recents_${item.videoId ?: item.browseId ?: item.title}" },
                                            ) { item ->
                                                RecentlyPlayedCard(
                                                    item = item,
                                                    isPlaying = item.videoId != null && item.videoId == currentlyPlayingId,
                                                    onClick = { onItemClick(item, displayRecents.title) },
                                                    onLongPress = onItemLongPress?.let { { it(item) } },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Made for You Section (Split layout: Daily Mix + 3 Pills)
                            item(key = "floro_made_for_you_section") {
                                MadeForYouSection(
                                    featuredMixItem = processed.dailyMixItem,
                                    onFeaturedMixClick = { onItemClick(it, "Made for You") },
                                    onVibeClick = { onMoodClick(it) },
                                    onSeeAllClick = { onMoodClick("Made for You") },
                                    modifier = Modifier.padding(bottom = 14.dp),
                                )
                            }

                            // 4. Explore by Mood Section (Chill, Romantic, Sad, Party, Focus, Sleep)
                            item(key = "floro_explore_by_mood_section") {
                                MoodExploreSection(
                                    onMoodClick = { onMoodClick(it) },
                                    onSeeAllClick = { onMoodClick("Moods") },
                                    modifier = Modifier.padding(bottom = 14.dp),
                                )
                            }

                            // 5. Discover / New Releases Section
                            val discoverShelf = processed.discoverShelf
                            if (discoverShelf != null && discoverShelf.items.isNotEmpty()) {
                                item(key = "floro_discover_shelf") {
                                    Column(Modifier.padding(bottom = 14.dp)) {
                                        FloroSectionHeader(
                                            title = stringResource(R.string.discover),
                                            actionLabel = stringResource(R.string.new_releases_for_you) + " ›",
                                            onActionClick = { onMoodClick("New releases") },
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            items(
                                                items = discoverShelf.items,
                                                key = { item -> "discover_${item.videoId ?: item.browseId ?: item.title}" },
                                            ) { item ->
                                                ShelfCard(
                                                    item = item,
                                                    onClick = { onItemClick(item, discoverShelf.title) },
                                                    onLongPress = onItemLongPress?.let { { it(item) } },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 6. Remaining Shelves (e.g. Charts, Videos, Community Playlists)
                            processed.remainingShelves.forEachIndexed { shelfIndex, shelf ->
                                item(key = "shelf_${shelf.title}_$shelfIndex") {
                                    Column(Modifier.padding(bottom = 20.dp)) {
                                        FloroSectionHeader(
                                            title = localizeShelfTitle(shelf.title),
                                            actionLabel = if (shelf.items.size > 5) stringResource(R.string.see_all) + " ›" else null,
                                            onActionClick = { onMoodClick(shelf.title) },
                                        )
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            items(
                                                items = shelf.items,
                                                key = { item -> "shelf_${shelf.title}_${item.videoId ?: item.browseId ?: item.title}" },
                                            ) { item ->
                                                ShelfCard(
                                                    item = item,
                                                    onClick = { onItemClick(item, shelf.title) },
                                                    onLongPress = onItemLongPress?.let { { it(item) } },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            }

                            if (loadingMore) feedMoreSkeleton()
                        }
                    }
                }
            }
        }
    }

    if (onLoadMore != null && state is UiState.Success) {
        val loadMore by rememberUpdatedState(onLoadMore)
        LaunchedEffect(listState, loadingMore) {
            snapshotFlow {
                val layout = listState.layoutInfo
                (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
            }.collect { (lastVisible, total) ->
                if (!loadingMore && total > 0 && lastVisible >= total - 3) loadMore()
            }
        }
    }
}

/**
 * Compact square card used in Recently Played section matching the reference screenshot.
 * Displays playing equalizer bars badge when track is active.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentlyPlayedCard(
    item: ShelfItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(136.dp),
) {
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cardShape)
                .thumbnailBorder(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.thumbnailUrl?.artworkAt(CARD_ART_PX),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Playing equalizer indicator in bottom-right corner when active
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.70f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FloroBeatIcons.Equalizer,
                        contentDescription = "Playing",
                        tint = FloroMint,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle.ifBlank { "Track" },
            color = FloroTextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SectionHeader(title: String, subtitle: String = "", onShowAll: (() -> Unit)? = null) {
    FloroSectionHeader(
        title = localizeShelfTitle(title),
        actionLabel = if (onShowAll != null) stringResource(R.string.show_all) + " ›" else null,
        onActionClick = onShowAll,
    )
}

@Composable
internal fun localizeShelfTitle(title: String): String {
    val trimmed = title.trim()
    return when {
        trimmed.equals("Recents", ignoreCase = true) ||
            trimmed.equals("Recently played", ignoreCase = true) ||
            trimmed.equals("Gần đây", ignoreCase = true) ||
            trimmed.equals("最近", ignoreCase = true) ->
            stringResource(R.string.shelf_recents)
        trimmed.equals("Playlists", ignoreCase = true) ||
            trimmed.equals("Danh sách phát", ignoreCase = true) ||
            trimmed.equals("再生リスト", ignoreCase = true) ->
            stringResource(R.string.playlists)
        trimmed.equals("Albums", ignoreCase = true) ||
            trimmed.equals("Album", ignoreCase = true) ||
            trimmed.equals("アルバム", ignoreCase = true) ->
            stringResource(R.string.albums)
        trimmed.equals("Artists", ignoreCase = true) ||
            trimmed.equals("Nghệ sĩ", ignoreCase = true) ||
            trimmed.equals("アーティスト", ignoreCase = true) ->
            stringResource(R.string.artists)
        trimmed.equals("Subscriptions", ignoreCase = true) ||
            trimmed.equals("Đã đăng ký", ignoreCase = true) ||
            trimmed.equals("登録チャンネル", ignoreCase = true) ->
            stringResource(R.string.subscriptions)
        trimmed.equals("Trending community playlists", ignoreCase = true) ||
            trimmed.equals("Danh sách phát cộng đồng thịnh hành", ignoreCase = true) ||
            trimmed.equals("Danh sách phát thịnh hành trong cộng đồng người dùng", ignoreCase = true) ||
            trimmed.equals("急上昇のコミュニティ再生リスト", ignoreCase = true) ->
            stringResource(R.string.shelf_trending_community_playlists)
        trimmed.equals("Featured playlists for you", ignoreCase = true) ||
            trimmed.equals("Danh sách phát đề xuất cho bạn", ignoreCase = true) ||
            trimmed.equals("Danh sách phát nổi bật dành cho bạn", ignoreCase = true) ||
            trimmed.equals("おすすめの再生リスト", ignoreCase = true) ->
            stringResource(R.string.shelf_featured_playlists_for_you)
        trimmed.equals("Quick picks", ignoreCase = true) ||
            trimmed.equals("Lựa chọn nhanh", ignoreCase = true) ||
            trimmed.equals("Chọn nhanh đài phát", ignoreCase = true) ||
            trimmed.equals("クイック ミックス", ignoreCase = true) ->
            stringResource(R.string.shelf_quick_picks)
        trimmed.equals("Listen again", ignoreCase = true) ||
            trimmed.equals("Nghe lại", ignoreCase = true) ||
            trimmed.equals("もう一度聴く", ignoreCase = true) ->
            stringResource(R.string.shelf_listen_again)
        trimmed.equals("Mixed for you", ignoreCase = true) ||
            trimmed.equals("Dành riêng cho bạn", ignoreCase = true) ||
            trimmed.equals("ミックス", ignoreCase = true) ->
            stringResource(R.string.shelf_mixed_for_you)
        trimmed.startsWith("Similar to", ignoreCase = true) -> {
            val rest = trimmed.substring(10).trim()
            stringResource(R.string.shelf_similar_to, rest)
        }
        trimmed.startsWith("Tương tự như", ignoreCase = true) -> {
            val rest = trimmed.substring(12).trim()
            stringResource(R.string.shelf_similar_to, rest)
        }
        trimmed.equals("Forgotten favorites", ignoreCase = true) ||
            trimmed.equals("Giai điệu quen thuộc", ignoreCase = true) ||
            trimmed.equals("よく聴いたお気に入りの曲", ignoreCase = true) ->
            stringResource(R.string.shelf_forgotten_favorites)
        trimmed.equals("Recommended music videos", ignoreCase = true) ||
            trimmed.equals("Video âm nhạc đề xuất", ignoreCase = true) ||
            trimmed.equals("おすすめのミュージック ビデオ", ignoreCase = true) ->
            stringResource(R.string.shelf_recommended_music_videos)
        trimmed.equals("From your library", ignoreCase = true) ||
            trimmed.equals("Từ thư viện của bạn", ignoreCase = true) ||
            trimmed.equals("ライブラリから", ignoreCase = true) ->
            stringResource(R.string.shelf_from_your_library)
        trimmed.equals("Charts", ignoreCase = true) ||
            trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
            trimmed.equals("チャート", ignoreCase = true) ->
            stringResource(R.string.shelf_charts)
        trimmed.equals("New releases", ignoreCase = true) ||
            trimmed.equals("Bản phát hành mới", ignoreCase = true) ||
            trimmed.equals("最新リリース", ignoreCase = true) ->
            stringResource(R.string.shelf_new_releases)
        trimmed.equals("Top music videos", ignoreCase = true) ||
            trimmed.equals("Video âm nhạc hàng đầu", ignoreCase = true) ||
            trimmed.equals("人気のミュージック ビデオ", ignoreCase = true) ->
            stringResource(R.string.shelf_top_music_videos)
        trimmed.equals("For you", ignoreCase = true) ||
            trimmed.equals("Dành cho bạn", ignoreCase = true) ||
            trimmed.equals("あなたへのおすすめ", ignoreCase = true) ->
            stringResource(R.string.shelf_for_you)
        trimmed.equals("Hits today", ignoreCase = true) ||
            trimmed.equals("Today's Hits", ignoreCase = true) ||
            trimmed.equals("Bản hit hôm nay", ignoreCase = true) ||
            trimmed.equals("今日のヒット曲", ignoreCase = true) ->
            stringResource(R.string.shelf_hits_today)
        trimmed.equals("Artists on the rise", ignoreCase = true) ||
            trimmed.equals("Nghệ sĩ đang lên", ignoreCase = true) ->
            stringResource(R.string.shelf_artists_on_the_rise)
        trimmed.equals("Concerts", ignoreCase = true) ||
            trimmed.equals("Buổi hòa nhạc", ignoreCase = true) ||
            trimmed.equals("コンサート", ignoreCase = true) ->
            stringResource(R.string.shelf_concerts)
        trimmed.startsWith("Shorts", ignoreCase = true) ||
            trimmed.equals("Shorts nổi bật", ignoreCase = true) ||
            trimmed.equals("ショート", ignoreCase = true) ->
            stringResource(R.string.shelf_shorts)
        trimmed.equals("Trending", ignoreCase = true) ||
            trimmed.equals("Thịnh hành", ignoreCase = true) ||
            trimmed.equals("急上昇", ignoreCase = true) ->
            stringResource(R.string.shelf_trending)
        else -> title
    }
}

@Composable
internal fun localizeCardSubtitle(subtitle: String): String {
    if (subtitle.isBlank()) return subtitle
    val delimiter = " • "
    val parts = subtitle.split(delimiter)
    val songLabel = stringResource(R.string.shelf_song_item)
    val singleLabel = stringResource(R.string.shelf_single)
    val chartLabel = stringResource(R.string.shelf_chart)
    val playlistLabel = stringResource(R.string.playlist)
    val localizedParts = parts.map { part ->
        val trimmed = part.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        when {
            trimmed.equals("Song", ignoreCase = true) || trimmed.equals("Titre", ignoreCase = true) ||
                trimmed.equals("Bài hát", ignoreCase = true) || trimmed.equals("曲", ignoreCase = true) -> songLabel
            trimmed.equals("Single", ignoreCase = true) || trimmed.equals("Đĩa đơn", ignoreCase = true) ||
                trimmed.equals("シングル", ignoreCase = true) -> singleLabel
            trimmed.equals("Chart", ignoreCase = true) || trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
                trimmed.equals("チャート", ignoreCase = true) -> chartLabel
            trimmed.equals("Playlist", ignoreCase = true) || trimmed.equals("Danh sách phát", ignoreCase = true) ||
                trimmed.equals("再生リスト", ignoreCase = true) -> playlistLabel
            lower.endsWith(" views") || lower.endsWith(" view") || lower.endsWith(" lượt xem") ||
                lower.endsWith(" 回視聴") || lower.endsWith("回視聴") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(R.string.card_views_format, count) else part
            }
            lower.endsWith(" plays") || lower.endsWith(" play") || lower.endsWith(" lượt phát") ||
                lower.endsWith(" 回再生") || lower.endsWith("回再生") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(R.string.card_plays_format, count) else part
            }
            lower.endsWith(" songs") || lower.endsWith(" song") || lower.endsWith(" bài hát") ||
                lower.endsWith(" 曲") || lower.endsWith("曲") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(R.string.card_songs_format, count) else part
            }
            else -> part
        }
    }
    return localizedParts.joinToString(delimiter)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    isPinned: Boolean = false,
) {
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        when (item.browseId) {
            "local:downloads" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(FloroCardMediumShape)
                        .thumbnailBorder(FloroCardMediumShape)
                        .background(
                            Brush.linearGradient(
                                listOf(FloroDeepTeal, FloroObsidian),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = FloroBeatIcons.Download,
                        contentDescription = null,
                        tint = FloroMint,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            "local:all" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(FloroCardMediumShape)
                        .thumbnailBorder(FloroCardMediumShape)
                        .background(
                            Brush.linearGradient(
                                listOf(FloroDeepTeal, Color(0xFF0F3B32)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = FloroMintLight,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(FloroCardMediumShape)
                        .thumbnailBorder(FloroCardMediumShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl?.artworkAt(CARD_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPinned) {
                Icon(
                    imageVector = FloroBeatIcons.Pin,
                    contentDescription = stringResource(R.string.pinned),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        val displaySubtitle = localizeCardSubtitle(item.subtitle)
        Text(
            text = displaySubtitle,
            color = FloroTextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * [leadingCard] rides at the head of the row, ahead of the content — the
 * Library tab's "New playlist" tile, which belongs among the playlists rather
 * than in a bar somewhere above them. [onItemLongPress] opens the album /
 * playlist menu, and is null only where a card points at something with no
 * track list behind it to act on.
 */
@Composable
internal fun Shelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    leadingCard: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.padding(bottom = 20.dp)) {
        FloroSectionHeader(
            title = shelf.title,
            actionLabel = shelf.subtitle.takeIf { it.isNotBlank() },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leadingCard?.let { card -> item(key = "leading") { card() } }
            items(
                items = shelf.items,
                key = { item -> "${shelf.title}_${item.videoId ?: item.browseId ?: item.title}" },
            ) { item ->
                ShelfCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = onItemLongPress?.let { { it(item) } },
                )
            }
        }
    }
}

/**
 * A card that isn't a thing yet — the dashed "New playlist" tile at the head
 * of the Library's playlist row, sized to sit in line with the covers beside
 * it rather than as a button bolted above them.
 */
@Composable
internal fun NewShelfCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141E28)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FloroMint,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = FloroTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

