package com.agentforandroid.repository

import android.content.Context
import com.agentforandroid.model.Skill
import com.agentforandroid.skill.SkillLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SkillRepository private constructor(private val context: Context) {

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    private val prefs by lazy {
        context.applicationContext.getSharedPreferences("skill_prefs", Context.MODE_PRIVATE)
    }

    private var loaded = false

    fun getUserSkillsPath(): String {
        val default = SkillLoader.getDefaultUserPath(context)
        return prefs.getString("user_skills_path", default) ?: default
    }

    fun setUserSkillsPath(path: String) {
        prefs.edit().putString("user_skills_path", path).apply()
        reloadSkills()
    }

    fun reloadSkills() {
        loaded = false
        loadSkills()
    }

    fun loadSkills() {
        if (loaded) return
        loaded = true
        val userPath = getUserSkillsPath()
        val loaded = SkillLoader.loadAll(context.applicationContext, userPath)
        val enabledSet = prefs.getStringSet("enabled_skills", emptySet()) ?: emptySet()
        val personalitySet = prefs.getStringSet("personality_skills", emptySet()) ?: emptySet()
        val personalityNames = loadPersonalityNames()
        val isFirstLaunch = prefs.getBoolean("first_launch", true)

        val pinnedSet = com.agentforandroid.skill.SkillParser.pinnedPersonalities
        val skillsWithState = loaded.map { skill ->
            val enabled = if (isFirstLaunch) {
                pinnedSet.contains(skill.name)  // pinned personalities enabled by default
            } else {
                enabledSet.contains(skill.name) || pinnedSet.contains(skill.name)
            }
            // Keep personality state from parser if pinned, else use stored pref
            val isPinned = pinnedSet.contains(skill.name)
            val isPersonality = if (isPinned) true
                else if (skill.isPersonality) true  // keep parser-set value
                else personalitySet.contains(skill.name)
            val pName = if (isPinned && skill.personalityName.isNotBlank()) skill.personalityName
                else personalityNames[skill.name] ?: skill.personalityName
            skill.copy(
                enabled = enabled,
                isPersonality = isPersonality,
                personalityName = pName.ifBlank { skill.displayName }
            )
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

    fun getBuiltinSkills(): List<Skill> = _skills.value.filter { it.isBuiltin }
    fun getUserSkills(): List<Skill> = _skills.value.filter { !it.isBuiltin }

    fun getPersonalitySkills(): List<Skill> = _skills.value.filter { it.isPersonality && it.enabled }

    fun togglePersonality(skillName: String, isPersonality: Boolean, personalityName: String) {
        _skills.value = _skills.value.map {
            if (it.name == skillName) it.copy(isPersonality = isPersonality, personalityName = personalityName) else it
        }
        val set = _skills.value.filter { it.isPersonality }.map { it.name }.toSet()
        prefs.edit().putStringSet("personality_skills", set).apply()
        // Save personality name mapping
        val names = _skills.value.filter { it.isPersonality && it.personalityName.isNotBlank() }
            .associate { it.name to it.personalityName }
        prefs.edit().putString("personality_names",
            names.entries.joinToString("||") { "${it.key}=${it.value}" }).apply()
    }

    private fun loadPersonalityNames(): Map<String, String> {
        val raw = prefs.getString("personality_names", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return try {
            raw.split("||").filter { it.contains("=") }
                .associate { val (k, v) = it.split("=", limit = 2); k to v }
        } catch (_: Exception) { emptyMap() }
    }

    companion object {
        @Volatile
        private var INSTANCE: SkillRepository? = null

        fun getInstance(context: Context): SkillRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SkillRepository(context.applicationContext).also {
                    it.loadSkills()
                    INSTANCE = it
                }
            }
        }
    }
}
