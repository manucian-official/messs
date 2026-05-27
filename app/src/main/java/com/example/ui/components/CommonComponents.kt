package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*

@Composable
fun UserAvatar(
    avatarUrl: String,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    showStatus: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .padding(2.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (avatarUrl.isNotEmpty() && (avatarUrl.startsWith("http") || avatarUrl.startsWith("content"))) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.5.dp, AccentIndigo, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder Avatar based on colored gradient circle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentCyan, AccentPurple)
                        )
                    )
                    .border(2.dp, DarkBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4f).sp
                )
            }
        }

        if (showStatus) {
            val statusColor = if (isOnline) StatusOnline else TextMuted
            val borderClr = DarkBackground
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(1.5.dp, borderClr, CircleShape)
            )
        }
    }
}

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(BubbleThem, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "dots")
        
        repeat(3) { index ->
            val delay = index * 150
            val bounce by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 600
                        0f at 0
                        -6f at 200
                        0f at 400
                        0f at 600
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delay)
                ),
                label = "bounce"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = bounce.dp)
                    .clip(CircleShape)
                    .background(AccentCyan)
            )
        }
    }
}

@Composable
fun EmojiReactionRow(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
    Row(
        modifier = modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(24.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEach { emoji ->
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onEmojiSelected(emoji) }
                    .padding(4.dp)
            )
        }
    }
}
