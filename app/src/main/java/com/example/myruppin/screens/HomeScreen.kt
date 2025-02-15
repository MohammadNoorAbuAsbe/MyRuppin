package com.example.myruppin.screens

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.navigation.NavController
import com.example.myruppin.data.TokenManager
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState

data class EventInfo(
    val title: String,
    val place: String,
    val startTime: String,
    val endTime: String
)

data class UpcomingEvent(
    val title: String,
    val date: String,
    val type: String = "",
    val isExam: Boolean
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Calculate scaling factors based on screen size
    // Using 960.dp as a baseline height
    val scaleFactor = screenHeight / 960.dp

    // Scale text sizes
    val titleSize = (20 * scaleFactor).sp
    val subtitleSize = (16 * scaleFactor).sp
    val bodySize = (14 * scaleFactor).sp
    val gpaSize = (36 * scaleFactor).sp

    // Scale padding
    val standardPadding = (16 * scaleFactor).dp
    val smallPadding = (8 * scaleFactor).dp
    val tinyPadding = (4 * scaleFactor).dp

    // Scale component sizes
    val iconSize = (48 * scaleFactor).dp
    val warningIconSize = (16 * scaleFactor).dp
    val cardElevation = (2 * scaleFactor).dp
    val context = LocalContext.current
    val tokenManager = TokenManager(context)
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    var gpa by remember { mutableStateOf("--") }
    var currentEvent by remember { mutableStateOf<EventInfo?>(null) }
    var isLoadingGpa by remember { mutableStateOf(true) }
    var isLoadingEvent by remember { mutableStateOf(true) }
    var upcomingEvents by remember { mutableStateOf<List<UpcomingEvent>>(emptyList()) }
    var isLoadingUpcoming by remember { mutableStateOf(true) }


    // Collect the token
    val token by tokenManager.token.collectAsState(initial = null)
    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Function to fetch current event
    fun fetchCurrentEvent(currentToken: String) {
        val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val jsonBody = JSONObject().apply {
            put("fromDate", "${today.split('T')[0]}T00:00:00")
            put("toDate", "${today.split('T')[0]}T00:00:00")
        }

        val request = Request.Builder()
            .url("https://ruppinet.ruppin.ac.il/Portals/api/Home/ScheduleData")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $currentToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                scope.launch {
                    isLoadingEvent = false
                    currentEvent = null
                }
            }

            override fun onResponse(call: Call, response: Response) {
                scope.launch {
                    try {
                        val responseBody = response.body?.string()
                        val jsonResponse = JSONObject(responseBody ?: "")
                        val eventsArray = jsonResponse.optJSONArray("events")

                        if (eventsArray != null && eventsArray.length() > 0) {
                            val firstEvent = eventsArray.getJSONObject(0)
                            val eventData = firstEvent.getJSONObject("data")

                            currentEvent = EventInfo(
                                title = eventData.optString("title", "No title"),
                                place = eventData.optString("place", "No location"),
                                startTime = eventData.optString("startTime", "").split('T')[1].substring(0, 5),
                                endTime = eventData.optString("endTime", "").split('T')[1].substring(0, 5)
                            )
                        } else {
                            currentEvent = null
                        }
                        isLoadingEvent = false
                    } catch (e: Exception) {
                        currentEvent = null
                        isLoadingEvent = false
                    }
                }
            }
        })
    }

    fun fetchUpcomingEvents(currentToken: String) {
        val request = Request.Builder()
            .url("https://ruppinet.ruppin.ac.il/Portals/api/Home/Data")
            .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $currentToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                scope.launch {
                    isLoadingUpcoming = false
                    upcomingEvents = emptyList()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                scope.launch {
                    try {
                        val responseBody = response.body?.string()
                        val jsonResponse = JSONObject(responseBody ?: "")
                        val eventsArray = jsonResponse.optJSONArray("events")

                        val events = mutableListOf<UpcomingEvent>()

                        for (i in 0 until (eventsArray?.length() ?: 0)) {
                            val event = eventsArray?.getJSONObject(i)
                            event?.let {
                                val date = it.optString("date", "")
                                    .split("T")[0] // Get just the date part
                                    .split("-") // Split into year, month, day
                                    .let { parts -> "${parts[2]}/${parts[1]}/${parts[0]}" } // Format as dd/mm/yyyy

                                events.add(UpcomingEvent(
                                    title = it.optString("title", "No title"),
                                    date = date,
                                    type = it.optString("type", ""),
                                    isExam = it.optString("type") == "StudentExams"
                                ))
                            }
                        }

                        upcomingEvents = events
                        isLoadingUpcoming = false
                    } catch (e: Exception) {
                        upcomingEvents = emptyList()
                        isLoadingUpcoming = false
                    }
                }
            }
        })
    }

    // Fetch data when screen loads
    LaunchedEffect(token) {
        token?.let { currentToken ->
            // Fetch GPA
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Home/AverageData")
                .post(ByteArray(0).toRequestBody(null))
                .header("Authorization", "Bearer $currentToken")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    scope.launch {
                        isLoadingGpa = false
                        gpa = "Error"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    scope.launch {
                        try {
                            val responseBody = response.body?.string()
                            val jsonResponse = JSONObject(responseBody ?: "")
                            gpa = jsonResponse.optString("average", "--")
                            isLoadingGpa = false
                        } catch (e: Exception) {
                            gpa = "Error"
                            isLoadingGpa = false
                        }
                    }
                }
            })

            fetchCurrentEvent(currentToken)
            fetchUpcomingEvents(currentToken)
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
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
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
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                    label = { Text("Schedule") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            // Add navigation logic here
                        }
                    }
                )

                // Add more drawer items as needed
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
                            Icon(Icons.Default.Warning, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            // Your existing content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // GPA Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "GPA",
                        fontSize = titleSize,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = smallPadding)
                    )
                    if (isLoadingGpa) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(iconSize),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = gpa,
                            fontSize = gpaSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Current Event Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(standardPadding)
                ) {
                    Text(
                        text = "Current Event",
                        fontSize = titleSize,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = smallPadding)
                    )
                    if (isLoadingEvent) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(iconSize),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(smallPadding),
                            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
                        ) {
                            if (currentEvent != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(standardPadding),
                                    verticalArrangement = Arrangement.spacedBy(smallPadding)
                                ) {
                                    Text(
                                        text = currentEvent!!.title,
                                        fontSize = subtitleSize,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Location: ${currentEvent!!.place}",
                                        fontSize = bodySize
                                    )
                                    Text(
                                        text = "Time: ${currentEvent!!.startTime} - ${currentEvent!!.endTime}",
                                        fontSize = bodySize
                                    )
                                }
                            } else {
                                Text(
                                    text = "No events scheduled for today",
                                    fontSize = bodySize,
                                    modifier = Modifier.padding(standardPadding),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Upcoming Events Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(standardPadding),
                    verticalArrangement = Arrangement.spacedBy(smallPadding)
                ) {
                    Text(
                        text = "Upcoming Events",
                        fontSize = titleSize,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = smallPadding)
                    )

                    if (isLoadingUpcoming) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(iconSize),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (upcomingEvents.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
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
                                    minOf(upcomingEvents.size * (60 * scaleFactor).dp, 4 * (60 * scaleFactor).dp)
                                )
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(smallPadding)
                            ) {
                                itemsIndexed(upcomingEvents) { _, event ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (event.isExam)
                                                MaterialTheme.colorScheme.errorContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(smallPadding),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(tinyPadding)
                                            ) {
                                                Text(
                                                    text = event.title,
                                                    fontSize = subtitleSize,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (event.isExam)
                                                        MaterialTheme.colorScheme.onErrorContainer
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = event.date,
                                                    fontSize = bodySize,
                                                    color = if (event.isExam)
                                                        MaterialTheme.colorScheme.onErrorContainer
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (event.isExam) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Exam",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(warningIconSize)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


            // Logout Button
            Button(
                onClick = {
                    scope.launch {
                        tokenManager.clearAll()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(standardPadding)
            ) {
                Text(
                    "Logout",
                    fontSize = bodySize
                )
            }
        }
        }
    }
}