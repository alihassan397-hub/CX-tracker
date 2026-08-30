package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiChatMessage
import com.example.ui.theme.HblLime
import com.example.ui.theme.HblOnLime
import com.example.ui.theme.HblPrimary
import com.example.ui.theme.HblPrimaryDark
import com.example.ui.theme.HblSecondary
import com.example.ui.theme.HblTertiaryGold
import com.example.ui.viewmodel.CxViewModel

@Composable
fun AiAdvisorScreen(
    viewModel: CxViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val chatMessages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val promptSuggestions = listOf(
        "⚡ How can we optimize CMU SLA TAT?",
        "📊 Evaluate department SLA & breach risks",
        "⚠️ Analyze critical SLA near-breach items",
        "💡 Give tips to improve customer CSAT & FCR",
        "🎯 Generate executive briefing for Unit Heads"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Hero AI Advisor Header
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().testTag("ai_advisor_header_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(HblLime, HblPrimary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Advisor",
                        tint = HblOnLime,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Gemini CX AI Intelligence",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HblPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = HblLime.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "2.5-Flash",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = HblPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Automated root-cause analysis, SLA forecasting & coaching",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt Chips Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(promptSuggestions) { prompt ->
                Surface(
                    color = HblPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(20.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(HblPrimary.copy(alpha = 0.3f), HblLime.copy(alpha = 0.3f)))
                    ),
                    modifier = Modifier.clickable {
                        viewModel.sendAiAdvisorPrompt(prompt)
                    }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HblPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat History List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Field and Send Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Ask Gemini about SLA, breaches, coaching...", fontSize = 13.sp) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input_field"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputPrompt.isNotBlank()) {
                        val text = inputPrompt
                        inputPrompt = ""
                        viewModel.sendAiAdvisorPrompt(text)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(HblPrimary)
                    .testTag("ai_send_prompt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: AiChatMessage) {
    val isAi = message.isFromAi

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
    ) {
        if (isAi) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(HblPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isAi) 2.dp else 16.dp,
                bottomEnd = if (isAi) 16.dp else 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isAi) MaterialTheme.colorScheme.surfaceVariant else HblPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isAi) HblPrimary else HblLime
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isAi) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                )
            }
        }
    }
}
