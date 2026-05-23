package com.agentforandroid.tool

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.util.Calendar

class ToolExecutor(private val context: Context) {
    // Callback to reload skills after create_skill
    var onSkillCreated: (() -> Unit)? = null

    private val userSkillsPath: String by lazy {
        com.agentforandroid.repository.SkillRepository.getInstance(context).getUserSkillsPath()
    }

    companion object {
        val TOOL_DESCRIPTIONS = """
## 可用工具
你可以使用以下工具。在回复中使用格式: [TOOL:工具名:{"参数":"值"}]

### create_skill — 创建新Skill
通过对话创建SKILL.md文件，自动部署并导入。
参数: {"name":"英文名","display_name":"中文名","description":"简介","content":"markdown正文"}
注意: name只含英文数字连字符, content换行用\\n

### add_event — 添加日历日程
参数: {"title":"标题","time":"HH:MM","duration_minutes":60}

### launch_app — 启动应用
参数: {"app_name":"应用名"}

重要: 工具调用放在回复最前面，一行一个。
""".trimIndent()
    }

    private val toolPattern = Regex("\\[TOOL:(\\w+):(\\{[^}]+\\})\\]")

    fun hasToolCall(text: String): Boolean = toolPattern.containsMatchIn(text)

    fun executeToolCalls(text: String): Pair<String, String> {
        val results = mutableListOf<String>()
        var cleanText = text

        toolPattern.findAll(text).forEach { match ->
            val toolName = match.groupValues[1]
            val paramsJson = match.groupValues[2]
            val result = executeTool(toolName, paramsJson)
            results.add(result)
            cleanText = cleanText.replace(match.value, "")
        }

        return Pair(cleanText.trim(), results.joinToString("\n"))
    }

    private fun executeTool(name: String, paramsJson: String): String {
        return try {
            val params = JSONObject(paramsJson)
            when (name) {
                "create_skill" -> createSkill(params)
                "add_event" -> addEvent(params)
                "launch_app" -> launchApp(params)
                else -> "❌ 未知工具: $name"
            }
        } catch (e: Exception) {
            "❌ 工具执行失败: ${e.localizedMessage}"
        }
    }

    private fun addEvent(params: JSONObject): String {
        // Check calendar permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {
            return "❌ 需要日历权限。请在系统设置 → 应用 → Agent Yang → 权限中允许"
        }

        val title = params.optString("title", "日程")
        val time = params.optString("time", "")
        val durationMin = params.optInt("duration_minutes", 60)

        if (!time.matches(Regex("\\d{1,2}:\\d{2}"))) {
            return "❌ 时间格式错误，请使用 HH:MM 格式"
        }

        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val values = ContentValues().apply {
            put(Events.CALENDAR_ID, 1) // Default calendar
            put(Events.TITLE, title)
            put(Events.DTSTART, calendar.timeInMillis)
            put(Events.DTEND, calendar.timeInMillis + durationMin * 60 * 1000)
            put(Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                put(Events.HAS_ALARM, 0)
            }
        }

        val uri = context.contentResolver.insert(Events.CONTENT_URI, values)
        return if (uri != null) {
            "✅ 日程已添加: $time ${title}(${durationMin}分钟)"
        } else {
            "❌ 添加日程失败，请检查日历权限"
        }
    }

    private fun createSkill(params: JSONObject): String {
        val name = params.optString("name", "").trim()
        val displayName = params.optString("display_name", name).trim()
        val description = params.optString("description", "").trim()
        val content = params.optString("content", "").trim().replace("\\n", "\n")

        if (name.isBlank()) return "❌ skill名称不能为空"
        if (!name.matches(Regex("^[a-zA-Z0-9_-]+$"))) return "❌ name只能用英文/数字/连字符"

        val skillDir = File(userSkillsPath, name)
        if (!skillDir.exists()) skillDir.mkdirs()

        val skillMd = File(skillDir, "SKILL.md")
        val frontmatter = "---\nname: $name\ndescription: $description\n---\n"
        val fullContent = frontmatter + "\n" +
            "# $displayName\n\n" +
            if (description.isNotBlank()) "> $description\n\n" else "" +
            content

        skillMd.writeText(fullContent)
        onSkillCreated?.invoke()
        return "✅ Skill '$displayName' 已创建并导入: ${skillDir.absolutePath}"
    }

    fun launchApp(params: JSONObject): String {
        val appName = params.optString("app_name", "")
        if (appName.isBlank()) return "❌ 请指定应用名称"

        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        val matches = mutableListOf<Pair<String, String>>() // (packageName, label)

        // Search by app name (case-insensitive partial match)
        for (app in apps) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(appName, ignoreCase = true) ||
                app.packageName.contains(appName, ignoreCase = true)) {
                matches.add(Pair(app.packageName, label))
            }
        }

        if (matches.isEmpty()) {
            return "❌ 未找到应用: $appName\n请确认应用名称正确"
        }

        // Try each match until one works
        for ((pkg, label) in matches) {
            try {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                    context.startActivity(intent)
                    return "✅ 已启动: $label"
                }
            } catch (_: Exception) {}
        }

        // Fallback: try the name as package name directly
        return try {
            val intent = pm.getLaunchIntentForPackage(appName)
            if (intent != null) {
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                context.startActivity(intent)
                "✅ 已启动: $appName"
            } else {
                "❌ 无法启动: $appName\n找到 ${matches.size} 个匹配但均无法启动"
            }
        } catch (e: Exception) {
            "❌ 启动失败: ${e.localizedMessage}"
        }
    }
}
