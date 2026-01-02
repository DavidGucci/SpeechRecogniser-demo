package com.example.todomap

import android.app.Application
import com.example.todomap.data.AppDatabase
import com.example.todomap.repository.TodoRepository

class ToDoMapApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val todoRepository: TodoRepository by lazy { TodoRepository(database.todoDao()) }
}

