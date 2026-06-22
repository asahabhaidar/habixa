package com.example.data

import androidx.room.Entity

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "dateText"]
)
data class HabitCompletion(
    val habitId: Long,
    val dateText: String // Format: YYYY-MM-DD
)
