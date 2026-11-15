package com.arin.civicreport

import android.app.Application
import android.content.Context
import com.arin.civicreport.data.AppContainer
import org.osmdroid.config.Configuration

class CivicReportApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        runCatching {
            Configuration.getInstance().load(this, prefs)
            Configuration.getInstance().userAgentValue = packageName
        }
        container.notifications.ensureChannel()
    }
}
