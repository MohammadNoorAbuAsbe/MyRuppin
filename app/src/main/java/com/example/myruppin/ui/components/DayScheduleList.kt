package com.example.myruppin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myruppin.data.models.DaySchedule
import java.time.LocalDate

@Composable
fun DayScheduleList(
    daySchedules: List<DaySchedule>,
    formatTimeFromDateTime: (String) -> String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (daySchedules.isEmpty()) {
            Text(
                text = "No events scheduled for this day",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp).align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            ) {
                items(daySchedules) { schedule ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = schedule.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = "${formatTimeFromDateTime(schedule.startTime)} - ${formatTimeFromDateTime(schedule.endTime)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!schedule.place.isNullOrBlank()) {
                                Text(
                                    text = "Location: ${schedule.place}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (!schedule.moreInfo.isNullOrBlank()) {
                                Text(
                                    text = "Instructor: ${schedule.moreInfo}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}