package com.florosoft.florobeat.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import coil3.compose.AsyncImage
import com.florosoft.florobeat.data.model.BrowseItem
import com.florosoft.florobeat.data.model.BrowseType
import com.florosoft.florobeat.data.model.ROW_ART_PX
import com.florosoft.florobeat.data.model.SearchFilter
import com.florosoft.florobeat.data.model.artworkAt
import com.florosoft.florobeat.data.model.SearchResult
import com.florosoft.florobeat.data.model.Song
import com.florosoft.florobeat.data.model.UiState
import com.florosoft.florobeat.R
import com.florosoft.florobeat.ui.components.MessageState
import com.florosoft.florobeat.ui.components.PAGE_GUTTER
import com.florosoft.florobeat.ui.components.topBarContentPadding
import com.florosoft.florobeat.ui.components.ROW_DIVIDER_INSET
import com.florosoft.florobeat.ui.components.SearchField
import com.florosoft.florobeat.ui.components.SongRow
import com.florosoft.florobeat.ui.components.thumbnailBorder
import com.florosoft.florobeat.ui.components.songListSkeleton
import com.florosoft.florobeat.ui.haptics.Haptic
import com.florosoft.florobeat.ui.haptics.rememberHaptics
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.florosoft.florobeat.ui.components.FloroSectionHeader
import com.florosoft.florobeat.ui.theme.FloroDarkCard
import com.florosoft.florobeat.ui.theme.FloroDarkCardBorder
import com.florosoft.florobeat.ui.theme.FloroMintLight
import com.florosoft.florobeat.ui.theme.FloroTextSecondary
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroObsidian
import java.util.Locale

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: SearchFilter,
    onFilterChange: (SearchFilter) -> Unit,
    results: UiState<List<SearchResult>>?,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    listState: LazyListState,
    scrollResetTrigger: Int,
    focusTrigger: Int = 0,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onTopResultPlay: (Song) -> Unit,
    onTopResultPlaylist: (Song) -> Unit,
    onBrowseClick: (BrowseItem) -> Unit,
    /**
     * Holding an album or playlist hit rather than tapping it — the same menu
     * the shelves open, so a release found by searching can go on the queue
     * without a trip through its page.
     */
    onBrowseLongPress: ((BrowseItem) -> Unit)? = null,
    history: List<String>,
    suggestions: List<String>,
    typeaheadResults: List<SearchResult>,
    onSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
    /** Long-press handler for typeahead rows — opens the song actions sheet. */
    onTypeaheadLongPress: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Re-tapping the search tab from the nav bar increments focusTrigger;
    // respond by focusing the field and opening the keyboard.
    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0) focusRequester.requestFocus()
    }
    // Search keeps one list state while its contents change. Reset it for each
    // new request so choosing a recent search cannot inherit the history's
    // previous scroll position (or a previous result page's position).
    LaunchedEffect(scrollResetTrigger) {
        if (scrollResetTrigger > 0) listState.scrollToItem(0)
    }
    // A non-empty suggestion list means the field is mid-edit — see
    // MainViewModel.suggestions. Nothing below it is worth showing while it is
    // up: the results are for whatever was searched before this edit began,
    // and so are the filter tabs above them.
    val suggesting = suggestions.isNotEmpty()
    // Live media results arrive from the parallel typeahead pipeline; show
    // them only while the user is still typing (suggestions visible), so they
    // appear as a dropdown beneath the text completions rather than floating
    // after the search has committed.
    val showTypeahead = typeaheadResults.isNotEmpty() && suggesting
    LaunchedEffect(listState, results, loadingMore) {
        if (results !is UiState.Success) return@LaunchedEffect
        snapshotFlow {
            val layout = listState.layoutInfo
            (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (!loadingMore && total > 0 && lastVisible >= total - 4) onLoadMore()
        }
    }

    val searchProcessed = remember(results, filter) {
        val data = (results as? UiState.Success)?.data
        if (data == null) null
        else {
            val tr = data.mapNotNull { row -> when (row) {
                is SearchResult.TopTrack -> row.song
                is SearchResult.Track -> row.song
                else -> null
            } }
            val top = data.filterIsInstance<SearchResult.TopTrack>().firstOrNull()
            val sec = searchSections(data, filter)
            Triple(tr, sec, top)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search field and filter tabs stay fixed at the top, outside the
        // scrolling list, so they're always reachable rather than scrolling
        // away with the results or recent searches beneath them.
        // The FrostedTopBar is visible on this tab (showing "Search"), so we
        // clear it fully — status bar inset + bar height + breathing gap — so
        // the search field sits cleanly below the bar instead of overlapping it.
        Column(modifier = Modifier.padding(top = topBarContentPadding())) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onSubmit = onSubmit,
                focusRequester = focusRequester,
                modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, bottom = 4.dp),
            )
            // The filters only mean something once there is a result set to narrow;
            // they stay up for an empty or failed search too, or picking a filter
            // that finds nothing would take away the control needed to leave it.
            if (results != null && !suggesting) {
                SearchFilterTabs(filter = filter, onFilterChange = onFilterChange)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            when {
                suggesting -> {
                    searchSuggestions(
                        suggestions = suggestions,
                        // Picking one is done typing, so the keyboard comes down
                        // with it and the results get the whole screen.
                        onClick = { term ->
                            onSuggestionClick(term)
                            focusManager.clearFocus()
                        },
                        onFill = onQueryChange,
                    )
                    if (showTypeahead) {
                        searchTypeaheadDropdown(
                            typeaheadResults = typeaheadResults,
                            onSongClick = { song -> onTopResultPlay(song) },
                            onSongLongPress = onTypeaheadLongPress,
                            onBrowseClick = { item ->
                                onBrowseClick(item)
                            },
                        )
                    }
                }
                results == null -> if (history.isEmpty()) {
                    item { MessageState(stringResource(R.string.search_empty)) }
                } else {
                    recentSearches(history, onHistoryClick, onHistoryRemove, onHistoryClear)
                }
                results is UiState.Loading -> songListSkeleton(circular = filter == SearchFilter.ARTISTS)
                results is UiState.Error -> item { MessageState(results.message) }
                results is UiState.Success -> {
                    val (tracks, sections, topResult) = searchProcessed ?: Triple(emptyList(), emptyList(), null)
                    if (filter == SearchFilter.ALL && topResult != null) {
                        item(key = "search:top-result:${topResult.song.videoId}") {
                            TopResultCard(
                                song = topResult.song,
                                onPlay = { onTopResultPlay(topResult.song) },
                                onPlaylist = { onTopResultPlaylist(topResult.song) },
                                onLongPress = { onSongLongPress(topResult.song) },
                            )
                        }
                    }
                    sections.forEach { section ->
                        section.title?.let { title ->
                            item(key = "search-section:$title") {
                                FloroSectionHeader(
                                    title = title,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                                )
                            }
                        }
                        itemsIndexed(
                            items = section.rows,
                            key = { index, row ->
                                when (row) {
                                    is SearchResult.Track -> "track_${row.song.videoId}"
                                    is SearchResult.Browse -> "browse_${row.item.browseId ?: row.item.title}_$index"
                                    is SearchResult.TopTrack -> "top_${row.song.videoId}"
                                    else -> "search_row_$index"
                                }
                            },
                        ) { index, row ->
                            when (row) {
                                is SearchResult.TopTrack -> Unit
                                is SearchResult.Track -> SongRow(
                                    song = row.song,
                                    onClick = {
                                        onSongClick(tracks, tracks.indexOf(row.song).coerceAtLeast(0))
                                    },
                                    onLongPress = { onSongLongPress(row.song) },
                                    onSwipeToQueue = { onSongSwipe(row.song) },
                                )
                                is SearchResult.Browse -> BrowseRow(
                                    item = row.item,
                                    onClick = { onBrowseClick(row.item) },
                                    onLongPress = onBrowseLongPress?.let { { it(row.item) } },
                                )
                            }
                            if (index < section.rows.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                    if (loadingMore) {
                        songListSkeleton(
                            count = 3,
                            keyPrefix = "skeleton:search:more",
                            circular = filter == SearchFilter.ARTISTS,
                        )
                    }
                }
            }
        }
    }
}

private data class SearchSection(val title: String?, val rows: List<SearchResult>)

/** The unfiltered page is useful only when its mixed result types are readable at a glance. */
private fun searchSections(rows: List<SearchResult>, filter: SearchFilter): List<SearchSection> {
    if (filter != SearchFilter.ALL) return listOf(SearchSection(null, rows))
    return listOf(
        SearchSection("Songs", rows.filterIsInstance<SearchResult.Track>()),
        SearchSection("Artists", rows.filterIsInstance<SearchResult.Browse>().filter { it.item.type == BrowseType.ARTIST }),
        SearchSection("Albums", rows.filterIsInstance<SearchResult.Browse>().filter { it.item.type == BrowseType.ALBUM }),
        SearchSection("Playlists", rows.filterIsInstance<SearchResult.Browse>().filter { it.item.type == BrowseType.PLAYLIST }),
        SearchSection("More", rows.filterIsInstance<SearchResult.Browse>().filter { it.item.type == BrowseType.OTHER }),
    ).filter { it.rows.isNotEmpty() }
}

/** The All response carries its highest-confidence music hit as a promoted card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopResultCard(
    song: Song,
    onPlay: () -> Unit,
    onPlaylist: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
    ) {
        FloroSectionHeader(
            title = "Top Result",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FloroDarkCard)
                .border(1.dp, FloroDarkCardBorder, RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onPlay, onLongClick = onLongPress)
                .padding(14.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = song.thumbnailUrl?.artworkAt(ROW_ART_PX),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .thumbnailBorder(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = FloroTextSecondary,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.shelf_song_item),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FloroMint,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                    IconButton(onClick = onLongPress, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.more),
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Prominent Play Button matching reference
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(FloroMint, FloroMintLight),
                                ),
                            )
                            .clickable(onClick = onPlay),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = FloroObsidian,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.play),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = FloroObsidian,
                            ),
                        )
                    }

                    // Add to Playlist Button
                    Row(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .clickable(onClick = onPlaylist)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.playlist_action),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * What YouTube would complete the half-typed query to, in place of the results
 * while it is being typed.
 *
 * The first row is the text as typed, put there by the view model rather than
 * taken from YouTube's answer, so running exactly what was asked for is always
 * the nearest row to the keyboard rather than something the thumb has to aim
 * past.
 */
private fun LazyListScope.searchSuggestions(
    suggestions: List<String>,
    onClick: (String) -> Unit,
    onFill: (String) -> Unit,
) {
    // This is a list-level inset rather than padding hidden inside the first
    // row. It keeps the gap under the field stable even when that row changes
    // its text or icon treatment.
    item(key = "suggestions:top-inset") { Spacer(Modifier.height(8.dp)) }
    // Skip the echo of the typed text (element 0) — it's already visible in
    // the search field itself — and cap at N so the list stays compact above
    // the playable-media cards.
    itemsIndexed(suggestions.drop(1).take(3), key = { _, term -> "suggest:$term" }) { _, term ->
        SuggestionRow(
            term = term,
            isQueryAction = false,
            onFill = { onFill(term) },
            onClick = { onClick(term) },
        )
    }
}

/**
 * One typeahead row: tap the text to search it, or the arrow to put it in the
 * field and carry on typing — the pair YouTube, Google and every mobile
 * keyboard's own suggestion strip use, and the reason a longer completion
 * isn't a dead end when it's only nearly right.
 */
@Composable
private fun SuggestionRow(
    term: String,
    isQueryAction: Boolean,
    onFill: (() -> Unit)?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = PAGE_GUTTER,
                end = 8.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            // The first row is the deliberate action to search the exact text
            // in the field, not a server-provided completion. Naming it makes
            // the otherwise duplicated wording read as intentional.
            text = if (isQueryAction) """${stringResource(R.string.search)} "$term"""" else term,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onFill != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.NorthWest,
                    contentDescription = stringResource(R.string.recent_search_edit, term),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            // Match the arrow button's full touch target, not only its width.
            // A width-only spacer made the first row shorter than the ones
            // below, so its vertical rhythm looked visibly uneven.
            Spacer(Modifier.size(40.dp))
        }
    }
}

/**
 * Hybrid dropdown shown beneath text suggestions while typing: a horizontal
 * divider, then live media rows (cover art + title + subtitle) that play on
 * tap and open the song menu on long-press.
 */
private fun LazyListScope.searchTypeaheadDropdown(
    typeaheadResults: List<SearchResult>,
    onSongClick: (Song) -> Unit,
    onSongLongPress: ((Song) -> Unit)?,
    onBrowseClick: (BrowseItem) -> Unit,
) {
    item(key = "typeahead:divider") {
        HorizontalDivider(
            modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
    }
    items(typeaheadResults, key = { result ->
        when (result) {
            is SearchResult.Track -> "ta:track:${result.song.videoId}"
            is SearchResult.Browse -> "ta:browse:${result.item.browseId}"
            is SearchResult.TopTrack -> "ta:top:${result.song.videoId}"
        }
    }) { result ->
        when (result) {
            is SearchResult.Track -> TypeaheadSongRow(
                song = result.song,
                onClick = { onSongClick(result.song) },
                onLongPress = onSongLongPress?.let { { it(result.song) } },
            )
            is SearchResult.Browse -> BrowseRow(
                item = result.item,
                onClick = { onBrowseClick(result.item) },
                onLongPress = onSongLongPress?.let { { /* browse long-press not applicable */ } },
            )
            is SearchResult.TopTrack -> TypeaheadSongRow(
                song = result.song,
                onClick = { onSongClick(result.song) },
                onLongPress = onSongLongPress?.let { { it(result.song) } },
            )
        }
    }
}

/**
 * A single media row inside the typeahead dropdown: 52dp cover art, title,
 * and artist/album subtitle. Tap plays the track; long-press opens the menu.
 */
@Composable
private fun TypeaheadSongRow(
    song: Song,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .thumbnailBorder(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOfNotNull(song.artist).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * What was searched for before, shown in place of the results while the field
 * is empty — the same spot Spotify and Apple Music put it, and the reason the
 * blank search page isn't just a sentence any more.
 */
private fun LazyListScope.recentSearches(
    history: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    item(key = "recent:header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.clear),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
    items(history, key = { "recent:$it" }) { term ->
        RecentSearchRow(
            term = term,
            onClick = { onClick(term) },
            onRemove = { onRemove(term) },
        )
    }
}

@Composable
private fun RecentSearchRow(term: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = PAGE_GUTTER, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = term,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.recent_search_remove, term),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowseRow(item: BrowseItem, onClick: () -> Unit, onLongPress: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(
                    if (item.type == BrowseType.ARTIST) CircleShape
                    else RoundedCornerShape(8.dp),
                )
                .thumbnailBorder(
                    if (item.type == BrowseType.ARTIST) CircleShape
                    else RoundedCornerShape(8.dp),
                )
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle.ifBlank { item.type.name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) } },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Filter pills rather than a tab row: squarish rounded rectangles, the selected
 * one inverted. They scroll horizontally so a long label set never squeezes the
 * text, and the gutter padding sits inside the scroll so it scrolls with them.
 */
@Composable
private fun SearchFilterTabs(filter: SearchFilter, onFilterChange: (SearchFilter) -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchFilter.entries.forEach { entry ->
            val selected = entry == filter
            Box(
                modifier = Modifier
                    .clip(FILTER_PILL_SHAPE)
                    .background(
                        if (selected) FloroMint
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable {
                        if (!selected) haptics.play(Haptic.Select)
                        onFilterChange(entry)
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) FloroObsidian
                    else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Rounded, but well short of a capsule — the corner reads as a cut, not a curve. */
private val FILTER_PILL_SHAPE = RoundedCornerShape(12.dp)
