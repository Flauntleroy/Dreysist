package com.example.dreyassist.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver for screen on/off events to update widgets and insights
 */
class ScreenReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                // Update all widgets when screen turns on
                DreysistWidget.updateAllWidgets(context)
                DreysistWidgetLarge.updateAllWidgets(context)
            }
        }
    }
}
