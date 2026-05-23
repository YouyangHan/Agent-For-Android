package com.agentforandroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentforandroid.model.Message
import com.agentforandroid.ui.components.ChatBubble
import com.agentforandroid.ui.components.MessageInput
import com.agentforandroid.viewmodel.ChatViewModel
import com.agentforandroid.viewmodel.ConfigViewModel
import com.agentforandroid.viewmodel.SkillViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatVM: ChatViewModel = viewModel(),
    configVM: ConfigViewModel = viewModel(),
    skillVM: SkillViewModel = viewModel()
) {
    val messages by chatVM.messages.collectAsState()
    val streamingText by chatVM.streamingText.collectAsState()
    val isLoading by chatVM.isLoading.collectAsState()
    val error by chatVM.error.collectAsState()
    val configs by configVM.configs.collectAsState()
    val skills by skillVM.skills.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var modelDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (configs.isNotEmpty()) {
            val firstId = configs.first().id
            chatVM.setSelectedConfigId(firstId)
            chatVM.initOrCreateSession(
                modelConfigId = firstId,
                enabledSkills = skills.filter { it.enabled }.map { it.name }
            )
        }
    }

    // Refresh personality list when skills change (restore only once on first load)
    var personaRestored by remember { mutableStateOf(false) }
    LaunchedEffect(skills) {
        chatVM.refreshPersonalities()
        if (!personaRestored && skills.isNotEmpty()) {
            chatVM.restorePersonality()
            personaRestored = true
        }
    }

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty()) {
            kotlinx.coroutines.delay(50) // wait for layout
            listState.scrollToItem(messages.size)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            chatVM.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = { Text("Agent Yang") }
            )
        },
        bottomBar = {
            Column {
                // Model + Personality in one row (always visible)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Model selector
                        if (configs.isNotEmpty()) {
                            val currentConfig = configs.find { it.id == chatVM.getSelectedConfigId() }
                                ?: configs.first()
                            Box {
                                TextButton(onClick = { modelDropdownExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(currentConfig.name,
                                        style = MaterialTheme.typography.labelSmall)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                                        modifier = Modifier.size(12.dp))
                                }
                                DropdownMenu(
                                    expanded = modelDropdownExpanded,
                                    onDismissRequest = { modelDropdownExpanded = false }
                                ) {
                                    configs.forEach { config ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(config.name,
                                                    style = MaterialTheme.typography.bodySmall)
                                            },
                                            onClick = {
                                                chatVM.setSelectedConfigId(config.id)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        Text("|", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(4.dp))

                        // Personality selector
                        var personalityExpanded by remember { mutableStateOf(false) }
                        val currentPersonality = chatVM.getPersonality()
                        Box {
                            TextButton(onClick = { personalityExpanded = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    currentPersonality?.personalityName ?: "Default",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                                    modifier = Modifier.size(12.dp))
                            }
                            DropdownMenu(
                                expanded = personalityExpanded,
                                onDismissRequest = { personalityExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Default", style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        chatVM.setPersonality(null)
                                        personalityExpanded = false
                                    }
                                )
                                val personalities by chatVM.personalitySkills.collectAsState()
                                personalities.forEach { skill ->
                                    DropdownMenuItem(
                                        text = { Text(skill.personalityName, style = MaterialTheme.typography.bodySmall) },
                                        onClick = {
                                            chatVM.setPersonality(skill)
                                            personalityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                MessageInput(
                    onSend = { text -> chatVM.sendMessage(text) },
                    enabled = true
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        content = msg.content,
                        isUser = msg.role == "user"
                    )
                }

                // Streaming text item
                if (streamingText.isNotEmpty()) {
                    item(key = "streaming") {
                        ChatBubble(content = streamingText, isUser = false)
                    }
                }

                // Loading indicator
                if (isLoading && streamingText.isEmpty()) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
