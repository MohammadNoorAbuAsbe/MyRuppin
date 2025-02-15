package com.example.myruppin.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myruppin.R
import com.example.myruppin.data.TokenManager
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
                val newGrades = parseGrades(responseBody)
                val storedGrades = tokenManager.grades.first()

                if (newGrades.toSet() != storedGrades) {
                    sendNotification("New Grade Update", "Your grades have been updated.")
                    tokenManager.saveGrades(newGrades.toSet())
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun parseGrades(responseBody: String): List<String> {
        val gradesList = mutableListOf<String>()
        val jsonObject = JSONObject(responseBody)
        val clientData = jsonObject.getJSONObject("collapsedCourses").getJSONArray("clientData")
        for (i in 0 until clientData.length()) {
            val course = clientData.getJSONObject(i)
            val grade = course.optString("moed_1_zin", "No grade")
            gradesList.add(grade)
        }
        return gradesList
    }

    private fun sendNotification(title: String, message: String) {
        createNotificationChannel()
        val builder = NotificationCompat.Builder(applicationContext, "grade_channel")
            .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists in your resources
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        try {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(1, builder.build())
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