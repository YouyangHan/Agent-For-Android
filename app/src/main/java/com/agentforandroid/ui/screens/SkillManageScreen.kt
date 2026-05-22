package com.agentforandroid.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillManageScreen(viewModel: SkillViewModel = viewModel()) {
    val skills by viewModel.skills.collectAsState()
    val context = LocalContext.current
    val repo = remember { SkillRepository.getInstance(context) }
    var selectedTab by remember { mutableStateOf(0) }
    var previewSkill by remember { mutableStateOf<Skill?>(null) }
    var showPathDialog by remember { mutableStateOf(false) }
    var showPersonalityDialog by remember { mutableStateOf<Skill?>(null) }

    val builtinSkills = skills.filter { it.isBuiltin }
    val userSkills = skills.filter { !it.isBuiltin }

    // Folder picker for skill import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                copySkillFromUri(context, uri, repo.getUserSkillsPath())
                repo.reloadSkills()
                Toast.makeText(context, "Skill 导入成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("内置 (${builtinSkills.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("用户 (${userSkills.size})") })
            }

            if (selectedTab == 0) {
                if (builtinSkills.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("无内置 Skills", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn {
                        items(builtinSkills, key = { it.name }) { skill ->
                            SkillCard(
                                skill = skill, viewModel = viewModel,
                                onClick = { previewSkill = skill },
                                onPersonality = { showPersonalityDialog = skill }
                            )
                        }
                    }
                }
            } else {
                LazyColumn {
                    // Import button
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("导入 Skill", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                val path = repo.getUserSkillsPath()
                                Text("存放: $path", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        importLauncher.launch(null)
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("选择文件夹")
                                    }
                                    OutlinedButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("path", path))
                                        Toast.makeText(context, "路径已复制", Toast.LENGTH_SHORT).show()
                                    }) { Text("复制路径") }
                                }
                            }
                        }
                    }

                    if (userSkills.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center) {
                                Text("暂无用户 Skills\n点击上方按钮导入",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    } else {
                        items(userSkills, key = { it.name }) { skill ->
                            SkillCard(
                                skill = skill, viewModel = viewModel,
                                onClick = { previewSkill = skill },
                                onPersonality = { showPersonalityDialog = skill }
                            )
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
                showPathDialog = false
            }
        )
    }

    // Personality dialog
    showPersonalityDialog?.let { skill ->
        PersonalityDialog(
            skill = skill,
            onDismiss = { showPersonalityDialog = null },
            onConfirm = { isPersonality, name ->
                viewModel.togglePersonality(skill.name, isPersonality, name)
                showPersonalityDialog = null
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
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("来源: ${skill.sourcePath}", style = MaterialTheme.typography.labelSmall)
                    if (skill.isPersonality) {
                        Text("性格: ${skill.personalityName}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(skill.content.take(500), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { previewSkill = null }) { Text("关闭") } }
        )
    }
}

@Composable
private fun SkillCard(
    skill: Skill,
    viewModel: SkillViewModel,
    onClick: () -> Unit,
    onPersonality: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(if (skill.isBuiltin) skill.displayName else skill.name,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(if (skill.isBuiltin) skill.displayDescription else skill.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                if (skill.isPersonality) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("性格: ${skill.personalityName}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Switch(
                checked = skill.enabled,
                onCheckedChange = { enabled -> viewModel.toggleSkill(skill.name, enabled) }
            )
        }
        // Personality button row (only for enabled skills)
        if (skill.enabled) {
            TextButton(onClick = onPersonality, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (skill.isPersonality) "性格: ${skill.personalityName} (点击修改)"
                    else "设为性格 Skill",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun PersonalityDialog(
    skill: Skill,
    onDismiss: () -> Unit,
    onConfirm: (isPersonality: Boolean, name: String) -> Unit
) {
    var isPersonality by remember { mutableStateOf(skill.isPersonality) }
    var name by remember { mutableStateOf(skill.personalityName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("性格设置 — ${skill.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("作为性格 Skill", modifier = Modifier.weight(1f))
                    Switch(checked = isPersonality, onCheckedChange = { isPersonality = it })
                }
                if (isPersonality) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("性格备注名") },
                        placeholder = { Text("如: 代码专家、创意作家...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("性格 Skill 会在对话中选择启用，其内容优先注入到 system prompt 中",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(isPersonality, name.trim()) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun PathEditDialog(currentPath: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var path by remember { mutableStateOf(currentPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skills 存储路径") },
        text = {
            Column {
                Text("存放导入 Skills 的目录。默认: 内部存储/agent_skills/",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = path, onValueChange = { path = it },
                    label = { Text("路径") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(path.trim()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun copySkillFromUri(context: Context, uri: Uri, targetDirPath: String) {
    val targetDir = File(targetDirPath)
    if (!targetDir.exists()) targetDir.mkdirs()

    // Get folder name from URI
    val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
        uri, android.provider.DocumentsContract.getTreeDocumentId(uri)
    )
    val cursor = context.contentResolver.query(docUri, null, null, null, null)
    var folderName = "imported_skill"
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            val displayName = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME))
            if (displayName != null) folderName = displayName
        }
        cursor.close()
    }

    val skillDestDir = File(targetDir, folderName)
    skillDestDir.mkdirs()

    // Copy files from picked folder to target
    val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
        uri, android.provider.DocumentsContract.getTreeDocumentId(uri)
    )
    val childrenCursor = context.contentResolver.query(childrenUri, null, null, null, null)
    if (childrenCursor != null) {
        while (childrenCursor.moveToNext()) {
            val childName = childrenCursor.getString(
                childrenCursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME))
            val docIdIdx = childrenCursor.getColumnIndex("document_id")
            val childId = if (docIdIdx >= 0) childrenCursor.getString(docIdIdx) else null
            if (childName != null && childId != null && childName.endsWith(".md")) {
                val childUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, childId)
                try {
                    context.contentResolver.openInputStream(childUri)?.use { input ->
                        File(skillDestDir, childName).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (_: Exception) { /* skip failed files */ }
            }
        }
        childrenCursor.close()
    }
}
