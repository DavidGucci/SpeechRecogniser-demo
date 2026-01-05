package com.example.todomap.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.todomap.ToDoMapApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, re-registering geofences")
            reRegisterGeofences(context)
        }
    }

    private fun reRegisterGeofences(context: Context) {
        val app = context.applicationContext as ToDoMapApp
        val repository = app.todoRepository
        val geofenceHelper = GeofenceHelper(context)

        CoroutineScope(Dispatchers.IO).launch {
            val todos = repository.getAllTodos()
            geofenceHelper.addAllGeofences(todos)
        }
    }
}

