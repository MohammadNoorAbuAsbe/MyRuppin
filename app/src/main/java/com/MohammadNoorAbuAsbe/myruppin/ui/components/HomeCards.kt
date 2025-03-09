
package com.MohammadNoorAbuAsbe.myruppin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.MohammadNoorAbuAsbe.myruppin.data.models.EventInfo
import com.MohammadNoorAbuAsbe.myruppin.data.models.UpcomingEvent

/**
 * Displays the current event card
 */
@Composable
fun CurrentEventCard(
    currentEvent: EventInfo?,
    isLoading: Boolean,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    bodySize: TextUnit,
    standardPadding: Dp,
    smallPadding: Dp,
    iconSize: Dp,
    title: String,
    remainingTime: String, // Add remainingTime parameter
    countdownLabel: String // Add countdownLabel parameter
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(standardPadding)
    ) {
        Text(
            text = title,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = smallPadding)
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(iconSize),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(smallPadding),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (currentEvent != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(standardPadding),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentEvent.title,
                            fontSize = subtitleSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.End)
                        )
                        Text(
                            text = "Location: ${currentEvent.place}",
                            fontSize = bodySize
                        )
                        Text(
                            text = "Time: ${currentEvent.startTime.substring(0, 5)} - ${currentEvent.endTime.substring(0, 5)}",
                            fontSize = bodySize
                        )
                        // Display the countdown timer
                        Text(
                            text = "$countdownLabel: $remainingTime",
                            fontSize = bodySize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Text(
                        text = "No more events scheduled for today",
                        fontSize = bodySize,
                        modifier = Modifier.padding(standardPadding),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Displays a single upcoming event card
 */
@Composable
fun UpcomingEventCard(
    event: UpcomingEvent,
    subtitleSize: TextUnit,
    bodySize: TextUnit,
    smallPadding: Dp,
    tinyPadding: Dp,
    warningIconSize: Dp
) {
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
            if (event.isExam) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Exam",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(warningIconSize),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.date,
                        fontSize = bodySize,
                        color = if (event.isExam)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Display days left in the same row
                    val daysLeft = event.calculateDaysLeft()
                    Text(
                        text = "($daysLeft days left)",
                        fontSize = bodySize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}