package com.agentforandroid.tool

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.widget.Toast
import org.json.JSONObject
import java.util.Calendar

class ToolExecutor(private val context: Context) {

    companion object {
        val TOOL_DESCRIPTIONS = """
## 可用工具
你可以使用以下工具操作手机。在回复中使用格式: [TOOL:工具名:{"参数":"值"}]

### set_alarm — 设置闹钟
参数: {"time":"HH:MM","label":"标签"}
示例: [TOOL:set_alarm:{"time":"08:00","label":"起床"}]

### add_event — 添加日历日程
参数: {"title":"标题","time":"HH:MM","duration_minutes":60}
示例: [TOOL:add_event:{"title":"会议","time":"14:00","duration_minutes":30}]

### launch_app — 启动应用
参数: {"app_name":"应用名"}
示例: [TOOL:launch_app:{"app_name":"计算器"}]

重要: 工具调用放在回复的最前面，一行一个。
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
                "set_alarm" -> setAlarm(params)
                "add_event" -> addEvent(params)
                "launch_app" -> launchApp(params)
                else -> "❌ 未知工具: $name"
            }
        } catch (e: Exception) {
            "❌ 工具执行失败: ${e.localizedMessage}"
        }
    }

    private fun setAlarm(params: JSONObject): String {
        val time = params.optString("time", "")
        val label = params.optString("label", "闹钟")

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
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1) // Tomorrow if time passed
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("label", label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, label.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
        )

        return "✅ 闹钟已设置: ${time} $label"
    }

    private fun addEvent(params: JSONObject): String {
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

    fun launchApp(params: JSONObject): String {
        val appName = params.optString("app_name", "")
        if (appName.isBlank()) return "❌ 请指定应用名称"

        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        var targetPkg: String? = null

        // Search by app name (case-insensitive partial match)
        for (app in apps) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(appName, ignoreCase = true)) {
                targetPkg = app.packageName
                break
            }
        }

        if (targetPkg == null) {
            // Try package name directly
            targetPkg = appName
        }

        return try {
            val intent = pm.getLaunchIntentForPackage(targetPkg!!)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "✅ 已启动: $appName"
            } else {
                "❌ 无法启动: $appName (应用可能没有启动界面)"
            }
        } catch (e: Exception) {
            "❌ 启动失败: ${e.localizedMessage}"
        }
    }
}
