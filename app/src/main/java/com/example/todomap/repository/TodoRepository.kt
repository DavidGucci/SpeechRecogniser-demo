package com.example.todomap.repository

import com.example.todomap.data.TodoDao
import com.example.todomap.model.TodoItem
import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao
) {

    fun observeAllTodos(): Flow<List<TodoItem>> = todoDao.observeAll()

    suspend fun getAllTodos(): List<TodoItem> = todoDao.getAll()

    suspend fun getTodoById(id: Long): TodoItem? = todoDao.getById(id)

    suspend fun saveTodo(todoItem: TodoItem) {
        todoDao.upsert(todoItem)
    }

    suspend fun deleteTodo(todoItem: TodoItem) {
        todoDao.delete(todoItem)
    }

    suspend fun deleteTodoById(id: Long) {
        todoDao.deleteById(id)
    }
}
