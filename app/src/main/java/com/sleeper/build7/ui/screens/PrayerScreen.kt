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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.sleeper.build7.audio.SleeperAudioEngine
import com.sleeper.build7.data.ALADHAN_CALCULATION_METHODS
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
    CityPreset("Dhaka, Bangladesh", 23.8103, 90.4125),
    CityPreset("Makkah, KSA", 21.3891, 39.8579),
    CityPreset("Madinah, KSA", 24.5247, 39.5692),
    CityPreset("London, UK", 51.5074, -0.1278),
    CityPreset("New York, USA", 40.7128, -74.0060),
    CityPreset("Dubai, UAE", 25.2048, 55.2708),
    CityPreset("Cairo, Egypt", 30.0444, 31.2357),
    CityPreset("Karachi, Pakistan", 24.8607, 67.0011),
    CityPreset("Jakarta, Indonesia", -6.2088, 106.8456),
    CityPreset("Istanbul, Turkey", 41.0082, 28.9784),
    CityPreset("Tokyo, Japan", 35.6762, 139.6503),
    CityPreset("Sydney, Australia", -33.8688, 151.2093),
    CityPreset("Toronto, Canada", 43.6532, -79.3832)
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

    val calcMethod by sleeperRepo.prayerCalculationMethod.collectAsState()
    val juristicSchool by sleeperRepo.prayerJuristicSchool.collectAsState()
    val reminderBufferMins by sleeperRepo.prayerReminderMinutesPrior.collectAsState()
    val manualTimes by sleeperRepo.prayerManualTimes.collectAsState()

    val audioMode by sleeperRepo.audioAlarmMode.collectAsState()
    val ttsVoice by sleeperRepo.ttsVoiceProfile.collectAsState()

    var lat by remember { mutableDoubleStateOf(23.8103) } // Default Dhaka
    var lng by remember { mutableDoubleStateOf(90.4125) }
    var locationName by remember { mutableStateOf("Dhaka, Bangladesh") }

    var showLocationDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var customLatInput by remember { mutableStateOf("23.81") }
    var customLngInput by remember { mutableStateOf("90.41") }

    // Dialog state for manual time adjustment
    var editingPrayerName by remember { mutableStateOf<String?>(null) }
    var editingPrayerTimeInput by remember { mutableStateOf("") }

    var prayerTimes by remember {
        mutableStateOf(
            mapOf(
                "Fajr" to "04:30",
                "Sunrise" to "05:45",
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
            val times = prayerRepo.getPrayerTimes(
                latitude = newLat,
                longitude = newLng,
                dateStr = dateStr,
                method = calcMethod,
                school = juristicSchool
            )
            prayerTimes = times
            isLoading = false
        }
    }

    // Helper to get effective time (manual override or API calculated)
    fun getEffectiveTime(prayerName: String): String {
        return manualTimes[prayerName] ?: prayerTimes[prayerName] ?: when (prayerName) {
            "Fajr" -> "04:30"
            "Dhuhr" -> "12:15"
            "Asr" -> "15:45"
            "Maghrib" -> "18:25"
            "Isha" -> "19:45"
            else -> "12:00"
        }
    }

    fun parseMinsFromTime(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    fun formatMinsToTime(totalMins: Int): String {
        val normalized = ((totalMins % 1440) + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
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

    LaunchedEffect(calcMethod, juristicSchool) {
        fetchTimesForCoords(lat, lng)
    }

    // Function to schedule exact alarm for a prayer
    fun schedulePrayerAlarm(prayerName: String, effectiveTime: String, bufferMinutes: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val prayerDate = sdf.parse(effectiveTime) ?: return

            val calendar = Calendar.getInstance().apply {
                val pCal = Calendar.getInstance().apply { time = prayerDate }
                set(Calendar.HOUR_OF_DAY, pCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, pCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                add(Calendar.MINUTE, -bufferMinutes)
            }

            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val bufferNotice = if (bufferMinutes > 0) "$bufferMinutes mins prior to" else "at exact"
            val title = "🕌 Salah Reminder: $prayerName"
            val message = "$prayerName Salah is at $effectiveTime. Prepare to attend Jam'ah in the mosque!"
            val ttsText = if (prayerName.equals("Fajr", ignoreCase = true)) {
                "As-salatu khayrun minan-nawm. It is time for Fajr Salah. Attend Jam'ah in the mosque. Time is $effectiveTime."
            } else {
                "It is time for $prayerName Salah. Congregation Jam'ah is at $effectiveTime. Prepare for prayer."
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("message", message)
                putExtra("audio_mode", audioMode)
                putExtra("voice_profile", ttsVoice)
                putExtra("custom_tts_text", ttsText)
                putExtra("target_screen", "prayer")
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

            val displayBuffer = if (bufferMinutes > 0) "$bufferMinutes min before" else "at"
            Toast.makeText(context, "Alarm armed for $prayerName ($displayBuffer $effectiveTime)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not set exact alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun armAllPrayerAlarms() {
        val prayerList = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        for (p in prayerList) {
            val effTime = getEffectiveTime(p)
            schedulePrayerAlarm(p, effTime, reminderBufferMins)
        }
        sleeperRepo.setPrayerAlarmsEnabled(true)
        Toast.makeText(context, "All 5 Salah alarms armed with $reminderBufferMins min Jam'ah buffer!", Toast.LENGTH_LONG).show()
    }

    // Manual Time Changing / Jam'ah Sync Dialog
    editingPrayerName?.let { prayerName ->
        val apiRawTime = prayerTimes[prayerName] ?: "12:00"
        val isOverridden = manualTimes.containsKey(prayerName)

        AlertDialog(
            onDismissRequest = { editingPrayerName = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = primaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jam'ah Time: $prayerName",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Synchronize with your local mosque Iqamah or congregation time. Every minute counts for attending Jam'ah!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Aladhan API Time: $apiRawTime",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryColor
                            )
                            if (isOverridden) {
                                Text(
                                    text = "Current Manual Override: ${manualTimes[prayerName]}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    // Direct Time Input Field
                    OutlinedTextField(
                        value = editingPrayerTimeInput,
                        onValueChange = { editingPrayerTimeInput = it },
                        label = { Text("Exact Time (HH:mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    val curMins = parseMinsFromTime(editingPrayerTimeInput)
                                    editingPrayerTimeInput = formatMinsToTime(curMins - 1)
                                }) {
                                    Icon(Icons.Default.Remove, contentDescription = "-1m", tint = primaryColor)
                                }
                                IconButton(onClick = {
                                    val curMins = parseMinsFromTime(editingPrayerTimeInput)
                                    editingPrayerTimeInput = formatMinsToTime(curMins + 1)
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "+1m", tint = primaryColor)
                                }
                            }
                        }
                    )

                    // Quick Minute Adjustments Chips
                    Text(
                        text = "Quick Mosque Offset Adjustments:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val quickOffsets = listOf(-15, -10, -5, 5, 10, 15, 20)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickOffsets) { offset ->
                            val label = if (offset > 0) "+$offset m" else "$offset m"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    val curMins = parseMinsFromTime(editingPrayerTimeInput)
                                    editingPrayerTimeInput = formatMinsToTime(curMins + offset)
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (isOverridden) {
                        OutlinedButton(
                            onClick = {
                                sleeperRepo.clearPrayerManualTime(prayerName)
                                if (alarmEnabled) {
                                    schedulePrayerAlarm(prayerName, apiRawTime, reminderBufferMins)
                                }
                                editingPrayerName = null
                                Toast.makeText(context, "Reverted $prayerName to Aladhan calculated time.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset to API Calculated Time ($apiRawTime)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editingPrayerTimeInput.trim()
                        if (trimmed.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]\$"))) {
                            sleeperRepo.setPrayerManualTime(prayerName, trimmed)
                            if (alarmEnabled) {
                                schedulePrayerAlarm(prayerName, trimmed, reminderBufferMins)
                            }
                            editingPrayerName = null
                            Toast.makeText(context, "Saved Jam'ah time $trimmed for $prayerName!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter valid HH:mm format (e.g. 05:15 or 13:30)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Apply Jam'ah Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPrayerName = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Calculation Method & Juristic School Dialog
    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = {
                Text(
                    text = "Aladhan API Calculation Setup",
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Juristic School (Asr Shadow Ratio):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (juristicSchool == 1) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (juristicSchool == 1) primaryColor else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { sleeperRepo.setPrayerJuristicSchool(1) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = juristicSchool == 1,
                                        onClick = { sleeperRepo.setPrayerJuristicSchool(1) }
                                    )
                                    Column {
                                        Text("Hanafi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("2x Shadow (South Asia)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (juristicSchool == 0) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (juristicSchool == 0) primaryColor else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { sleeperRepo.setPrayerJuristicSchool(0) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = juristicSchool == 0,
                                        onClick = { sleeperRepo.setPrayerJuristicSchool(0) }
                                    )
                                    Column {
                                        Text("Standard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Shafi'i, Maliki, Hanbali", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calculation Method:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = primaryColor
                        )
                    }

                    items(ALADHAN_CALCULATION_METHODS) { method ->
                        val isSelected = calcMethod == method.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) primaryColor else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sleeperRepo.setPrayerCalculationMethod(method.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { sleeperRepo.setPrayerCalculationMethod(method.id) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = method.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = method.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text("Apply & Close")
                }
            }
        )
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

    val activeMethodName = ALADHAN_CALCULATION_METHODS.firstOrNull { it.id == calcMethod }?.name ?: "Karachi"
    val activeSchoolName = if (juristicSchool == 1) "Hanafi" else "Standard (Shafi'i)"

    AnimatedGlassyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar with Back Navigation
            GlassCard(modifier = Modifier.padding(bottom = 12.dp)) {
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
                                text = "Salah & Jam'ah Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "$locationName • $activeSchoolName",
                                fontSize = 11.sp,
                                color = secondaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { showMethodDialog = true },
                            modifier = Modifier.testTag("calc_method_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Calculation Method",
                                tint = primaryColor
                            )
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
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // STATS OVERHAUL: Daily Completion Ring & Key Analytics
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

                // JAM'AH ALARM & TTS SERVICE HUB
                item {
                    GlassCard(borderColor = primaryColor) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AlarmOn,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Jam'ah Alarm & TTS Voice Service",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = if (alarmEnabled) "Armed ($reminderBufferMins min Jam'ah Buffer)" else "Alarms Paused",
                                            fontSize = 12.sp,
                                            color = if (alarmEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = alarmEnabled,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            armAllPrayerAlarms()
                                        } else {
                                            sleeperRepo.setPrayerAlarmsEnabled(false)
                                            Toast.makeText(context, "Prayer alarms disabled.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("prayer_alarm_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Jam'ah Advance Notice Buffer:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Buffer choices
                            val bufferOptions = listOf(
                                0 to "At Salah Time",
                                5 to "5m Prior",
                                10 to "10m (Walk)",
                                15 to "15m (Jam'ah Prep)"
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(bufferOptions) { (mins, label) ->
                                    val isSelected = reminderBufferMins == mins
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) primaryColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) primaryColor else Color.Transparent),
                                        modifier = Modifier.clickable {
                                            sleeperRepo.setPrayerReminderMinutesPrior(mins)
                                            if (alarmEnabled) {
                                                armAllPrayerAlarms()
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        SleeperAudioEngine.playAlarmOrTts(
                                            context = context,
                                            mode = audioMode,
                                            customText = "As-salatu khayrun minan-nawm. It is time for Fajr prayer. Prepare for Jam'ah in the mosque. Time is ${getEffectiveTime("Fajr")}.",
                                            voiceProfile = ttsVoice
                                        )
                                        Toast.makeText(context, "Testing Voice & Alarm Sound...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("test_prayer_tts_btn")
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Voice & Chime", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { armAllPrayerAlarms() },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("arm_all_alarms_btn")
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Arm All 5 Prayers", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Sun & Astronomical Times Banner
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SUNRISE (ISHRĀQ)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryColor)
                                Text(prayerTimes["Sunrise"] ?: "05:45", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryColor)
                            }
                            Column {
                                Text("METHOD & JURISPRUDENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryColor)
                                Text("$activeSchoolName • $activeMethodName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                            if (manualTimes.isNotEmpty()) {
                                TextButton(onClick = {
                                    sleeperRepo.clearAllPrayerManualTimes()
                                    Toast.makeText(context, "Cleared all manual prayer overrides.", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Reset All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
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

                // Prayer Items List with Manual Overrides & Alarm Buttons
                val prayers = listOf(
                    "Fajr" to todayRecord.fajr,
                    "Dhuhr" to todayRecord.dhuhr,
                    "Asr" to todayRecord.asr,
                    "Maghrib" to todayRecord.maghrib,
                    "Isha" to todayRecord.isha
                )

                items(prayers) { (name, isCompleted) ->
                    val effectiveTime = getEffectiveTime(name)
                    val isOverridden = manualTimes.containsKey(name)
                    val rawApiTime = prayerTimes[name] ?: "12:00"

                    GlassCard(borderColor = if (isOverridden) primaryColor else Color.Transparent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = isCompleted,
                                    onCheckedChange = { sleeperRepo.updatePrayerCheck(name, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                                    modifier = Modifier.testTag("prayer_check_$name")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isCompleted) primaryColor else MaterialTheme.colorScheme.onBackground
                                        )
                                        if (isOverridden) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "🕌 JAM'AH TIME",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            editingPrayerName = name
                                            editingPrayerTimeInput = effectiveTime
                                        }
                                    ) {
                                        Text(
                                            text = effectiveTime,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isOverridden) primaryColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isOverridden) {
                                            Text(
                                                text = " (API: $rawApiTime)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Time",
                                            tint = secondaryColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingPrayerName = name
                                        editingPrayerTimeInput = effectiveTime
                                    },
                                    modifier = Modifier.testTag("edit_time_btn_$name")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreTime,
                                        contentDescription = "Change Jam'ah Time",
                                        tint = secondaryColor
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        schedulePrayerAlarm(name, effectiveTime, reminderBufferMins)
                                    },
                                    enabled = alarmEnabled,
                                    modifier = Modifier.testTag("alarm_btn_$name")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "Set Alarm",
                                        tint = if (alarmEnabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
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
