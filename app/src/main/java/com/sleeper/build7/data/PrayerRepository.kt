package com.sleeper.build7.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class CalcMethodInfo(
    val id: Int,
    val name: String,
    val description: String
)

val ALADHAN_CALCULATION_METHODS = listOf(
    CalcMethodInfo(1, "Univ. of Islamic Sciences, Karachi", "South Asia (Hanafi, Bangladesh, Pakistan, India)"),
    CalcMethodInfo(2, "Islamic Society of North America (ISNA)", "North America (USA, Canada)"),
    CalcMethodInfo(3, "Muslim World League (MWL)", "Europe, Far East, parts of America"),
    CalcMethodInfo(4, "Umm Al-Qura University, Makkah", "Saudi Arabia & Arabian Peninsula"),
    CalcMethodInfo(5, "Egyptian General Authority of Survey", "Egypt, Africa, Syria, Lebanon, Iraq"),
    CalcMethodInfo(13, "Diyanet İşleri Başkanlığı", "Turkey"),
    CalcMethodInfo(16, "Dubai", "United Arab Emirates")
)

class PrayerRepository(private val context: Context) {

    suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        dateStr: String,
        method: Int = 1,
        school: Int = 1
    ): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            // Convert dateStr (e.g. 2026-07-29) to dd-MM-yyyy format expected by Aladhan API
            val apiDateParam = try {
                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfOut = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                val parsed = sdfIn.parse(dateStr)
                if (parsed != null) sdfOut.format(parsed) else dateStr
            } catch (e: Exception) {
                dateStr
            }

            val apiUrl = "https://api.aladhan.com/v1/timings/$apiDateParam?latitude=$latitude&longitude=$longitude&method=$method&school=$school"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 7000
            connection.readTimeout = 7000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val timings = json.getJSONObject("data").getJSONObject("timings")
                mapOf(
                    "Fajr" to cleanTime(timings.getString("Fajr")),
                    "Sunrise" to cleanTime(timings.getString("Sunrise")),
                    "Dhuhr" to cleanTime(timings.getString("Dhuhr")),
                    "Asr" to cleanTime(timings.getString("Asr")),
                    "Sunset" to cleanTime(timings.getString("Sunset")),
                    "Maghrib" to cleanTime(timings.getString("Maghrib")),
                    "Isha" to cleanTime(timings.getString("Isha")),
                    "Imsak" to cleanTime(timings.optString("Imsak", "")),
                    "Midnight" to cleanTime(timings.optString("Midnight", ""))
                )
            } else {
                calculateOfflinePrayerTimes(latitude, longitude, dateStr, method, school)
            }
        } catch (e: Exception) {
            // Offline astronomical calculation fallback
            calculateOfflinePrayerTimes(latitude, longitude, dateStr, method, school)
        }
    }

    private fun cleanTime(timeStr: String): String {
        return timeStr.split(" ")[0].trim()
    }

    /**
     * High precision offline calculation based on solar positioning equations,
     * configured for specific twilight angles and Asr juristic school (Shafi'i vs Hanafi).
     */
    fun calculateOfflinePrayerTimes(
        lat: Double,
        lng: Double,
        dateStr: String = "",
        method: Int = 1,
        school: Int = 1
    ): Map<String, String> {
        val calendar = Calendar.getInstance()
        if (dateStr.isNotBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsed = sdf.parse(dateStr)
                if (parsed != null) {
                    calendar.time = parsed
                }
            } catch (e: Exception) {
                // fallback to current
            }
        }

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val timezoneOffset = TimeZone.getDefault().getOffset(calendar.timeInMillis) / (1000.0 * 3600.0)

        // Declination of Sun
        val d = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)))

        // Equation of Time (minutes)
        val b = Math.toRadians(360.0 / 365.0 * (dayOfYear - 81))
        val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)

        // Solar Noon
        val noon = 12.0 + (15.0 * timezoneOffset - lng) / 15.0 - (eot / 60.0)

        // Hour Angle helper
        fun hourAngle(angle: Double): Double {
            val radLat = Math.toRadians(lat)
            val radDec = Math.toRadians(d)
            val radAngle = Math.toRadians(angle)
            val cosHA = (sin(radAngle) - sin(radLat) * sin(radDec)) / (cos(radLat) * cos(radDec))
            val clampedCosHA = cosHA.coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(clampedCosHA)) / 15.0
        }

        val (fajrAngle, ishaAngle) = when (method) {
            2 -> -15.0 to -15.0 // ISNA
            3 -> -18.0 to -17.0 // MWL
            4 -> -18.5 to -19.0 // Makkah
            5 -> -19.5 to -17.5 // Egyptian
            else -> -18.0 to -18.0 // Karachi / Default
        }

        val fajrHA = hourAngle(fajrAngle)
        val sunriseHA = hourAngle(-0.833)
        val maghribHA = hourAngle(-0.833)
        val ishaHA = hourAngle(ishaAngle)

        // Asr: school 0 = Shafi'i (factor 1.0), school 1 = Hanafi (factor 2.0)
        val radLat = Math.toRadians(lat)
        val radDec = Math.toRadians(d)
        val shadowFactor = if (school == 1) 2.0 else 1.0
        val acotAsr = atan(shadowFactor + tan(abs(radLat - radDec)))
        val asrHA = Math.toDegrees(acos((cos(acotAsr) - sin(radLat) * sin(radDec)) / (cos(radLat) * cos(radDec)))) / 15.0

        val fajrTime = noon - fajrHA
        val sunriseTime = noon - sunriseHA
        val dhuhrTime = noon + (2.0 / 60.0) // 2 mins after solar noon for safety
        val asrTime = noon + asrHA
        val maghribTime = noon + maghribHA
        val ishaTime = noon + ishaHA

        fun formatHours(hours: Double): String {
            var h = (hours + 24) % 24
            val m = ((h - floor(h)) * 60).toInt()
            val hh = floor(h).toInt()
            return String.format(Locale.US, "%02d:%02d", hh, m)
        }

        return mapOf(
            "Fajr" to formatHours(fajrTime),
            "Sunrise" to formatHours(sunriseTime),
            "Dhuhr" to formatHours(dhuhrTime),
            "Asr" to formatHours(asrTime),
            "Sunset" to formatHours(maghribTime),
            "Maghrib" to formatHours(maghribTime),
            "Isha" to formatHours(ishaTime),
            "Imsak" to formatHours(fajrTime - (10.0 / 60.0)),
            "Midnight" to formatHours(noon + 12.0)
        )
    }
}
