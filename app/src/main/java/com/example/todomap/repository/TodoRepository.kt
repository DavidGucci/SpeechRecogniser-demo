package com.example.todomap.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.todomap.model.TodoItem
import org.json.JSONArray
import org.json.JSONObject

class TodoRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "todo_prefs"
        private const val KEY_TODOS = "todos"
    }

    fun getAllTodos(): MutableList<TodoItem> {
        val jsonString = sharedPreferences.getString(KEY_TODOS, null) ?: return mutableListOf()
        return try {
            val jsonArray = JSONArray(jsonString)
            val todoList = mutableListOf<TodoItem>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                todoList.add(jsonToTodoItem(jsonObject))
            }
            todoList
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveTodo(todoItem: TodoItem) {
        val todos = getAllTodos()
        val existingIndex = todos.indexOfFirst { it.id == todoItem.id }
        if (existingIndex >= 0) {
            todos[existingIndex] = todoItem
        } else {
            todos.add(todoItem)
        }
        saveAllTodos(todos)
    }

    fun deleteTodo(todoItem: TodoItem) {
        val todos = getAllTodos()
        todos.removeAll { it.id == todoItem.id }
        saveAllTodos(todos)
    }

    fun getTodoById(id: Long): TodoItem? {
        return getAllTodos().find { it.id == id }
    }

    private fun saveAllTodos(todos: List<TodoItem>) {
        val jsonArray = JSONArray()
        todos.forEach { todo ->
            jsonArray.put(todoItemToJson(todo))
        }
        sharedPreferences.edit().putString(KEY_TODOS, jsonArray.toString()).apply()
    }

    private fun todoItemToJson(todoItem: TodoItem): JSONObject {
        return JSONObject().apply {
            put("id", todoItem.id)
            put("title", todoItem.title)
            put("description", todoItem.description)
            put("latitude", todoItem.latitude)
            put("longitude", todoItem.longitude)
            put("locationName", todoItem.locationName)
            put("isCompleted", todoItem.isCompleted)
            put("notifyOnLocation", todoItem.notifyOnLocation)
            put("radiusMeters", todoItem.radiusMeters)
        }
    }

    private fun jsonToTodoItem(jsonObject: JSONObject): TodoItem {
        return TodoItem(
            id = jsonObject.getLong("id"),
            title = jsonObject.getString("title"),
            description = jsonObject.optString("description", ""),
            latitude = jsonObject.optDouble("latitude", 0.0),
            longitude = jsonObject.optDouble("longitude", 0.0),
            locationName = jsonObject.optString("locationName", ""),
            isCompleted = jsonObject.optBoolean("isCompleted", false),
            notifyOnLocation = jsonObject.optBoolean("notifyOnLocation", true),
            radiusMeters = jsonObject.optInt("radiusMeters", TodoItem.DEFAULT_RADIUS)
        )
    }
}

