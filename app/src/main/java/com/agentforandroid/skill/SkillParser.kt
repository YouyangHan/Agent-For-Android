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
