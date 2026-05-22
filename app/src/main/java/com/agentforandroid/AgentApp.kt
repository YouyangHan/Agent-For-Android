package com.agentforandroid

import android.app.Application
import com.agentforandroid.data.local.AppDatabase

class AgentApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
    }
}
