package com.MohammadNoorAbuAsbe.myruppin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.MohammadNoorAbuAsbe.myruppin.data.models.Course
import com.MohammadNoorAbuAsbe.myruppin.data.models.Detail
import com.MohammadNoorAbuAsbe.myruppin.data.models.SubDetail

@Composable
fun CourseCard(course: Course) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = course.grade, style = MaterialTheme.typography.bodyMedium)
                Text(text = course.name, style = MaterialTheme.typography.bodyMedium)
            }

            if (expanded) {
                course.details.forEach { detail ->
                    DetailCard(detail)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = course.courseWeight,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "נקודות זכות",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun DetailCard(detail: Detail) {
    var detailExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { detailExpanded = !detailExpanded }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (detailExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Check for both null and "null" string, default to "N/A"
            val finalGradeText = if (detail.finalGrade.isNullOrBlank() || detail.finalGrade == "null") {
                "N/A"
            } else {
                detail.finalGrade
            }

            Text(
                text = "Final Grade: $finalGradeText",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = detail.name,
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (detailExpanded) {
        detail.subDetails.forEach { subDetail ->
            SubDetailCard(subDetail)
        }
    }
}

@Composable
fun SubDetailCard(subDetail: SubDetail) {
    Column(
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (subDetail.date.isNotEmpty() && subDetail.time.isNotEmpty()) {
                    Text(
                        text = "Date: ${subDetail.date}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Time: ${subDetail.time}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val subGradeText = if (subDetail.grade.isNullOrBlank() || subDetail.grade == "null") {
                    "N/A"
                } else {
                    subDetail.grade
                }

                Text(
                    text = "Grade: $subGradeText",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = subDetail.groupName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}