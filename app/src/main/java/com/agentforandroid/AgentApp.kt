package com.agentforandroid

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.agentforandroid.data.local.AppDatabase
import java.util.Locale

class AgentApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        applySavedLocale()
    }

    private fun applySavedLocale() {
        val lang = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("language", "zh") ?: "zh"
        setLocale(this, lang)
    }

    companion object {
        fun setLocale(context: Context, lang: String) {
            val locale = if (lang == "en") Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
