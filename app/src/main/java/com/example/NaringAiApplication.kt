package com.example

import android.app.Application
import com.example.data.model.SwedishFoodDatabase
import com.example.report.WeeklyReportScheduler

class NaringAiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SwedishFoodDatabase.initialize(this)
        WeeklyReportScheduler.sync(this)
    }
}
