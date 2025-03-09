package com.MohammadNoorAbuAsbe.myruppin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.MohammadNoorAbuAsbe.myruppin.data.models.ScheduleCourse

@Composable
fun CourseList(
    courses: List<ScheduleCourse>,
    modifier: Modifier = Modifier
) {
    // Define the order of days from Sunday to Saturday
    val dayOrder = listOf("א", "ב", "ג", "ד", "ה", "ו", "ת")

    // Group courses by day and sort by the defined order
    val groupedCourses = courses.filter { it.day != "null" }.groupBy { it.day }
        .toSortedMap(compareBy { dayOrder.indexOf(it) })

    LazyColumn(modifier = modifier) {
        groupedCourses.forEach { (day, dayCourses) ->
            item {
                Text(
                    text = "Day: $day",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
            items(dayCourses) { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Instructor: ${course.instructor}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Time: ${course.startTime} - ${course.endTime}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Location: ${course.location}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}