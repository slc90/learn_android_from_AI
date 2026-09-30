package com.example.learnandroidfromai

import android.app.Application

class LearnAndroidApplication : Application() {

    val appContainer by lazy {
        AppContainer()
    }
}