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
import androidx.compose.ui.platform.LocalContext
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

    // Init once — didInit in ViewModel survives tab switches
    LaunchedEffect(configs, skills) {
        if (!chatVM.didInit && configs.isNotEmpty() && skills.isNotEmpty()) {
            val firstId = configs.first().id
            chatVM.setSelectedConfigId(firstId)
            chatVM.initOrCreateSession(
                modelConfigId = firstId,
                enabledSkills = skills.filter { it.enabled }.map { it.name }
            )
            chatVM.refreshPersonalities()
            chatVM.restorePersonality()
            chatVM.didInit = true
        }
    }

    // Keep personality list updated as skills change
    LaunchedEffect(skills) {
        if (chatVM.didInit) chatVM.refreshPersonalities()
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

    // Show personality switch confirmation
    val switchMsg by chatVM.personaSwitchMessage.collectAsState()
    LaunchedEffect(switchMsg) {
        switchMsg?.let {
            snackbarHostState.showSnackbar(it)
            chatVM.clearSwitchMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val ctx = LocalContext.current
                    val name = com.agentforandroid.ui.theme.AppPreferences.getAppName(ctx)
                    Text(name)
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Scroll to bottom
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) listState.scrollToItem(messages.size)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("↓", style = MaterialTheme.typography.titleSmall)
                }
                // Clear context
                FloatingActionButton(
                    onClick = {
                        chatVM.clearSession()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("上下文已清空")
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text("✕", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
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
                        val currentPersonality by chatVM.selectedPersonality.collectAsState()
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
