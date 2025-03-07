package com.example.myruppin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarGridView(
    currentMonth: YearMonth,
    onMonthChanged: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    selectedDay: LocalDate?,
    hasEvents: (LocalDate) -> Boolean
) {
    val today = LocalDate.now()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Month Navigation - reduce vertical padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onMonthChanged(currentMonth.minusMonths(1))
            }) {
                Text("<")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = {
                onMonthChanged(currentMonth.plusMonths(1))
            }) {
                Text(">")
            }
        }

        // Days of the Week Header - reduce vertical padding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(text = day, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Days Grid - reduce height
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfMonth = LocalDate.of(currentMonth.year, currentMonth.month, 1).dayOfWeek.value % 7
        val totalCells = daysInMonth + firstDayOfMonth

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            items(totalCells) { index ->
                if (index < firstDayOfMonth) {
                    Box(modifier = Modifier.size(36.dp))
                } else {
                    val day = index - firstDayOfMonth + 1
                    val date = LocalDate.of(currentMonth.year, currentMonth.month, day)
                    val events = hasEvents(date)
                    val isPastDay = date.isBefore(today)
                    val isToday = date.isEqual(today)
                    val isSelected = selectedDay == date

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .padding(2.dp)
                            .background(
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.background
                                },
                                shape = CircleShape
                            )
                            .clickable {
                                onDaySelected(date)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Day number
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.secondary
                                isPastDay -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.onBackground
                            }
                        )

                        // Indicator for days with events
                        if (events) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 1.dp)
                                    .background(
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onSecondary
                                            isPastDay -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}