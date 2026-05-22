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
