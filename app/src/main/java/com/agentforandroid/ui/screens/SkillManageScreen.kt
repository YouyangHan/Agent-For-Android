package com.agentforandroid.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentforandroid.model.Skill
import com.agentforandroid.repository.SkillRepository
import com.agentforandroid.viewmodel.SkillViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillManageScreen(viewModel: SkillViewModel = viewModel()) {
    val skills by viewModel.skills.collectAsState()
    val context = LocalContext.current
    val repo = remember { SkillRepository.getInstance(context) }
    var selectedTab by remember { mutableStateOf(0) }
    var previewSkill by remember { mutableStateOf<Skill?>(null) }
    var showPathDialog by remember { mutableStateOf(false) }

    val builtinSkills = skills.filter { it.isBuiltin }
    val userSkills = skills.filter { !it.isBuiltin }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skill 管理") },
                actions = {
                    IconButton(onClick = { showPathDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "路径")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPathDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "路径设置")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("内置 (${builtinSkills.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("用户 (${userSkills.size})") }
                )
            }

            if (selectedTab == 0) {
                // Built-in skills tab
                if (builtinSkills.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("无内置 Skills", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn {
                        items(builtinSkills, key = { it.name }) { skill ->
                            SkillCard(skill = skill, viewModel = viewModel) { previewSkill = skill }
                        }
                    }
                }
            } else {
                // User skills tab
                if (userSkills.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("暂无用户 Skills", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            val path = repo.getUserSkillsPath()
                            Text(path, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("在此目录下创建文件夹，放入 SKILL.md",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                } else {
                    LazyColumn {
                        items(userSkills, key = { it.name }) { skill ->
                            SkillCard(skill = skill, viewModel = viewModel) { previewSkill = skill }
                        }
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("导入 Skills", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val path = repo.getUserSkillsPath()
                                    Text("路径: $path",
                                        style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("在此目录下创建文件夹结构:\n  agent_skills/\n    my-skill/\n      SKILL.md",
                                        style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("path", path))
                                        Toast.makeText(context, "路径已复制", Toast.LENGTH_SHORT).show()
                                    }) { Text("复制路径") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Path edit dialog
    if (showPathDialog) {
        PathEditDialog(
            currentPath = repo.getUserSkillsPath(),
            onDismiss = { showPathDialog = false },
            onConfirm = { newPath ->
                repo.setUserSkillsPath(newPath)
                viewModel.reloadSkills()
                showPathDialog = false
            }
        )
    }

    // Preview dialog
    previewSkill?.let { skill ->
        AlertDialog(
            onDismissRequest = { previewSkill = null },
            title = { Text(if (skill.isBuiltin) skill.displayName else skill.name) },
            text = {
                Column {
                    Text(if (skill.isBuiltin) skill.displayDescription else skill.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("来源: ${skill.sourcePath}",
                        style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(skill.content.take(500),
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { previewSkill = null }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun SkillCard(
    skill: Skill,
    viewModel: SkillViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (skill.isBuiltin) skill.displayName else skill.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (skill.isBuiltin) skill.displayDescription else skill.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = skill.enabled,
                onCheckedChange = { enabled ->
                    viewModel.toggleSkill(skill.name, enabled)
                }
            )
        }
    }
}

@Composable
private fun PathEditDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var path by remember { mutableStateOf(currentPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skills 存储路径") },
        text = {
            Column {
                Text("用户自定义 Skills 的存放目录。\n默认: 内部存储/agent_skills/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("路径") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(path.trim()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
