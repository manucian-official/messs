package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.ui.components.EmojiReactionRow
import com.example.ui.components.TypingIndicator
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    conversation: ConversationEntity,
    messages: List<MessageEntity>,
    currentUser: UserEntity,
    isTyping: Boolean,
    replyingTo: MessageEntity?,
    members: List<UserEntity> = emptyList(),
    allUsers: List<UserEntity> = emptyList(),
    onBack: () -> Unit,
    onSendMessage: (String, replyTo: MessageEntity?, attachments: List<AttachmentEntity>) -> Unit,
    onPinMessage: (MessageEntity) -> Unit,
    onEditMessage: (MessageEntity, String) -> Unit,
    onDeleteMessage: (MessageEntity) -> Unit,
    onAddReaction: (messageId: String, emoji: String) -> Unit,
    onCancelReply: () -> Unit,
    onUpdateGroup: (title: String, avatarUrl: String) -> Unit = { _, _ -> },
    onInviteMembers: (List<UserEntity>) -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    
    var inputText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var scaleImageDialogUrl by remember { mutableStateOf<String?>(null) }
    
    // Bottom Sheet message settings controllers
    var activeContextMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }

    var showGroupDetailsDialog by remember { mutableStateOf(false) }

    // Scroll automatically when list receives new messages or user keyboard enters
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (conversation.isGroup) {
                                showGroupDetailsDialog = true
                            }
                        }
                    ) {
                        val avatarUrl = if (conversation.isGroup) {
                            conversation.avatarUrl ?: "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=150&q=80"
                        } else {
                            // Opposing avatar placeholder derived
                            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80"
                        }
                        UserAvatar(
                            avatarUrl = avatarUrl,
                            size = 40.dp,
                            isOnline = !conversation.isGroup,
                            showStatus = !conversation.isGroup
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = conversation.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (conversation.isGroup) "Chi tiết nhóm ℹ️" else "Đang hoạt động",
                                fontSize = 11.sp,
                                color = StatusOnline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AccentCyan)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = AccentCyan)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = AccentCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                modifier = Modifier.border(0.5.dp, DarkBorder, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Pinned Messages Panel Header (if any pinned exist)
            val pinnedMessage = messages.find { it.isPinned }
            if (pinnedMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant)
                        .border(0.5.dp, DarkBorder)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tin ghim: ${pinnedMessage.text}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Bỏ ghim",
                        color = ErrorRadical,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onPinMessage(pinnedMessage) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Central Message List
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                items(messages) { msg ->
                    if (msg.senderId == "system") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
                                shape = CircleShape,
                                modifier = Modifier.border(0.5.dp, DarkBorder.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "System message",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = msg.text,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        val isMe = msg.senderId == currentUser.id
                        val isMsgEdited = msg.isEdited
                        val isMsgDeleted = msg.isDeleted

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (!isMe) {
                                UserAvatar(
                                    avatarUrl = msg.senderAvatar,
                                    size = 32.dp,
                                    isOnline = false,
                                    showStatus = false
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                // Sub title sender nickname inside group
                                if (conversation.isGroup && !isMe) {
                                    Text(
                                        text = msg.senderName,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                    )
                                }

                                // Reply Reference preview inside bubble
                            if (msg.replyToMessageId != null && msg.replyToMessageText != null) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            DarkSurfaceVariant.copy(alpha = 0.6f),
                                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                        )
                                        .border(width = 0.5.dp, color = DarkBorder, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                        .padding(8.dp)
                                        .widthIn(max = 240.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Reply,
                                            contentDescription = "Reply Icon",
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = msg.replyToMessageText,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Principal bubble card
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .combinedClickable(
                                        onClick = { /* normal Tap */ },
                                        onLongClick = {
                                            activeContextMessage = msg
                                            showActionMenu = true
                                        }
                                    ),
                                shape = if (isMe) {
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = if (msg.replyToMessageId != null) 4.dp else 4.dp
                                    )
                                } else {
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 16.dp
                                    )
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) BubbleMe else BubbleThem
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    
                                    // Render attachments if any exist
                                    // Note: we can associate attachment loading directly
                                    // (In our seed mock messages have id: msg_m3 containing a PNG attachment preview!)
                                    if (msg.id == "msg_m3") {
                                        AsyncImage(
                                            model = "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=600&q=80",
                                            contentDescription = "Attachment preview",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { scaleImageDialogUrl = "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=600&q=80" }
                                                .padding(bottom = 6.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(6.dp)
                                        ) {
                                            Icon(Icons.Default.InsertDriveFile, contentDescription = "Doc", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("marketing_analytics_report.png", color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    // Render custom file simulator attachments uploaded on the fly
                                    if (msg.text.contains("📎 đã đính kèm:")) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { scaleImageDialogUrl = "https://images.unsplash.com/photo-1581291518633-83b4ebd1d83e?auto=format&fit=crop&w=600&q=80" }
                                        ) {
                                            AsyncImage(
                                                model = "https://images.unsplash.com/photo-1581291518633-83b4ebd1d83e?auto=format&fit=crop&w=600&q=80",
                                                contentDescription = "mock file upload url",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    Text(
                                        text = msg.text,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    
                                    // Display small timestamps
                                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    val formattedTime = format.format(Date(msg.timestamp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = formattedTime,
                                            fontSize = 9.sp,
                                            color = if (isMe) Color.White.copy(alpha = 0.7f) else TextSecondary
                                        )
                                        if (isMsgEdited) {
                                            Text("Đã chỉnh sửa", fontSize = 8.sp, color = AccentCyan)
                                        }
                                        if (msg.isPinned) {
                                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = AccentCyan, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                            }

                            // Reaction labels line
                            // Render reaction emojis overlay below bubble
                            // (msg_g4 has static reactions in database!)
                            if (msg.id == "msg_g4" || msg.isPinned && !isMsgDeleted) {
                                Row(
                                    modifier = Modifier
                                        .offset(y = (-4).dp)
                                        .background(DarkSurfaceVariant, CircleShape)
                                        .border(0.5.dp, DarkBorder, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("❤️", fontSize = 10.sp)
                                    Text("🔥", fontSize = 10.sp)
                                    Text("2", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

                if (isTyping) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                size = 28.dp,
                                isOnline = false,
                                showStatus = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TypingIndicator()
                        }
                    }
                }
            }

            // Input Console panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .border(width = 0.5.dp, color = DarkBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // If currently Replying preview
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Reply, contentDescription = "Reply indicator", tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Phản hồi @${replyingTo.senderName}: \"${replyingTo.text}\"",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        IconButton(onClick = onCancelReply, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ErrorRadical, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // If currently editing text
                if (editingMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit check", tint = AccentPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đang chỉnh sửa tin nhắn...",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { 
                            editingMessage = null
                            inputText = ""
                        }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ErrorRadical, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Main form row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    
                    // Quick Attachment select (Simulator)
                    IconButton(
                        onClick = {
                            // Automatically insert a mock image filename to simulate S3/Local file uploads instantly
                            val mockFileAttach = AttachmentEntity(
                                id = UUID.randomUUID().toString(),
                                messageId = "",
                                fileUrl = "https://images.unsplash.com/photo-1581291518633-83b4ebd1d83e?auto=format&fit=crop&w=600&q=80",
                                fileType = "image/png",
                                fileName = "sprint_architecture_redis_pg.png",
                                fileSize = 2097152L
                            )
                            onSendMessage("📎 đã đính kèm: sprint_architecture_redis_pg.png", null, listOf(mockFileAttach))
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Image attachment", tint = AccentCyan)
                    }

                    // Keyboard Input
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Nhập tin nhắn của bạn...", color = TextMuted, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(width = 0.5.dp, color = DarkBorder, shape = CircleShape)
                            .clip(CircleShape)
                            .heightIn(max = 80.dp),
                        singleLine = false
                    )

                    // Emoticon fast trigger
                    IconButton(onClick = { inputText += " 🔥 " }) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji panel", tint = AccentPurple)
                    }

                    // Send Button
                    val isMsgValid = inputText.isNotBlank()
                    IconButton(
                        onClick = {
                            if (isMsgValid) {
                                val currentEditing = editingMessage
                                if (currentEditing != null) {
                                    onEditMessage(currentEditing, inputText)
                                    editingMessage = null
                                } else {
                                    onSendMessage(inputText, replyingTo, emptyList())
                                }
                                inputText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isMsgValid) AccentIndigo else DarkSurfaceVariant
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (isMsgValid) Color.White else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG SCALE PREVIEW WORKSPACE ---
    if (scaleImageDialogUrl != null) {
        Dialog(onDismissRequest = { scaleImageDialogUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = scaleImageDialogUrl,
                    contentDescription = "Zoom image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                // Close button top right
                IconButton(
                    onClick = { scaleImageDialogUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    // --- LONG PRESS OVERLAY MENU BOTTOMSHEET DIALOG ---
    if (showActionMenu && activeContextMessage != null) {
        val targetMsg = activeContextMessage!!
        val isSenderMe = targetMsg.senderId == currentUser.id

        Dialog(onDismissRequest = { showActionMenu = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Phản ứng tin nhắn",
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    // Emoji reactions rail
                    EmojiReactionRow(
                        onEmojiSelected = { emoji ->
                            onAddReaction(targetMsg.id, emoji)
                            showActionMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = DarkBorder, thickness = 0.5.dp)

                    // Actions list
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPinMessage(targetMsg)
                                showActionMenu = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (targetMsg.isPinned) "Bỏ ghim tin nhắn" else "Ghim tin nhắn này", color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSendMessage(inputText, targetMsg, emptyList())
                                showActionMenu = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = "Reply", tint = AccentIndigo, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Phản hồi tin nhắn", color = TextPrimary)
                    }

                    if (isSenderMe && !targetMsg.isDeleted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingMessage = targetMsg
                                    inputText = targetMsg.text
                                    showActionMenu = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Chỉnh sửa tin nhắn", color = TextPrimary)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDeleteMessage(targetMsg)
                                    showActionMenu = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRadical, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Thu hồi tin nhắn", color = ErrorRadical, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showGroupDetailsDialog && conversation.isGroup) {
        Dialog(onDismissRequest = { showGroupDetailsDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(24.dp)),
                color = DarkBackground
            ) {
                var editedTitle by remember(conversation.title) { mutableStateOf(conversation.title) }
                val presets = listOf(
                    "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=150&q=80",
                    "https://images.unsplash.com/photo-1549692520-acc6669e2f0c?auto=format&fit=crop&w=150&q=80",
                    "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?auto=format&fit=crop&w=150&q=80",
                    "https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=150&q=80"
                )
                var selectedAvatar by remember(conversation.avatarUrl) { mutableStateOf(conversation.avatarUrl ?: presets.first()) }
                
                var inviteTabActive by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thông tin nhóm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        IconButton(onClick = { showGroupDetailsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }

                    // Large Avatar with preset selectors
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserAvatar(
                            avatarUrl = selectedAvatar,
                            size = 72.dp,
                            isOnline = false,
                            showStatus = false
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            presets.forEach { presetUrl ->
                                val isSelected = selectedAvatar == presetUrl
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) AccentCyan else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatar = presetUrl }
                                ) {
                                    UserAvatar(
                                        presetUrl,
                                        size = 36.dp,
                                        isOnline = false,
                                        showStatus = false
                                    )
                                }
                            }
                        }
                    }

                    // Rename text input
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Tên nhóm chat", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = AccentCyan
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (editedTitle.isNotBlank()) {
                                onUpdateGroup(editedTitle, selectedAvatar)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cập nhật thông tin", color = Color(0xFF003062), fontWeight = FontWeight.Bold)
                    }

                    // Tab selector for Members Vs Invite Users
                    TabRow(
                        selectedTabIndex = if (inviteTabActive) 1 else 0,
                        containerColor = DarkSurface,
                        contentColor = AccentCyan,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = !inviteTabActive,
                            onClick = { inviteTabActive = false },
                            text = { Text("Thành viên (${members.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = inviteTabActive,
                            onClick = { inviteTabActive = true },
                            text = { Text("Mời thêm", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        if (!inviteTabActive) {
                            // Render current members
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(members) { member ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        UserAvatar(
                                            avatarUrl = member.avatarUrl,
                                            size = 36.dp,
                                            isOnline = member.isOnline,
                                            showStatus = true
                                        )
                                        Column {
                                            Text(
                                                text = member.displayName,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "@${member.username}",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Render candidates you can invite
                            val nonMembers = allUsers.filter { user -> !members.any { it.id == user.id } && user.id != "system" }
                            val candidatesToInvite = remember { mutableStateListOf<UserEntity>() }

                            if (nonMembers.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tất cả mọi người đều có mặt.",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(nonMembers) { candidate ->
                                            val isSelected = candidatesToInvite.contains(candidate)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        if (isSelected) candidatesToInvite.remove(candidate)
                                                        else candidatesToInvite.add(candidate)
                                                    }
                                                    .padding(6.dp)
                                            ) {
                                                UserAvatar(
                                                    avatarUrl = candidate.avatarUrl,
                                                    size = 34.dp,
                                                    isOnline = candidate.isOnline,
                                                    showStatus = true
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = candidate.displayName,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = {
                                                        if (isSelected) candidatesToInvite.remove(candidate)
                                                        else candidatesToInvite.add(candidate)
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = AccentCyan,
                                                        uncheckedColor = DarkBorder
                                                    ),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onInviteMembers(candidatesToInvite.toList())
                                            candidatesToInvite.clear()
                                        },
                                        enabled = candidatesToInvite.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentPurple,
                                            disabledContainerColor = DarkSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .padding(top = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Mời thành viên đã chọn", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Leave Group button
                    Button(
                        onClick = {
                            showGroupDetailsDialog = false
                            onLeaveGroup()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRadical),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Leave", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rời khỏi nhóm trò chuyện", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
