package com.example

import android.app.Application
import com.example.data.model.SwedishFoodDatabase

class NaringAiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SwedishFoodDatabase.initialize(this)
    }
}
