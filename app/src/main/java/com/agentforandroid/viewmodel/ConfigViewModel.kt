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
