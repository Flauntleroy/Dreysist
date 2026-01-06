package com.example.dreyassist.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.dreyassist.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TemperatureWidget : AppWidgetProvider() {

    companion object {
        private const val API_URL = "https://monitoring.rsudhabdulazizmarabahan.com/api/sensors/dht/latest"
        private const val ACTION_UPDATE = "com.example.dreyassist.ACTION_TEMPERATURE_UPDATE"
        private const val UPDATE_INTERVAL_MS = 5000L // 5 seconds
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TemperatureWidget::class.java)
            )
            for (widgetId in widgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
        
        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_temperature)
            
            // Set click listener to refresh
            val refreshIntent = Intent(context, TemperatureWidget::class.java).apply {
                action = ACTION_UPDATE
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, refreshPendingIntent)
            
            // Fetch data from API
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = fetchTemperatureData()
                    
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            // Temperature - Hero (just number with degree symbol)
                            views.setTextViewText(R.id.text_temperature, String.format("%.1f°", result.temperature))
                            
                            // Humidity - Secondary
                            views.setTextViewText(R.id.text_humidity, String.format("Humidity %.0f%%", result.humidity))
                            
                            // Condition based on temp and humidity
                            val condition = getCondition(result.temperature, result.humidity)
                            views.setTextViewText(R.id.text_condition, condition)
                            
                            // Time - Tertiary
                            views.setTextViewText(R.id.text_last_update, result.lastUpdateFormatted)
                            
                            // Determine status based on data age
                            val ageSeconds = (System.currentTimeMillis() - result.recordedAtMillis) / 1000
                            val statusDrawable = when {
                                ageSeconds <= 60 -> R.drawable.bg_status_dot_online
                                ageSeconds > 300 -> R.drawable.bg_status_dot_offline
                                else -> R.drawable.bg_status_dot_online
                            }
                            views.setImageViewResource(R.id.status_dot, statusDrawable)
                        } else {
                            views.setTextViewText(R.id.text_temperature, "--°")
                            views.setTextViewText(R.id.text_humidity, "Humidity --%")
                            views.setTextViewText(R.id.text_condition, "--")
                            views.setTextViewText(R.id.text_last_update, "No data")
                            views.setImageViewResource(R.id.status_dot, R.drawable.bg_status_dot_offline)
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.text_temperature, "--°")
                        views.setTextViewText(R.id.text_humidity, "Humidity --%")
                        views.setTextViewText(R.id.text_condition, "--")
                        views.setTextViewText(R.id.text_last_update, "Error")
                        views.setImageViewResource(R.id.status_dot, R.drawable.bg_status_dot_offline)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
            
            // Show loading state immediately
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        private fun getCondition(temp: Double, humidity: Double): String {
            return when {
                temp < 18 -> "Cold"
                temp > 30 -> "Hot"
                humidity < 30 -> "Dry"
                humidity > 70 -> "Humid"
                temp in 20.0..26.0 && humidity in 40.0..60.0 -> "Comfortable"
                temp in 18.0..28.0 && humidity in 30.0..70.0 -> "Pleasant"
                else -> "Moderate"
            }
        }
        
        private fun fetchTemperatureData(): TemperatureData? {
            return try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    
                    val temperature = json.getDouble("temperature_c")
                    val humidity = json.getDouble("humidity")
                    val recordedAt = json.getString("recorded_at")
                    
                    // Parse the timestamp
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val date = inputFormat.parse(recordedAt)
                    val recordedAtMillis = date?.time ?: System.currentTimeMillis()
                    val formattedTime = if (date != null) outputFormat.format(date) else recordedAt
                    
                    TemperatureData(temperature, humidity, formattedTime, recordedAtMillis)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        
        private fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TemperatureWidget::class.java).apply {
                action = ACTION_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + UPDATE_INTERVAL_MS,
                pendingIntent
            )
        }
        
        private fun cancelUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TemperatureWidget::class.java).apply {
                action = ACTION_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
    
    data class TemperatureData(
        val temperature: Double,
        val humidity: Double,
        val lastUpdateFormatted: String,
        val recordedAtMillis: Long
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleNextUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_UPDATE) {
            updateAllWidgets(context)
            scheduleNextUpdate(context)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget added - start updates
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        // Last widget removed - stop updates
        cancelUpdates(context)
    }
}
