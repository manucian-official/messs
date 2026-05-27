package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val reactionDao = db.reactionDao()
    private val notificationDao = db.notificationDao()

    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val notifications: Flow<List<NotificationEntity>> = notificationDao.getNotifications()

    fun getMembersOfConversation(convId: String): Flow<List<UserEntity>> {
        return conversationDao.getMembersOfConversation(convId)
    }

    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(convId)
    }

    fun getReactionsForMessage(messageId: String): Flow<List<ReactionEntity>> {
        return reactionDao.getReactionsForMessage(messageId)
    }

    fun getAttachmentsForMessage(messageId: String): Flow<List<AttachmentEntity>> {
        return messageDao.getAttachmentsForMessage(messageId)
    }

    suspend fun saveUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun getUser(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    suspend fun createConversation(conversation: ConversationEntity, memberIds: List<String>) = withContext(Dispatchers.IO) {
        conversationDao.insertConversation(conversation)
        val memberships = memberIds.map { userId ->
            ConversationMemberEntity(conversation.id, userId)
        }
        conversationDao.insertMembers(memberships)
    }

    suspend fun insertMessage(message: MessageEntity, attachments: List<AttachmentEntity> = emptyList()) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
        if (attachments.isNotEmpty()) {
            messageDao.insertAttachments(attachments)
        }
        // Update last message in the parent conversation
        val conversation = conversationDao.getConversationById(message.conversationId)
        if (conversation != null) {
            val updatedConv = conversation.copy(
                lastMessageText = message.text,
                lastMessageTime = message.timestamp
            )
            conversationDao.insertConversation(updatedConv)
        }
    }

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        val msg = messageDao.getMessageById(messageId)
        if (msg != null) {
            messageDao.markMessageAsDeleted(messageId)
            // Retrieve latest remaining message and update conversation
            // (For simplicity we just notify conversation is deleted)
            val conversation = conversationDao.getConversationById(msg.conversationId)
            if (conversation != null && conversation.lastMessageTime <= msg.timestamp) {
                val updatedConv = conversation.copy(lastMessageText = "Tin nhắn đã bị thu hồi")
                conversationDao.insertConversation(updatedConv)
            }
        }
    }

    suspend fun editMessage(messageId: String, newText: String) = withContext(Dispatchers.IO) {
        val msg = messageDao.getMessageById(messageId)
        if (msg != null) {
            messageDao.updateMessageText(messageId, newText)
            val conversation = conversationDao.getConversationById(msg.conversationId)
            if (conversation != null && conversation.lastMessageTime <= msg.timestamp) {
                val updatedConv = conversation.copy(lastMessageText = newText)
                conversationDao.insertConversation(updatedConv)
            }
        }
    }

    suspend fun pinMessage(messageId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        messageDao.updateMessagePinned(messageId, isPinned)
    }

    suspend fun addReaction(messageId: String, userId: String, username: String, emoji: String) = withContext(Dispatchers.IO) {
        val id = "$messageId-$userId-$emoji"
        reactionDao.insertReaction(ReactionEntity(id, messageId, userId, username, emoji))
    }

    suspend fun removeReaction(messageId: String, userId: String) = withContext(Dispatchers.IO) {
        reactionDao.removeReaction(messageId, userId)
    }

    suspend fun insertNotification(notification: NotificationEntity) = withContext(Dispatchers.IO) {
        notificationDao.insertNotification(notification)
    }

    suspend fun markNotificationRead(id: String) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        notificationDao.deleteNotification(id)
    }

    suspend fun removeMemberFromConversation(convId: String, userId: String) = withContext(Dispatchers.IO) {
        conversationDao.removeMember(convId, userId)
    }

    suspend fun updateGroupDetails(convId: String, newTitle: String, newAvatarUrl: String?) = withContext(Dispatchers.IO) {
        conversationDao.updateGroupDetails(convId, newTitle, newAvatarUrl)
    }

    suspend fun insertMembers(memberships: List<ConversationMemberEntity>) = withContext(Dispatchers.IO) {
        conversationDao.insertMembers(memberships)
    }

    suspend fun seedInitialDataIfEmpty(currentUserId: String, currentUserName: String, currentUserAvatar: String) = withContext(Dispatchers.IO) {
        val existingUsers = userDao.getAllUsers().firstOrNull()
        if (existingUsers.isNullOrEmpty()) {
            // 1. Create Core Users
            val systemUsers = listOf(
                UserEntity(
                    id = currentUserId,
                    username = "khoiplus",
                    displayName = currentUserName,
                    avatarUrl = currentUserAvatar,
                    isOnline = true,
                    lastSeen = System.currentTimeMillis()
                ),
                UserEntity(
                    id = "user_tien",
                    username = "tien_nguyen",
                    displayName = "Kiều Tiến Nguyễn",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis()
                ),
                UserEntity(
                    id = "user_mai",
                    username = "hoa_mai",
                    displayName = "Mai Hoa Lê",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
                    isOnline = false,
                    lastSeen = System.currentTimeMillis() - 12 * 60 * 1000 // 12 minutes ago
                ),
                UserEntity(
                    id = "user_trung",
                    username = "trung_pham",
                    displayName = "Trung Phạm Minh",
                    avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=150&q=80",
                    isOnline = false,
                    lastSeen = System.currentTimeMillis() - 2 * 60 * 60 * 1000 // 2 hours ago
                )
            )

            userDao.insertUsers(systemUsers)

            // 2. Create Conversations
            // Conversation A: 1-1 with Tien
            val convTienId = "conv_tien"
            val convTien = ConversationEntity(
                id = convTienId,
                title = "Kiều Tiến Nguyễn",
                isGroup = false,
                createdAt = System.currentTimeMillis() - 24 * 60 * 60 * 1000,
                lastMessageText = "Hôm nay họp lúc mấy giờ thế ông?",
                lastMessageTime = System.currentTimeMillis() - 12 * 60 * 1000
            )

            // Conversation B: 1-1 with Mai
            val convMaiId = "conv_mai"
            val convMai = ConversationEntity(
                id = convMaiId,
                title = "Mai Hoa Lê",
                isGroup = false,
                createdAt = System.currentTimeMillis() - 48 * 60 * 60 * 1000,
                lastMessageText = "Em đã gửi anh file báo cáo marketing rồi nhé!",
                lastMessageTime = System.currentTimeMillis() - 1 * 60 * 60 * 1000
            )

            // Conversation C: Group Conversation "Đội Ngũ Tech 🔥"
            val convGroupId = "conv_group_tech"
            val convGroup = ConversationEntity(
                id = convGroupId,
                title = "Đội Ngũ Tech 🔥",
                isGroup = true,
                createdAt = System.currentTimeMillis() - 72 * 60 * 60 * 1000,
                lastMessageText = "Đã fix xong bug realtime socket rồi nha ae",
                lastMessageTime = System.currentTimeMillis() - 5 * 60 * 1000,
                avatarUrl = "https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?auto=format&fit=crop&w=150&q=80"
            )

            conversationDao.insertConversation(convTien)
            conversationDao.insertConversation(convMai)
            conversationDao.insertConversation(convGroup)

            // 3. Register Members
            val memberships = listOf(
                // Conv Tien
                ConversationMemberEntity(convTienId, currentUserId),
                ConversationMemberEntity(convTienId, "user_tien"),
                // Conv Mai
                ConversationMemberEntity(convMaiId, currentUserId),
                ConversationMemberEntity(convMaiId, "user_mai"),
                // Conv Group (Me, Tien, Mai, Trung)
                ConversationMemberEntity(convGroupId, currentUserId),
                ConversationMemberEntity(convGroupId, "user_tien"),
                ConversationMemberEntity(convGroupId, "user_mai"),
                ConversationMemberEntity(convGroupId, "user_trung")
            )
            conversationDao.insertMembers(memberships)

            // 4. Seeding historical messages
            // Conversation A with Tien
            val messagesTien = listOf(
                MessageEntity(
                    id = "msg_t1",
                    conversationId = convTienId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    senderAvatar = currentUserAvatar,
                    text = "Chào Tiến, dự án mới chạy ok chưa ông?",
                    timestamp = System.currentTimeMillis() - 4 * 60 * 60 * 1000
                ),
                MessageEntity(
                    id = "msg_t2",
                    conversationId = convTienId,
                    senderId = "user_tien",
                    senderName = "Kiều Tiến Nguyễn",
                    senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                    text = "Cực kỳ mượt nha Khôi! Mọi request DB đều đã tối ưu hoàn chỉnh.",
                    timestamp = System.currentTimeMillis() - 3 * 60 * 60 * 1000
                ),
                MessageEntity(
                    id = "msg_t3",
                    conversationId = convTienId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    senderAvatar = currentUserAvatar,
                    text = "Đỉnh quá! Tầm chiều rảnh họp bàn tiếp nhé.",
                    timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000
                ),
                MessageEntity(
                    id = "msg_t4",
                    conversationId = convTienId,
                    senderId = "user_tien",
                    senderName = "Kiều Tiến Nguyễn",
                    senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                    text = "Hôm nay họp lúc mấy giờ thế ông?",
                    timestamp = System.currentTimeMillis() - 12 * 60 * 1000
                )
            )

            // Conversation B with Mai
            val messagesMai = listOf(
                MessageEntity(
                    id = "msg_m1",
                    conversationId = convMaiId,
                    senderId = "user_mai",
                    senderName = "Mai Hoa Lê",
                    senderAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
                    text = "Hi anh Khôi, thông tin chiến dịch Marketing tuần sau đây ạ.",
                    timestamp = System.currentTimeMillis() - 5 * 60 * 60 * 1000
                ),
                MessageEntity(
                    id = "msg_m2",
                    conversationId = convMaiId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    senderAvatar = currentUserAvatar,
                    text = "Ok em gửi anh file PDF hoặc link Figma anh check nhé.",
                    timestamp = System.currentTimeMillis() - 4 * 60 * 60 * 1000
                ),
                MessageEntity(
                    id = "msg_m3",
                    conversationId = convMaiId,
                    senderId = "user_mai",
                    senderName = "Mai Hoa Lê",
                    senderAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
                    text = "Em đã gửi anh file báo cáo marketing rồi nhé!",
                    timestamp = System.currentTimeMillis() - 1 * 60 * 60 * 1000
                )
            )

            // Conversation C: Group
            val messagesGroup = listOf(
                MessageEntity(
                    id = "msg_g1",
                    conversationId = convGroupId,
                    senderId = "user_trung",
                    senderName = "Trung Phạm Minh",
                    senderAvatar = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=150&q=80",
                    text = "Ae ơi tình hình API thế nào rồi nhỉ, kịp deadline ko?",
                    timestamp = System.currentTimeMillis() - 30 * 60 * 1000
                ),
                MessageEntity(
                    id = "msg_g2",
                    conversationId = convGroupId,
                    senderId = "user_tien",
                    senderName = "Kiều Tiến Nguyễn",
                    senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                    text = "Kịp chứ bro. Đang tối ưu hóa socket IO adapter với Redis.",
                    timestamp = System.currentTimeMillis() - 25 * 60 * 1000
                ),
                MessageEntity(
                    id = "msg_g3",
                    conversationId = convGroupId,
                    senderId = "user_mai",
                    senderName = "Mai Hoa Lê",
                    senderAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
                    text = "Em làm xong logo và assets cho app rồi nha.",
                    timestamp = System.currentTimeMillis() - 15 * 60 * 1000
                ),
                MessageEntity(
                    id = "msg_g4",
                    conversationId = convGroupId,
                    senderId = "user_tien",
                    senderName = "Kiều Tiến Nguyễn",
                    senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                    text = "Đã fix xong bug realtime socket rồi nha ae",
                    timestamp = System.currentTimeMillis() - 5 * 60 * 1000
                )
            )

            // Insert messages to DB
            for (m in messagesTien) { messageDao.insertMessage(m) }
            for (m in messagesMai) { messageDao.insertMessage(m) }
            for (m in messagesGroup) { messageDao.insertMessage(m) }

            // Attachments for Mai's conversation
            val attachment = AttachmentEntity(
                id = "attach_r1",
                messageId = "msg_m3",
                fileUrl = "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=600&q=80",
                fileType = "image/png",
                fileName = "marketing_report_v2.png",
                fileSize = 1048576L // 1MB
            )
            messageDao.insertAttachment(attachment)

            // Pinned system messages
            messageDao.updateMessagePinned("msg_g4", true)

            // Seed Reactions
            reactionDao.insertReaction(ReactionEntity("react1", "msg_g4", "user_trung", "Trung Phạm Minh", "🔥"))
            reactionDao.insertReaction(ReactionEntity("react2", "msg_g4", "user_mai", "Mai Hoa Lê", "❤️"))
            reactionDao.insertReaction(ReactionEntity("react3", "msg_t2", currentUserId, currentUserName, "👍"))

            // Seed Notifications (system triggers)
            val notifics = listOf(
                NotificationEntity(
                    id = "notif_1",
                    title = "Kiều Tiến Nguyễn nhắc đến bạn",
                    content = "\"@khoiplus Dự án cực mượt rồi, chuẩn bị demo nhé ông anh!\"",
                    timestamp = System.currentTimeMillis() - 5 * 60 * 1000,
                    isRead = false
                ),
                NotificationEntity(
                    id = "notif_2",
                    title = "Tin nhắn ghim mới",
                    content = "Tiến Nguyễn đã ghim một tin nhắn trong Đội Ngũ Tech 🔥",
                    timestamp = System.currentTimeMillis() - 4 * 60 * 800,
                    isRead = true
                )
            )
            for (n in notifics) {
                notificationDao.insertNotification(n)
            }
        }
    }
}
