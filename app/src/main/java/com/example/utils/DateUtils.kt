package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return dateFormat.format(cal.time)
    }

    fun getOffsetDateString(offset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, offset)
        return dateFormat.format(cal.time)
    }

    // Get list of last 7 days for the weekly view
    // Returns List of Pair(dateText like "2026-06-21", displayLabel like "Mon\n21")
    fun getLast7Days(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.US) // e.g. Mon, Tue
        val numberFormat = SimpleDateFormat("d", Locale.US) // e.g. 21
        
        for (i in -6..0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, i)
            val dateStr = dateFormat.format(cal.time)
            val dayLabel = dayFormat.format(cal.time)
            val dayNum = numberFormat.format(cal.time)
            list.add(Pair(dateStr, "$dayLabel\n$dayNum"))
        }
        return list
    }

    fun parseDate(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    // Calculate streaks given list of date strings (yyyy-MM-dd)
    // Returns Pair(currentStreak, maxStreak)
    fun calculateStreaks(completedDates: Set<String>): Pair<Int, Int> {
        if (completedDates.isEmpty()) return Pair(0, 0)
        
        // Let's parse all dates and sort them ascending
        val datesList = completedDates.mapNotNull { parseDate(it) }.sorted()
        if (datesList.isEmpty()) return Pair(0, 0)

        // Calculate maximum streak (consecutive days)
        var maxStreak = 0
        var currentStreakInLoop = 0
        var prevCal: Calendar? = null

        for (date in datesList) {
            val currCal = Calendar.getInstance().apply { time = date }
            currCal.set(Calendar.HOUR_OF_DAY, 0)
            currCal.set(Calendar.MINUTE, 0)
            currCal.set(Calendar.SECOND, 0)
            currCal.set(Calendar.MILLISECOND, 0)

            if (prevCal == null) {
                currentStreakInLoop = 1
            } else {
                // Check if currCal is exactly 1 day after prevCal
                val tempCal = Calendar.getInstance().apply { 
                    time = prevCal!!.time 
                    add(Calendar.DATE, 1)
                }
                if (tempCal.get(Calendar.YEAR) == currCal.get(Calendar.YEAR) &&
                    tempCal.get(Calendar.DAY_OF_YEAR) == currCal.get(Calendar.DAY_OF_YEAR)) {
                    currentStreakInLoop++
                } else if (currCal.after(tempCal)) {
                    if (currentStreakInLoop > maxStreak) {
                        maxStreak = currentStreakInLoop
                    }
                    currentStreakInLoop = 1
                }
            }
            prevCal = currCal
        }
        if (currentStreakInLoop > maxStreak) {
            maxStreak = currentStreakInLoop
        }

        // Calculate current streak (dynamic backwards check from today/yesterday)
        var currentStreak = 0
        val todayStr = getTodayDateString()
        val yesterdayStr = getYesterdayDateString()

        if (completedDates.contains(todayStr)) {
            currentStreak = 1
            val checkCal = Calendar.getInstance()
            while (true) {
                checkCal.add(Calendar.DATE, -1)
                val checkStr = dateFormat.format(checkCal.time)
                if (completedDates.contains(checkStr)) {
                    currentStreak++
                } else {
                    break
                }
            }
        } else if (completedDates.contains(yesterdayStr)) {
            currentStreak = 1
            val checkCal = Calendar.getInstance()
            checkCal.add(Calendar.DATE, -1) // Yesterday
            while (true) {
                checkCal.add(Calendar.DATE, -1)
                val checkStr = dateFormat.format(checkCal.time)
                if (completedDates.contains(checkStr)) {
                    currentStreak++
                } else {
                    break
                }
            }
        }

        return Pair(currentStreak, maxStreak)
    }
}
