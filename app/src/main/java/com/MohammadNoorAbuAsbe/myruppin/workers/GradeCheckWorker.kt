package com.MohammadNoorAbuAsbe.myruppin.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.MohammadNoorAbuAsbe.myruppin.MainActivity
import com.MohammadNoorAbuAsbe.myruppin.R
import com.MohammadNoorAbuAsbe.myruppin.data.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GradeCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val client = OkHttpClient()
    private val tokenManager = TokenManager(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val currentToken = tokenManager.token.first() ?: return@withContext Result.failure()
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Grades/Data")
                .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $currentToken")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val newGrades = parseGrades(responseBody).toSet()
                val storedGrades = tokenManager.grades.first().map {
                    val parts = it.split(": ")
                    parts[0] to parts[1]
                }.toSet()

                if (newGrades != storedGrades) {
                    val newEntries = newGrades - storedGrades
                    val existingCourses = storedGrades.map { it.first }.toSet()
                    newEntries.forEachIndexed { index, (course, grade) ->
                        if (course in existingCourses) {
                            sendNotification("New Grade Update", "New grade for $course: $grade", index)
                        }
                    }
                    tokenManager.saveGrades(newGrades.map { "${it.first}: ${it.second}" }.toSet())
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun parseGrades(responseBody: String): List<Pair<String, String>> {
        val gradesList = mutableListOf<Pair<String, String>>()
        val jsonObject = JSONObject(responseBody)
        val clientData = jsonObject.getJSONObject("collapsedCourses").getJSONArray("clientData")
        for (i in 0 until clientData.length()) {
            val course = clientData.getJSONObject(i)
            val courseName = course.getString("krs_shm")
            val grade = course.optString("moed_1_zin", "No grade")
            gradesList.add(courseName to grade)
        }
        return gradesList
    }

    private fun sendNotification(title: String, message: String, notificationId: Int) {
        createNotificationChannel()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateTo", "grades")
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(applicationContext, "grade_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        try {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Grade Updates"
            val descriptionText = "Notifications for grade updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("grade_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}