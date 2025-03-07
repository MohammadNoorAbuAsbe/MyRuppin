package com.example.myruppin.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myruppin.data.TokenManager
import com.example.myruppin.data.repository.ScheduleRepository
import com.example.myruppin.ui.components.CalendarGridView
import com.example.myruppin.ui.components.CourseList
import com.example.myruppin.ui.components.DayScheduleList
import com.example.myruppin.viewmodels.ScheduleViewModel
import com.example.myruppin.viewmodels.ScheduleViewModelFactory
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val client = remember { OkHttpClient() }
    val repository = remember { ScheduleRepository(client) }

    // Create ViewModel with factory
    val viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(repository, tokenManager)
    )

    // Collect state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val showSchedule by viewModel.showSchedule.collectAsState()
    val scheduleData by viewModel.scheduleData.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()

    // Local UI state
    var filterExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Toggle Button
            Button(onClick = { viewModel.toggleScheduleView() }) {
                Text(if (showSchedule) "Show Calendar View" else "Show Schedule View")
            }

            if (showSchedule) {
                // Schedule View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val studyYears = scheduleData.map { it.studyYear }.distinct()
                    val semesters = scheduleData.map { it.semester }.distinct().reversed()
                    val combinedFilters = studyYears.flatMap { year ->
                        semesters.map { semester -> year to semester }
                    }

                    TextButton(onClick = { filterExpanded = !filterExpanded }) {
                        Text("Filter: ${selectedFilter?.let { "${it.first} - ${it.second}" } ?: "None"}")
                    }
                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        combinedFilters.forEach { (year, semester) ->
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.setFilter(year to semester)
                                    filterExpanded = false
                                },
                                text = { Text(text = "$year - $semester") }
                            )
                        }
                    }
                }

                // Schedule List
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    val filteredCourses = scheduleData.filter { course ->
                        selectedFilter == null || (course.studyYear == selectedFilter!!.first && course.semester == selectedFilter!!.second)
                    }

                    CourseList(
                        courses = filteredCourses,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Calendar View
                CalendarGridView(
                    currentMonth = currentMonth,
                    onMonthChanged = { viewModel.changeMonth(it) },
                    onDaySelected = { viewModel.selectDay(it) },
                    selectedDay = selectedDay,
                    hasEvents = { date -> viewModel.getScheduleForDay(date).isNotEmpty() }
                )

                // Display events for selected day
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    selectedDay?.let { date ->
                        val daySchedule = viewModel.getScheduleForDay(date)
                        DayScheduleList(
                            daySchedules = daySchedule,
                            formatTimeFromDateTime = { dateTimeStr ->
                                try {
                                    if (dateTimeStr.length >= 16) {
                                        dateTimeStr.substring(11, 16)
                                    } else {
                                        dateTimeStr
                                    }
                                } catch (e: Exception) {
                                    dateTimeStr
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true)
                        )
                    } ?: run {
                        Text(
                            text = "Select a day to view events",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

//HERE IS AN OLDER CODE THAT WORKS COMPLETELY FINE BUT IS HARD TO READ AND WAS REPLACED WITH SEPARATE FILES
//TO HANDLE EACH MATTER ALONE, THIS DOES EVERYTHING AT ONCE
//package com.example.myruppin.screens

//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import kotlinx.coroutines.launch
//import okhttp3.*
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//import java.io.IOException
//import com.example.myruppin.data.TokenManager
//import java.time.LocalDate
//import java.time.YearMonth
//import java.time.format.TextStyle
//import java.util.Locale
//
//data class Course2(
//    val name: String,
//    val instructor: String,
//    val startTime: String,
//    val endTime: String,
//    val day: String,
//    val location: String,
//    val semester: String,
//    val studyYear: String
//)
//
//data class ScheduleParams(
//    val hash: String,
//    val pt: Int,
//    val ptMsl: Int,
//    val shl: Int
//)
//
//data class DaySchedule(
//    val date: String,
//    val title: String,
//    val startTime: String,
//    val endTime: String,
//    val place: String?,
//    val moreInfo: String?
//)
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ScheduleScreen(navController: NavController) {
//    val scope = rememberCoroutineScope()
//    val client = remember { OkHttpClient() }
//    var scheduleData by remember { mutableStateOf<List<Course2>>(emptyList()) }
//    var isLoading by remember { mutableStateOf(true) }
//    val context = LocalContext.current
//    val tokenManager = TokenManager(context)
//    val token by tokenManager.token.collectAsState(initial = null)
//
//    // State for combined filter
//    var selectedFilter by remember { mutableStateOf<Pair<String, String>?>(null) }
//    var filterExpanded by remember { mutableStateOf(false) }
//
//    // State for toggling content
//    var showSchedule by remember { mutableStateOf(false) }
//
//    // Store all fetched schedules with date as key
//    var allMonthSchedules by remember { mutableStateOf<Map<String, List<DaySchedule>>>(emptyMap()) }
//
//    // Track which date ranges have been fetched to avoid duplicates
//    var fetchedDateRanges by remember { mutableStateOf<Set<String>>(emptySet()) }
//
//    // Current month's schedule data
//    var monthSchedule by remember { mutableStateOf<List<DaySchedule>>(emptyList()) }
//
//    // Track if we're currently fetching data
//    var isFetchingMonthData by remember { mutableStateOf(false) }
//
//    // Current month for calendar view
//    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
//
//    // Selected day in calendar
//    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
//
//    // Schedule parameters
//    var scheduleParams by remember { mutableStateOf<ScheduleParams?>(null) }
//
//    // Function to extract just the time portion from a datetime string
//    fun formatTimeFromDateTime(dateTimeStr: String): String {
//        return try {
//            // Check if the string has the expected format
//            if (dateTimeStr.length >= 16) {
//                // Extract just the time portion (HH:MM)
//                dateTimeStr.substring(11, 16)
//            } else {
//                dateTimeStr // Return as is if format is unexpected
//            }
//        } catch (e: Exception) {
//            // Return original string if any error occurs
//            dateTimeStr
//        }
//    }
//    // Function to get schedule for a specific day, sorted by start time
//    fun getScheduleForDay(date: LocalDate): List<DaySchedule> {
//        return monthSchedule
//            .filter { it.date.startsWith(date.toString()) }
//            .sortedBy { formatTimeFromDateTime(it.startTime) } // Sort by start time
//    }
//
//    fun parseDaySchedule(jsonResponse: JSONObject): List<DaySchedule> {
//        val clientData = jsonResponse.getJSONObject("scheduleViewItemWeek").getJSONArray("clientData")
//        val schedules = mutableListOf<DaySchedule>()
//
//        for (i in 0 until clientData.length()) {
//            val item = clientData.getJSONObject(i)
//            val schedule = DaySchedule(
//                date = item.getString("date"),
//                title = item.getString("title"),
//                startTime = item.getString("mar_full_start"),
//                endTime = item.getString("mar_full_end"),
//                place = item.optString("place"),
//                moreInfo = item.optString("moreinfo")
//            )
//            schedules.add(schedule)
//        }
//        return schedules
//    }
//    // Function to update the current month's schedule from the cache
//    fun updateMonthScheduleFromCache(yearMonth: YearMonth) {
//        val firstDayOfMonth = yearMonth.atDay(1)
//        val lastDayOfMonth = yearMonth.atEndOfMonth()
//
//        val relevantDates = mutableListOf<String>()
//        var currentDay = firstDayOfMonth
//
//        while (currentDay.isBefore(lastDayOfMonth) || currentDay.isEqual(lastDayOfMonth)) {
//            relevantDates.add(currentDay.toString())
//            currentDay = currentDay.plusDays(1)
//        }
//
//        val relevantSchedules = mutableListOf<DaySchedule>()
//        for (date in relevantDates) {
//            allMonthSchedules[date]?.let { schedules ->
//                relevantSchedules.addAll(schedules)
//            }
//        }
//
//        monthSchedule = relevantSchedules
//    }
//
//
//    // Enhanced function to fetch month schedule with tracking of fetched date ranges
//    fun fetchMonthSchedule(currentToken: String, yearMonth: YearMonth, params: ScheduleParams) {
//        if (isFetchingMonthData) return
//        isFetchingMonthData = true
//        isLoading = true
//
//        val firstDayOfMonth = yearMonth.atDay(1)
//        val lastDayOfMonth = yearMonth.atEndOfMonth()
//
//        // Calculate which weeks we need to fetch
//        val weeksToFetch = mutableListOf<LocalDate>()
//
//        // Start with the first day of the month
//        var currentDay = firstDayOfMonth
//
//        // Get the first day of the week containing the first day of the month
//        // This ensures we fetch the entire first week even if it starts in the previous month
//        val firstDayOfFirstWeek = currentDay.minusDays(currentDay.dayOfWeek.value % 7L)
//        currentDay = firstDayOfFirstWeek
//
//        // Continue fetching weeks until we've covered the entire month
//        while (currentDay.isBefore(lastDayOfMonth) || currentDay.isEqual(lastDayOfMonth)) {
//            val weekStartKey = "${currentDay}T00:00:00.000Z"
//            if (!fetchedDateRanges.contains(weekStartKey)) {
//                weeksToFetch.add(currentDay)
//                // Add to fetched ranges immediately to prevent duplicate requests
//                fetchedDateRanges = fetchedDateRanges + weekStartKey
//            }
//            currentDay = currentDay.plusDays(7)
//        }
//
//        // Add one more week if the last day of the month is not in the last fetched week
//        // This ensures we don't miss any days at the end of the month
//        if (currentDay.minusDays(7).isBefore(lastDayOfMonth)) {
//            val weekStartKey = "${currentDay}T00:00:00.000Z"
//            if (!fetchedDateRanges.contains(weekStartKey)) {
//                weeksToFetch.add(currentDay)
//                fetchedDateRanges = fetchedDateRanges + weekStartKey
//            }
//        }
//
//        if (weeksToFetch.isEmpty()) {
//            // All weeks for this month have been fetched already
//            // Just update the monthSchedule with the relevant data from allMonthSchedules
//            updateMonthScheduleFromCache(yearMonth)
//            isFetchingMonthData = false
//            isLoading = false
//            return
//        }
//
//        val url = "https://ruppinet.ruppin.ac.il/Portals/api/StudentScheduleCommon/DateChanged"
//        var completedFetches = 0
//
//        for (startDay in weeksToFetch) {
//            val body = JSONObject().apply {
//                put("_ScheduleParams", JSONObject().apply {
//                    put("__hash", params.hash)
//                    put("pt", params.pt)
//                    put("ptMsl", params.ptMsl)
//                    put("shl", params.shl)
//                })
//                put("date", "${startDay}T00:00:00.000Z")
//            }.toString().toRequestBody("application/json".toMediaType())
//
//            client.newCall(Request.Builder()
//                .url(url)
//                .post(body)
//                .header("Authorization", "Bearer $currentToken")
//                .build()).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    scope.launch {
//                        completedFetches++
//                        if (completedFetches == weeksToFetch.size) {
//                            updateMonthScheduleFromCache(yearMonth)
//                            isFetchingMonthData = false
//                            isLoading = false
//                        }
//                    }
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    scope.launch {
//                        try {
//                            if (response.isSuccessful) {
//                                val responseString = response.body!!.string()
//                                val jsonResponse = JSONObject(responseString)
//                                val weekSchedule = parseDaySchedule(jsonResponse)
//
//                                // Add to our global cache
//                                val newCache = allMonthSchedules.toMutableMap()
//                                for (schedule in weekSchedule) {
//                                    val dateKey = schedule.date.split("T")[0] // Get just the date part
//                                    val existingList = newCache[dateKey] ?: emptyList()
//                                    newCache[dateKey] = existingList + schedule
//                                }
//                                allMonthSchedules = newCache
//                            }
//                        } catch (e: Exception) {
//                            // Handle exception
//                        } finally {
//                            completedFetches++
//                            if (completedFetches == weeksToFetch.size) {
//                                updateMonthScheduleFromCache(yearMonth)
//                                isFetchingMonthData = false
//                                isLoading = false
//                            }
//                        }
//                    }
//                }
//            })
//        }
//    }
//
//
//    fun parseScheduleData(jsonResponse: JSONObject): List<Course2> {
//        val courses = mutableListOf<Course2>()
//        val clientData = jsonResponse.getJSONObject("scheduleViewItemSms").getJSONArray("clientData")
//        for (i in 0 until clientData.length()) {
//            val item = clientData.getJSONObject(i)
//            fun parseTimeString(timeStr: String): String {
//                return try {
//                    if (timeStr.length >= 16) {
//                        timeStr.substring(11, 16)
//                    } else {
//                        timeStr
//                    }
//                } catch (e: Exception) {
//                    "00:00"
//                }
//            }
//            val course = Course2(
//                name = item.getString("krs_shm"),
//                instructor = item.getString("pm_shm"),
//                startTime = parseTimeString(item.optString("krs_moed_meshaa", "00:00")),
//                endTime = parseTimeString(item.optString("krs_moed_adshaa", "00:00")),
//                day = item.optString("krs_moed_yom", "Unknown"),
//                location = item.optString("hdr_shm", "Unknown"),
//                semester = item.optString("krs_moed_sms", "Unknown"),
//                studyYear = item.optString("krs_snl", "Unknown")
//            )
//            courses.add(course)
//        }
//        return courses.sortedBy { it.startTime }
//    }
//
//    fun fetchSchedule(currentToken: String, params: ScheduleParams) {
//        val url = "https://ruppinet.ruppin.ac.il/Portals/api/StudentScheduleCommon/GetSchedule"
//        val body = JSONObject().apply {
//            put("__hash", params.hash)
//            put("pt", params.pt)
//            put("ptMsl", params.ptMsl)
//            put("shl", params.shl)
//        }.toString().toRequestBody("application/json".toMediaType())
//
//        val request = Request.Builder()
//            .url(url)
//            .post(body)
//            .header("Authorization", "Bearer $currentToken")
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                scope.launch {
//                    scheduleData = emptyList()
//                    isLoading = false
//                }
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                scope.launch {
//                    try {
//                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
//                        val responseString = response.body!!.string()
//                        val jsonResponse = JSONObject(responseString)
//                        scheduleData = parseScheduleData(jsonResponse)
//                    } catch (e: Exception) {
//                        scheduleData = emptyList()
//                    } finally {
//                        isLoading = false
//                    }
//                }
//            }
//        })
//    }
//
//    fun fetchScheduleParams(currentToken: String, onParamsFetched: (ScheduleParams) -> Unit) {
//        val request = Request.Builder()
//            .url("https://ruppinet.ruppin.ac.il/Portals/api/StudentSchedule/Data")
//            .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
//            .header("Authorization", "Bearer $currentToken")
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                scope.launch {
//                    isLoading = false
//                }
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                scope.launch {
//                    try {
//                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
//                        val responseString = response.body!!.string()
//                        val jsonResponse = JSONObject(responseString)
//                        val scheduleParamsJson = jsonResponse.getJSONObject("_ScheduleParams")
//                        val params = ScheduleParams(
//                            hash = scheduleParamsJson.getString("__hash"),
//                            pt = scheduleParamsJson.getInt("pt"),
//                            ptMsl = scheduleParamsJson.getInt("ptMsl"),
//                            shl = scheduleParamsJson.getInt("shl")
//                        )
//                        scheduleParams = params
//                        onParamsFetched(params)
//                    } catch (e: Exception) {
//                        isLoading = false
//                    }
//                }
//            }
//        })
//    }
//
//    @Composable
//    fun CalendarGridView(
//        currentMonth: YearMonth,
//        onMonthChanged: (YearMonth) -> Unit,
//        onDaySelected: (LocalDate) -> Unit,
//        selectedDay: LocalDate?
//    ) {
//        val today = LocalDate.now()
//
//        Column(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Top
//        ) {
//            // Month Navigation - reduce vertical padding
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 4.dp), // Reduced from default
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                IconButton(onClick = {
//                    onMonthChanged(currentMonth.minusMonths(1))
//                }) {
//                    Text("<")
//                }
//                Text(
//                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
//                    style = MaterialTheme.typography.titleMedium
//                )
//                IconButton(onClick = {
//                    onMonthChanged(currentMonth.plusMonths(1))
//                }) {
//                    Text(">")
//                }
//            }
//
//            // Days of the Week Header - reduce vertical padding
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 2.dp), // Reduced padding
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
//                    Text(text = day, style = MaterialTheme.typography.bodySmall)
//                }
//            }
//
//            // Days Grid - reduce height
//            val daysInMonth = currentMonth.lengthOfMonth()
//            val firstDayOfMonth = LocalDate.of(currentMonth.year, currentMonth.month, 1).dayOfWeek.value % 7
//            val totalCells = daysInMonth + firstDayOfMonth
//
//            LazyVerticalGrid(
//                columns = GridCells.Fixed(7),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(220.dp), // Reduced from 300.dp to 220.dp
//                contentPadding = PaddingValues(0.dp) // No extra padding
//            ) {
//                items(totalCells) { index ->
//                    if (index < firstDayOfMonth) {
//                        Box(modifier = Modifier.size(36.dp)) // Slightly smaller
//                    } else {
//                        val day = index - firstDayOfMonth + 1
//                        val date = LocalDate.of(currentMonth.year, currentMonth.month, day)
//                        val hasEvents = getScheduleForDay(date).isNotEmpty()
//                        val isPastDay = date.isBefore(today)
//                        val isToday = date.isEqual(today)
//                        val isSelected = selectedDay == date
//
//                        Box(
//                            modifier = Modifier
//                                .size(36.dp) // Slightly smaller
//                                .padding(2.dp) // Reduced padding
//                                .background(
//                                    color = when {
//                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
//                                        isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
//                                        else -> MaterialTheme.colorScheme.background
//                                    },
//                                    shape = CircleShape
//                                )
//                                .clickable {
//                                    onDaySelected(date)
//                                },
//                            contentAlignment = Alignment.Center
//                        ) {
//                            // Day number
//                            Text(
//                                text = day.toString(),
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = when {
//                                    isSelected -> MaterialTheme.colorScheme.primary
//                                    isToday -> MaterialTheme.colorScheme.secondary
//                                    isPastDay -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
//                                    else -> MaterialTheme.colorScheme.onBackground
//                                }
//                            )
//
//                            // Indicator for days with events
//                            if (hasEvents) {
//                                Box(
//                                    modifier = Modifier
//                                        .size(4.dp) // Slightly smaller
//                                        .align(Alignment.BottomCenter)
//                                        .padding(bottom = 1.dp) // Reduced padding
//                                        .background(
//                                            color = when {
//                                                isSelected -> MaterialTheme.colorScheme.onPrimary
//                                                isToday -> MaterialTheme.colorScheme.onSecondary
//                                                isPastDay -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
//                                                else -> MaterialTheme.colorScheme.primary
//                                            },
//                                            shape = CircleShape
//                                        )
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//
//
//    // Effect to fetch schedule params and initial data when token is available
//    LaunchedEffect(token) {
//        token?.let { currentToken ->
//            fetchScheduleParams(currentToken) { params ->
//                fetchSchedule(currentToken, params)
//                fetchMonthSchedule(currentToken, currentMonth, params)
//            }
//        }
//    }
//
//    // Effect to fetch data when month changes
//    LaunchedEffect(currentMonth) {
//        scheduleParams?.let { params ->
//            token?.let { currentToken ->
//                fetchMonthSchedule(currentToken, currentMonth, params)
//            }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("My Schedule") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Top
//        ) {
//            // Toggle Button
//            Button(onClick = { showSchedule = !showSchedule }) {
//                Text(if (showSchedule) "Show Calendar View" else "Show Schedule View")
//            }
//
//            if (showSchedule) {
//                // Schedule View
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    val studyYears = scheduleData.map { it.studyYear }.distinct()
//                    val semesters = scheduleData.map { it.semester }.distinct().reversed()
//                    val combinedFilters = studyYears.flatMap { year ->
//                        semesters.map { semester -> year to semester }
//                    }
//
//                    // Set the initial filter to the first available option
//                    if (selectedFilter == null && combinedFilters.isNotEmpty()) {
//                        selectedFilter = combinedFilters.first()
//                    }
//
//                    TextButton(onClick = { filterExpanded = !filterExpanded }) {
//                        Text("Filter: ${selectedFilter?.let { "${it.first} - ${it.second}" } ?: "None"}")
//                    }
//                    DropdownMenu(
//                        expanded = filterExpanded,
//                        onDismissRequest = { filterExpanded = false }
//                    ) {
//                        combinedFilters.forEach { (year, semester) ->
//                            DropdownMenuItem(
//                                onClick = {
//                                    selectedFilter = year to semester
//                                    filterExpanded = false
//                                },
//                                text = { Text(text = "$year - $semester") }
//                            )
//                        }
//                    }
//                }
//
//                // Schedule List
//                if (isLoading) {
//                    CircularProgressIndicator(
//                        modifier = Modifier.align(Alignment.CenterHorizontally)
//                    )
//                } else {
//                    LazyColumn {
//                        val filteredCourses = scheduleData.filter { course ->
//                            selectedFilter == null || (course.studyYear == selectedFilter!!.first && course.semester == selectedFilter!!.second)
//                        }
//
//                        // Define the order of days from Sunday to Saturday
//                        val dayOrder = listOf("א", "ב", "ג", "ד", "ה", "ו", "ת")
//
//                        // Group courses by day and sort by the defined order
//                        val groupedCourses = filteredCourses.filter { it.day != "null" }.groupBy { it.day }
//                            .toSortedMap(compareBy { dayOrder.indexOf(it) })
//
//                        groupedCourses.forEach { (day, courses) ->
//                            item {
//                                Text(
//                                    text = "Day: $day",
//                                    style = MaterialTheme.typography.titleMedium,
//                                    modifier = Modifier.padding(8.dp)
//                                )
//                            }
//                            items(courses) { course ->
//                                Card(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(8.dp),
//                                    colors = CardDefaults.cardColors(
//                                        containerColor = MaterialTheme.colorScheme.primaryContainer
//                                    )
//                                ) {
//                                    Column(modifier = Modifier.padding(8.dp)) {
//                                        Row(
//                                            modifier = Modifier.fillMaxWidth(),
//                                            horizontalArrangement = Arrangement.End
//                                        ) {
//                                            Text(
//                                                text = course.name,
//                                                style = MaterialTheme.typography.bodyLarge,
//                                                maxLines = 1,
//                                                overflow = TextOverflow.Ellipsis
//                                            )
//                                        }
//
//                                        Text(
//                                            text = "Instructor: ${course.instructor}",
//                                            style = MaterialTheme.typography.bodySmall
//                                        )
//
//                                        Text(
//                                            text = "Time: ${course.startTime} - ${course.endTime}",
//                                            style = MaterialTheme.typography.bodySmall
//                                        )
//
//                                        Text(
//                                            text = "Location: ${course.location}",
//                                            style = MaterialTheme.typography.bodySmall
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            } else {
//                // Calendar View
//                CalendarGridView(
//                    currentMonth = currentMonth,
//                    onMonthChanged = { newMonth ->
//                        currentMonth = newMonth
//                    },
//                    onDaySelected = { date ->
//                        selectedDay = date
//                    },
//                    selectedDay = selectedDay
//                )
//
//                // Display events for selected day
//                if (isLoading) {
//                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
//                } else {
//                    // Inside the ScheduleScreen composable, replace the LazyColumn for displaying selected day events with this:
//                    selectedDay?.let { date ->
//                        val daySchedule = getScheduleForDay(date)
//                        if (daySchedule.isEmpty()) {
//                            Text(
//                                text = "No events scheduled for this day",
//                                style = MaterialTheme.typography.bodyMedium,
//                                modifier = Modifier.padding(8.dp)
//                            )
//                        } else {
//                            // Add a Box with weight to make the LazyColumn take available space
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .weight(1f, fill = true) // This makes the Box take available space
//                            ) {
//                                LazyColumn(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .fillMaxHeight() // Fill the Box height
//                                        .padding(horizontal = 8.dp)
//                                ) {
//                                    items(daySchedule) { schedule ->
//                                        Card(
//                                            modifier = Modifier
//                                                .fillMaxWidth()
//                                                .padding(vertical = 4.dp),
//                                            colors = CardDefaults.cardColors(
//                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
//                                            )
//                                        ) {
//                                            Column(modifier = Modifier.padding(12.dp)) {
//                                                Row(
//                                                    modifier = Modifier.fillMaxWidth(),
//                                                    horizontalArrangement = Arrangement.End
//                                                ) {
//                                                    Text(
//                                                        text = schedule.title,
//                                                        style = MaterialTheme.typography.titleSmall
//                                                    )
//                                                }
//                                                Text(
//                                                    text = "${formatTimeFromDateTime(schedule.startTime)} - ${formatTimeFromDateTime(schedule.endTime)}",
//                                                    style = MaterialTheme.typography.bodySmall
//                                                )
//                                                if (!schedule.place.isNullOrBlank()) {
//                                                    Text(
//                                                        text = "Location: ${schedule.place}",
//                                                        style = MaterialTheme.typography.bodySmall
//                                                    )
//                                                }
//                                                if (!schedule.moreInfo.isNullOrBlank()) {
//                                                    Text(
//                                                        text = "Instructor: ${schedule.moreInfo}",
//                                                        style = MaterialTheme.typography.bodySmall
//                                                    )
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    } ?: run {
//                        Text(
//                            text = "Select a day to view events",
//                            style = MaterialTheme.typography.bodyMedium,
//                            modifier = Modifier.padding(16.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}