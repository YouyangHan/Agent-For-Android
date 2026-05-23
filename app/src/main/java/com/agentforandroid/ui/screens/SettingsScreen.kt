package com.agentforandroid.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentforandroid.model.ModelConfig
import com.agentforandroid.viewmodel.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ConfigViewModel = viewModel()) {
    val configs by viewModel.configs.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ModelConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("模型设置") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("通用设置") })
            }

            if (selectedTab == 0) {
                // Model settings
                if (configs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("还没有配置模型", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("点击右下角 + 添加", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                } else {
                    LazyColumn {
                        items(configs, key = { it.id }) { config ->
                            ConfigItem(
                                config = config,
                                onClick = { editingConfig = config },
                                onDelete = { viewModel.delete(config) }
                            )
                        }
                    }
                }
            } else {
                // General settings
                LazyColumn {
                    // App name
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("名称", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                val context = LocalContext.current
                                var appName by remember {
                                    mutableStateOf(com.agentforandroid.ui.theme.AppPreferences.getAppName(context))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = appName,
                                        onValueChange = { appName = it },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        com.agentforandroid.ui.theme.AppPreferences.setAppName(context, appName.trim())
                                    }) { Text("保存") }
                                }
                            }
                        }
                    }

                    // Language & Theme
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("偏好设置", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Theme toggle (Light/Dark only)
                                val context = LocalContext.current
                                val savedTheme = com.agentforandroid.ui.theme.AppPreferences.getThemeMode(context)
                                var isDark by remember { mutableStateOf(savedTheme == com.agentforandroid.ui.theme.ThemeMode.DARK) }
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text("深色模式", modifier = Modifier.weight(1f))
                                    Switch(checked = isDark, onCheckedChange = { dark ->
                                        isDark = dark
                                        val mode = if (dark) com.agentforandroid.ui.theme.ThemeMode.DARK
                                            else com.agentforandroid.ui.theme.ThemeMode.LIGHT
                                        com.agentforandroid.ui.theme.AppPreferences.setThemeMode(context, mode)
                                        (context as? android.app.Activity)?.recreate()
                                    })
                                }
                            }
                        }
                    }

                    // Donate section
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("支持开发者", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("如果觉得好用，请我喝杯咖啡吧 ☕",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)

                                var showDonate by remember { mutableStateOf(false) }
                                var qrPage by remember { mutableStateOf(0) }

                                if (!showDonate) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = { showDonate = true }) {
                                        Text("❤️ 捐赠")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // QR code image - tap to switch between WeChat and Alipay
                                    val qrRes = if (qrPage == 0) com.agentforandroid.R.drawable.donate_wx
                                        else com.agentforandroid.R.drawable.donate_alipay
                                    val label = if (qrPage == 0) "微信" else "支付宝"

                                    Text(label, style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary)

                                    Image(
                                        painter = androidx.compose.ui.res.painterResource(qrRes),
                                        contentDescription = label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(280.dp)
                                            .clickable { qrPage = 1 - qrPage }  // tap to switch
                                    )

                                    Text("点击图片切换", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.fillMaxWidth())

                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { showDonate = false }) {
                                        Text("收起")
                                    }
                                }
                            }
                        }
                    }

                    // About section
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("关于", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Agent Yang",
                                    style = MaterialTheme.typography.titleMedium)
                                Text("版本: V1.0.0.0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("基于 Claude Code Skill 系统的 AI Agent",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("支持多模型、Skill 扩展、手机工具调用",
                                    style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Created by HanYouyang with AI Agent assistance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ConfigDialog(
            title = "添加模型",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, modelId, apiKey, baseUrl, apiType ->
                viewModel.add(name, modelId, apiKey, baseUrl, apiType)
                showAddDialog = false
            }
        )
    }

    editingConfig?.let { config ->
        ConfigDialog(
            title = "编辑模型",
            initial = config,
            onDismiss = { editingConfig = null },
            onConfirm = { name, modelId, apiKey, baseUrl, apiType ->
                viewModel.update(config.copy(name = name, modelId = modelId, apiKey = apiKey, baseUrl = baseUrl, apiType = apiType))
                editingConfig = null
            }
        )
    }
}

@Composable
private fun ConfigItem(
    config: ModelConfig,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(config.name, style = MaterialTheme.typography.titleSmall)
                Text(config.modelId, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
                Text(config.baseUrl, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
                Text(if (config.apiType == "anthropic") "Anthropic" else "OpenAI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigDialog(
    title: String,
    initial: ModelConfig? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, modelId: String, apiKey: String, baseUrl: String, apiType: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var modelId by remember { mutableStateOf(initial?.modelId ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiType by remember { mutableStateOf(initial?.apiType ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var apiTypeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("模型名称") }, singleLine = true)
                OutlinedTextField(value = modelId, onValueChange = { modelId = it },
                    label = { Text("模型 ID (可选)") }, singleLine = true)
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it },
                    label = { Text("API Key") }, maxLines = 3,
                    visualTransformation = if (showKey) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "隐藏" else "显示")
                        }
                    }
                )
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it },
                    label = { Text("Base URL") }, singleLine = true)

                // API type selector
                Box {
                    OutlinedTextField(
                        value = when (apiType) {
                            "anthropic" -> "Anthropic"
                            "openai" -> "OpenAI"
                            else -> "选择 API 类型..."
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("API 类型") },
                        trailingIcon = {
                            IconButton(onClick = { apiTypeExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = apiTypeExpanded,
                        onDismissRequest = { apiTypeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("OpenAI (Chat Completions)") },
                            onClick = { apiType = "openai"; apiTypeExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Anthropic (Messages)") },
                            onClick = { apiType = "anthropic"; apiTypeExpanded = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && apiKey.isNotBlank() && baseUrl.isNotBlank() && apiType.isNotBlank()) {
                    onConfirm(name, modelId, apiKey, baseUrl, apiType)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
