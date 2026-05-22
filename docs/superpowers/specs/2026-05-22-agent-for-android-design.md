# Agent For Android — Design Spec

**Date**: 2026-05-22  
**Status**: Approved  
**MVP Scope**: Chat + Multi-Model Config + Skill System

## 1. Overview

"Agent For Android" is an Android APK that brings AI Agent capabilities to mobile phones. It functions similarly to Claude Code: conversational AI with a pluggable Skill system that extends the Agent's expertise.

### MVP In-Scope
- Multi-model API configuration (model name, API key, base URL)
- Streaming chat with Markdown rendering
- Skill system (folder-per-skill, SKILL.md files with frontmatter)
- Built-in skills (bundled in APK assets) + user skills (filesDir)

### Out-of-Scope (Future Iterations)
- MCP protocol integration
- Phone operations (contacts, SMS, alarms, accessibility service)
- Multimodal input (camera, voice)

## 2. Tech Stack

| Category | Choice | Version |
|----------|--------|---------|
| Language | Kotlin | 1.9+ |
| UI Framework | Jetpack Compose + Material 3 | BOM 2024.06.00 |
| Navigation | Navigation Compose | 2.7.7 |
| Database | Room | 2.6.1 |
| HTTP Client | OkHttp (SSE streaming) | 4.12.0 |
| JSON | Moshi | 1.15.1 |
| Markdown Render | Markwon | 4.6.2 |
| Async | Kotlin Coroutines + Flow | 1.8.1 |
| Architecture | MVVM + Repository | — |

## 3. Architecture

```
┌─────────────────────────────────────────────┐
│  UI Layer (Compose)                         │
│  ChatScreen  SettingsScreen  SkillScreen    │
├─────────────────────────────────────────────┤
│  ViewModel Layer                            │
│  ChatVM    ConfigVM    SkillVM              │
├─────────────────────────────────────────────┤
│  Repository Layer                           │
│  ChatRepo  ConfigRepo  SkillRepo            │
├─────────────────────────────────────────────┤
│  Data Layer                                 │
│  LLM Client  Room DB  FileSystem (Skills)   │
└─────────────────────────────────────────────┘
```

## 4. Project Structure

```
AgentForAndroid/
├── app/
│   └── src/main/java/com/agentforandroid/
│       ├── AgentApp.kt
│       ├── MainActivity.kt
│       ├── ui/
│       │   ├── theme/
│       │   ├── screens/
│       │   │   ├── ChatScreen.kt
│       │   │   ├── SettingsScreen.kt
│       │   │   └── SkillManageScreen.kt
│       │   └── components/
│       │       ├── ChatBubble.kt
│       │       ├── MessageInput.kt
│       │       └── StreamingText.kt
│       ├── viewmodel/
│       │   ├── ChatViewModel.kt
│       │   ├── ConfigViewModel.kt
│       │   └── SkillViewModel.kt
│       ├── repository/
│       │   ├── ChatRepository.kt
│       │   ├── ConfigRepository.kt
│       │   └── SkillRepository.kt
│       ├── data/
│       │   ├── local/
│       │   │   ├── AppDatabase.kt
│       │   │   ├── dao/
│       │   │   │   ├── MessageDao.kt
│       │   │   │   ├── ConfigDao.kt
│       │   │   │   └── SessionDao.kt
│       │   │   └── entity/
│       │   │       ├── MessageEntity.kt
│       │   │       ├── ConfigEntity.kt
│       │   │       └── SessionEntity.kt
│       │   └── remote/
│       │       └── LLMClient.kt
│       ├── model/
│       │   ├── Message.kt
│       │   ├── ModelConfig.kt
│       │   ├── Skill.kt
│       │   └── ChatSession.kt
│       └── skill/
│           ├── SkillLoader.kt
│           └── SkillParser.kt
├── app/src/main/assets/skills/   (default to hello-agents)
│   └── hello-agents/
│       └── SKILL.md
└── resource/
    └── icon.jpg
```

## 5. Data Models

```kotlin
// Model configuration
data class ModelConfig(
    val id: String,           // UUID
    val name: String,         // Display name
    val modelId: String,      // Model ID: "gpt-4", "deepseek-chat", etc.
    val apiKey: String,       // API key
    val baseUrl: String,      // "https://api.openai.com/v1"
    val isDefault: Boolean
)

// Skill definition (parsed from SKILL.md)
data class Skill(
    val name: String,         // From frontmatter
    val description: String,  // From frontmatter
    val content: String,      // Markdown body (injected into system prompt)
    val sourcePath: String,   // Where the SKILL.md was loaded from
    val isBuiltin: Boolean,
    val enabled: Boolean
)

// Chat session
data class ChatSession(
    val id: String,
    val title: String,        // Auto-extracted from first message
    val modelConfigId: String,
    val enabledSkills: List<String>,
    val createdAt: Long
)

// Message
data class Message(
    val id: String,
    val sessionId: String,
    val role: String,         // "user" | "assistant" | "system"
    val content: String,
    val timestamp: Long
)
```

## 6. Core Flow: Chat Request → Streaming Response

```
User types message
    │
    ▼
ChatViewModel.sendMessage(text)
    │
    ├── Build system prompt
    │   = base_prompt + all enabled Skill.content concatenated
    │
    ├── Build messages array
    │   = [{"role":"system","content":systemPrompt}] + history + [{"role":"user","content":text}]
    │
    ├── Get current model config (from ConfigRepo)
    │
    └── LLMClient.streamChat(model, baseUrl, apiKey, messages)
        │
        ├── POST {baseUrl}/chat/completions (SSE, stream: true)
        ├── Parse SSE "data: " lines
        ├── Extract delta.content from each chunk
        ├── Emit via Kotlin Flow<String>
        └── On complete: save messages to Room
```

### System Prompt Construction

```
Base prompt: "You are Agent For Android, an AI assistant running on mobile..."
  + "\n\n## Active Skills\n"
  + Skill #1 (name + content from SKILL.md)
  + Skill #2 (name + content from SKILL.md)
  + ...
```

Skills are concatenated in alphabetical order by name. Disabled skills are skipped.

## 7. Skill System

### SKILL.md Format

```markdown
---
name: my-skill
description: A one-line summary of the skill
---

# Skill Content (markdown body)

This entire body is injected into the system prompt.
It can contain markdown, code blocks, tables, etc.
```

### Loading Mechanism

1. On app start, SkillLoader scans both `assets/skills/` and `filesDir/skills/`
2. Each directory is expected to contain one subdirectory per skill, with a `SKILL.md` inside
3. SkillParser extracts frontmatter via regex matching `---` delimiters
4. Skills are loaded into memory with `enabled` state persisted in SharedPreferences

### Skill Files Location

| Source | Path | Editable |
|--------|------|----------|
| Built-in | `assets/skills/<skill-name>/SKILL.md` | No (bundled in APK) |
| User | `context.filesDir/skills/<skill-name>/SKILL.md` | Yes (via USB file transfer) |

## 8. UI Layout

### Navigation: Bottom Nav (3 tabs)

| Tab | Screen | Icon |
|-----|--------|------|
| Chat | ChatScreen | 💬 |
| Skills | SkillManageScreen | 🧩 |
| Settings | SettingsScreen | ⚙️ |

### ChatScreen

- TopAppBar: session title + model selector dropdown
- LazyColumn: chat messages with markdown rendering
- Code blocks: syntax highlighting + copy button
- Streaming response: animated cursor + incremental text
- Bottom bar: message input + send button
- Model + active skills indicator card below TopAppBar

### SettingsScreen

- List of configured model APIs with default indicator
- Add button → BottomSheet with form fields (name, model ID, API key, base URL)
- Edit: tap item → BottomSheet pre-filled
- Delete: swipe or context menu
- Long press to set as default

### SkillManageScreen

- List of loaded skills with enable/disable switch
- Tap to preview SKILL.md content
- Display file path and size
- Copy path button for user skills directory
- Note explaining how to add user skills via USB

## 9. Error Handling

| Error Type | Handling |
|------------|----------|
| Network timeout (60s) | Auto-retry once, then show "请求超时，请检查网络" |
| HTTP 401 | "API Key 无效，请检查设置" |
| HTTP 404 | "模型 ID 不匹配，请检查配置" |
| HTTP 429 | "请求过于频繁，请稍后重试" |
| HTTP 5xx | "模型服务异常，请稍后重试" |
| Malformed SKILL.md | Skip the skill, notify user |
| Database error | Log, show Snackbar, no crash |
| App icon | Use `resource/icon.jpg` (150×150 JPEG) as app launcher icon |

All user-facing errors use Snackbar, not dialogs, to avoid blocking navigation.

## 10. Dependencies (build.gradle.kts)

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.06.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// OkHttp
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Moshi
implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

// Markdown
implementation("io.noties.markwon:core:4.6.2")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
```

## 11. Future Iterations (Post-MVP)

- **MCP Protocol**: Connect to MCP servers for extended tool capabilities
- **Phone Operations**: Read contacts, send SMS, set alarms via Android APIs + AccessibilityService
- **Multimodal**: Camera input, voice input/output
- **ToolAction System**: Enable Skills to define executable tool functions (like `hello_agents.tools.base.Tool`)
- **Conversation Export**: Export chat history as JSON/Markdown
- **Widget**: Home screen widget for quick chat
