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
