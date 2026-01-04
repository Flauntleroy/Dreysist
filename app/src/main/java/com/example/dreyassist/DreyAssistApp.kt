package com.example.dreyassist

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.example.dreyassist.widget.ScreenReceiver

class DreyAssistApp : Application() {
    
    private val screenReceiver = ScreenReceiver()
    
    override fun onCreate() {
        super.onCreate()
        
        // Register screen receiver programmatically (required for Android 8+)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }
}
