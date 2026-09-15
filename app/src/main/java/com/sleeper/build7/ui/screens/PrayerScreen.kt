package com.sleeper.build7.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.sleeper.build7.data.PrayerRepository
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.receiver.AlarmReceiver
import com.sleeper.build7.ui.components.AnimatedGlassyBackground
import com.sleeper.build7.ui.components.AnimatedProgressRing
import com.sleeper.build7.ui.components.GlassButton
import com.sleeper.build7.ui.components.GlassCard
import com.sleeper.build7.ui.components.MetricStatCard
import com.sleeper.build7.ui.components.WeeklyConsistencyHeatmap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CityPreset(val name: String, val lat: Double, val lng: Double)

val CITY_PRESETS = listOf(
    CityPreset("Makkah, KSA", 21.3891, 39.8579),
    CityPreset("Madinah, KSA", 24.5247, 39.5692),
    CityPreset("Dhaka, Bangladesh", 23.8103, 90.4125),
    CityPreset("London, UK", 51.5074, -0.1278),
    CityPreset("New York, USA", 40.7128, -74.0060),
    CityPreset("Tokyo, Japan", 35.6762, 139.6503),
    CityPreset("Cairo, Egypt", 30.0444, 31.2357),
    CityPreset("Sydney, Australia", -33.8688, 151.2093),
    CityPreset("Dubai, UAE", 25.2048, 55.2708),
    CityPreset("Toronto, Canada", 43.6532, -79.3832),
    CityPreset("Berlin, Germany", 52.5200, 13.4050),
    CityPreset("Jakarta, Indonesia", -6.2088, 106.8456),
    CityPreset("Istanbul, Turkey", 41.0082, 28.9784)
)

@Composable
fun PrayerScreen(
    sleeperRepo: SleeperRepository,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prayerRepo = remember { PrayerRepository(context) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val todayRecord by sleeperRepo.todayPrayerRecord.collectAsState()
    val allPrayerRecords by sleeperRepo.allPrayerRecords.collectAsState()
    val alarmEnabled by sleeperRepo.prayerAlarmsEnabled.collectAsState()

    var lat by remember { mutableDoubleStateOf(23.8103) } // Default Dhaka
    var lng by remember { mutableDoubleStateOf(90.4125) }
    var locationName by remember { mutableStateOf("Dhaka, Bangladesh") }

    var showLocationDialog by remember { mutableStateOf(false) }
    var customLatInput by remember { mutableStateOf("23.81") }
    var customLngInput by remember { mutableStateOf("90.41") }

    var prayerTimes by remember {
        mutableStateOf(
            mapOf(
                "Fajr" to "04:30",
                "Dhuhr" to "12:15",
                "Asr" to "15:45",
                "Maghrib" to "18:25",
                "Isha" to "19:45"
            )
        )
    }

    var isLoading by remember { mutableStateOf(false) }

    fun fetchTimesForCoords(newLat: Double, newLng: Double) {
        coroutineScope.launch {
            isLoading = true
            lat = newLat
            lng = newLng
            val dateStr = sleeperRepo.getTodayDate()
            val times = prayerRepo.getPrayerTimes(newLat, newLng, dateStr)
            prayerTimes = times
            isLoading = false
        }
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        locationName = "GPS (${String.format(Locale.US, "%.2f, %.2f", loc.latitude, loc.longitude)})"
                        fetchTimesForCoords(loc.latitude, loc.longitude)
                    } else {
                        fetchTimesForCoords(lat, lng)
                    }
                }
            } catch (e: SecurityException) {
                fetchTimesForCoords(lat, lng)
            }
        } else {
            Toast.makeText(context, "Location permission denied; using current coordinates.", Toast.LENGTH_SHORT).show()
            fetchTimesForCoords(lat, lng)
        }
    }

    LaunchedEffect(Unit) {
        fetchTimesForCoords(lat, lng)
    }

    // Location Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Text(
                    text = "Select / Search Location",
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        GlassButton(
                            text = "📍 Use GPS Auto-Detect",
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                                showLocationDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(
                            text = "Famous City Presets:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = secondaryColor,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(CITY_PRESETS) { city ->
                        TextButton(
                            onClick = {
                                locationName = city.name
                                fetchTimesForCoords(city.lat, city.lng)
                                showLocationDialog = false
                                Toast.makeText(context, "Updated location to ${city.name}", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(city.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                Text(
                                    "${String.format(Locale.US, "%.1f", city.lat)}, ${String.format(Locale.US, "%.1f", city.lng)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Manual Coordinates Entry:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = primaryColor
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customLatInput,
                                onValueChange = { customLatInput = it },
                                label = { Text("Latitude") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customLngInput,
                                onValueChange = { customLngInput = it },
                                label = { Text("Longitude") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassButton(
                            text = "Apply Custom Lat/Lng",
                            onClick = {
                                val nLat = customLatInput.toDoubleOrNull()
                                val nLng = customLngInput.toDoubleOrNull()
                                if (nLat != null && nLng != null) {
                                    locationName = "Custom ($nLat, $nLng)"
                                    fetchTimesForCoords(nLat, nLng)
                                    showLocationDialog = false
                                } else {
                                    Toast.makeText(context, "Invalid Lat/Lng numbers", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    fun schedule5MinAlarm(prayerName: String, timeStr: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val prayerDate = sdf.parse(timeStr) ?: return

            val calendar = Calendar.getInstance().apply {
                val pCal = Calendar.getInstance().apply { time = prayerDate }
                set(Calendar.HOUR_OF_DAY, pCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, pCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                add(Calendar.MINUTE, -5)
            }

            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val audioMode = sleeperRepo.audioAlarmMode.value
            val voiceProfile = sleeperRepo.ttsVoiceProfile.value
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("title", "🕌 Prayer Reminder: $prayerName")
                putExtra("message", "$prayerName prayer is in 5 minutes ($timeStr). Prepare for Salah.")
                putExtra("audio_mode", audioMode)
                putExtra("voice_profile", voiceProfile)
                putExtra("custom_tts_text", "$prayerName prayer is in five minutes. Prepare for Salah.")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                prayerName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            Toast.makeText(context, "Alarm set 5m prior to $prayerName ($timeStr)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not set exact alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val completedTodayCount = listOf(
        todayRecord.fajr,
        todayRecord.dhuhr,
        todayRecord.asr,
        todayRecord.maghrib,
        todayRecord.isha
    ).count { it }

    // Dynamic Streak & Weekly Analytics
    val weeklyDays = remember(allPrayerRecords, completedTodayCount) {
        val todayStr = sleeperRepo.getTodayDate()
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            val dayInitial = SimpleDateFormat("E", Locale.US).format(cal.time).take(1)
            val isDone = if (dStr == todayStr) {
                completedTodayCount >= 4
            } else {
                val r = allPrayerRecords.firstOrNull { it.date == dStr }
                r != null && listOf(r.fajr, r.dhuhr, r.asr, r.maghrib, r.isha).count { it } >= 4
            }
            dayInitial to isDone
        }
    }

    val consecutiveStreak = remember(allPrayerRecords, completedTodayCount) {
        var streak = if (completedTodayCount >= 3) 1 else 0
        val sorted = allPrayerRecords.filter { it.date != sleeperRepo.getTodayDate() }.sortedByDescending { it.date }
        for (r in sorted) {
            val count = listOf(r.fajr, r.dhuhr, r.asr, r.maghrib, r.isha).count { it }
            if (count >= 3) streak++ else break
        }
        streak
    }

    AnimatedGlassyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar with Back Navigation
            GlassCard(
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("prayer_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Hub",
                                tint = primaryColor
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = "Prayer",
                            tint = primaryColor,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Prayer Times",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = locationName,
                                fontSize = 11.sp,
                                color = secondaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { showLocationDialog = true },
                        modifier = Modifier.testTag("location_permission_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditLocationAlt,
                            contentDescription = "Change Location",
                            tint = secondaryColor
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // PREMIUM STATS OVERHAUL: Daily Completion Ring & Key Analytics
                item {
                    GlassCard(borderColor = primaryColor) {
                        Text(
                            text = "Salah Compliance Analytics",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Circular Progress Ring
                            AnimatedProgressRing(
                                progress = completedTodayCount / 5f,
                                size = 96.dp,
                                strokeWidth = 10.dp,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(completedTodayCount / 5f * 100).toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "$completedTodayCount/5",
                                        fontSize = 11.sp,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Key Stat Badges
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricStatCard(
                                        title = "Streak",
                                        value = "$consecutiveStreak d",
                                        icon = Icons.Default.Bolt,
                                        badgeText = "Spiritual",
                                        accentColor = primaryColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricStatCard(
                                        title = "Accuracy",
                                        value = "${(completedTodayCount * 20)}%",
                                        icon = Icons.Default.CheckCircle,
                                        badgeText = "Today",
                                        accentColor = secondaryColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Weekly Consistency Heatmap
                item {
                    WeeklyConsistencyHeatmap(
                        days = weeklyDays,
                        title = "7-Day Prayer Activity",
                        activeColor = primaryColor
                    )
                }

                // Alarms & Progress Card
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "5-Minute Prior Exact Alarms",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (alarmEnabled) "Active exact reminders armed" else "Alarms paused",
                                    fontSize = 12.sp,
                                    color = if (alarmEnabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = alarmEnabled,
                                onCheckedChange = { sleeperRepo.setPrayerAlarmsEnabled(it) },
                                modifier = Modifier.testTag("prayer_alarm_switch")
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = primaryColor)
                        }
                    }
                }
            // Today's Prayer Checkbox Cards
            val prayers = listOf(
                "Fajr" to (todayRecord.fajr to (prayerTimes["Fajr"] ?: "04:30")),
                "Dhuhr" to (todayRecord.dhuhr to (prayerTimes["Dhuhr"] ?: "12:15")),
                "Asr" to (todayRecord.asr to (prayerTimes["Asr"] ?: "15:45")),
                "Maghrib" to (todayRecord.maghrib to (prayerTimes["Maghrib"] ?: "18:25")),
                "Isha" to (todayRecord.isha to (prayerTimes["Isha"] ?: "19:45"))
            )

            items(prayers) { (name, data) ->
                val (isCompleted, timeStr) = data
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = { sleeperRepo.updatePrayerCheck(name, it) },
                                colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                                modifier = Modifier.testTag("prayer_check_$name")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isCompleted) primaryColor else MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = timeStr,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                schedule5MinAlarm(name, timeStr)
                            },
                            enabled = alarmEnabled,
                            modifier = Modifier.testTag("alarm_btn_$name")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Set Alarm",
                                tint = if (alarmEnabled) secondaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // Tracker List Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Prayer History Tracker Logs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Historical Prayer Log Cards
            if (allPrayerRecords.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "No past prayer logs recorded yet. Complete prayers above to build your spiritual streak tracker!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(allPrayerRecords) { rec ->
                    val doneCount = listOf(rec.fajr, rec.dhuhr, rec.asr, rec.maghrib, rec.isha).count { it }
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = rec.date,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = primaryColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("F: ${if (rec.fajr) "✓" else "✗"}", fontSize = 12.sp, color = if (rec.fajr) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("D: ${if (rec.dhuhr) "✓" else "✗"}", fontSize = 12.sp, color = if (rec.dhuhr) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("A: ${if (rec.asr) "✓" else "✗"}", fontSize = 12.sp, color = if (rec.asr) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("M: ${if (rec.maghrib) "✓" else "✗"}", fontSize = 12.sp, color = if (rec.maghrib) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("I: ${if (rec.isha) "✓" else "✗"}", fontSize = 12.sp, color = if (rec.isha) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Text(
                                text = "$doneCount / 5 Completed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (doneCount == 5) primaryColor else secondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
    }
}
