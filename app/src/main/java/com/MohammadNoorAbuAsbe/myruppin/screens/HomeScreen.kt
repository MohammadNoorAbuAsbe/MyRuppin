package com.MohammadNoorAbuAsbe.myruppin.screens

import android.os.CountDownTimer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.MohammadNoorAbuAsbe.myruppin.data.TokenManager
import com.MohammadNoorAbuAsbe.myruppin.data.repository.HomeRepository
import com.MohammadNoorAbuAsbe.myruppin.ui.components.CurrentEventCard
import com.MohammadNoorAbuAsbe.myruppin.ui.components.UpcomingEventCard
import com.MohammadNoorAbuAsbe.myruppin.viewmodels.HomeViewModel
import com.MohammadNoorAbuAsbe.myruppin.viewmodels.HomeViewModelFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Screen dimensions and scaling
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val scaleFactor = screenHeight / 960.dp

    // Scaled text sizes
    val titleSize = (20 * scaleFactor).sp
    val subtitleSize = (16 * scaleFactor).sp
    val bodySize = (14 * scaleFactor).sp
    val gpaSize = (36 * scaleFactor).sp

    // Scaled paddings as Modifiers for easier composition
    val standardPadding = (16 * scaleFactor).dp
    val smallPadding = (8 * scaleFactor).dp
    val tinyPadding = (4 * scaleFactor).dp

    // Scaled component sizes
    val iconSize = (48 * scaleFactor).dp
    val warningIconSize = (16 * scaleFactor).dp
    val cardElevation = 2 * scaleFactor

    // Context and dependencies
    val context = LocalContext.current
    val tokenManager = TokenManager(context)
    val client = remember { OkHttpClient() }
    val repository = remember { HomeRepository(client) }
    val viewModelFactory = remember { HomeViewModelFactory(repository, tokenManager) }
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)

    val token by tokenManager.token.collectAsState(initial = null)

    // Collect states
    val currentEvent by viewModel.currentEvent.collectAsState()
    val nextEvent by viewModel.nextEvent.collectAsState()
    val isLoadingEvent by viewModel.isLoadingEvent.collectAsState()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
    val isLoadingUpcoming by viewModel.isLoadingUpcoming.collectAsState()
    val error by viewModel.error.collectAsState()
    val logoutComplete by viewModel.logoutComplete.collectAsState()
    val userName by viewModel.userName.collectAsState()

    // Drawer state
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Timer state
    var remainingTime by remember { mutableStateOf("") }
    var countdownLabel by remember { mutableStateOf("Ends In") }

    // Update the timer using CountDownTimer
    LaunchedEffect(currentEvent, nextEvent) {
        val now = LocalTime.now()
        val eventTimeString = currentEvent?.endTime?.trim() ?: nextEvent?.startTime?.trim()
        countdownLabel = if (currentEvent != null) "Ends In" else "Starts In"
        println(eventTimeString)
        if (eventTimeString != null) {
            try {
                val eventTime = LocalTime.parse(eventTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"))
                val duration = Duration.between(now, eventTime)

                val timer = object : CountDownTimer(duration.toMillis(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val hours = millisUntilFinished / 3600000
                        val minutes = (millisUntilFinished % 3600000) / 60000
                        val seconds = (millisUntilFinished % 60000) / 1000
                        remainingTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    }

                    override fun onFinish() {
                        remainingTime = "Event ended"
                        viewModel.fetchEvents(token!!)
                    }
                }
                timer.start()
            } catch (e: DateTimeParseException) {
                remainingTime = "Invalid time format"
            }
        } else {
            remainingTime = "No event time provided"
        }
    }

    LaunchedEffect(logoutComplete) {
        if (logoutComplete) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Drawer header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(standardPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Menu",
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                // Drawer items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Grades") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("grades")
                        }
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text("Schedule") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("schedule")
                        }
                    }
                )

                HorizontalDivider()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Credits") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("credits")
                        }
                    }
                )

            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Home") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        Text(
                            text = userName ?: "Loading...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                    // Current Event Section
                    CurrentEventCard(
                        currentEvent = currentEvent ?: nextEvent,
                        isLoading = isLoadingEvent,
                        titleSize = titleSize,
                        subtitleSize = subtitleSize,
                        bodySize = bodySize,
                        standardPadding = standardPadding,
                        smallPadding = smallPadding,
                        iconSize = iconSize,
                        title = when {
                            currentEvent != null -> "Current Event"
                            else -> "Next Event"
                        },
                        remainingTime = remainingTime, // Pass remainingTime to CurrentEventCard
                        countdownLabel = countdownLabel // Pass countdownLabel to CurrentEventCard
                    )

                // Upcoming Events Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(standardPadding),
                    verticalArrangement = Arrangement.spacedBy(smallPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Upcoming Major Events",
                        fontSize = titleSize,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = smallPadding)
                    )

                    if (isLoadingUpcoming) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier =  Modifier.size(iconSize),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (upcomingEvents.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(standardPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No upcoming events",
                                    fontSize = bodySize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    // Scale the height of upcoming events based on screen size
                                    minOf(
                                        upcomingEvents.size * (60 * scaleFactor).dp,
                                        4 * (60 * scaleFactor).dp
                                    )
                                )
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(smallPadding)
                            ) {
                                itemsIndexed(upcomingEvents) { _, event ->
                                    UpcomingEventCard(
                                        event = event,
                                        subtitleSize = subtitleSize,
                                        bodySize = bodySize,
                                        smallPadding = smallPadding,
                                        tinyPadding = tinyPadding,
                                        warningIconSize = warningIconSize
                                    )
                                }
                            }
                        }
                    }
                }

                // Logout Button
                Button(
                    onClick = {
                        viewModel.logout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(standardPadding)
                ) {
                    Text("Logout", fontSize = bodySize)
                }
            }
        }
    }
}

