package com.agentforandroid.skill

import com.agentforandroid.model.Skill

object SkillParser {

    private val frontmatterRegex = Regex("---\\s*\\n([\\s\\S]*?)\\n---\\s*\\n?([\\s\\S]*)")

    // Chinese-friendly display names for built-in skills
    private val builtinDisplayNames = mapOf(
        "auto-permit" to Pair("自动权限配置", "所有命令默认自动执行，不再逐一询问"),
        "brainstorming" to Pair("头脑风暴", "创意工作前的需求探索与设计讨论"),
        "executing-plans" to Pair("计划执行", "在独立会话中按检查点执行实施计划"),
        "openclaude" to Pair("通用问答", "提供高质量的通用回答和帮助"),
        "planning-with-files" to Pair("文件规划", "基于文件的规划方式组织复杂任务"),
        "prd" to Pair("PRD生成", "为新功能生成产品需求文档"),
        "ralph" to Pair("Ralph转换", "将PRD转换为Ralph系统的JSON格式"),
        "subagent-driven-development" to Pair("子代理开发", "使用独立子代理分步执行实施计划"),
        "using-superpowers" to Pair("超能力引导", "启动对话时建立如何查找和使用技能"),
        "writing-plans" to Pair("计划编写", "为多步骤任务编写实施计划"),
        // Built-in personality skills
        "tong-jincheng-perspective" to Pair("深情女", "童锦程视角：真诚情感与人际关系思维"),
        "性感风情御姐.skill" to Pair("魅力男", "性感风情御姐：优雅魅力与成熟气质"),
    )

    // Pinned built-in personalities - always personality, cannot be removed
    val pinnedPersonalities = setOf("tong-jincheng-perspective", "性感风情御姐.skill")

    fun parse(markdown: String, sourcePath: String, isBuiltin: Boolean): Skill? {
        val match = frontmatterRegex.find(markdown) ?: return null

        val frontmatter = match.groupValues[1]
        val content = match.groupValues[2].trim()

        val name = extractField(frontmatter, "name") ?: return null
        val description = extractField(frontmatter, "description") ?: ""

        val (displayName, displayDesc) = if (isBuiltin) {
            builtinDisplayNames[name] ?: Pair(name, description)
        } else {
            Pair(name, description)
        }

        val isPinnedPersonality = isBuiltin && pinnedPersonalities.contains(name)

        return Skill(
            name = name,
            description = description,
            content = content,
            sourcePath = sourcePath,
            isBuiltin = isBuiltin,
            displayName = displayName,
            displayDescription = displayDesc,
            isPersonality = isPinnedPersonality,
            personalityName = if (isPinnedPersonality) displayName else ""
        )
    }

    private fun extractField(frontmatter: String, key: String): String? {
        val regex = Regex("^$key\\s*:\\s*(.+)$", RegexOption.MULTILINE)
        return regex.find(frontmatter)?.groupValues?.get(1)?.trim()
    }
}
