package com.agentforandroid.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
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
    var showPersonalityDialog by remember { mutableStateOf<Skill?>(null) }

    val builtinSkills = skills.filter { it.isBuiltin && !it.isPersonality }
    val personalitySkills = skills.filter { it.isPersonality && it.enabled }
    val userSkills = skills.filter { !it.isBuiltin && !(it.isPersonality && it.enabled) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val targetDir = repo.getUserSkillsPath()
            // Ensure target directory exists
            val dir = File(targetDir)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) {
                    Toast.makeText(context, "无法创建目录: $targetDir", Toast.LENGTH_LONG).show()
                    return@rememberLauncherForActivityResult
                }
            }

            try {
                val result = copySkillFromUri(context, uri, targetDir)
                if (result != null) {
                    repo.reloadSkills()
                    Toast.makeText(context, "Skill '$result' 导入成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "导入失败: 未找到 SKILL.md 文件", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SkillImport", "Import failed", e)
                Toast.makeText(context, "导入失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Skill 管理") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("内置 (${builtinSkills.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("性格 (${personalitySkills.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("用户 (${userSkills.size})") })
            }

            when (selectedTab) {
                0 -> {
                    // Built-in: just view + toggle, no personality button
                    if (builtinSkills.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("无内置 Skills", color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        LazyColumn {
                            items(builtinSkills, key = { it.name }) { skill ->
                                BuiltinSkillCard(
                                    skill = skill, viewModel = viewModel,
                                    onClick = { previewSkill = skill }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Personality: show personality skills with remove button
                    if (personalitySkills.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("暂无性格 Skills", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("在\"用户\"选项卡中，点击 ⭐ 将 Skill 设为性格",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    } else {
                        LazyColumn {
                            items(personalitySkills, key = { it.name }) { skill ->
                                PersonalitySkillCard(
                                    skill = skill, viewModel = viewModel,
                                    onClick = { previewSkill = skill },
                                    onRemovePersonality = {
                                        viewModel.togglePersonality(skill.name, false, "")
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // User: import button + user skills with promote-to-personality button
                    LazyColumn {
                        item {
                            ImportCard(context, repo) { importLauncher.launch(null) }
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
                                UserSkillCard(
                                    skill = skill, viewModel = viewModel,
                                    onClick = { previewSkill = skill },
                                    onPromote = { showPersonalityDialog = skill }
                                )
                            }
                        }
                    }
                }
            }
        }
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

// Built-in: name + description + toggle only, NO personality button
@Composable
private fun BuiltinSkillCard(skill: Skill, viewModel: SkillViewModel, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(skill.displayDescription, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Switch(checked = skill.enabled, onCheckedChange = { viewModel.toggleSkill(skill.name, it) })
        }
    }
}

// Personality: show name + personality name + toggle + remove-star button
@Composable
private fun PersonalitySkillCard(
    skill: Skill, viewModel: SkillViewModel, onClick: () -> Unit, onRemovePersonality: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() }) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(skill.personalityName.ifBlank { skill.displayName },
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            // Only show remove button for non-pinned personalities
            if (!com.agentforandroid.skill.SkillParser.pinnedPersonalities.contains(skill.name)) {
                TextButton(onClick = onRemovePersonality, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("取消性格", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// User: show name + desc + toggle + promote-star button
@Composable
private fun UserSkillCard(
    skill: Skill, viewModel: SkillViewModel, onClick: () -> Unit, onPromote: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() }) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(skill.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (skill.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(skill.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
                Switch(checked = skill.enabled, onCheckedChange = { viewModel.toggleSkill(skill.name, it) })
            }
            if (skill.enabled) {
                TextButton(onClick = onPromote, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("设为性格", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ImportCard(context: Context, repo: SkillRepository, onImport: () -> Unit) {
    val path = repo.getUserSkillsPath()
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("导入 Skill", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("存放: $path", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onImport) {
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

@Composable
private fun PersonalityDialog(
    skill: Skill, onDismiss: () -> Unit,
    onConfirm: (isPersonality: Boolean, name: String) -> Unit
) {
    var isPersonality by remember { mutableStateOf(skill.isPersonality) }
    var name by remember { mutableStateOf(skill.personalityName) }

    // Default to ON for user skills being promoted
    LaunchedEffect(Unit) {
        if (!skill.isPersonality) {
            isPersonality = true
        }
    }

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
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("性格 Skill 会在对话中选择启用，其内容优先注入到 system prompt 中",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(isPersonality, name.trim()) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** Copy a skill folder from SAF URI to target, returns the folder name if successful */
private fun copySkillFromUri(context: Context, uri: Uri, targetDirPath: String): String? {
    val targetDir = File(targetDirPath)
    if (!targetDir.exists()) {
        val created = targetDir.mkdirs()
        if (!created) return null
    }

    // Get folder display name
    val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
        uri, android.provider.DocumentsContract.getTreeDocumentId(uri))
    var folderName: String? = null
    context.contentResolver.query(docUri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) folderName = cursor.getString(idx)
        }
    }
    if (folderName == null) folderName = "imported_skill_${System.currentTimeMillis()}"
    val skillDestDir = File(targetDir, folderName!!)
    if (!skillDestDir.exists()) skillDestDir.mkdirs()

    // Copy all files from the picked folder
    var hasSkilMd = false
    val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
        uri, android.provider.DocumentsContract.getTreeDocumentId(uri))
    context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        val idIdx = cursor.getColumnIndex("document_id")
        while (cursor.moveToNext()) {
            val childName = if (nameIdx >= 0) cursor.getString(nameIdx) else null ?: continue
            val childId = if (idIdx >= 0) cursor.getString(idIdx) else null ?: continue
            val childUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, childId)
            try {
                context.contentResolver.openInputStream(childUri)?.use { input ->
                    val destFile = File(skillDestDir, childName)
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                if (childName == "SKILL.md") hasSkilMd = true
            } catch (e: Exception) {
                Log.w("SkillImport", "Failed to copy $childName: ${e.message}")
            }
        }
    }

    return if (hasSkilMd) folderName else null
}
