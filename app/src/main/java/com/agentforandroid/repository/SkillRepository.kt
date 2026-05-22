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
        val default = SkillLoader.getDefaultUserPath()
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

    fun getBuiltinSkills(): List<Skill> = _skills.value.filter { it.isBuiltin }
    fun getUserSkills(): List<Skill> = _skills.value.filter { !it.isBuiltin }

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
