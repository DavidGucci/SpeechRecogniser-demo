package com.example.todomap

import android.app.Application
import com.example.todomap.data.AppDatabase
import com.example.todomap.geofence.GeofenceHelper
import com.example.todomap.notification.NotificationHelper
import com.example.todomap.repository.TodoRepository
import com.example.todomap.settings.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ToDoMapApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val todoRepository: TodoRepository by lazy { TodoRepository(database.todoDao()) }

    override fun onCreate() {
        super.onCreate()

        ThemePreferences.applyTheme(this)

        NotificationHelper.createNotificationChannels(this)

        applicationScope.launch(Dispatchers.IO) {
            val geofenceHelper = GeofenceHelper(this@ToDoMapApp)
            val todos = todoRepository.getAllTodos()
            geofenceHelper.addAllGeofences(todos)
        }
    }
}
