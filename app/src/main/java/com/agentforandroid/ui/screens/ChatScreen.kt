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

    var initialized by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var selectedConfigId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(configs) {
        if (!initialized && configs.isNotEmpty()) {
            chatVM.initOrCreateSession(
                modelConfigId = configs.firstOrNull { it.isDefault }?.id ?: configs.first().id,
                enabledSkills = skills.filter { it.enabled }.map { it.name }
            )
            initialized = true
        }
    }

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            chatVM.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent For Android") },
                actions = {
                    if (configs.isNotEmpty()) {
                        val currentConfig = configs.find { it.id == selectedConfigId }
                            ?: configs.firstOrNull { it.isDefault }
                            ?: configs.first()
                        Box {
                            TextButton(onClick = { modelDropdownExpanded = true }) {
                                Text(currentConfig.name,
                                    style = MaterialTheme.typography.labelMedium)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = modelDropdownExpanded,
                                onDismissRequest = { modelDropdownExpanded = false }
                            ) {
                                configs.forEach { config ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${config.name} ${if (config.isDefault) "✓" else ""}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = {
                                            selectedConfigId = config.id
                                            modelDropdownExpanded = false
                                            // Switch to this model for new conversations
                                            configVM.setDefault(config.id)
                                            initialized = false // trigger re-init
                                        },
                                        leadingIcon = {
                                            if (config.id == currentConfig.id) {
                                                Icon(Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            MessageInput(
                onSend = { text -> chatVM.sendMessage(text) },
                enabled = !isLoading
            )
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
