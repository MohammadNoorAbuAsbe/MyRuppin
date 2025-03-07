package com.example.myruppin.utils

import java.time.LocalDate
import java.time.YearMonth

object DateUtils {
    /**
     * Format time from a datetime string
     */
    fun formatTimeFromDateTime(dateTimeStr: String): String {
        return try {
            if (dateTimeStr.length >= 16) {
                dateTimeStr.substring(11, 16)
            } else {
                dateTimeStr
            }
        } catch (e: Exception) {
            dateTimeStr
        }
    }

    /**
     * Get the first day of the week containing the given date
     */
    fun getFirstDayOfWeek(date: LocalDate): LocalDate {
        return date.minusDays(date.dayOfWeek.value % 7L)
    }

    /**
     * Get all dates in a month
     */
    fun getDatesInMonth(yearMonth: YearMonth): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()

        while (currentDay.isBefore(lastDay) || currentDay.isEqual(lastDay)) {
            dates.add(currentDay)
            currentDay = currentDay.plusDays(1)
        }

        return dates
    }
}