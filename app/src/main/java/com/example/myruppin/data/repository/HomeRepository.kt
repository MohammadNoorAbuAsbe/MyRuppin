package com.example.myruppin.data.repository

import com.example.myruppin.data.models.EventInfo
import com.example.myruppin.data.models.UpcomingEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeRepository(private val client: OkHttpClient) {

    /**
     * Fetches the current event for today
     */
    suspend fun fetchCurrentEvent(token: String): EventInfo? {
        return withContext(Dispatchers.IO) {
            val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val jsonBody = JSONObject().apply {
                put("fromDate", "${today.split('T')[0]}T00:00:00")
                put("toDate", "${today.split('T')[0]}T00:00:00")
            }

            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Home/ScheduleData")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                parseCurrentEvent(responseBody)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Fetches upcoming events
     */
    suspend fun fetchUpcomingEvents(token: String): List<UpcomingEvent> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Home/Data")
                .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                parseUpcomingEvents(responseBody)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Parses the current event from JSON response
     */
    private fun parseCurrentEvent(responseBody: String): EventInfo? {
        val jsonResponse = JSONObject(responseBody)
        val eventsArray = jsonResponse.optJSONArray("events")

        if (eventsArray != null && eventsArray.length() > 0) {
            val firstEvent = eventsArray.getJSONObject(0)
            val eventData = firstEvent.getJSONObject("data")

            return EventInfo(
                title = eventData.optString("title", "No title"),
                place = eventData.optString("place", "No location"),
                startTime = eventData.optString("startTime", "").split('T')[1].substring(0, 5),
                endTime = eventData.optString("endTime", "").split('T')[1].substring(0, 5)
            )
        }

        return null
    }

    /**
     * Parses upcoming events from JSON response
     */
    private fun parseUpcomingEvents(responseBody: String): List<UpcomingEvent> {
        val jsonResponse = JSONObject(responseBody)
        val eventsArray = jsonResponse.optJSONArray("events")
        val events = mutableListOf<UpcomingEvent>()

        for (i in 0 until (eventsArray?.length() ?: 0)) {
            val event = eventsArray?.getJSONObject(i)
            event?.let {
                val date = it.optString("date", "")
                    .split("T")[0] // Get just the date part
                    .split("-") // Split into year, month, day
                    .let { parts -> "${parts[2]}/${parts[1]}/${parts[0]}" } // Format as dd/mm/yyyy

                events.add(
                    UpcomingEvent(
                        title = it.optString("title", "No title"),
                        date = date,
                        type = it.optString("type", ""),
                        isExam = it.optString("type") == "StudentExams"
                    )
                )
            }
        }

        return events
    }

    suspend fun fetchUserName(token: String): String? {
        return withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().apply {}

            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Account/UserInfo")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected response code: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                parseUserName(responseBody)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseUserName(responseBody: String): String? {
        val jsonResponse = JSONObject(responseBody)
        val userInfo = jsonResponse.optJSONObject("userInfo")
        if (userInfo != null) {
            val firstName = userInfo.optString("smp", "")
            val lastName = userInfo.optString("smm", "")
            return "$firstName $lastName".trim().takeIf { it.isNotEmpty() }
        }
        return null
    }
}