package com.example.todomap.model

data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var description: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var locationName: String = "",
    var isCompleted: Boolean = false,
    var notifyOnLocation: Boolean = true,
    var radiusMeters: Int = 100
) {
    companion object {
        const val DEFAULT_RADIUS = 100
    }
}

