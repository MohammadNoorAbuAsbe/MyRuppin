package com.MohammadNoorAbuAsbe.myruppin.screens

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
import com.MohammadNoorAbuAsbe.myruppin.data.TokenManager
import com.MohammadNoorAbuAsbe.myruppin.data.repository.ScheduleRepository
import com.MohammadNoorAbuAsbe.myruppin.ui.components.CalendarGridView
import com.MohammadNoorAbuAsbe.myruppin.ui.components.CourseList
import com.MohammadNoorAbuAsbe.myruppin.ui.components.DayScheduleList
import com.MohammadNoorAbuAsbe.myruppin.viewmodels.ScheduleViewModel
import com.MohammadNoorAbuAsbe.myruppin.viewmodels.ScheduleViewModelFactory
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
    val monthSchedule by viewModel.monthSchedule.collectAsState()

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