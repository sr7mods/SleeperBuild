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

class PrayerRepository(private val context: Context) {

    suspend fun getPrayerTimes(latitude: Double, longitude: Double, dateStr: String): Map<String, String> =
        withContext(Dispatchers.IO) {
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

                val apiUrl = "https://api.aladhan.com/v1/timings/$apiDateParam?latitude=$latitude&longitude=$longitude&method=2"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val timings = json.getJSONObject("data").getJSONObject("timings")
                    mapOf(
                        "Fajr" to cleanTime(timings.getString("Fajr")),
                        "Dhuhr" to cleanTime(timings.getString("Dhuhr")),
                        "Asr" to cleanTime(timings.getString("Asr")),
                        "Maghrib" to cleanTime(timings.getString("Maghrib")),
                        "Isha" to cleanTime(timings.getString("Isha"))
                    )
                } else {
                    calculateOfflinePrayerTimes(latitude, longitude, dateStr)
                }
            } catch (e: Exception) {
                // Offline astronomical calculation fallback
                calculateOfflinePrayerTimes(latitude, longitude, dateStr)
            }
        }

    private fun cleanTime(timeStr: String): String {
        return timeStr.split(" ")[0]
    }

    /**
     * Precise offline calculation method based on solar position algorithms.
     */
    fun calculateOfflinePrayerTimes(lat: Double, lng: Double, dateStr: String = ""): Map<String, String> {
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

        val fajrHA = hourAngle(-18.0)
        val maghribHA = hourAngle(-0.833)
        val ishaHA = hourAngle(-18.0)

        // Asr (Shafi'i factor 1)
        val radLat = Math.toRadians(lat)
        val radDec = Math.toRadians(d)
        val acotAsr = atan(1.0 + tan(abs(radLat - radDec)))
        val asrHA = Math.toDegrees(acos((cos(acotAsr) - sin(radLat) * sin(radDec)) / (cos(radLat) * cos(radDec)))) / 15.0

        val fajrTime = noon - fajrHA
        val dhuhrTime = noon
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
            "Dhuhr" to formatHours(dhuhrTime),
            "Asr" to formatHours(asrTime),
            "Maghrib" to formatHours(maghribTime),
            "Isha" to formatHours(ishaTime)
        )
    }
}
