package com.MohammadNoorAbuAsbe.myruppin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MohammadNoorAbuAsbe.myruppin.data.TokenManager
import com.MohammadNoorAbuAsbe.myruppin.data.models.ScheduleCourse
import com.MohammadNoorAbuAsbe.myruppin.data.models.DaySchedule
import com.MohammadNoorAbuAsbe.myruppin.data.models.ScheduleParams
import com.MohammadNoorAbuAsbe.myruppin.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.time.LocalDate
import java.time.YearMonth
import com.MohammadNoorAbuAsbe.myruppin.utils.DateUtils.formatTimeFromDateTime
import kotlinx.coroutines.awaitAll

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // UI State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showSchedule = MutableStateFlow(false)
    val showSchedule: StateFlow<Boolean> = _showSchedule.asStateFlow()

    // Schedule Data
    private val _scheduleData = MutableStateFlow<List<ScheduleCourse>>(emptyList())
    val scheduleData: StateFlow<List<ScheduleCourse>> = _scheduleData.asStateFlow()

    private val _scheduleParams = MutableStateFlow<ScheduleParams?>(null)
    val scheduleParams: StateFlow<ScheduleParams?> = _scheduleParams.asStateFlow()

    // Filter State
    private val _selectedFilter = MutableStateFlow<Pair<String, String>?>(null)
    val selectedFilter: StateFlow<Pair<String, String>?> = _selectedFilter.asStateFlow()

    // Calendar State
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDay = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDay: StateFlow<LocalDate?> = _selectedDay.asStateFlow()

    private val _monthSchedule = MutableStateFlow<List<DaySchedule>>(emptyList())
    val monthSchedule: StateFlow<List<DaySchedule>> = _monthSchedule.asStateFlow()

    // Cache for all fetched schedules
    private val _allMonthSchedules = MutableStateFlow<Map<String, List<DaySchedule>>>(emptyMap())
    private val _fetchedDateRanges = MutableStateFlow<Set<String>>(emptySet())
    private val _isFetchingMonthData = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            tokenManager.token.collectLatest { token ->
                token?.let { currentToken ->
                    try {
                        _isLoading.value = true
                        val paramsDeferred = async { repository.fetchScheduleParams(currentToken) }
                        val params = paramsDeferred.await()
                        _scheduleParams.value = params

                        // Fetch schedule data concurrently
                        val coursesDeferred = async { repository.fetchSchedule(currentToken, params) }
                        val courses = coursesDeferred.await()
                        _scheduleData.value = courses

                        // Set initial filter if available
                        if (_selectedFilter.value == null && courses.isNotEmpty()) {
                            val studyYears = courses.map { it.studyYear }.distinct()
                            val semesters = courses.map { it.semester }.distinct().reversed()
                            if (studyYears.isNotEmpty() && semesters.isNotEmpty()) {
                                _selectedFilter.value = studyYears.first() to semesters.first()
                            }
                        }

                        // Fetch month schedule
                        fetchMonthSchedule(currentToken, _currentMonth.value, params)
                    } catch (e: Exception) {
                        // Handle error
                    } finally {
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun toggleScheduleView() {
        _showSchedule.value = !_showSchedule.value
    }

    fun setFilter(filter: Pair<String, String>) {
        _selectedFilter.value = filter
    }

    fun changeMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _currentMonth.value = yearMonth
            _scheduleParams.value?.let { params ->
                // Get the current token and use it
                tokenManager.token.collectLatest { token ->
                    token?.let { currentToken ->
                        fetchMonthSchedule(currentToken, yearMonth, params)
                    }
                }
            }
        }
    }

    fun selectDay(date: LocalDate) {
        _selectedDay.value = date
    }

    fun getScheduleForDay(date: LocalDate): List<DaySchedule> {
        return _monthSchedule.value
            .filter { it.date.startsWith(date.toString()) }
            .sortedBy { formatTimeFromDateTime(it.startTime) }
    }

    private suspend fun fetchMonthSchedule(token: String, yearMonth: YearMonth, params: ScheduleParams) {
        if (_isFetchingMonthData.value) return
        _isFetchingMonthData.value = true
        _isLoading.value = true

        try {
            val firstDayOfMonth = yearMonth.atDay(1)
            val lastDayOfMonth = yearMonth.atEndOfMonth()

            // Calculate which weeks we need to fetch
            val weeksToFetch = mutableListOf<LocalDate>()
            var currentDay = firstDayOfMonth.minusDays(firstDayOfMonth.dayOfWeek.value % 7L)

            // Continue fetching weeks until we've covered the entire month
            while (currentDay.isBefore(lastDayOfMonth) || currentDay.isEqual(lastDayOfMonth)) {
                val weekStartKey = "${currentDay}T00:00:00.000Z"
                if (!_fetchedDateRanges.value.contains(weekStartKey)) {
                    weeksToFetch.add(currentDay)
                    // Add to fetched ranges immediately to prevent duplicate requests
                    _fetchedDateRanges.value = _fetchedDateRanges.value + weekStartKey
                }
                currentDay = currentDay.plusDays(7)
            }

            // Add one more week if the last day of the month is not in the last fetched week
            if (currentDay.minusDays(7).isBefore(lastDayOfMonth)) {
                val weekStartKey = "${currentDay}T00:00:00.000Z"
                if (!_fetchedDateRanges.value.contains(weekStartKey)) {
                    weeksToFetch.add(currentDay)
                    _fetchedDateRanges.value = _fetchedDateRanges.value + weekStartKey
                }
            }

            if (weeksToFetch.isEmpty()) {
                // All weeks for this month have been fetched already
                updateMonthScheduleFromCache(yearMonth)
                _isFetchingMonthData.value = false
                _isLoading.value = false
                return
            }

            // Fetch all needed weeks concurrently
            val weekSchedules = weeksToFetch.map { startDay ->
                viewModelScope.async { repository.fetchWeekSchedule(token, params, startDay) }
            }.awaitAll()

            // Add to our global cache
            val newCache = _allMonthSchedules.value.toMutableMap()
            weekSchedules.flatten().forEach { schedule ->
                val dateKey = schedule.date.split("T")[0] // Get just the date part
                val existingList = newCache[dateKey] ?: emptyList()
                newCache[dateKey] = existingList + schedule
            }
            _allMonthSchedules.value = newCache

            // Update the current month's schedule from the cache
            updateMonthScheduleFromCache(yearMonth)
        } catch (e: Exception) {
            // Handle overall fetch failure
        } finally {
            _isFetchingMonthData.value = false
            _isLoading.value = false
        }
    }

    private fun updateMonthScheduleFromCache(yearMonth: YearMonth) {
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()

        val relevantDates = mutableListOf<String>()
        var currentDay = firstDayOfMonth

        while (currentDay.isBefore(lastDayOfMonth) || currentDay.isEqual(lastDayOfMonth)) {
            relevantDates.add(currentDay.toString())
            currentDay = currentDay.plusDays(1)
        }

        val relevantSchedules = mutableListOf<DaySchedule>()
        for (date in relevantDates) {
            _allMonthSchedules.value[date]?.let { schedules ->
                relevantSchedules.addAll(schedules)
            }
        }

        _monthSchedule.value = relevantSchedules
    }
}