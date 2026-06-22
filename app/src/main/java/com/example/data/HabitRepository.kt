package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletion>> = habitDao.getAllCompletions()

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    suspend fun completeHabit(habitId: Long, dateText: String) {
        habitDao.insertCompletion(HabitCompletion(habitId, dateText))
    }

    suspend fun uncompleteHabit(habitId: Long, dateText: String) {
        habitDao.deleteCompletion(habitId, dateText)
    }

    suspend fun resetCompletionsForDate(dateText: String) {
        habitDao.deleteCompletionsForDate(dateText)
    }

    suspend fun clearAllCompletions() {
        habitDao.clearAllCompletions()
    }

    suspend fun populateOnboardingHabits() {
        // Sample habits representing clean, engaging daily starting habits
        val sample1 = Habit(
            title = "Stay Hydrated 💧",
            description = "Drink 8 glasses of water to keep your body refreshed and mind clean.",
            iconName = "Water",
            colorHex = "#2196F3" // Blue
        )
        val sample2 = Habit(
            title = "Read 15 Pages 📚",
            description = "Spend 10-15 minutes reading a physical book or learning article.",
            iconName = "Book",
            colorHex = "#9C27B0" // Purple
        )
        val sample3 = Habit(
            title = "Morning Stretch 🏃‍♂️",
            description = "Wake up your muscles with simple stretching exercises or a brisk walk.",
            iconName = "Fitness",
            colorHex = "#4CAF50" // Green
        )
        habitDao.insertHabit(sample1)
        habitDao.insertHabit(sample2)
        habitDao.insertHabit(sample3)
    }
}
