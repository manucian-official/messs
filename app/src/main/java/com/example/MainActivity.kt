package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.*
import com.example.data.repository.ChatRepository
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize persistent Room DB and Repository layer
        val database = AppDatabase.getDatabase(this)
        val repository = ChatRepository(database)

        // 2. Setup ViewModel Factory for simple Constructor Injection
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            MyApplicationTheme {
                val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)

                // 3. Observe the core States reactively with Lifecycle-safety
                val currentUser by chatViewModel.currentUser.collectAsStateWithLifecycle()
                val conversations by chatViewModel.conversations.collectAsStateWithLifecycle()
                val activeConversation by chatViewModel.activeConversation.collectAsStateWithLifecycle()
                val activeMessages by chatViewModel.activeMessages.collectAsStateWithLifecycle()
                val allUsers by chatViewModel.allUsers.collectAsStateWithLifecycle()
                val notifications by chatViewModel.notifications.collectAsStateWithLifecycle()
                val isTyping by chatViewModel.isTyping.collectAsStateWithLifecycle()
                val replyingToMessage by chatViewModel.replyingToMessage.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val appModifier = Modifier.padding(innerPadding)

                    when {
                        currentUser == null -> {
                            // User is forced to register/log in to set up username and avatar
                            LoginScreen(
                                onLoginSuccess = { username, name, avatar ->
                                    chatViewModel.login(username, name, avatar)
                                },
                                modifier = appModifier
                            )
                        }
                        activeConversation != null -> {
                            // Deep Chat Window portal active
                            ChatScreen(
                                conversation = activeConversation!!,
                                messages = activeMessages,
                                currentUser = currentUser!!,
                                isTyping = isTyping,
                                replyingTo = replyingToMessage,
                                onBack = { chatViewModel.selectOrAddConversation("", false, emptyList()) }, // goes back
                                onSendMessage = { text, reply, attachments ->
                                    chatViewModel.sendMessage(text, reply, attachments)
                                },
                                onPinMessage = { msg -> chatViewModel.togglePinMessage(msg) },
                                onEditMessage = { msg, newText -> chatViewModel.editMessage(msg, newText) },
                                onDeleteMessage = { msg -> chatViewModel.deleteMessage(msg) },
                                onAddReaction = { id, emoji -> chatViewModel.addReaction(id, emoji) },
                                onCancelReply = { chatViewModel.setReplyingTo(null) },
                                modifier = appModifier
                            )
                        }
                        else -> {
                            // Master Dashboard navigation list
                            HomeScreen(
                                conversations = conversations,
                                notifications = notifications,
                                currentUser = currentUser!!,
                                allUsers = allUsers,
                                onConversationSelected = { conv -> chatViewModel.selectConversation(conv) },
                                onCreateGroup = { title, selectedMembers ->
                                    chatViewModel.selectOrAddConversation(title, true, selectedMembers)
                                },
                                onLogout = { chatViewModel.logout() },
                                onMarkNotifRead = { id -> chatViewModel.markNotificationAsRead(id) },
                                onClearNotif = { id -> chatViewModel.clearNotification(id) },
                                modifier = appModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
