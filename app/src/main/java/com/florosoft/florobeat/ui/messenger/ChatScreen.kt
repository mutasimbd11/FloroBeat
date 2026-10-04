package com.florosoft.florobeat.ui.messenger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.florosoft.florobeat.R
import com.florosoft.florobeat.data.messenger.MessageDeliveryStatus
import com.florosoft.florobeat.data.messenger.MessengerMessage
import com.florosoft.florobeat.data.messenger.MessengerRepository
import com.florosoft.florobeat.data.messenger.MessengerUser
import com.florosoft.florobeat.ui.theme.FloroDarkCard
import com.florosoft.florobeat.ui.theme.FloroDarkCardBorder
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroObsidian
import com.florosoft.florobeat.ui.theme.FloroTextMuted
import com.florosoft.florobeat.ui.theme.FloroTextSecondary
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    otherUser: MessengerUser,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by MessengerRepository.activeChatMessages.collectAsStateWithLifecycle()
    val typingMap by MessengerRepository.typingState.collectAsStateWithLifecycle()
    val isTyping = typingMap[conversationId] == true

    var inputText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<MessengerMessage?>(null) }
    var selectedMessageForActions by remember { mutableStateOf<MessengerMessage?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(MessengerRepository.isUserBlocked(otherUser.id)) }

    var typingStopJob by remember { mutableStateOf<Job?>(null) }

    // Initialize and cleanup conversation in repository
    LaunchedEffect(conversationId, otherUser) {
        MessengerRepository.openConversation(conversationId, otherUser)
    }

    DisposableEffect(conversationId) {
        onDispose {
            MessengerRepository.sendTyping(false)
            MessengerRepository.closeConversation()
        }
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FloroDarkCard)
                .border(0.5.dp, FloroDarkCardBorder)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showProfileDialog = true }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MessengerAvatar(
                    user = otherUser,
                    size = 40.dp,
                    showOnlineIndicator = true,
                    hazeState = hazeState
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = otherUser.displayName.ifBlank { otherUser.username },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val statusText = when {
                        isTyping -> stringResource(R.string.typing)
                        otherUser.isOnline -> stringResource(R.string.online)
                        otherUser.lastSeenAt > 0L -> {
                            val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            "${stringResource(R.string.last_seen)} ${format.format(Date(otherUser.lastSeenAt))}"
                        }
                        else -> stringResource(R.string.offline)
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isTyping) FloroMint else if (otherUser.isOnline) Color(0xFF10B981) else FloroTextMuted,
                            fontWeight = if (isTyping || otherUser.isOnline) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.user_profile)) },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showProfileDialog = true
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(if (isBlocked) stringResource(R.string.unblock_user) else stringResource(R.string.block_user))
                        },
                        leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            if (isBlocked) {
                                MessengerRepository.unblockUser(otherUser.id)
                                isBlocked = false
                            } else {
                                MessengerRepository.blockUser(otherUser)
                                isBlocked = true
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.report_user)) },
                        leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showReportDialog = true
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.clear_chat)) },
                        leadingIcon = { Icon(Icons.Rounded.ClearAll, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showClearConfirm = true
                        }
                    )
                }
            }
        }

        // --- MESSAGES AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        MessengerAvatar(user = otherUser, size = 64.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_messages_yet_title, otherUser.displayName),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.no_messages_yet_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(color = FloroTextSecondary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = messages.asReversed(),
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            onLongClick = { selectedMessageForActions = message },
                            onRetryClick = { MessengerRepository.retryMessage(message) }
                        )
                    }
                }
            }
        }

        // --- COMPOSER SECTION ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FloroDarkCard)
                .navigationBarsPadding()
        ) {
            // Reply quote preview banner
            AnimatedVisibility(
                visible = replyingTo != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                replyingTo?.let { replyTarget ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FloroObsidian.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(32.dp)
                                .background(FloroMint, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${stringResource(R.string.replying_to)} ${replyTarget.replyToSenderName ?: otherUser.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FloroMint,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = replyTarget.content,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { replyingTo = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = FloroTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newText ->
                        inputText = newText
                        // Debounce typing indicator
                        typingStopJob?.cancel()
                        if (newText.isNotBlank()) {
                            MessengerRepository.sendTyping(true)
                            typingStopJob = scope.launch {
                                delay(3000L)
                                MessengerRepository.sendTyping(false)
                            }
                        } else {
                            MessengerRepository.sendTyping(false)
                        }
                    },
                    placeholder = {
                        Text(
                            text = if (isBlocked) stringResource(R.string.user_blocked_input_disabled)
                            else stringResource(R.string.type_message),
                            color = FloroTextMuted
                        )
                    },
                    enabled = !isBlocked,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FloroMint,
                        unfocusedBorderColor = FloroDarkCardBorder,
                        focusedContainerColor = FloroObsidian,
                        unfocusedContainerColor = FloroObsidian,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                val canSend = inputText.trim().isNotBlank() && !isBlocked
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (canSend) FloroMint else FloroDarkCardBorder)
                        .clickable(enabled = canSend) {
                            val textToSend = inputText.trim()
                            if (textToSend.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                typingStopJob?.cancel()
                                MessengerRepository.sendTyping(false)
                                MessengerRepository.sendMessage(
                                    conversationId = conversationId,
                                    receiverId = otherUser.id,
                                    content = textToSend,
                                    replyTo = replyingTo
                                )
                                inputText = ""
                                replyingTo = null
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.send),
                        tint = if (canSend) FloroObsidian else FloroTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // --- MESSAGE ACTION SHEET ---
    selectedMessageForActions?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForActions = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = FloroDarkCard,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = msg.content.take(50) + if (msg.content.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Reply
                ActionRow(
                    icon = Icons.Rounded.Reply,
                    label = stringResource(R.string.reply),
                    onClick = {
                        replyingTo = msg
                        selectedMessageForActions = null
                    }
                )

                // Copy
                ActionRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = stringResource(R.string.copy_text),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Message", msg.content))
                        selectedMessageForActions = null
                    }
                )

                // Delete for me
                ActionRow(
                    icon = Icons.Rounded.Delete,
                    label = stringResource(R.string.delete_for_me),
                    isDestructive = true,
                    onClick = {
                        MessengerRepository.deleteMessage(msg.id, forEveryone = false)
                        selectedMessageForActions = null
                    }
                )

                // Delete for everyone (only for own outgoing messages)
                if (msg.isOutgoing) {
                    ActionRow(
                        icon = Icons.Rounded.Delete,
                        label = stringResource(R.string.delete_for_everyone),
                        isDestructive = true,
                        onClick = {
                            MessengerRepository.deleteMessage(msg.id, forEveryone = true)
                            selectedMessageForActions = null
                        }
                    )
                }
            }
        }
    }

    // Profile Dialog
    if (showProfileDialog) {
        UserProfileDialog(
            user = otherUser,
            isSelf = false,
            isBlocked = isBlocked,
            hazeState = hazeState,
            onDismiss = { showProfileDialog = false },
            onBlockToggle = {
                if (isBlocked) {
                    MessengerRepository.unblockUser(otherUser.id)
                    isBlocked = false
                } else {
                    MessengerRepository.blockUser(otherUser)
                    isBlocked = true
                }
            },
            onReport = {
                showProfileDialog = false
                showReportDialog = true
            }
        )
    }

    // Report Dialog
    if (showReportDialog) {
        ReportUserDialog(
            user = otherUser,
            onDismiss = { showReportDialog = false },
            onSubmitReport = { reason, details ->
                MessengerRepository.reportUser(otherUser.id, reason, details)
            }
        )
    }

    // Clear Chat Confirmation
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.clear_chat_title)) },
            text = { Text(stringResource(R.string.clear_chat_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        MessengerRepository.clearConversation(conversationId)
                        showClearConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessengerMessage,
    onLongClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val bubbleColor = if (isOutgoing) FloroMint else FloroDarkCard
    val textColor = if (isOutgoing) FloroObsidian else Color.White
    val timeColor = if (isOutgoing) FloroObsidian.copy(alpha = 0.65f) else FloroTextMuted

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .then(
                    if (!isOutgoing) Modifier.border(0.5.dp, FloroDarkCardBorder, bubbleShape)
                    else Modifier
                )
                .background(bubbleColor)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                // Reply reference preview inside bubble
                if (!message.replyToText.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isOutgoing) Color.Black.copy(alpha = 0.1f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .height(26.dp)
                                .background(
                                    if (isOutgoing) FloroObsidian else FloroMint,
                                    RoundedCornerShape(1.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            message.replyToSenderName?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isOutgoing) FloroObsidian else FloroMint
                                    )
                                )
                            }
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.75f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Message text
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Time and delivery status
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    Text(
                        text = timeFormat.format(Date(message.createdAt)),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = timeColor
                        )
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageDeliveryStatus.SENDING -> {
                                Icon(
                                    imageVector = Icons.Rounded.HourglassEmpty,
                                    contentDescription = stringResource(R.string.status_sending),
                                    tint = timeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageDeliveryStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = stringResource(R.string.status_sent),
                                    tint = timeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageDeliveryStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Rounded.DoneAll,
                                    contentDescription = stringResource(R.string.status_delivered),
                                    tint = timeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            MessageDeliveryStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Rounded.DoneAll,
                                    contentDescription = stringResource(R.string.status_read),
                                    tint = if (isOutgoing) FloroObsidian else FloroMint,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            MessageDeliveryStatus.FAILED -> {
                                Row(
                                    modifier = Modifier.clickable(onClick = onRetryClick),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = stringResource(R.string.status_failed),
                                        tint = Color.Red,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = stringResource(R.string.retry),
                                        tint = Color.Red,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error else Color.White
            )
        )
    }
}
