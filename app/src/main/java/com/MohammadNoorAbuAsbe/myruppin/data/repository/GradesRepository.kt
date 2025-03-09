package com.MohammadNoorAbuAsbe.myruppin.data.repository

import com.MohammadNoorAbuAsbe.myruppin.data.models.Course
import com.MohammadNoorAbuAsbe.myruppin.data.models.Detail
import com.MohammadNoorAbuAsbe.myruppin.data.models.GradesAverages
import com.MohammadNoorAbuAsbe.myruppin.data.models.GradesData
import com.MohammadNoorAbuAsbe.myruppin.data.models.SubDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class GradesRepository(private val client: OkHttpClient) {

    /**
     * Fetches grades data from the server
     */
    suspend fun fetchGradesData(token: String): GradesData {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://ruppinet.ruppin.ac.il/Portals/api/Grades/Data")
                .post("""{"urlParameters":{}}""".toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Unexpected response code: ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            parseGradesData(responseBody)
        }
    }

    /**
     * Parses the JSON response into GradesData object
     */
    private suspend fun parseGradesData(responseBody: String): GradesData {
        return withContext(Dispatchers.Default) {
            val jsonObject = JSONObject(responseBody)

            // Parse averages
            val averagesArray = jsonObject.getJSONArray("averages")
            val cumulativeAverage = averagesArray.getJSONObject(0).getString("cumulativeAverage")
            val annualAverages = mutableListOf<String>()

            for (i in 0 until averagesArray.length()) {
                val annualAverage = averagesArray.getJSONObject(i).getString("annualAverage")
                annualAverages.add(annualAverage)
            }
            annualAverages.reverse()

            // Parse courses
            val courses = parseCourses(jsonObject)

            GradesData(
                courses = courses,
                averages = GradesAverages(
                    cumulativeAverage = cumulativeAverage,
                    annualAverages = annualAverages
                )
            )
        }
    }

    /**
     * Parses courses from the JSON object
     */
    private suspend fun parseCourses(jsonObject: JSONObject): List<Course> {
        return withContext(Dispatchers.Default) {
            val coursesList = mutableListOf<Course>()
            val clientData = jsonObject.getJSONObject("collapsedCourses").getJSONArray("clientData")

            for (i in 0 until clientData.length()) {
                val course = clientData.getJSONObject(i)
                val courseName = course.getString("krs_shm")
                val grade = course.optString("moed_1_zin", "Not graded yet")
                val krsSnl = course.getString("krs_snl")
                val courseWeight = course.getString("zikui_mishkal")
                val details = parseDetails(course)

                coursesList.add(Course(courseName, grade, krsSnl, courseWeight,details))
            }

            coursesList
        }
    }

    /**
     * Parses details from a course JSON object
     */
    private fun parseDetails(course: JSONObject): List<Detail> {
        val details = mutableListOf<Detail>()
        val bodyArray = course.optJSONArray("__body")

        if (bodyArray != null) {
            for (j in 0 until bodyArray.length()) {
                val innerBody = bodyArray.getJSONObject(j)
                val innerCourseName = innerBody.optString("krs_shm", "No name")
                val finalGrade = innerBody.optString("bhnzin", "No final grade")
                val subDetails = parseSubDetails(innerBody)

                details.add(Detail(innerCourseName, finalGrade, subDetails))
            }
        }

        return details
    }

    /**
     * Parses sub-details from a detail JSON object
     */
    private fun parseSubDetails(innerBody: JSONObject): List<SubDetail> {
        val subDetails = mutableListOf<SubDetail>()
        val subBodyArray = innerBody.optJSONArray("__body")

        if (subBodyArray != null) {
            for (k in 0 until subBodyArray.length()) {
                val subBody = subBodyArray.getJSONObject(k)
                val groupName = subBody.optString("zin_sug", "No group name")
                val date = subBody.optString("bhn_moed_dtmoed", "")
                val time = subBody.optString("bhn_moed_time", "")
                val subGrade = subBody.optString("moed_1_zin", "Not graded yet")

                subDetails.add(SubDetail(groupName, date, time, subGrade))
            }
        }

        return subDetails
    }
}