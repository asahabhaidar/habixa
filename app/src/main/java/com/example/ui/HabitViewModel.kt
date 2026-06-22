package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Habit
import com.example.data.HabitCompletion
import com.example.data.HabitRepository
import com.example.data.HabixaDatabase
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Data class to hold calculated stats for an individual habit
data class HabitStats(
    val currentStreak: Int,
    val maxStreak: Int,
    val totalCompletions: Int,
    val isCompletedOnSelectedDate: Boolean,
    val completedDates: Set<String>
)

// Data class containing the aggregated UI state
data class HabitUiState(
    val habits: List<Habit> = emptyList(),
    val selectedDate: String = "",
    val isToday: Boolean = true,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val completionPercentage: Int = 0,
    val habitStats: Map<Long, HabitStats> = emptyMap(),
    val totalCompletions: Int = 0,
    val bestActiveStreak: Int = 0,
    val topPerformedHabit: String = "None yet"
)

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val database = HabixaDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())

    // Currently selected date string (Format: yyyy-MM-dd)
    val selectedDate = MutableStateFlow(DateUtils.getTodayDateString())

    // Expose raw flows if needed
    val habits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completions: StateFlow<List<HabitCompletion>> = repository.allCompletions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Compound UI state that updates reactively to any changes in habits, completions, or date selections
    val uiState: StateFlow<HabitUiState> = combine(habits, completions, selectedDate) { habitList, completionList, date ->
        val totalHabits = habitList.size
        
        // Find which habits have been completed on the chosen date
        val completionsForSelectedDate = completionList
            .filter { it.dateText == date }
            .map { it.habitId }
            .toSet()

        val completedCountSelectedDate = habitList.count { completionsForSelectedDate.contains(it.id) }
        val completionPercentage = if (totalHabits > 0) {
            (completedCountSelectedDate.toFloat() / totalHabits * 100).toInt()
        } else {
            0
        }

        // Compute local streams, max historic streaks and total count for each habit item
        val habitStatsMap = habitList.associate { habit ->
            val habitCompletedDates = completionList
                .filter { it.habitId == habit.id }
                .map { it.dateText }
                .toSet()

            val (currentStreak, maxStreak) = DateUtils.calculateStreaks(habitCompletedDates)
            val totalCompletions = habitCompletedDates.size

            habit.id to HabitStats(
                currentStreak = currentStreak,
                maxStreak = maxStreak,
                totalCompletions = totalCompletions,
                isCompletedOnSelectedDate = completionsForSelectedDate.contains(habit.id),
                completedDates = habitCompletedDates
            )
        }

        // Overall stats calculations
        val totalCompletionsOverall = completionList.size
        val bestStreakOverall = habitStatsMap.values.maxOfOrNull { it.currentStreak } ?: 0
        val topPerformedHabitName = habitList.maxByOrNull { habit ->
            habitStatsMap[habit.id]?.totalCompletions ?: 0
        }?.let { 
            val completionsCount = habitStatsMap[it.id]?.totalCompletions ?: 0
            if (completionsCount > 0) "${it.title} ($completionsCount)" else null
        } ?: "None yet"

        HabitUiState(
            habits = habitList,
            selectedDate = date,
            isToday = date == DateUtils.getTodayDateString(),
            completedCount = completedCountSelectedDate,
            totalCount = totalHabits,
            completionPercentage = completionPercentage,
            habitStats = habitStatsMap,
            totalCompletions = totalCompletionsOverall,
            bestActiveStreak = bestStreakOverall,
            topPerformedHabit = topPerformedHabitName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitUiState()
    )

    init {
        // Automatically prefill onboarding starter habits if empty on boot
        viewModelScope.launch {
            val currentList = repository.allHabits.first()
            if (currentList.isEmpty()) {
                repository.populateOnboardingHabits()
            }
        }
    }

    // Toggle completion of a habit for the currently active selectedDate
    fun toggleHabitCompletion(habitId: Long) {
        viewModelScope.launch {
            val dateText = selectedDate.value
            val currentCompletions = completions.value
            val exists = currentCompletions.any { it.habitId == habitId && it.dateText == dateText }
            if (exists) {
                repository.uncompleteHabit(habitId, dateText)
            } else {
                repository.completeHabit(habitId, dateText)
            }
        }
    }

    // Add a brand-new habit with customizable accents
    fun addHabit(title: String, description: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val newHabit = Habit(
                title = title.trim(),
                description = description.trim(),
                iconName = iconName,
                colorHex = colorHex
            )
            repository.insertHabit(newHabit)
        }
    }

    // Clean up or remove a habit from the database
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    // Reset today's / selected date's completions only
    fun resetSelectedDateTasks() {
        viewModelScope.launch {
            repository.resetCompletionsForDate(selectedDate.value)
        }
    }

    // Fully reset all historical completions for debugging / fresh slate
    fun resetAllHistoricalProgress() {
        viewModelScope.launch {
            repository.clearAllCompletions()
        }
    }

    // Sets the active view date
    fun selectDate(dateText: String) {
        selectedDate.value = dateText
    }
}
