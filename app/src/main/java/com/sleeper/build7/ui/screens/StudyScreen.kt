package com.sleeper.build7.ui.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sleeper.build7.audio.SleeperAudioEngine
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.data.StudySession
import com.sleeper.build7.receiver.AlarmReceiver
import com.sleeper.build7.ui.components.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StudyScreen(
    sleeperRepo: SleeperRepository,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val studySessions by sleeperRepo.studySessions.collectAsState()
    val audioMode by sleeperRepo.audioAlarmMode.collectAsState()
    val ttsVoice by sleeperRepo.ttsVoiceProfile.collectAsState()
    val ttsText by sleeperRepo.ttsCustomText.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    var sessionTitle by remember { mutableStateOf("Deep Focus / Coding Session") }
    var startTime by remember { mutableStateOf("14:00") }
    var durationMinutes by remember { mutableStateOf("45") }

    // TTS / Audio Config State
    var customAlarmText by remember(ttsText) { mutableStateOf(ttsText) }
    var selectedAudioMode by remember(audioMode) { mutableStateOf(audioMode) }
    var selectedVoiceProfile by remember(ttsVoice) { mutableStateOf(ttsVoice) }

    // Active Timer State
    var activeTimerSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var currentTimerSessionTitle by remember { mutableStateOf("") }

    LaunchedEffect(isTimerRunning, activeTimerSeconds) {
        if (isTimerRunning && activeTimerSeconds > 0) {
            delay(1000L)
            activeTimerSeconds -= 1
            if (activeTimerSeconds == 0) {
                isTimerRunning = false
                // Trigger customized TTS or ringtone audio upon timer completion
                SleeperAudioEngine.playAlarmOrTts(
                    context = context,
                    mode = selectedAudioMode,
                    customText = customAlarmText,
                    voiceProfile = selectedVoiceProfile
                )
                Toast.makeText(context, "🎉 Session complete!", Toast.LENGTH_LONG).show()
            }
        }
    }

    val totalCompletedMins = studySessions.filter { it.isCompleted }.sumOf { it.durationMins }
    val totalSessionsCount = studySessions.size
    val completedCount = studySessions.count { it.isCompleted }
    val dailyTargetMins = 180f
    val progressFraction = (totalCompletedMins / dailyTargetMins).coerceIn(0f, 1f)

    // Weekly consistency computation
    val weeklyDays = remember(studySessions) {
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            val dayInitial = SimpleDateFormat("E", Locale.US).format(cal.time).take(1)
            val isDone = studySessions.any { it.date == dStr && it.isCompleted }
            dayInitial to isDone
        }
    }

    AnimatedGlassyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar with Back Navigation: Schedules ("Study & Others")
            GlassCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("study_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Hub",
                            tint = primaryColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Schedules",
                        tint = primaryColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Schedules",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Study & Others",
                            fontSize = 12.sp,
                            color = secondaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Focus Analytics
                item {
                    GlassCard(borderColor = primaryColor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AnimatedProgressRing(
                                progress = progressFraction,
                                size = 90.dp,
                                strokeWidth = 9.dp,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(progressFraction * 100).toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Goal 3h",
                                        fontSize = 10.sp,
                                        color = secondaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricStatCard(
                                    title = "Focus Time",
                                    value = "${totalCompletedMins / 60}h ${totalCompletedMins % 60}m",
                                    icon = Icons.Default.Timer,
                                    badgeText = "Total",
                                    accentColor = primaryColor,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    title = "Done",
                                    value = "$completedCount/$totalSessionsCount",
                                    icon = Icons.Default.CheckCircle,
                                    badgeText = "Tasks",
                                    accentColor = secondaryColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Consistency Heatmap
                item {
                    WeeklyConsistencyHeatmap(
                        days = weeklyDays,
                        title = "7-Day Focus Consistency",
                        activeColor = primaryColor
                    )
                }

                // Active Timer Banner (if running)
                if (activeTimerSeconds > 0 || isTimerRunning) {
                    item {
                        GlassCard(borderColor = primaryColor) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ACTIVE TIMER: $currentTimerSessionTitle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = primaryColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val mins = activeTimerSeconds / 60
                                val secs = activeTimerSeconds % 60
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 38.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { isTimerRunning = !isTimerRunning },
                                        colors = ButtonDefaults.buttonColors(containerColor = secondaryColor)
                                    ) {
                                        Text(if (isTimerRunning) "Pause" else "Resume")
                                    }
                                    Button(
                                        onClick = {
                                            isTimerRunning = false
                                            activeTimerSeconds = 0
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Stop")
                                    }
                                }
                            }
                        }
                    }
                }

                // CUSTOM AUDIO ENGINE & TTS CONTROL PANEL (Fix #8)
                item {
                    GlassCard(borderColor = secondaryColor) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = secondaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Alarm & Voice Alert Sounds",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = secondaryColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (selectedAudioMode == "tts") "TTS Active" else "Ringtone",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Mode Selector: TTS vs System Ringtone
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedAudioMode = "tts"
                                            sleeperRepo.setAudioAlarmMode("tts")
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedAudioMode == "tts") primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selectedAudioMode == "tts") primaryColor else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedAudioMode == "tts",
                                            onClick = {
                                                selectedAudioMode = "tts"
                                                sleeperRepo.setAudioAlarmMode("tts")
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Custom TTS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedAudioMode = "ringtone"
                                            sleeperRepo.setAudioAlarmMode("ringtone")
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedAudioMode == "ringtone") primaryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selectedAudioMode == "ringtone") primaryColor else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedAudioMode == "ringtone",
                                            onClick = {
                                                selectedAudioMode = "ringtone"
                                                sleeperRepo.setAudioAlarmMode("ringtone")
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ringtone",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (selectedAudioMode == "tts") {
                                // Voice Profile Selector (Cute Girl vs Cute Boy)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedVoiceProfile = "girl"
                                                sleeperRepo.setTtsVoiceProfile("girl")
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selectedVoiceProfile == "girl") Color(0xFFEC4899).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedVoiceProfile == "girl") Color(0xFFEC4899) else Color.Transparent
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedVoiceProfile == "girl",
                                                onClick = {
                                                    selectedVoiceProfile = "girl"
                                                    sleeperRepo.setTtsVoiceProfile("girl")
                                                },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFEC4899))
                                            )
                                            Text(
                                                text = "Cute Girl (~1.35x)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedVoiceProfile == "girl") Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedVoiceProfile = "boy"
                                                sleeperRepo.setTtsVoiceProfile("boy")
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selectedVoiceProfile == "boy") Color(0xFF06B6D4).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedVoiceProfile == "boy") Color(0xFF06B6D4) else Color.Transparent
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedVoiceProfile == "boy",
                                                onClick = {
                                                    selectedVoiceProfile = "boy"
                                                    sleeperRepo.setTtsVoiceProfile("boy")
                                                },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF06B6D4))
                                            )
                                            Text(
                                                text = "Cute Boy (~0.90x)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedVoiceProfile == "boy") Color(0xFF06B6D4) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = customAlarmText,
                                    onValueChange = {
                                        customAlarmText = it
                                        sleeperRepo.setTtsCustomText(it)
                                    },
                                    label = { Text("Custom Text To Read Aloud") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("tts_text_input"),
                                    singleLine = false,
                                    maxLines = 2
                                )

                                // In-app "Test Voice" button
                                OutlinedButton(
                                    onClick = {
                                        SleeperAudioEngine.speakTts(
                                            context = context,
                                            text = customAlarmText.ifBlank { "Sleeper discipline alert! Stay locked in." },
                                            voiceProfile = selectedVoiceProfile
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_voice_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Test Voice Speech Preview")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        SleeperAudioEngine.playSystemRingtone(context)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Test System Ringtone")
                                }
                            }
                        }
                    }
                }

                // Add New Schedule / Session
                item {
                    GlassCard {
                        Text(
                            text = "Add Schedule / Study Session",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = sessionTitle,
                            onValueChange = { sessionTitle = it },
                            label = { Text("Schedule Title / Subject") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("study_title_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Start Time (HH:mm)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("study_time_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = durationMinutes,
                                onValueChange = { durationMinutes = it },
                                label = { Text("Duration (mins)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("study_duration_input"),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassButton(
                            text = "Save Schedule & Set Exact Alarm",
                            onClick = {
                                val dur = durationMinutes.toIntOrNull() ?: 45
                                if (sessionTitle.isNotBlank() && startTime.isNotBlank()) {
                                    val newSession = StudySession(
                                        title = sessionTitle,
                                        startTime = startTime,
                                        durationMins = dur,
                                        date = sleeperRepo.getTodayDate()
                                    )
                                    sleeperRepo.saveStudySession(newSession)

                                    try {
                                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                                            putExtra("title", "Schedule: $sessionTitle")
                                            putExtra("message", "Time for scheduled session: $sessionTitle")
                                            putExtra("audio_mode", selectedAudioMode)
                                            putExtra("voice_profile", selectedVoiceProfile)
                                            putExtra("custom_tts_text", customAlarmText)
                                        }
                                        val pendingIntent = PendingIntent.getBroadcast(
                                            context,
                                            newSession.id.hashCode(),
                                            intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        val triggerTime = try {
                                            val sdf = SimpleDateFormat("HH:mm", Locale.US)
                                            val parsedTime = sdf.parse(startTime.trim())
                                            if (parsedTime != null) {
                                                val cal = Calendar.getInstance()
                                                val timeCal = Calendar.getInstance().apply { time = parsedTime }
                                                cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                                                cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                                                cal.set(Calendar.SECOND, 0)
                                                if (cal.timeInMillis <= System.currentTimeMillis()) {
                                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                                }
                                                cal.timeInMillis
                                            } else {
                                                System.currentTimeMillis() + 60 * 1000
                                            }
                                        } catch (e: Exception) {
                                            System.currentTimeMillis() + 60 * 1000
                                        }

                                        alarmManager.setExactAndAllowWhileIdle(
                                            AlarmManager.RTC_WAKEUP,
                                            triggerTime,
                                            pendingIntent
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    sessionTitle = ""
                                    Toast.makeText(context, "Schedule saved & alarm configured!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "add_study_btn"
                        )
                    }
                }

                item {
                    Text(
                        text = "Scheduled Sessions (${studySessions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(studySessions) { session ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${session.startTime} • ${session.durationMins} minutes | ${session.date}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        currentTimerSessionTitle = session.title
                                        activeTimerSeconds = session.durationMins * 60
                                        isTimerRunning = true
                                    },
                                    modifier = Modifier.testTag("start_timer_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Start Timer",
                                        tint = secondaryColor
                                    )
                                }

                                Checkbox(
                                    checked = session.isCompleted,
                                    onCheckedChange = { sleeperRepo.toggleStudySessionCompleted(session.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                                    modifier = Modifier.testTag("check_study_${session.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
