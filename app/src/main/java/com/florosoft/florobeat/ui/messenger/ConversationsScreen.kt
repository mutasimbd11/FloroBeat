package com.florosoft.florobeat.ui.messenger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.florosoft.florobeat.R
import com.florosoft.florobeat.data.messenger.MessageDeliveryStatus
import com.florosoft.florobeat.data.messenger.MessengerClient
import com.florosoft.florobeat.data.messenger.MessengerConversation
import com.florosoft.florobeat.data.messenger.MessengerRepository
import com.florosoft.florobeat.data.messenger.MessengerUser
import com.florosoft.florobeat.ui.icons.FloroBeatIcons
import com.florosoft.florobeat.ui.theme.FloroDarkCard
import com.florosoft.florobeat.ui.theme.FloroDarkCardBorder
import com.florosoft.florobeat.ui.theme.FloroDeepTeal
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroMintLight
import com.florosoft.florobeat.ui.theme.FloroObsidian
import com.florosoft.florobeat.ui.theme.FloroTextMuted
import com.florosoft.florobeat.ui.theme.FloroTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main conversations list and user discovery hub for FloroBeat Messenger.
 */
@Composable
fun ConversationsScreen(
    onConversationClick: (conversationId: String, otherUser: MessengerUser) -> Unit,
    onOpenOwnProfile: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val conversations by MessengerRepository.conversations.collectAsStateWithLifecycle()
    val connectionStatus by MessengerRepository.connectionStatus.collectAsStateWithLifecycle()
    val currentUser by MessengerRepository.currentUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<MessengerUser>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            searchLoading = true
            val results = MessengerRepository.searchUsers(searchQuery)
            searchResults = results
            searchLoading = false
        } else {
            searchResults = emptyList()
            searchLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.messenger_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    // Live connection indicator
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionStatus) {
                                    MessengerClient.ConnectionStatus.CONNECTED -> FloroMint
                                    MessengerClient.ConnectionStatus.CONNECTING -> Color(0xFFFFB300)
                                    MessengerClient.ConnectionStatus.OFFLINE -> Color(0xFF757575)
                                }
                            )
                    )
                }
                Text(
                    text = currentUser?.displayName ?: stringResource(R.string.messenger_connecting),
                    style = MaterialTheme.typography.bodySmall.copy(color = FloroTextSecondary),
                )
            }

            Row {
                IconButton(onClick = { isSearching = !isSearching }) {
                    Icon(
                        imageVector = if (isSearching) Icons.Rounded.Close else Icons.Rounded.PersonSearch,
                        contentDescription = stringResource(R.string.search_users),
                        tint = if (isSearching) FloroMint else Color.White,
                    )
                }
                IconButton(onClick = onOpenOwnProfile) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = stringResource(R.string.my_profile),
                        tint = Color.White,
                    )
                }
            }
        }

        // Connection Banner if offline
        AnimatedVisibility(visible = connectionStatus == MessengerClient.ConnectionStatus.OFFLINE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A1C1C))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = Color(0xFFFF8A80),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.messenger_offline_notice),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCDD2)),
                )
            }
        }

        // Search Bar when active
        AnimatedVisibility(visible = isSearching) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_users_hint), color = FloroTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FloroMint,
                        unfocusedBorderColor = FloroDarkCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = FloroTextSecondary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                )
            }
        }

        // Search Results List
        if (isSearching) {
            if (searchLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FloroMint, modifier = Modifier.size(32.dp))
                }
            } else if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.no_users_found),
                        style = MaterialTheme.typography.bodyMedium.copy(color = FloroTextSecondary),
                    )
                }
            } else if (searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(searchResults, key = { it.id }) { user ->
                        UserSearchRow(
                            user = user,
                            onClick = {
                                val myId = currentUser?.id.orEmpty()
                                val convId = "c_${minOf(myId, user.id)}_${maxOf(myId, user.id)}"
                                onConversationClick(convId, user)
                            },
                        )
                    }
                }
            }
            return
        }

        // Conversations List
        if (conversations.isEmpty()) {
            EmptyConversationsView(onStartSearch = { isSearching = true })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id, conversation.otherUser) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: MessengerConversation,
    onClick: () -> Unit,
) {
    val user = conversation.otherUser
    val lastMsg = conversation.lastMessage
    val hasUnread = conversation.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FloroDarkCard)
            .border(0.5.dp, FloroDarkCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with online status badge
        MessengerAvatar(
            user = user,
            size = 50.dp,
            showOnlineIndicator = true
        )

        Spacer(Modifier.width(14.dp))

        // Center: Name & last message
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatMessageTime(conversation.updatedAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (hasUnread) FloroMint else FloroTextMuted,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (lastMsg != null && lastMsg.isOutgoing) {
                    when (lastMsg.status) {
                        MessageDeliveryStatus.SENDING -> Icon(
                            Icons.Rounded.HourglassEmpty,
                            contentDescription = null,
                            tint = FloroTextMuted,
                            modifier = Modifier.size(13.dp),
                        )
                        MessageDeliveryStatus.SENT -> Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = FloroTextMuted,
                            modifier = Modifier.size(13.dp),
                        )
                        MessageDeliveryStatus.DELIVERED -> Icon(
                            Icons.Rounded.DoneAll,
                            contentDescription = null,
                            tint = FloroTextMuted,
                            modifier = Modifier.size(13.dp),
                        )
                        MessageDeliveryStatus.READ -> Icon(
                            Icons.Rounded.DoneAll,
                            contentDescription = null,
                            tint = FloroMint,
                            modifier = Modifier.size(13.dp),
                        )
                        MessageDeliveryStatus.FAILED -> Text("!", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Text(
                    text = lastMsg?.content ?: user.handle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (hasUnread) Color.White else FloroTextSecondary,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(FloroMint)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FloroObsidian,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSearchRow(
    user: MessengerUser,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FloroDarkCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessengerAvatar(
            user = user,
            size = 44.dp,
            showOnlineIndicator = true
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
            )
            Text(
                text = user.handle,
                style = MaterialTheme.typography.bodySmall.copy(color = FloroMint),
            )
            if (!user.bio.isNullOrBlank()) {
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodySmall.copy(color = FloroTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = FloroMint,
                contentColor = FloroObsidian,
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(stringResource(R.string.message_action), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyConversationsView(onStartSearch: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(FloroDarkCard)
                    .border(1.dp, FloroDarkCardBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                    contentDescription = null,
                    tint = FloroMint,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.no_conversations_yet),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.no_conversations_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FloroTextSecondary,
                    lineHeight = 20.sp,
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onStartSearch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FloroMint,
                    contentColor = FloroObsidian,
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.discover_users),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m"
        diff < 86_400_000L -> "${diff / 3_600_000L}h"
        diff < 172_800_000L -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
