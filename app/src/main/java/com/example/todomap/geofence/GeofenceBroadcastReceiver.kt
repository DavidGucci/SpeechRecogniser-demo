package com.example.todomap.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.todomap.R
import com.example.todomap.ToDoMapApp
import com.example.todomap.notification.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            if (triggeringGeofences.isNullOrEmpty()) {
                Log.e(TAG, "No triggering geofences")
                return
            }

            for (geofence in triggeringGeofences) {
                val todoId = geofence.requestId.toLongOrNull() ?: continue
                handleGeofenceEnter(context, todoId)
            }
        } else {
            Log.d(TAG, "Unknown geofence transition: $geofenceTransition")
        }
    }

    private fun handleGeofenceEnter(context: Context, todoId: Long) {
        Log.d(TAG, "Entered geofence for todo: $todoId")

        val app = context.applicationContext as ToDoMapApp
        val repository = app.todoRepository

        CoroutineScope(Dispatchers.IO).launch {
            val todo = repository.getTodoById(todoId)

            if (todo != null && !todo.isCompleted && todo.notifyOnLocation) {
                val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

                if (notificationsEnabled) {
                    NotificationHelper.showGeofenceNotification(
                        context = context,
                        todoId = todoId,
                        title = context.getString(R.string.notification_nearby_todo),
                        message = todo.title
                    )
                }
            }
        }
    }
}

