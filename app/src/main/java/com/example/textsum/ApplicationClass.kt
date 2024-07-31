package com.example.textsum

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseAppLifecycleListener

class ApplicationClass:Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("srujal","called applicationclass")
        FirebaseApp.initializeApp(this)
    }
}