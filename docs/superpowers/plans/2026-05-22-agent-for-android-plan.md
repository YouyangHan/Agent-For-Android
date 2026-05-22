# Agent For Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android APK "Agent For Android" — an AI agent with streaming chat, multi-model API config, and pluggable Skill system using Kotlin + Jetpack Compose + MVVM.

**Architecture:** Single-Activity Compose app with BottomNavigation (Chat/Skills/Settings). MVVM with Repository pattern. Room for persistence, OkHttp SSE for streaming LLM calls. Skills are SKILL.md folders loaded from assets and filesDir.

**Tech Stack:** Kotlin 2.0, Compose BOM 2024.06, Material 3, Room 2.6.1, OkHttp 4.12, Moshi 1.15, Markwon 4.6

---

## File Map

```
AgentForAndroid/
├── build.gradle.kts                    (root) — plugins declaration
├── settings.gradle.kts                 — module setup
├── gradle.properties                   — JVM/Android props
├── gradle/wrapper/                     — gradle-wrapper.jar + properties
├── gradlew / gradlew.bat              — wrapper scripts
├── app/
│   ├── build.gradle.kts                — dependencies, compose config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/skills/
│       │   └── hello-agents/SKILL.md   — built-in skill
│       ├── res/
│       │   ├── values/strings.xml
│       │   ├── values/themes.xml
│       │   ├── mipmap-*/ic_launcher.webp  (converted from icon.jpg)
│       │   └── drawable/ic_chat.xml, ic_skills.xml, ic_settings.xml
│       └── java/com/agentforandroid/
│           ├── AgentApp.kt             — Application class
│           ├── MainActivity.kt         — single Activity + NavHost
│           ├── ui/theme/
│           │   └── Theme.kt            — Material3 theme
│           ├── ui/screens/
│           │   ├── ChatScreen.kt       — main chat UI
│           │   ├── SettingsScreen.kt   — model config
│           │   └── SkillManageScreen.kt — skill management
│           ├── ui/components/
│           │   ├── ChatBubble.kt       — message bubble + markdown
│           │   ├── MessageInput.kt     — text input bar
│           │   └── StreamingText.kt    — animated streaming text
│           ├── viewmodel/
│           │   ├── ChatViewModel.kt    — chat state + streaming
│           │   ├── ConfigViewModel.kt  — model CRUD
│           │   └── SkillViewModel.kt   — skill load + toggle
│           ├── repository/
│           │   ├── ChatRepository.kt   — message persistence + LLM call
│           │   ├── ConfigRepository.kt — model config persistence
│           │   └── SkillRepository.kt  — skill scan + parse
│           ├── data/local/
│           │   ├── AppDatabase.kt      — Room DB
│           │   ├── dao/
│           │   │   ├── MessageDao.kt
│           │   │   ├── ConfigDao.kt
│           │   │   └── SessionDao.kt
│           │   └── entity/
│           │       ├── MessageEntity.kt
│           │       ├── ConfigEntity.kt
│           │       └── SessionEntity.kt
│           ├── data/remote/
│           │   └── LLMClient.kt        — OkHttp SSE streaming
│           ├── model/
│           │   ├── Message.kt
│           │   ├── ModelConfig.kt
│           │   ├── Skill.kt
│           │   └── ChatSession.kt
│           └── skill/
│               ├── SkillLoader.kt      — filesystem scan
│               └── SkillParser.kt      — frontmatter + body parse
```

---

### Task 1: Gradle Project Scaffold

**Files:**
- Create: `build.gradle.kts` (root)
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`

- [ ] **Step 1: Create root build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.22" apply false
}
```

- [ ] **Step 2: Create settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "AgentForAndroid"
include(":app")
```

- [ ] **Step 3: Create gradle.properties**

```properties
org.gradle.jvm.args=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 4: Create app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.agentforandroid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.agentforandroid"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures { compose = true }

    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

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

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 5: Setup Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.5` (from project root using an existing Gradle installation or download wrapper files manually)

- [ ] **Step 6: Verify project structure**

Run: `./gradlew projects`
Expected: lists `:app` as subproject

---

### Task 2: AndroidManifest + Application + Resources

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/com/agentforandroid/AgentApp.kt`

- [ ] **Step 1: Create AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".AgentApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AgentForAndroid">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 2: Create strings.xml**

```xml
<resources>
    <string name="app_name">Agent For Android</string>
    <string name="tab_chat">对话</string>
    <string name="tab_skills">Skills</string>
    <string name="tab_settings">设置</string>
</resources>
```

- [ ] **Step 3: Create themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.AgentForAndroid" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 4: Create AgentApp.kt**

```kotlin
package com.agentforandroid

import android.app.Application
import com.agentforandroid.data.local.AppDatabase

class AgentApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}
```

- [ ] **Step 5: Convert icon.jpg to Android launcher icons**

Run ImageMagick to generate required densities (or use Android Studio Image Asset tool):
```bash
# If ImageMagick is available:
convert resource/icon.jpg -resize 48x48   app/src/main/res/mipmap-mdpi/ic_launcher.webp
convert resource/icon.jpg -resize 72x72   app/src/main/res/mipmap-hdpi/ic_launcher.webp
convert resource/icon.jpg -resize 96x96   app/src/main/res/mipmap-xhdpi/ic_launcher.webp
convert resource/icon.jpg -resize 144x144 app/src/main/res/mipmap-xxhdpi/ic_launcher.webp
convert resource/icon.jpg -resize 192x192 app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp
```

If ImageMagick not available, create the directories and place a resized copy — the icon will be refined later via Android Studio.

---

### Task 3: Domain Models

**Files:**
- Create: `app/src/main/java/com/agentforandroid/model/ModelConfig.kt`
- Create: `app/src/main/java/com/agentforandroid/model/Skill.kt`
- Create: `app/src/main/java/com/agentforandroid/model/ChatSession.kt`
- Create: `app/src/main/java/com/agentforandroid/model/Message.kt`

- [ ] **Step 1: Create ModelConfig.kt**

```kotlin
package com.agentforandroid.model

data class ModelConfig(
    val id: String,
    val name: String,
    val modelId: String,
    val apiKey: String,
    val baseUrl: String,
    val isDefault: Boolean = false
)
```

- [ ] **Step 2: Create Skill.kt**

```kotlin
package com.agentforandroid.model

data class Skill(
    val name: String,
    val description: String,
    val content: String,
    val sourcePath: String,
    val isBuiltin: Boolean,
    val enabled: Boolean = true
)
```

- [ ] **Step 3: Create ChatSession.kt**

```kotlin
package com.agentforandroid.model

data class ChatSession(
    val id: String,
    val title: String,
    val modelConfigId: String,
    val enabledSkills: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: Create Message.kt**

```kotlin
package com.agentforandroid.model

data class Message(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

### Task 4: Room Entities + DAOs + Database

**Files:**
- Create: `app/src/main/java/com/agentforandroid/data/local/entity/ConfigEntity.kt`
- Create: `app/src/main/java/com/agentforandroid/data/local/entity/SessionEntity.kt`
- Create: `app/src/main/java/com/agentforandroid/data/local/entity/MessageEntity.kt`
- Create: `app/src/main/java/com/agentforandroid/data/local/dao/ConfigDao.kt`
- Create: `app/src/main/java/com/agentforandroid/data/local/dao/SessionDao.kt`
- Create: `app/src/main/java/com/agentforandroid/data/local/dao/MessageDao.kt`
- Create: `app/src/main/java/com/agentforandroid/data/local/AppDatabase.kt`

- [ ] **Step 1: Create ConfigEntity.kt**

```kotlin
package com.agentforandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agentforandroid.model.ModelConfig

@Entity(tableName = "model_configs")
data class ConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val modelId: String,
    val apiKey: String,
    val baseUrl: String,
    val isDefault: Boolean = false
) {
    fun toModel(): ModelConfig = ModelConfig(
        id = id,
        name = name,
        modelId = modelId,
        apiKey = apiKey,
        baseUrl = baseUrl,
        isDefault = isDefault
    )

    companion object {
        fun fromModel(config: ModelConfig): ConfigEntity = ConfigEntity(
            id = config.id,
            name = config.name,
            modelId = config.modelId,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl,
            isDefault = config.isDefault
        )
    }
}
```

- [ ] **Step 2: Create SessionEntity.kt**

```kotlin
package com.agentforandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agentforandroid.model.ChatSession

@Entity(tableName = "chat_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val modelConfigId: String,
    val enabledSkills: String = "",  // comma-separated skill names
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toModel(): ChatSession = ChatSession(
        id = id,
        title = title,
        modelConfigId = modelConfigId,
        enabledSkills = if (enabledSkills.isBlank()) emptyList() else enabledSkills.split(","),
        createdAt = createdAt
    )

    companion object {
        fun fromModel(session: ChatSession): SessionEntity = SessionEntity(
            id = session.id,
            title = session.title,
            modelConfigId = session.modelConfigId,
            enabledSkills = session.enabledSkills.joinToString(","),
            createdAt = session.createdAt
        )
    }
}
```

- [ ] **Step 3: Create MessageEntity.kt**

```kotlin
package com.agentforandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agentforandroid.model.Message

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toModel(): Message = Message(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        timestamp = timestamp
    )

    companion object {
        fun fromModel(message: Message): MessageEntity = MessageEntity(
            id = message.id,
            sessionId = message.sessionId,
            role = message.role,
            content = message.content,
            timestamp = message.timestamp
        )
    }
}
```

- [ ] **Step 4: Create ConfigDao.kt**

```kotlin
package com.agentforandroid.data.local.dao

import androidx.room.*
import com.agentforandroid.data.local.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM model_configs ORDER BY isDefault DESC")
    fun getAll(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ConfigEntity?

    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getById(id: String): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ConfigEntity)

    @Update
    suspend fun update(config: ConfigEntity)

    @Delete
    suspend fun delete(config: ConfigEntity)

    @Query("UPDATE model_configs SET isDefault = 0")
    suspend fun clearDefaults()

    @Query("UPDATE model_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)
}
```

- [ ] **Step 5: Create SessionDao.kt**

```kotlin
package com.agentforandroid.data.local.dao

import androidx.room.*
import com.agentforandroid.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)
}
```

- [ ] **Step 6: Create MessageDao.kt**

```kotlin
package com.agentforandroid.data.local.dao

import androidx.room.*
import com.agentforandroid.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySession(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getBySessionPaged(sessionId: String, limit: Int, offset: Int): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
```

- [ ] **Step 7: Create AppDatabase.kt**

```kotlin
package com.agentforandroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.agentforandroid.data.local.dao.ConfigDao
import com.agentforandroid.data.local.dao.MessageDao
import com.agentforandroid.data.local.dao.SessionDao
import com.agentforandroid.data.local.entity.ConfigEntity
import com.agentforandroid.data.local.entity.MessageEntity
import com.agentforandroid.data.local.entity.SessionEntity

@Database(
    entities = [ConfigEntity::class, SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agent_for_android.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

---

### Task 5: Skill System (Parser + Loader + Repository)

**Files:**
- Create: `app/src/main/java/com/agentforandroid/skill/SkillParser.kt`
- Create: `app/src/main/java/com/agentforandroid/skill/SkillLoader.kt`
- Create: `app/src/main/java/com/agentforandroid/repository/SkillRepository.kt`

- [ ] **Step 1: Create SkillParser.kt**

```kotlin
package com.agentforandroid.skill

import com.agentforandroid.model.Skill

object SkillParser {

    private val frontmatterRegex = Regex("---\\s*\\n([\\s\\S]*?)\\n---\\s*\\n?([\\s\\S]*)")

    fun parse(markdown: String, sourcePath: String, isBuiltin: Boolean): Skill? {
        val match = frontmatterRegex.find(markdown) ?: return null

        val frontmatter = match.groupValues[1]
        val content = match.groupValues[2].trim()

        val name = extractField(frontmatter, "name") ?: return null
        val description = extractField(frontmatter, "description") ?: ""

        return Skill(
            name = name,
            description = description,
            content = content,
            sourcePath = sourcePath,
            isBuiltin = isBuiltin
        )
    }

    private fun extractField(frontmatter: String, key: String): String? {
        val regex = Regex("^$key\\s*:\\s*(.+)$", RegexOption.MULTILINE)
        return regex.find(frontmatter)?.groupValues?.get(1)?.trim()
    }
}
```

- [ ] **Step 2: Create SkillLoader.kt**

```kotlin
package com.agentforandroid.skill

import android.content.Context
import com.agentforandroid.model.Skill
import java.io.File

object SkillLoader {

    fun loadAll(context: Context): List<Skill> {
        val skills = mutableListOf<Skill>()

        // Load built-in skills from assets
        skills.addAll(loadFromAssets(context))

        // Load user skills from filesDir
        skills.addAll(loadFromFiles(context))

        return skills.sortedBy { it.name }
    }

    private fun loadFromAssets(context: Context): List<Skill> {
        val skills = mutableListOf<Skill>()
        try {
            val skillsDir = "skills"
            val skillFolders = context.assets.list(skillsDir) ?: return skills

            for (folder in skillFolders) {
                val skillFile = "$skillsDir/$folder/SKILL.md"
                try {
                    val content = context.assets.open(skillFile).bufferedReader().readText()
                    val skill = SkillParser.parse(
                        markdown = content,
                        sourcePath = "assets/skills/$folder/",
                        isBuiltin = true
                    )
                    if (skill != null) skills.add(skill)
                } catch (_: Exception) { /* Skip malformed skill */ }
            }
        } catch (_: Exception) { /* assets/skills/ not found */ }
        return skills
    }

    private fun loadFromFiles(context: Context): List<Skill> {
        val skills = mutableListOf<Skill>()
        val skillsDir = File(context.filesDir, "skills")
        if (!skillsDir.exists() || !skillsDir.isDirectory) return skills

        for (folder in skillsDir.listFiles() ?: emptyArray()) {
            if (!folder.isDirectory) continue
            val skillMd = File(folder, "SKILL.md")
            if (!skillMd.exists()) continue

            try {
                val content = skillMd.readText()
                val skill = SkillParser.parse(
                    markdown = content,
                    sourcePath = skillMd.absolutePath,
                    isBuiltin = false
                )
                if (skill != null) skills.add(skill)
            } catch (_: Exception) { /* Skip malformed skill */ }
        }
        return skills
    }
}
```

- [ ] **Step 3: Create SkillRepository.kt**

```kotlin
package com.agentforandroid.repository

import android.content.Context
import com.agentforandroid.model.Skill
import com.agentforandroid.skill.SkillLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SkillRepository(private val context: Context) {

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    private val prefs by lazy {
        context.getSharedPreferences("skill_prefs", Context.MODE_PRIVATE)
    }

    fun loadSkills() {
        val loaded = SkillLoader.loadAll(context)
        val enabledSet = prefs.getStringSet("enabled_skills", emptySet()) ?: emptySet()

        // First launch: enable all. After that, respect stored pref.
        val isFirstLaunch = prefs.getBoolean("first_launch", true)
        val skillsWithState = loaded.map { skill ->
            val enabled = if (isFirstLaunch) true else enabledSet.contains(skill.name)
            skill.copy(enabled = enabled)
        }
        _skills.value = skillsWithState

        if (isFirstLaunch) {
            prefs.edit().putBoolean("first_launch", false).apply()
        }
    }

    fun toggleSkill(skillName: String, enabled: Boolean) {
        _skills.value = _skills.value.map {
            if (it.name == skillName) it.copy(enabled = enabled) else it
        }
        val enabledNames = _skills.value.filter { it.enabled }.map { it.name }.toSet()
        prefs.edit().putStringSet("enabled_skills", enabledNames).apply()
    }

    fun getEnabledSkills(): List<Skill> = _skills.value.filter { it.enabled }
}
```

---

### Task 6: LLM Client (OkHttp SSE Streaming)

**Files:**
- Create: `app/src/main/java/com/agentforandroid/data/remote/LLMClient.kt`

- [ ] **Step 1: Create LLMClient.kt**

```kotlin
package com.agentforandroid.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LLMClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class LLMRequest(
        val model: String,
        val baseUrl: String,
        val apiKey: String,
        val messages: List<Map<String, String>>
    )

    sealed class LLMResult {
        data class Chunk(val text: String) : LLMResult()
        data class Error(val message: String) : LLMResult()
        data object Done : LLMResult()
    }

    fun streamChat(request: LLMRequest): Flow<LLMResult> = callbackFlow {
        val bodyJson = JSONObject().apply {
            put("model", request.model)
            put("messages", JSONArray(request.messages.map { msg ->
                JSONObject().apply {
                    put("role", msg["role"])
                    put("content", msg["content"])
                }
            }))
            put("stream", true)
        }

        val url = request.baseUrl.trimEnd('/') + "/chat/completions"
        val requestBody = bodyJson.toString()
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${request.apiKey}")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                trySend(LLMResult.Error(
                    when (response.code) {
                        401 -> "API Key 无效，请检查设置"
                        404 -> "模型 ID 不匹配，请检查配置"
                        429 -> "请求过于频繁，请稍后重试"
                        in 500..599 -> "模型服务异常，请稍后重试"
                        else -> "请求失败 (${response.code})"
                    }
                ))
                close()
                return@callbackFlow
            }

            response.body?.source()?.let { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val json = line.removePrefix("data: ").trim()
                        if (json == "[DONE]") continue
                        try {
                            val delta = JSONObject(json)
                                .optJSONArray("choices")
                                ?.optJSONObject(0)
                                ?.optJSONObject("delta")
                                ?.optString("content", "") ?: ""
                            if (delta.isNotEmpty()) {
                                trySend(LLMResult.Chunk(delta))
                            }
                        } catch (_: Exception) { /* skip bad chunks */ }
                    }
                }
            }
            trySend(LLMResult.Done)
        } catch (e: IOException) {
            trySend(LLMResult.Error("网络连接失败: ${e.localizedMessage}"))
        }
        close()
    }

    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
```

---

### Task 7: Repositories (ChatRepository + ConfigRepository)

**Files:**
- Create: `app/src/main/java/com/agentforandroid/repository/ConfigRepository.kt`
- Create: `app/src/main/java/com/agentforandroid/repository/ChatRepository.kt`

- [ ] **Step 1: Create ConfigRepository.kt**

```kotlin
package com.agentforandroid.repository

import com.agentforandroid.data.local.AppDatabase
import com.agentforandroid.data.local.entity.ConfigEntity
import com.agentforandroid.model.ModelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ConfigRepository(private val database: AppDatabase) {

    private val dao = database.configDao()

    fun getAll(): Flow<List<ModelConfig>> =
        dao.getAll().map { entities -> entities.map { it.toModel() } }

    suspend fun getDefault(): ModelConfig? = dao.getDefault()?.toModel()

    suspend fun getById(id: String): ModelConfig? = dao.getById(id)?.toModel()

    suspend fun add(
        name: String,
        modelId: String,
        apiKey: String,
        baseUrl: String
    ): ModelConfig {
        val config = ModelConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            modelId = modelId,
            apiKey = apiKey,
            baseUrl = baseUrl
        )
        dao.insert(ConfigEntity.fromModel(config))
        return config
    }

    suspend fun update(config: ModelConfig) {
        dao.update(ConfigEntity.fromModel(config))
    }

    suspend fun delete(config: ModelConfig) {
        dao.delete(ConfigEntity.fromModel(config))
    }

    suspend fun setDefault(id: String) {
        dao.clearDefaults()
        dao.setDefault(id)
    }
}
```

- [ ] **Step 2: Create ChatRepository.kt**

```kotlin
package com.agentforandroid.repository

import com.agentforandroid.data.local.AppDatabase
import com.agentforandroid.data.local.entity.MessageEntity
import com.agentforandroid.data.local.entity.SessionEntity
import com.agentforandroid.data.remote.LLMClient
import com.agentforandroid.model.ChatSession
import com.agentforandroid.model.Message
import com.agentforandroid.model.ModelConfig
import com.agentforandroid.model.Skill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(private val database: AppDatabase) {

    private val sessionDao = database.sessionDao()
    private val messageDao = database.messageDao()
    private val llmClient = LLMClient()

    fun getSessions(): Flow<List<ChatSession>> =
        sessionDao.getAll().map { entities -> entities.map { it.toModel() } }

    fun getMessages(sessionId: String): Flow<List<Message>> =
        messageDao.getBySession(sessionId).map { entities -> entities.map { it.toModel() } }

    suspend fun createSession(modelConfigId: String, enabledSkills: List<String>): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "新对话",
            modelConfigId = modelConfigId,
            enabledSkills = enabledSkills
        )
        sessionDao.insert(SessionEntity.fromModel(session))
        return session
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) {
        val entity = sessionDao.getById(sessionId) ?: return
        sessionDao.update(entity.copy(title = title))
    }

    suspend fun saveMessage(message: Message) {
        messageDao.insert(MessageEntity.fromModel(message))
    }

    fun buildSystemPrompt(basePrompt: String, skills: List<Skill>): String {
        if (skills.isEmpty()) return basePrompt
        val skillsSection = skills.joinToString("\n\n") { skill ->
            "[Skill: ${skill.name}]\n${skill.content}"
        }
        return "$basePrompt\n\n## Active Skills\n\n$skillsSection"
    }

    fun streamChat(
        config: ModelConfig,
        messages: List<Map<String, String>>
    ): Flow<LLMClient.LLMResult> {
        return llmClient.streamChat(
            LLMClient.LLMRequest(
                model = config.modelId,
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                messages = messages
            )
        )
    }

    fun shutdown() {
        llmClient.shutdown()
    }
}
```

---

### Task 8: ViewModels

**Files:**
- Create: `app/src/main/java/com/agentforandroid/viewmodel/SkillViewModel.kt`
- Create: `app/src/main/java/com/agentforandroid/viewmodel/ConfigViewModel.kt`
- Create: `app/src/main/java/com/agentforandroid/viewmodel/ChatViewModel.kt`

- [ ] **Step 1: Create SkillViewModel.kt**

```kotlin
package com.agentforandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentforandroid.model.Skill
import com.agentforandroid.repository.SkillRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SkillViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SkillRepository(application)

    val skills: StateFlow<List<Skill>> = repository.skills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repository.loadSkills()
    }

    fun toggleSkill(skillName: String, enabled: Boolean) {
        repository.toggleSkill(skillName, enabled)
    }

    fun getEnabledSkills(): List<Skill> = repository.getEnabledSkills()
}
```

- [ ] **Step 2: Create ConfigViewModel.kt**

```kotlin
package com.agentforandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentforandroid.AgentApp
import com.agentforandroid.model.ModelConfig
import com.agentforandroid.repository.ConfigRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository(
        (application as AgentApp).database
    )

    val configs: StateFlow<List<ModelConfig>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, modelId: String, apiKey: String, baseUrl: String) {
        viewModelScope.launch {
            val newConfig = repository.add(name, modelId, apiKey, baseUrl)
            // If this is the first config, set it as default
            if (configs.value.isEmpty()) {
                repository.setDefault(newConfig.id)
            }
        }
    }

    fun update(config: ModelConfig) {
        viewModelScope.launch { repository.update(config) }
    }

    fun delete(config: ModelConfig) {
        viewModelScope.launch { repository.delete(config) }
    }

    fun setDefault(id: String) {
        viewModelScope.launch { repository.setDefault(id) }
    }
}
```

- [ ] **Step 3: Create ChatViewModel.kt**

```kotlin
package com.agentforandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentforandroid.AgentApp
import com.agentforandroid.data.remote.LLMClient
import com.agentforandroid.model.ChatSession
import com.agentforandroid.model.Message
import com.agentforandroid.repository.ChatRepository
import com.agentforandroid.repository.ConfigRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AgentApp
    private val chatRepo = ChatRepository(app.database)
    private val configRepo = ConfigRepository(app.database)
    private val skillRepo = com.agentforandroid.repository.SkillRepository(application)

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentSession: ChatSession? = null

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    companion object {
        const val BASE_SYSTEM_PROMPT =
            "You are Agent For Android, an AI assistant running on a mobile phone. " +
            "You provide helpful, accurate responses. When Skills are active, " +
            "use their guidance to enhance your answers. " +
            "Use markdown formatting for code blocks, tables, and lists."
    }

    suspend fun initOrCreateSession(modelConfigId: String, enabledSkills: List<String>) {
        currentSession = chatRepo.createSession(modelConfigId, enabledSkills)
    }

    fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            chatRepo.getMessages(sessionId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val session = currentSession ?: return@launch
            _error.value = null
            _isLoading.value = true
            _streamingText.value = ""

            // Save user message
            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                role = "user",
                content = text
            )
            chatRepo.saveMessage(userMsg)
            _messages.value = _messages.value + userMsg

            // Auto-title: use first user message
            if (_messages.value.size == 1) {
                val title = if (text.length > 30) text.take(30) + "..." else text
                chatRepo.updateSessionTitle(session.id, title)
            }

            // Get model config
            val config = configRepo.getDefault()
            if (config == null) {
                _error.value = "请先在设置中配置一个模型"
                _isLoading.value = false
                return@launch
            }

            // Build system prompt with skills
            val enabledSkills = skillRepo.getEnabledSkills()
            val systemPrompt = chatRepo.buildSystemPrompt(BASE_SYSTEM_PROMPT, enabledSkills)

            // Build messages for LLM
            val llmMessages = mutableListOf<Map<String, String>>()
            llmMessages.add(mapOf("role" to "system", "content" to systemPrompt))

            val historyMessages = _messages.value.map {
                mapOf("role" to it.role, "content" to it.content)
            }
            llmMessages.addAll(historyMessages)

            // Stream response
            val fullResponse = StringBuilder()
            chatRepo.streamChat(config, llmMessages).collect { result ->
                when (result) {
                    is LLMClient.LLMResult.Chunk -> {
                        fullResponse.append(result.text)
                        _streamingText.value = fullResponse.toString()
                    }
                    is LLMClient.LLMResult.Error -> {
                        _error.value = result.message
                    }
                    is LLMClient.LLMResult.Done -> {
                        val assistantMsg = Message(
                            id = UUID.randomUUID().toString(),
                            sessionId = session.id,
                            role = "assistant",
                            content = fullResponse.toString()
                        )
                        chatRepo.saveMessage(assistantMsg)
                        _messages.value = _messages.value + assistantMsg
                    }
                }
            }
            _streamingText.value = ""
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        chatRepo.shutdown()
    }
}
```

---

### Task 9: UI Theme

**Files:**
- Create: `app/src/main/java/com/agentforandroid/ui/theme/Theme.kt`

- [ ] **Step 1: Create Theme.kt**

```kotlin
package com.agentforandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF5F6368),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    error = Color(0xFFD93025)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF003A75),
    primaryContainer = Color(0xFF004A9F),
    secondary = Color(0xFF9AA0A6),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    error = Color(0xFFF28B82)
)

@Composable
fun AgentForAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
```

---

### Task 10: UI Components

**Files:**
- Create: `app/src/main/java/com/agentforandroid/ui/components/MessageInput.kt`
- Create: `app/src/main/java/com/agentforandroid/ui/components/ChatBubble.kt`
- Create: `app/src/main/java/com/agentforandroid/ui/components/StreamingText.kt`

- [ ] **Step 1: Create MessageInput.kt**

```kotlin
package com.agentforandroid.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageInput(
    onSend: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                        text = ""
                    }
                },
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "发送")
            }
        }
    }
}
```

- [ ] **Step 2: Create ChatBubble.kt**

```kotlin
package com.agentforandroid.ui.components

import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

@Composable
fun ChatBubble(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = rememberMarkwon()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface,
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        setPadding(32, 16, 32, 16)
                        setTextIsSelectable(true)
                        textSize = 15f
                    }
                },
                update = { textView ->
                    markwon.setMarkdown(textView, content)
                },
                modifier = Modifier.wrapContentWidth()
            )
        }
    }
}

@Composable
fun rememberMarkwon(): Markwon {
    val context = LocalContext.current
    return remember {
        Markwon.builder(context).build()
    }
}
```

- [ ] **Step 3: Create StreamingText.kt**

```kotlin
package com.agentforandroid.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreamingText(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = 4.dp, bottomEnd = 16.dp
            ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ChatBubble(content = text, isUser = false)
                // Blinking cursor
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn()
                ) {
                    Text(
                        text = "▍",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
```

---

### Task 11: UI Screens (Settings + Skills)

**Files:**
- Create: `app/src/main/java/com/agentforandroid/ui/screens/SettingsScreen.kt`
- Create: `app/src/main/java/com/agentforandroid/ui/screens/SkillManageScreen.kt`

- [ ] **Step 1: Create SettingsScreen.kt**

```kotlin
package com.agentforandroid.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加")
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
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility, contentDescription = null)
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
```

- [ ] **Step 2: Create SkillManageScreen.kt**

```kotlin
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agentforandroid.model.Skill
import com.agentforandroid.viewmodel.SkillViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillManageScreen(viewModel: SkillViewModel = viewModel()) {
    val skills by viewModel.skills.collectAsState()
    val context = LocalContext.current
    var previewSkill by remember { mutableStateOf<Skill?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Skill 管理") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    "已加载 Skills (${skills.size})",
                    modifier = Modifier.padding(16.dp, 8.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(skills, key = { it.sourcePath }) { skill ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { previewSkill = skill }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(skill.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (skill.isBuiltin) "内置" else "用户",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(skill.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary)
                            Text(skill.sourcePath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
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

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("用户 Skills 存放位置",
                            style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        val skillsPath = "${context.filesDir}/skills/"
                        Text(skillsPath,
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("skills_path", skillsPath))
                            Toast.makeText(context, "路径已复制", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制路径")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("通过 USB 传输 SKILL.md 文件夹到此目录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    previewSkill?.let { skill ->
        AlertDialog(
            onDismissRequest = { previewSkill = null },
            title = { Text(skill.name) },
            text = {
                Column {
                    Text("描述: ${skill.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("路径: ${skill.sourcePath}",
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
```

---

### Task 12: ChatScreen + MainActivity + Wiring

**Files:**
- Create: `app/src/main/java/com/agentforandroid/ui/screens/ChatScreen.kt`
- Create: `app/src/main/java/com/agentforandroid/MainActivity.kt`

- [ ] **Step 1: Create ChatScreen.kt**

```kotlin
package com.agentforandroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
                    val defaultConfig = configs.firstOrNull { it.isDefault }
                    if (defaultConfig != null) {
                        Text(
                            defaultConfig.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
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
            // Active skills indicator
            val activeSkills = skills.filter { it.enabled }
            if (activeSkills.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Skills: ${activeSkills.joinToString(", ") { it.name }}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

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
```

- [ ] **Step 2: Create MainActivity.kt**

```kotlin
package com.agentforandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agentforandroid.ui.screens.ChatScreen
import com.agentforandroid.ui.screens.SettingsScreen
import com.agentforandroid.ui.screens.SkillManageScreen
import com.agentforandroid.ui.theme.AgentForAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgentForAndroidTheme {
                MainApp()
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainApp() {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem("chat", "对话", Icons.Filled.Chat, Icons.Outlined.Chat),
        BottomNavItem("skills", "Skills", Icons.Filled.Extension, Icons.Outlined.Extension),
        BottomNavItem("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("chat") { ChatScreen() }
            composable("skills") { SkillManageScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: APK built at `app/build/outputs/apk/debug/app-debug.apk`

---

### Task 13: Built-in Skill

**Files:**
- Create: `app/src/main/assets/skills/hello-agents/SKILL.md`

- [ ] **Step 1: Create hello-agents SKILL.md**

Copy the content from `d:\workspace\Agent For Android\.claude\skills\hello-agents.md` (the global skill) to `app/src/main/assets/skills/hello-agents/SKILL.md`.

```bash
mkdir -p "app/src/main/assets/skills/hello-agents"
cp ".claude/skills/hello-agents.md" "app/src/main/assets/skills/hello-agents/SKILL.md"
```

---

### Task 14: Final Integration & Verification

- [ ] **Step 1: Full build with all sources**

Run: `./gradlew assembleDebug`

- [ ] **Step 2: Verify APK size and contents**

Run: `ls -lh app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Verify by installing on device/emulator**

Run: `adb install app/build/outputs/apk/debug/app-debug.apk`

**Manual verification checklist:**
1. App launches with custom icon ✓
2. Settings: Add model config → save → verify it appears ✓
3. Skills: Toggle hello-agents on/off ✓
4. Chat: Send message → verify streaming response ✓
5. Rotate device → verify state preserved ✓
6. Kill and relaunch → verify history persists ✓
