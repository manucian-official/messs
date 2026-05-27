package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    // Current logged-in Session state
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Screen states
    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversation = MutableStateFlow<ConversationEntity?>(null)
    val activeConversation: StateFlow<ConversationEntity?> = _activeConversation.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages: StateFlow<List<MessageEntity>> = _activeMessages.asStateFlow()

    private val _activeMembers = MutableStateFlow<List<UserEntity>>(emptyList())
    val activeMembers: StateFlow<List<UserEntity>> = _activeMembers.asStateFlow()

    // Realtime UI states
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<MessageEntity?>(null)
    val replyingToMessage: StateFlow<MessageEntity?> = _replyingToMessage.asStateFlow()

    private var messageObserveJob: Job? = null
    private var memberObserveJob: Job? = null
    private var simulatorJob: Job? = null

    init {
        // Automatically verify or set pre-saved user
        viewModelScope.launch {
            val defaultMe = UserEntity(
                id = "user_me",
                username = "khoiplus",
                displayName = "Khôi Plus",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80",
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
            repository.saveUser(defaultMe)
            _currentUser.value = defaultMe
            
            // Seed base values for an incredible UI experience
            repository.seedInitialDataIfEmpty(
                currentUserId = defaultMe.id,
                currentUserName = defaultMe.displayName,
                currentUserAvatar = defaultMe.avatarUrl
            )
        }

        // Start background socket emulation
        startSocketEmulation()
    }

    fun login(username: String, displayName: String, avatarUrl: String) {
        viewModelScope.launch {
            val user = UserEntity(
                id = "user_me",
                username = username.lowercase().replace(" ", ""),
                displayName = displayName,
                avatarUrl = avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80" },
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
            repository.saveUser(user)
            _currentUser.value = user
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeConversation.value = null
        _activeMessages.value = emptyList()
    }

    fun deselectConversation() {
        _activeConversation.value = null
        _activeMessages.value = emptyList()
        _activeMembers.value = emptyList()
    }

    fun selectConversation(conversation: ConversationEntity) {
        _activeConversation.value = conversation
        
        // Listen to Messages corresponding to selection
        messageObserveJob?.cancel()
        messageObserveJob = viewModelScope.launch {
            repository.getMessagesForConversation(conversation.id).collect { list ->
                _activeMessages.value = list
            }
        }

        // Listen to members list
        memberObserveJob?.cancel()
        memberObserveJob = viewModelScope.launch {
            repository.getMembersOfConversation(conversation.id).collect { list ->
                _activeMembers.value = list
            }
        }
    }

    fun sendMessage(text: String, replyTo: MessageEntity? = null, attachments: List<AttachmentEntity> = emptyList()) {
        val user = _currentUser.value ?: return
        val conv = _activeConversation.value ?: return

        viewModelScope.launch {
            val message = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conv.id,
                senderId = user.id,
                senderName = user.displayName,
                senderAvatar = user.avatarUrl,
                text = text,
                timestamp = System.currentTimeMillis(),
                replyToMessageId = replyTo?.id,
                replyToMessageText = replyTo?.let { if (it.isDeleted) "Tin nhắn đã bị thu hồi" else it.text }
            )

            repository.insertMessage(message, attachments)
            _replyingToMessage.value = null

            // Trigger simulated "Socket Response" logic
            triggerSimulatedResponse(text)
        }
    }

    fun togglePinMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.pinMessage(message.id, !message.isPinned)
        }
    }

    fun editMessage(message: MessageEntity, newText: String) {
        viewModelScope.launch {
            repository.editMessage(message.id, newText)
        }
    }

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.deleteMessage(message.id)
        }
    }

    fun setReplyingTo(message: MessageEntity?) {
        _replyingToMessage.value = message
    }

    fun addReaction(messageId: String, emoji: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.addReaction(messageId, user.id, user.displayName, emoji)
        }
    }

    fun removeReaction(messageId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.removeReaction(messageId, user.id)
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun clearNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun selectOrAddConversation(title: String, isGroup: Boolean, selectedUsers: List<UserEntity>, avatarUrl: String? = null) {
        viewModelScope.launch {
            val convId = "conv_" + UUID.randomUUID().toString().take(6)
            val newConv = ConversationEntity(
                id = convId,
                title = title,
                isGroup = isGroup,
                createdAt = System.currentTimeMillis(),
                lastMessageText = "Cuộc hội thoại mới đã được khởi tạo",
                lastMessageTime = System.currentTimeMillis(),
                avatarUrl = avatarUrl ?: if (isGroup) "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=150&q=80" else null
            )
            
            val memberIds = selectedUsers.map { it.id }.toMutableList()
            _currentUser.value?.id?.let { memberIds.add(it) }

            repository.createConversation(newConv, memberIds)
            _activeConversation.value = newConv
            selectConversation(newConv)
        }
    }

    fun leaveGroup(convId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val announcement = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                senderId = "system",
                senderName = "Hệ thống",
                senderAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=150&q=80",
                text = "${user.displayName} đã rời khỏi nhóm chat",
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(announcement)
            repository.removeMemberFromConversation(convId, user.id)

            _activeConversation.value = null
            _activeMessages.value = emptyList()
            _activeMembers.value = emptyList()
        }
    }

    fun inviteMembersToGroup(convId: String, invitedUsers: List<UserEntity>) {
        if (invitedUsers.isEmpty()) return
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val memberships = invitedUsers.map {
                ConversationMemberEntity(convId, it.id)
            }
            repository.insertMembers(memberships)

            val namesString = invitedUsers.joinToString(", ") { it.displayName }
            val announcement = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                senderId = "system",
                senderName = "Hệ thống",
                senderAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=150&q=80",
                text = "${user.displayName} đã thêm $namesString vào nhóm",
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(announcement)
        }
    }

    fun updateGroupSettings(convId: String, newName: String, newAvatarUrl: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateGroupDetails(convId, newName, newAvatarUrl)
            
            val announcement = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                senderId = "system",
                senderName = "Hệ thống",
                senderAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=150&q=80",
                text = "${user.displayName} đã cập nhật thông tin nhóm thành \"$newName\"",
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(announcement)

            // Reload exact active snapshot configuration in view state in real-time
            val updatedConfig = repository.conversations.firstOrNull()?.find { it.id == convId }
            if (updatedConfig != null) {
                _activeConversation.value = updatedConfig
            }
        }
    }

    // --- SOCKET.IO SIMULATION SUITE ---
    private fun startSocketEmulation() {
        simulatorJob?.cancel()
        simulatorJob = viewModelScope.launch {
            while (isActive) {
                delay(24000) // Sleep 24 seconds, periodically firing system alerts or notifications
                val currentConv = _activeConversation.value
                val notificationCount = notifications.value.filter { !it.isRead }.size
                
                // Randomly trigger typing indicator sometimes in active group or active chat
                if (currentConv != null && !_isTyping.value) {
                    val senderId = if (currentConv.isGroup) "user_tien" else currentConv.id.replace("conv_", "user_")
                    val sender = repository.getUser(senderId)
                    if (sender != null && sender.isOnline) {
                        _isTyping.value = true
                        delay(2500)
                        _isTyping.value = false
                        
                        // Pick a smart preset sentences depending on group / name
                        val mockSentence = listOf(
                            "Mọi người nghe tin gì chưa, dự án vừa test benchmark đạt hơn 10k TPS đấy!",
                            "Code clean chuẩn enterprise chạy sướng thật.",
                            "Tối nay làm cốc bia mừng app deploy thành công nha ae!",
                            "Vừa tối ưu xong API, load mượt nổ mắt."
                        ).random()

                        val autoMsg = MessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = currentConv.id,
                            senderId = sender.id,
                            senderName = sender.displayName,
                            senderAvatar = sender.avatarUrl,
                            text = mockSentence,
                            timestamp = System.currentTimeMillis()
                        )
                        repository.insertMessage(autoMsg)
                    }
                } else if (currentConv == null && notificationCount < 3) {
                    // Throw background system notification
                    val notifId = UUID.randomUUID().toString()
                    val sysNotif = NotificationEntity(
                        id = notifId,
                        title = "Tin nhắn mới từ Đội Ngũ Tech 🔥",
                        content = "Mai Hoa Lê: \"Em đã cập nhật UI hoàn thành rồi nha!\"",
                        timestamp = System.currentTimeMillis(),
                        isRead = false
                    )
                    repository.insertNotification(sysNotif)
                }
            }
        }
    }

    private fun triggerSimulatedResponse(triggerText: String) {
        val currentConv = _activeConversation.value ?: return
        viewModelScope.launch {
            // Wait 1.2s then flash typing bubble
            delay(1200)
            _isTyping.value = true
            delay(2000) // typing for 2 seconds
            _isTyping.value = false

            // Answer based on typing keyword triggers
            val answer = when {
                triggerText.contains("họp", ignoreCase = true) || triggerText.contains("meeting", ignoreCase = true) -> {
                    "Nhất trí ông ơi! Tầm 15h chiều mình gộp zoom làm quả sprint review nhé."
                }
                triggerText.contains("bug", ignoreCase = true) || triggerText.contains("lỗi", ignoreCase = true) -> {
                    "Lỗi ở đâu thế? Đã check logs của Prisma + PostgreSQL, kết nối DB vẫn cực kỳ ổn định nha."
                }
                triggerText.contains("logo", ignoreCase = true) || triggerText.contains("ảnh", ignoreCase = true) -> {
                    "Logo với assets tối nay em xuất bản vẽ gửi qua ổ S3 cho mọi người download luôn."
                }
                triggerText.contains("socket", ignoreCase = true) || triggerText.contains("realtime", ignoreCase = true) -> {
                    "Tối ưu adapter Socket IO xong xuôi rồi, ping giảm chỉ còn 8ms thôi nhé ae!"
                }
                else -> {
                    listOf(
                        "Rất chất lượng anh em ơi! 🚀",
                        "Quá dữ, code này rà soát kỹ lắm rồi, ko lo bug đâu.",
                        "Em đồng ý, thiết kế UI/UX đợt này sang xịn mịn hơn hẳn.",
                        "Ok luôn ông nhé, tôi đang tối ưu thêm index dưới PostgreSQL để tăng load.",
                        "Đã nhận thông tin nhé!"
                    ).random()
                }
            }

            // Assign who responds
            val senderId = if (currentConv.isGroup) {
                listOf("user_tien", "user_mai", "user_trung").random()
            } else {
                currentConv.id.replace("conv_", "user_")
            }
            val sender = repository.getUser(senderId) ?: UserEntity("user_tien", "tien_nguyen", "Kiều Tiến Nguyễn", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80", true, System.currentTimeMillis())

            val responseMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = currentConv.id,
                senderId = sender.id,
                senderName = sender.displayName,
                senderAvatar = sender.avatarUrl,
                text = answer,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(responseMsg)

            // Trigger emoji reaction to your text automatically after 3 seconds for visual fun!
            delay(2500)
            val randomEmoji = listOf("❤️", "🔥", "👍", "😂").random()
            repository.addReaction(responseMsg.id, "user_me", "Khôi Plus", randomEmoji)
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageObserveJob?.cancel()
        memberObserveJob?.cancel()
        simulatorJob?.cancel()
    }
}
