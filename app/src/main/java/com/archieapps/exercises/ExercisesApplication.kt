package com.archieapps.exercises

import android.app.Application
import com.archieapps.exercises.core.di.AppContainer

class ExercisesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
