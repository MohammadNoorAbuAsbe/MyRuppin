package com.example.myruppin.data.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


data class ScheduleCourse(
    val name: String,
    val instructor: String,
    val startTime: String,
    val endTime: String,
    val day: String,
    val location: String,
    val semester: String,
    val studyYear: String
)

data class ScheduleParams(
    val hash: String,
    val pt: Int,
    val ptMsl: Int,
    val shl: Int
)

data class DaySchedule(
    val date: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val place: String?,
    val moreInfo: String?
)

/**
 * Represents a course with its grade information
 */
data class Course(
    val name: String,
    val grade: String,
    val krs_snl: String,
    val courseWeight: String,
    val details: List<Detail>
)

/**
 * Represents detailed information about a course
 */
data class Detail(
    val name: String,
    val finalGrade: String,
    val subDetails: List<SubDetail>
)

/**
 * Represents sub-details of a course detail
 */
data class SubDetail(
    val groupName: String,
    val date: String,
    val time: String,
    val grade: String
)

/**
 * Represents the average grades information
 */
data class GradesAverages(
    val cumulativeAverage: String,
    val annualAverages: List<String>
)

/**
 * Represents the complete grades data
 */
data class GradesData(
    val courses: List<Course>,
    val averages: GradesAverages
)

/**
 * Represents information about a current event
 */
data class EventInfo(
    val title: String,
    val place: String,
    val startTime: String,
    val endTime: String
)

/**
 * Represents an upcoming event
 */
data class UpcomingEvent(
    val title: String,
    val date: String,
    val type: String = "",
    val isExam: Boolean
) {
    /**
     * Calculate days left until this event
     */
    fun calculateDaysLeft(): Long {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val eventLocalDate = LocalDate.parse(date, formatter)
        val currentDate = LocalDate.now()
        return ChronoUnit.DAYS.between(currentDate, eventLocalDate)
    }
}

/**
 * Represents all home screen data
 */
data class HomeData(
    val currentEvent: EventInfo?,
    val upcomingEvents: List<UpcomingEvent>
)