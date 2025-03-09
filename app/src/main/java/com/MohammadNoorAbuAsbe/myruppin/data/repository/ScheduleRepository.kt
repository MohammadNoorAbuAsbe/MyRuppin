package com.MohammadNoorAbuAsbe.myruppin.data.repository

import com.MohammadNoorAbuAsbe.myruppin.data.models.ScheduleCourse
import com.MohammadNoorAbuAsbe.myruppin.data.models.DaySchedule
import com.MohammadNoorAbuAsbe.myruppin.data.models.ScheduleParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ScheduleRepository(private val client: OkHttpClient) {

    suspend fun fetchScheduleParams(token: String): ScheduleParams = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/StudentSchedule/Data")
                .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val responseString = response.body!!.string()
                        val jsonResponse = JSONObject(responseString)
                        val scheduleParamsJson = jsonResponse.getJSONObject("_ScheduleParams")
                        val params = ScheduleParams(
                            hash = scheduleParamsJson.getString("__hash"),
                            pt = scheduleParamsJson.getInt("pt"),
                            ptMsl = scheduleParamsJson.getInt("ptMsl"),
                            shl = scheduleParamsJson.getInt("shl")
                        )
                        continuation.resume(params)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    suspend fun fetchSchedule(token: String, params: ScheduleParams): List<ScheduleCourse> = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val url = "https://ruppinet.ruppin.ac.il/Portals/api/StudentScheduleCommon/GetSchedule"
            val body = JSONObject().apply {
                put("__hash", params.hash)
                put("pt", params.pt)
                put("ptMsl", params.ptMsl)
                put("shl", params.shl)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val responseString = response.body!!.string()
                        val jsonResponse = JSONObject(responseString)
                        val courses = parseScheduleData(jsonResponse)
                        continuation.resume(courses)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    suspend fun fetchWeekSchedule(token: String, params: ScheduleParams, startDay: LocalDate): List<DaySchedule> = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            val url = "https://ruppinet.ruppin.ac.il/Portals/api/StudentScheduleCommon/DateChanged"
            val body = JSONObject().apply {
                put("_ScheduleParams", JSONObject().apply {
                    put("__hash", params.hash)
                    put("pt", params.pt)
                    put("ptMsl", params.ptMsl)
                    put("shl", params.shl)
                })
                put("date", "${startDay}T00:00:00.000Z")
            }.toString().toRequestBody("application/json".toMediaType())

            client.newCall(Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer $token")
                .build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val responseString = response.body!!.string()
                        val jsonResponse = JSONObject(responseString)
                        val weekSchedule = parseDaySchedule(jsonResponse)
                        continuation.resume(weekSchedule)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    private fun parseScheduleData(jsonResponse: JSONObject): List<ScheduleCourse> {
        val courses = mutableListOf<ScheduleCourse>()
        val clientData = jsonResponse.getJSONObject("scheduleViewItemSms").getJSONArray("clientData")
        for (i in 0 until clientData.length()) {
            val item = clientData.getJSONObject(i)
            val course = ScheduleCourse(
                name = item.getString("krs_shm"),
                instructor = item.getString("pm_shm"),
                startTime = parseTimeString(item.optString("krs_moed_meshaa", "00:00")),
                endTime = parseTimeString(item.optString("krs_moed_adshaa", "00:00")),
                day = item.optString("krs_moed_yom", "Unknown"),
                location = item.optString("hdr_shm", "Unknown"),
                semester = item.optString("krs_moed_sms", "Unknown"),
                studyYear = item.optString("krs_snl", "Unknown")
            )
            courses.add(course)
        }
        return courses.sortedBy { it.startTime }
    }

    private fun parseDaySchedule(jsonResponse: JSONObject): List<DaySchedule> {
        val clientData = jsonResponse.getJSONObject("scheduleViewItemWeek").getJSONArray("clientData")
        val schedules = mutableListOf<DaySchedule>()

        for (i in 0 until clientData.length()) {
            val item = clientData.getJSONObject(i)
            val schedule = DaySchedule(
                date = item.getString("date"),
                title = item.getString("title"),
                startTime = item.getString("mar_full_start"),
                endTime = item.getString("mar_full_end"),
                place = item.optString("place"),
                moreInfo = item.optString("moreinfo")
            )
            schedules.add(schedule)
        }
        return schedules
    }

    private fun parseTimeString(timeStr: String): String {
        return try {
            if (timeStr.length >= 16) {
                timeStr.substring(11, 16)
            } else {
                timeStr
            }
        } catch (e: Exception) {
            "00:00"
        }
    }
}