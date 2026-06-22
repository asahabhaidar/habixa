package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val iconName: String = "Star", // e.g. "Star", "Fitness", "Water", "Book"
    val colorHex: String = "#800080" // Purple default
)
