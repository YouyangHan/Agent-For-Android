package com.agentforandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentforandroid.model.Skill
import com.agentforandroid.repository.SkillRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SkillViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SkillRepository.getInstance(application)

    val skills: StateFlow<List<Skill>> = repository.skills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repository.loadSkills()
    }

    fun toggleSkill(skillName: String, enabled: Boolean) {
        repository.toggleSkill(skillName, enabled)
    }

    fun getEnabledSkills(): List<Skill> = repository.getEnabledSkills()

    fun getPersonalitySkills(): List<Skill> = repository.getPersonalitySkills()

    fun togglePersonality(skillName: String, isPersonality: Boolean, personalityName: String) {
        repository.togglePersonality(skillName, isPersonality, personalityName)
    }

    fun reloadSkills() {
        repository.reloadSkills()
    }
}
