package com.MohammadNoorAbuAsbe.myruppin.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.MohammadNoorAbuAsbe.myruppin.data.repository.GradesRepository
import com.MohammadNoorAbuAsbe.myruppin.ui.components.CourseCard
import com.MohammadNoorAbuAsbe.myruppin.viewmodels.GradesViewModel
import com.MohammadNoorAbuAsbe.myruppin.viewmodels.GradesViewModelFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(navController: NavController) {
    // Setup dependencies
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val repository = remember { GradesRepository(client) }
    val viewModel: GradesViewModel = viewModel(
        factory = GradesViewModelFactory(repository, tokenManager)
    )

    // Collect state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val uniqueKrsSnl by viewModel.uniqueKrsSnl.collectAsState()
    val error by viewModel.error.collectAsState()

    // UI state
    var filterDropdownExpanded by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Grades") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Filter and Average Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    TextButton(onClick = { filterDropdownExpanded = true }) {
                        Text("Filter: $selectedFilter", color = MaterialTheme.colorScheme.primary)
                    }

                    DropdownMenu(
                        expanded = filterDropdownExpanded,
                        onDismissRequest = { filterDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                viewModel.setFilter("All")
                                filterDropdownExpanded = false
                            },
                            text = { Text(text = "All") }
                        )

                        uniqueKrsSnl.reversed().forEach { krsSnl ->
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.setFilter(krsSnl)
                                    filterDropdownExpanded = false
                                },
                                text = { Text(text = krsSnl) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = viewModel.getDisplayAverage(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                val filteredCourses = viewModel.getFilteredCourses()

                if (filteredCourses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No courses found for the selected filter")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCourses) { course ->
                            CourseCard(course)
                        }
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
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ExpandLess
//import androidx.compose.material.icons.filled.ExpandMore
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.example.myruppin.data.TokenManager
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.*
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//import java.io.IOException
//import java.util.concurrent.TimeUnit
//
//data class Course(val name: String, val grade: String, val krs_snl: String, val details: List<Detail>)
//data class Detail(val name: String, val finalGrade: String, val subDetails: List<SubDetail>)
//data class SubDetail(val groupName: String, val date: String, val time: String, val grade: String)
//
//@Composable
//fun GradesScreen(navController: NavController) {
//    val context = LocalContext.current
//    val tokenManager = remember { TokenManager(context) }
//    val scope = rememberCoroutineScope()
//    val client = remember {
//        OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .build()
//    }
//    var courses by remember { mutableStateOf<List<Course>?>(null) }
//    var cumulativeAverage by remember { mutableStateOf<String?>("N/A") }
//    var annualAverages by remember { mutableStateOf<List<String>>(emptyList()) }
//    var isLoading by remember { mutableStateOf(true) }
//    val token by tokenManager.token.collectAsState(initial = null)
//
//    var selectedFilter by remember { mutableStateOf("All") }
//    var expanded by remember { mutableStateOf(false) }
//    val uniqueKrsSnl = courses?.map { it.krs_snl }?.distinct()?.sorted() ?: emptyList()
//
//    LaunchedEffect(token) {
//        token?.let { fetchCourseData(it, client, scope) { result ->
//            courses = result.first
//            cumulativeAverage = result.second.first
//            annualAverages = result.second.second
//            isLoading = false
//        }}
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(top = 32.dp, start = 16.dp, end = 16.dp)
//    ) {
//        Text(
//            text = "Course Grades",
//            style = MaterialTheme.typography.titleLarge,
//            modifier = Modifier
//                .align(Alignment.CenterHorizontally)
//                .padding(bottom = 16.dp)
//        )
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Box(
//                modifier = Modifier.weight(1f),
//                contentAlignment = Alignment.CenterStart
//            ) {
//                TextButton(onClick = { expanded = true }) {
//                    Text("Filter: $selectedFilter", color = MaterialTheme.colorScheme.primary)
//                }
//                DropdownMenu(
//                    expanded = expanded,
//                    onDismissRequest = { expanded = false }
//                ) {
//                    DropdownMenuItem(
//                        onClick = {
//                            selectedFilter = "All"
//                            expanded = false
//                        },
//                        text = { Text(text = "All") }
//                    )
//                    uniqueKrsSnl.forEachIndexed { index, krsSnl ->
//                        DropdownMenuItem(
//                            onClick = {
//                                selectedFilter = krsSnl
//                                expanded = false
//                            },
//                            text = { Text(text = krsSnl) }
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            val averageText = if (selectedFilter == "All") {
//                "Cumulative Average: $cumulativeAverage"
//            } else {
//                val index = uniqueKrsSnl.indexOf(selectedFilter)
//                "Annual Average: ${annualAverages.getOrNull(index) ?: "N/A"}"
//            }
//            Text(
//                text = averageText,
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.secondary,
//                modifier = Modifier.align(Alignment.CenterVertically)
//            )
//        }
//
//        if (isLoading) {
//            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                CircularProgressIndicator()
//            }
//        } else {
//            val filteredCourses = if (selectedFilter == "All") {
//                courses ?: emptyList()
//            } else {
//                courses?.filter { it.krs_snl == selectedFilter } ?: emptyList()
//            }
//
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(filteredCourses) { course ->
//                    CourseCard(course)
//                }
//            }
//        }
//    }
//}
//
//private fun fetchCourseData(
//    token: String,
//    client: OkHttpClient,
//    scope: CoroutineScope,
//    onResult: (Pair<List<Course>, Pair<String?, List<String>>>) -> Unit
//) {
//    val request = Request.Builder()
//        .url("https://ruppinet.ruppin.ac.il/Portals/api/Grades/Data")
//        .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
//        .header("Authorization", "Bearer $token")
//        .build()
//
//    client.newCall(request).enqueue(object : Callback {
//        override fun onFailure(call: Call, e: IOException) {
//            scope.launch {
//                onResult(Pair(listOf(Course("Error", e.message.orEmpty(), "", emptyList())), Pair(null, emptyList())))
//            }
//        }
//
//        override fun onResponse(call: Call, response: Response) {
//            scope.launch {
//                try {
//                    val responseBody = response.body?.string()
//                    val jsonObject = JSONObject(responseBody ?: "")
//                    val averagesArray = jsonObject.getJSONArray("averages")
//                    val cumulativeAverage = averagesArray.getJSONObject(0).getString("cumulativeAverage")
//                    val annualAverages = mutableListOf<String>()
//
//                    for (i in 0 until averagesArray.length()) {
//                        val annualAverage = averagesArray.getJSONObject(i).getString("annualAverage")
//                        annualAverages.add(annualAverage)
//                    }
//
//                    annualAverages.reverse()
//
//                    val courses = parseCourses(jsonObject)
//                    onResult(Pair(courses, Pair(cumulativeAverage, annualAverages)))
//                } catch (e: Exception) {
//                    onResult(Pair(listOf(Course("Error", "Error parsing response: ${e.message}", "", emptyList())), Pair(null, emptyList())))
//                }
//            }
//        }
//    })
//}
//
//private suspend fun parseCourses(jsonObject: JSONObject): List<Course> = withContext(Dispatchers.Default) {
//    val coursesList = mutableListOf<Course>()
//    val clientData = jsonObject.getJSONObject("collapsedCourses").getJSONArray("clientData")
//    for (i in 0 until clientData.length()) {
//        val course = clientData.getJSONObject(i)
//        val courseName = course.getString("krs_shm")
//        val grade = course.optString("moed_1_zin", "Not graded yet")
//        val krsSnl = course.getString("krs_snl")
//
//        val details = parseDetails(course)
//        coursesList.add(Course(courseName, grade, krsSnl, details))
//    }
//    coursesList
//}
//
//private fun parseDetails(course: JSONObject): List<Detail> {
//    val details = mutableListOf<Detail>()
//    val bodyArray = course.optJSONArray("__body")
//    if (bodyArray != null) {
//        for (j in 0 until bodyArray.length()) {
//            val innerBody = bodyArray.getJSONObject(j)
//            val innerCourseName = innerBody.optString("krs_shm", "No name")
//            val finalGrade = innerBody.optString("bhnzin", "No final grade")
//            val subDetails = parseSubDetails(innerBody)
//            details.add(Detail(innerCourseName, finalGrade, subDetails))
//        }
//    }
//    return details
//}
//
//private fun parseSubDetails(innerBody: JSONObject): List<SubDetail> {
//    val subDetails = mutableListOf<SubDetail>()
//    val subBodyArray = innerBody.optJSONArray("__body")
//    if (subBodyArray != null) {
//        for (k in 0 until subBodyArray.length()) {
//            val subBody = subBodyArray.getJSONObject(k)
//            val groupName = subBody.optString("zin_sug", "No group name")
//            val date = subBody.optString("bhn_moed_dtmoed", "")
//            val time = subBody.optString("bhn_moed_time", "")
//            val subGrade = subBody.optString("moed_1_zin", "Not graded yet")
//            subDetails.add(SubDetail(groupName, date, time, subGrade))
//        }
//    }
//    return subDetails
//}
//
//@Composable
//fun CourseCard(course: Course) {
//    var expanded by remember { mutableStateOf(false) }
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { expanded = !expanded },
//        elevation = CardDefaults.cardElevation(4.dp),
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(text = course.grade, style = MaterialTheme.typography.bodyMedium)
//                Text(text = course.name, style = MaterialTheme.typography.bodyMedium)
//            }
//            if (expanded) {
//                course.details.forEach { detail ->
//                    DetailCard(detail)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun DetailCard(detail: Detail) {
//    var detailExpanded by remember { mutableStateOf(false) }
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { detailExpanded = !detailExpanded }
//            .padding(vertical = 8.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = if (detailExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
//                contentDescription = null,
//                tint = MaterialTheme.colorScheme.primary
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            // Check for both null and "null" string, default to "N/A"
//            val finalGradeText = if (detail.finalGrade.isNullOrBlank() || detail.finalGrade == "null") "N/A" else detail.finalGrade
//            Text(text = "Final Grade: $finalGradeText", style = MaterialTheme.typography.bodySmall)
//        }
//        Text(text = detail.name, style = MaterialTheme.typography.bodySmall)
//    }
//    if (detailExpanded) {
//        detail.subDetails.forEach { subDetail ->
//            SubDetailCard(subDetail)
//        }
//    }
//}
//
//@Composable
//fun SubDetailCard(subDetail: SubDetail) {
//    Column(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Column {
//                if (subDetail.date.isNotEmpty() && subDetail.time.isNotEmpty()) {
//                    Text(text = "Date: ${subDetail.date}", style = MaterialTheme.typography.bodySmall)
//                    Text(text = "Time: ${subDetail.time}", style = MaterialTheme.typography.bodySmall)
//                }
//                val subGradeText = if (subDetail.grade.isNullOrBlank() || subDetail.grade == "null") "N/A" else subDetail.grade
//                Text(text = "Grade: $subGradeText", style = MaterialTheme.typography.bodySmall)
//            }
//            Text(text = subDetail.groupName, style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}

