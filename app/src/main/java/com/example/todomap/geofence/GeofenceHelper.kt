package com.example.todomap.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.todomap.model.TodoItem
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(private val context: Context) {

    companion object {
        private const val TAG = "GeofenceHelper"
        private const val GEOFENCE_EXPIRATION_MS = Geofence.NEVER_EXPIRE
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun addGeofence(todoItem: TodoItem, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        if (!hasLocationPermission()) {
            onFailure("Location permission not granted")
            return
        }

        if (todoItem.latitude == 0.0 && todoItem.longitude == 0.0) {
            onFailure("Invalid location")
            return
        }

        if (!todoItem.notifyOnLocation) {
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(todoItem.id.toString())
            .setCircularRegion(
                todoItem.latitude,
                todoItem.longitude,
                todoItem.radiusMeters.toFloat()
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence added for todo: ${todoItem.id}")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add geofence: ${e.message}")
                    onFailure(e.message ?: "Unknown error")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            onFailure("Security exception: ${e.message}")
        }
    }

    fun removeGeofence(todoId: Long, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        geofencingClient.removeGeofences(listOf(todoId.toString()))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence removed for todo: $todoId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofence: ${e.message}")
                onFailure(e.message ?: "Unknown error")
            }
    }

    fun removeAllGeofences(onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "All geofences removed")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove all geofences: ${e.message}")
                onFailure(e.message ?: "Unknown error")
            }
    }

    fun addAllGeofences(todos: List<TodoItem>) {
        todos.filter {
            it.notifyOnLocation &&
            !it.isCompleted &&
            (it.latitude != 0.0 || it.longitude != 0.0)
        }.forEach { todo ->
            addGeofence(todo)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

