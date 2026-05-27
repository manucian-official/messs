package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val isOnline: Boolean,
    val lastSeen: Long
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isGroup: Boolean,
    val createdAt: Long,
    val lastMessageText: String,
    val lastMessageTime: Long,
    val avatarUrl: String? = null
)

@Entity(tableName = "conversation_members", primaryKeys = ["conversationId", "userId"])
data class ConversationMemberEntity(
    val conversationId: String,
    val userId: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToMessageText: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val fileUrl: String,
    val fileType: String,
    val fileName: String,
    val fileSize: Long
)

@Entity(tableName = "reactions")
data class ReactionEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val userId: String,
    val userName: String,
    val emoji: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
