package com.agentforandroid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ModelConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("模型配置") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        if (configs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有配置模型", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击右下角 + 添加", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(configs, key = { it.id }) { config ->
                    ConfigItem(
                        config = config,
                        onClick = { editingConfig = config },
                        onDelete = { viewModel.delete(config) },
                        onSetDefault = { viewModel.setDefault(config.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ConfigDialog(
            title = "添加模型",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, modelId, apiKey, baseUrl ->
                viewModel.add(name, modelId, apiKey, baseUrl)
                showAddDialog = false
            }
        )
    }

    editingConfig?.let { config ->
        ConfigDialog(
            title = "编辑模型",
            initial = config,
            onDismiss = { editingConfig = null },
            onConfirm = { name, modelId, apiKey, baseUrl ->
                viewModel.update(config.copy(name = name, modelId = modelId, apiKey = apiKey, baseUrl = baseUrl))
                editingConfig = null
            }
        )
    }
}

@Composable
private fun ConfigItem(
    config: ModelConfig,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.isDefault) {
                Icon(Icons.Default.Check, contentDescription = "默认",
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(config.name, style = MaterialTheme.typography.titleSmall)
                Text(config.modelId, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
                Text(config.baseUrl, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
            }
            TextButton(onClick = onSetDefault) { Text("设为默认") }
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
    onConfirm: (name: String, modelId: String, apiKey: String, baseUrl: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var modelId by remember { mutableStateOf(initial?.modelId ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "https://api.openai.com/v1") }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("模型名称") }, singleLine = true)
                OutlinedTextField(value = modelId, onValueChange = { modelId = it },
                    label = { Text("模型 ID") }, singleLine = true,
                    placeholder = { Text("gpt-4 / deepseek-chat") })
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it },
                    label = { Text("API Key") }, singleLine = true,
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && modelId.isNotBlank() && apiKey.isNotBlank() && baseUrl.isNotBlank()) {
                    onConfirm(name, modelId, apiKey, baseUrl)
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
