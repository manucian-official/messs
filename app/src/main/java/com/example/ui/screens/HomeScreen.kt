package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ConversationEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.UserEntity
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    conversations: List<ConversationEntity>,
    notifications: List<NotificationEntity>,
    currentUser: UserEntity,
    allUsers: List<UserEntity>,
    onConversationSelected: (ConversationEntity) -> Unit,
    onCreateGroup: (title: String, members: List<UserEntity>) -> Unit,
    onLogout: () -> Unit,
    onMarkNotifRead: (String) -> Unit,
    onClearNotif: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Chats, 1 = Groups, 2 = Notifications, 3 = Profile
    val unreadNotifications = notifications.filter { !it.isRead }.size

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(width = 0.5.dp, color = DarkBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                    label = { Text("Đoạn chat", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        selectedTextColor = AccentCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DarkSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = "Groups") },
                    label = { Text("Tạo nhóm", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPurple,
                        selectedTextColor = AccentPurple,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DarkSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge(
                                        containerColor = ErrorRadical,
                                        contentColor = Color.White
                                    ) {
                                        Text(unreadNotifications.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    },
                    label = { Text("Thông báo", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        selectedTextColor = AccentCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DarkSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Cá nhân", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPurple,
                        selectedTextColor = AccentPurple,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
            }
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "home_tabs"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ChatsTab(
                        conversations = conversations,
                        allUsers = allUsers,
                        currentUser = currentUser,
                        onConversationSelected = onConversationSelected
                    )
                    1 -> CreateGroupTab(
                        allUsers = allUsers.filter { it.id != currentUser.id },
                        onCreateGroup = { name, users ->
                            onCreateGroup(name, users)
                            selectedTab = 0 // return to chats
                        }
                    )
                    2 -> NotificationsTab(
                        notifications = notifications,
                        onMarkNotifRead = onMarkNotifRead,
                        onClearNotif = onClearNotif
                    )
                    3 -> ProfileTab(
                        currentUser = currentUser,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

// ---------------- SUB TAB VIEWS ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTab(
    conversations: List<ConversationEntity>,
    allUsers: List<UserEntity>,
    currentUser: UserEntity,
    onConversationSelected: (ConversationEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredConversations = conversations.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.lastMessageText.contains(searchQuery, ignoreCase = true)
    }

    val onlineUsers = allUsers.filter { it.isOnline && it.id != "user_me" }
    
    val initials = remember(currentUser.displayName) {
        currentUser.displayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        // App Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentCyan, AccentIndigo)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003062),
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = "Chats",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
            
            // Header Settings icon of the immersive design
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Search Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search text field
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkBorder, CircleShape)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Active Online Users Slider (with customized Your Story layout)
        Text(
            text = "Đang hoạt động (${onlineUsers.size})",
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Immersive dash styled "Your Story" circle container
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { /* Your story handler */ }
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .border(width = 1.5.dp, color = DarkBorder, shape = CircleShape)
                            .clip(CircleShape)
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tin của bạn",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            items(onlineUsers) { user ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val existingChat = conversations.find { !it.isGroup && it.title == user.displayName }
                        if (existingChat != null) {
                            onConversationSelected(existingChat)
                        }
                    }
                ) {
                    UserAvatar(
                        avatarUrl = user.avatarUrl,
                        size = 54.dp,
                        isOnline = true,
                        showStatus = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.displayName.split(" ").last(),
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))

        // Conversation items
        Text(
            text = "Hộp thư đến",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        if (filteredConversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Empty",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Không tìm thấy đoạn chat nào",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredConversations) { conv ->
                    val resolvedAvatar = if (conv.isGroup) {
                        "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=150&q=80"
                    } else {
                        allUsers.find { it.displayName == conv.title }?.avatarUrl ?: ""
                    }
                    val isUserOnline = allUsers.find { it.displayName == conv.title }?.isOnline ?: false

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onConversationSelected(conv) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            avatarUrl = resolvedAvatar,
                            size = 52.dp,
                            isOnline = isUserOnline,
                            showStatus = !conv.isGroup
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = conv.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            val lastMsgPreview = if (conv.lastMessageText.length > 35) conv.lastMessageText.take(32) + "..." else conv.lastMessageText
                            Text(
                                text = lastMsgPreview,
                                color = if (conv.lastMessageText.contains("đã gửi") || conv.lastMessageText.contains("họp")) TextPrimary else TextSecondary,
                                fontWeight = if (conv.lastMessageText.contains("đã gửi") || conv.lastMessageText.contains("họp")) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Time components & Pinned Indicator count badge
                        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val timeString = format.format(Date(conv.lastMessageTime))
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = timeString,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (conv.isGroup) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(AccentCyan),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "3",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF003062)
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = "Seen",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(13.dp)
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
fun CreateGroupTab(
    allUsers: List<UserEntity>,
    onCreateGroup: (String, List<UserEntity>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<UserEntity>() }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tạo phòng chat nhóm",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.White
        )

        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Tên cuộc thảo luận nhóm") },
            placeholder = { Text("e.g. Sắp bàn giao dự án 🚀") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = AccentPurple
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Thêm thành viên vào phòng (${selectedMembers.size})",
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(allUsers) { user ->
                val isSelected = selectedMembers.contains(user)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isSelected) selectedMembers.remove(user) else selectedMembers.add(user)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        avatarUrl = user.avatarUrl,
                        size = 44.dp,
                        isOnline = user.isOnline,
                        showStatus = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                        Text("@${user.username}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            if (isSelected) selectedMembers.remove(user) else selectedMembers.add(user)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentPurple,
                            uncheckedColor = DarkBorder
                        )
                    )
                }
            }
        }

        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = ErrorRadical, fontSize = 12.sp)
        }

        Button(
            onClick = {
                if (groupName.isBlank()) {
                    errorMsg = "Vui lòng nhập tên nhóm!"
                } else if (selectedMembers.isEmpty()) {
                    errorMsg = "Chọn ít nhất 1 thành viên!"
                } else {
                    onCreateGroup(groupName, selectedMembers.toList())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text("Tạo cuộc trò chuyện nhóm mới", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NotificationsTab(
    notifications: List<NotificationEntity>,
    onMarkNotifRead: (String) -> Unit,
    onClearNotif: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Hệ thống thông báo",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CircleNotifications,
                        contentDescription = "Empty",
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Không có thông báo mới nào", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (!notif.isRead) 1.dp else 0.dp,
                                brush = Brush.horizontalGradient(listOf(AccentCyan, AccentPurple)),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.isRead) DarkSurfaceVariant else DarkSurface
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    if (!notif.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(AccentCyan)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.content,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            Row {
                                if (!notif.isRead) {
                                    IconButton(onClick = { onMarkNotifRead(notif.id) }) {
                                        Icon(Icons.Default.Done, contentDescription = "Read", tint = AccentCyan)
                                    }
                                }
                                IconButton(onClick = { onClearNotif(notif.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRadical)
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
fun ProfileTab(
    currentUser: UserEntity,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Thông tin cá nhân",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Avatar with pulse frame
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(AccentCyan, AccentPurple, AccentCyan)
                    )
                )
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                avatarUrl = currentUser.avatarUrl,
                size = 104.dp,
                isOnline = true,
                showStatus = false
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentUser.displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                text = "@${currentUser.username}",
                fontSize = 13.sp,
                color = AccentCyan
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info components
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Trạng thái", color = TextSecondary, fontSize = 13.sp)
                    Text("Đang trực tuyến 🟢", color = StatusOnline, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = DarkBorder, thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Thành phần API", color = TextSecondary, fontSize = 13.sp)
                    Text("Socket.IO + Next.js Server", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Divider(color = DarkBorder, thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cơ sở dữ liệu", color = TextSecondary, fontSize = 13.sp)
                    Text("PostgreSQL (Room DB Core)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRadical)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng xuất tài khoản", fontWeight = FontWeight.Bold)
        }
    }
}
