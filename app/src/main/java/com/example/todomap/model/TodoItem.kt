package com.example.todomap.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var description: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var locationName: String = "",
    var isCompleted: Boolean = false,
    var notifyOnLocation: Boolean = true,
    var radiusMeters: Int = DEFAULT_RADIUS
) {
    companion object {
        const val DEFAULT_RADIUS = 100
    }
}
