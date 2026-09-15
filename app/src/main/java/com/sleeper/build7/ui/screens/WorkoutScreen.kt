package com.sleeper.build7.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeper.build7.data.ExerciseStep
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.data.WorkoutPlan
import com.sleeper.build7.ui.components.AnimatedGlassyBackground
import com.sleeper.build7.ui.components.GlassButton
import com.sleeper.build7.ui.components.GlassCard
import com.sleeper.build7.ui.components.GlassyWidgetHolder
import com.sleeper.build7.ui.components.GradientChip
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

enum class WorkoutTab(val title: String) {
    COACH("⚡ Coach"),
    CARDIO("💓 Cardio"),
    ROUTINES("🏋️ Routines"),
    HYDRATION("💧 Hydration"),
    LOGS("📜 Logs")
}

@Composable
fun WorkoutScreen(
    sleeperRepo: SleeperRepository,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val workoutPlans by sleeperRepo.workoutPlans.collectAsState()
    val musicPkg by sleeperRepo.musicAppPackage.collectAsState()
    val selectedWidgetProvider by sleeperRepo.selectedWidgetProvider.collectAsState()
    val waterIntakeMl by sleeperRepo.waterIntakeMl.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    var currentTab by remember { mutableStateOf(WorkoutTab.COACH) }

    // Body Metrics & Plan Inputs
    var weightInput by remember { mutableStateOf("72.0") }
    var heightInput by remember { mutableStateOf("178.0") }
    var selectedEquipmentMode by remember { mutableStateOf("Bodyweight") }
    var workoutTime by remember { mutableStateOf("17:00") }
    var durationMins by remember { mutableStateOf("35") }

    var calculatedBmi by remember { mutableFloatStateOf(22.7f) }
    var bmiCategory by remember { mutableStateOf("Normal Weight") }
    var rankTitle by remember { mutableStateOf("Sleeper Warrior (Lv 2)") }

    // TTS Setup for Voice Coaching
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        tts.language = Locale.US
        ttsInstance = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speakVoice(text: String) {
        if (isTtsReady && ttsInstance != null) {
            ttsInstance?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WorkoutCoachTTS")
        }
    }

    // Audio Tone Generator for Countdown Chimes & Step Finishes
    val toneGen = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            null
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGen?.release()
            } catch (e: Exception) {}
        }
    }

    fun playChime(tone: Int, durationMs: Int = 180) {
        try {
            toneGen?.startTone(tone, durationMs)
        } catch (e: Exception) {}
    }

    // Active Routine Exercises (Editable)
    var activeExercises by remember {
        mutableStateOf(
            listOf(
                ExerciseStep(name = "Push-ups", target = "10 Reps", restSecs = 30, formTip = "Keep core rigid, lower chest within an inch of the floor."),
                ExerciseStep(name = "Sit-ups", target = "10 Reps", restSecs = 30, formTip = "Engage upper abdominal wall, do not pull on neck."),
                ExerciseStep(name = "Squats", target = "15 Reps", restSecs = 30, formTip = "Maintain heels grounded, hips back and down."),
                ExerciseStep(name = "Pull-ups", target = "8 Reps", restSecs = 35, formTip = "Full extension at bottom, clear chin over bar."),
                ExerciseStep(name = "Plank Hold", target = "45 Secs", restSecs = 30, formTip = "Straight spine line, active glutes and steady breathing.")
            )
        )
    }

    // Real-time clock for scheduled reminder check
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            currentTimeString = sdf.format(Date())
            delay(3000L)
        }
    }

    val todayDate = sleeperRepo.getTodayDate()
    val todayPlan = workoutPlans.firstOrNull { it.date == todayDate }

    // User requirement: "it should guide us on realtime when it's time to workout. like, it's time but we didn't tapped the start yet, it will remind us to start."
    val isScheduledTimeArrived = remember(currentTimeString, workoutTime, todayPlan) {
        if (todayPlan != null && todayPlan.isCompleted) false
        else {
            try {
                val currentMins = parseMins(currentTimeString)
                val targetMins = parseMins(workoutTime)
                currentMins >= targetMins && targetMins > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    // Interactive Realtime Session States
    var isGuidedWorkoutActive by remember { mutableStateOf(false) }
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var activeSessionSeconds by remember { mutableIntStateOf(0) }
    var isRestingMode by remember { mutableStateOf(false) }
    var restTimeRemainingSecs by remember { mutableIntStateOf(0) }
    var isWorkoutFinishedCelebration by remember { mutableStateOf(false) }
    var simulatedHeartRate by remember { mutableIntStateOf(138) }

    // Exercise Dialog
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var newExName by remember { mutableStateOf("") }
    var newExTarget by remember { mutableStateOf("12 Reps") }

    fun recalculateBmi() {
        val w = weightInput.toFloatOrNull() ?: 72f
        val hCm = heightInput.toFloatOrNull() ?: 178f
        val hM = hCm / 100f
        if (hM > 0) {
            val bmi = w / (hM * hM)
            calculatedBmi = bmi
            bmiCategory = when {
                bmi < 18.5f -> "Underweight (Need Surplus)"
                bmi < 25f -> "Normal Weight (Optimal Recomp)"
                bmi < 30f -> "Overweight (Metabolic Cut)"
                else -> "High Body Fat (Cardio Shred)"
            }

            val totalWorkouts = workoutPlans.count { it.isCompleted }
            rankTitle = when {
                totalWorkouts >= 30 -> "Sleeper God (Lv 5)"
                totalWorkouts >= 15 -> "Iron Titan (Lv 4)"
                totalWorkouts >= 7 -> "Beast Mode (Lv 3)"
                totalWorkouts >= 3 -> "Sleeper Warrior (Lv 2)"
                else -> "Sleeper Initiate (Lv 1)"
            }
        }
    }

    LaunchedEffect(weightInput, heightInput) {
        recalculateBmi()
    }

    // Live Coach Session Loop: Seconds tick, Rest countdown, Audio Cues
    LaunchedEffect(isGuidedWorkoutActive, isWorkoutFinishedCelebration, isRestingMode) {
        while (isGuidedWorkoutActive && !isWorkoutFinishedCelebration) {
            delay(1000L)
            activeSessionSeconds += 1

            // Dynamic Heart Rate variation (132 - 156 BPM during workout, 110 - 124 during rest)
            simulatedHeartRate = if (isRestingMode) {
                (115 + sin(activeSessionSeconds.toDouble() * 0.3) * 6).toInt()
            } else {
                (142 + sin(activeSessionSeconds.toDouble() * 0.4) * 10).toInt()
            }

            if (isRestingMode && restTimeRemainingSecs > 0) {
                restTimeRemainingSecs -= 1

                // Audio beep countdown on 3, 2, 1
                if (restTimeRemainingSecs in 1..3) {
                    playChime(ToneGenerator.TONE_PROP_BEEP, 120)
                }

                if (restTimeRemainingSecs == 0) {
                    playChime(ToneGenerator.TONE_PROP_ACK, 250)
                    isRestingMode = false
                    if (currentExerciseIndex < activeExercises.size - 1) {
                        currentExerciseIndex += 1
                        val nextEx = activeExercises[currentExerciseIndex]
                        speakVoice("Rest complete! Now perform ${nextEx.target} of ${nextEx.name}. Tap complete when done!")
                    } else {
                        isWorkoutFinishedCelebration = true
                        speakVoice("Outstanding effort! Workout completed! You have leveled up!")
                    }
                }
            }
        }
    }

    AnimatedGlassyBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar with Back Navigation
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("workout_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Hub",
                                    tint = primaryColor
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "Workout",
                                tint = primaryColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Workout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // HERO PRO ATHLETE HEADER
                item {
                    GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FITBIT PRO ATHLETE",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = primaryColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = secondaryColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "LIVE ACTIVITY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = rankTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Streak: ${workoutPlans.count { it.isCompleted }} Workouts • Recomp Stage",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = "Fitness Icon",
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // FITBIT-STYLE 3-RING ACTIVITY DASHBOARD
            item {
                FitbitActivityRingsCard(
                    activeMins = activeSessionSeconds / 60,
                    caloriesBurned = (activeSessionSeconds * 0.18f).toInt() + (if (todayPlan?.isCompleted == true) 280 else 40),
                    exercisesDone = if (isGuidedWorkoutActive) currentExerciseIndex else if (todayPlan?.isCompleted == true) activeExercises.size else 0,
                    totalExercises = activeExercises.size,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor
                )
            }

            // NAVIGATION PILL ROW
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(WorkoutTab.values()) { tab ->
                        GradientChip(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            label = tab.title,
                            testTag = "tab_${tab.name.lowercase()}"
                        )
                    }
                }
            }

            // TAB 1: COACH (LIVE WORKOUT & REALTIME GUIDED FLOW)
            if (currentTab == WorkoutTab.COACH) {
                // REALTIME SCHEDULED NOTIFICATION REMINDER BANNER
                if (isScheduledTimeArrived && !isGuidedWorkoutActive && todayPlan?.isCompleted != true) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(MaterialTheme.colorScheme.error, primaryColor)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("realtime_workout_alert"),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Workout Alert",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TIME TO WORKOUT! ($workoutTime)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "It is scheduled workout time, but you haven't tapped start yet! Your coach is waiting to guide you step-by-step.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                GlassButton(
                                    text = "🔥 START GUIDED SESSION NOW",
                                    onClick = {
                                        isGuidedWorkoutActive = true
                                        currentExerciseIndex = 0
                                        activeSessionSeconds = 0
                                        isRestingMode = false
                                        isWorkoutFinishedCelebration = false
                                        playChime(ToneGenerator.TONE_PROP_ACK, 250)
                                        val first = activeExercises.firstOrNull()
                                        speakVoice("Workout started! First exercise: ${first?.target} of ${first?.name}. Keep good form and tap complete when finished!")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "urgent_start_workout_btn"
                                )
                            }
                        }
                    }
                }

                // GUIDED WORKOUT ACTIVE ENGINE
                if (isGuidedWorkoutActive) {
                    item {
                        if (isWorkoutFinishedCelebration) {
                            // VICTORY CELEBRATION SUMMARY
                            GlassCard(borderColor = primaryColor) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Victory Trophy",
                                        tint = primaryColor,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "WORKOUT ACCOMPLISHED!",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = primaryColor
                                    )
                                    Text(
                                        text = "All ${activeExercises.size} exercises completed with flawless pacing!",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        SessionMetricBadge("Time", formatTimer(activeSessionSeconds), primaryColor)
                                        SessionMetricBadge("Burned", "${(activeSessionSeconds * 0.18f).toInt()} kcal", secondaryColor)
                                        SessionMetricBadge("Peak HR", "154 BPM", primaryColor)
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    GlassButton(
                                        text = "Save & Log Session to History",
                                        onClick = {
                                            val plan = WorkoutPlan(
                                                date = todayDate,
                                                weightKg = weightInput.toFloatOrNull() ?: 72f,
                                                heightCm = heightInput.toFloatOrNull() ?: 178f,
                                                bmi = calculatedBmi,
                                                equipmentMode = selectedEquipmentMode,
                                                targetSetsReps = activeExercises.joinToString(", ") { "${it.name}: ${it.target}" },
                                                workoutTime = workoutTime,
                                                durationMins = (activeSessionSeconds / 60).coerceAtLeast(1),
                                                isCompleted = true,
                                                levelRank = rankTitle
                                            )
                                            sleeperRepo.saveWorkoutPlan(plan)
                                            isGuidedWorkoutActive = false
                                            isWorkoutFinishedCelebration = false
                                            Toast.makeText(context, "Workout saved! Great job!", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "save_finished_workout_btn"
                                    )
                                }
                            }
                        } else if (isRestingMode) {
                            // INTERACTIVE REST INTERVAL SCREEN
                            val nextExercise = activeExercises.getOrNull(currentExerciseIndex + 1)
                            GlassCard(borderColor = secondaryColor) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "REST & RECOVERY INTERVAL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = secondaryColor
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Circular Rest Timer
                                    CircularTimerIndicator(
                                        remainingSecs = restTimeRemainingSecs,
                                        totalSecs = activeExercises[currentExerciseIndex].restSecs,
                                        color = secondaryColor
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (nextExercise != null) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp)),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "UP NEXT:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = primaryColor
                                                )
                                                Text(
                                                    text = "${nextExercise.target} of ${nextExercise.name}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = nextExercise.formTip,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { restTimeRemainingSecs += 10 },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("+10s Rest")
                                        }
                                        GlassButton(
                                            text = "Skip Rest ▶",
                                            onClick = {
                                                isRestingMode = false
                                                playChime(ToneGenerator.TONE_PROP_BEEP, 120)
                                                if (currentExerciseIndex < activeExercises.size - 1) {
                                                    currentExerciseIndex += 1
                                                    val nEx = activeExercises[currentExerciseIndex]
                                                    speakVoice("Next exercise: ${nEx.target} of ${nEx.name}. Tap complete when done!")
                                                } else {
                                                    isWorkoutFinishedCelebration = true
                                                    speakVoice("Workout completed! Awesome work!")
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            testTag = "skip_rest_btn"
                                        )
                                    }
                                }
                            }
                        } else {
                            // ACTIVE EXERCISE STEP INSTRUCTION CARD
                            val currentEx = activeExercises.getOrNull(currentExerciseIndex) ?: activeExercises.first()
                            GlassCard(borderColor = primaryColor) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Telemetry row: Step number & Live Heart Rate
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "EXERCISE ${currentExerciseIndex + 1} OF ${activeExercises.size}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = secondaryColor
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Heart Rate",
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$simulatedHeartRate BPM",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.Red
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = formatTimer(activeSessionSeconds),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = primaryColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Progress bar
                                    LinearProgressIndicator(
                                        progress = { (currentExerciseIndex + 1).toFloat() / activeExercises.size },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = primaryColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Exercise Title & Target
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentEx.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 24.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "Target: ${currentEx.target}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = primaryColor
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                speakVoice("Perform ${currentEx.target} of ${currentEx.name}. ${currentEx.formTip}")
                                            },
                                            modifier = Modifier.testTag("tts_repeat_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Voice Tip",
                                                tint = secondaryColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Form Tip Card
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp)),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Form Tip",
                                                tint = secondaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = currentEx.formTip,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Complete Button
                                    // User instruction: "like do 10 push-ups, after we complete it, we'll tap on complete, then it'll tell us to do 10 sit ups..."
                                    GlassButton(
                                        text = "✅ COMPLETE EXERCISE",
                                        onClick = {
                                            playChime(ToneGenerator.TONE_PROP_ACK, 200)
                                            if (currentExerciseIndex < activeExercises.size - 1) {
                                                isRestingMode = true
                                                restTimeRemainingSecs = currentEx.restSecs
                                                val nextEx = activeExercises[currentExerciseIndex + 1]
                                                speakVoice("Great set! Take a ${currentEx.restSecs} seconds rest. Next exercise will be ${nextEx.target} of ${nextEx.name}.")
                                            } else {
                                                isWorkoutFinishedCelebration = true
                                                speakVoice("Awesome! You completed all exercises!")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "complete_exercise_btn"
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(
                                        onClick = { isGuidedWorkoutActive = false },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("Pause Session", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // COACH START BANNER (If session not active)
                    item {
                        GlassCard(borderColor = primaryColor) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Interactive Realtime Coach",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = primaryColor
                                        )
                                        Text(
                                            text = "${activeExercises.size} exercises • Est. ${(activeExercises.size * 4)} mins",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { showAddExerciseDialog = true },
                                        modifier = Modifier.testTag("add_custom_ex_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Step",
                                            tint = secondaryColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                GlassButton(
                                    text = "▶ START REALTIME GUIDED SESSION",
                                    onClick = {
                                        isGuidedWorkoutActive = true
                                        currentExerciseIndex = 0
                                        activeSessionSeconds = 0
                                        isRestingMode = false
                                        isWorkoutFinishedCelebration = false
                                        playChime(ToneGenerator.TONE_PROP_ACK, 250)
                                        val first = activeExercises.firstOrNull()
                                        speakVoice("Workout started! First: ${first?.target} of ${first?.name}. Tap complete when done!")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "start_guided_session_btn"
                                )
                            }
                        }
                    }
                }

                // EXERCISE ROUTINE STEP AUDIT
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Routine Steps Overview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = primaryColor
                            )
                            TextButton(onClick = { showAddExerciseDialog = true }) {
                                Text("+ Add Exercise", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        activeExercises.forEachIndexed { idx, ex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(24.dp),
                                        shape = CircleShape,
                                        color = if (isGuidedWorkoutActive && currentExerciseIndex == idx) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${idx + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGuidedWorkoutActive && currentExerciseIndex == idx) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = ex.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "${ex.target} • ${ex.restSecs}s rest", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (!isGuidedWorkoutActive && activeExercises.size > 1) {
                                    IconButton(
                                        onClick = { activeExercises = activeExercises.filterIndexed { i, _ -> i != idx } }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (idx < activeExercises.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            // TAB 2: CARDIO & FITBIT TELEMETRY
            if (currentTab == WorkoutTab.CARDIO) {
                item {
                    GlassCard {
                        Text(
                            text = "Fitbit Cardio Intensity Zones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Heart rate zone list
                        CardioZoneBar("Peak Zone (155 - 185 BPM)", "Max athletic output & VO2 max boost", Color(0xFFE53935), 0.85f)
                        Spacer(modifier = Modifier.height(8.dp))
                        CardioZoneBar("Cardio Zone (130 - 154 BPM)", "Cardiovascular endurance & stamina", Color(0xFFFF9800), 0.65f)
                        Spacer(modifier = Modifier.height(8.dp))
                        CardioZoneBar("Fat Burn Zone (100 - 129 BPM)", "Optimal lipid oxidation & fat loss", Color(0xFF4CAF50), 0.45f)
                        Spacer(modifier = Modifier.height(8.dp))
                        CardioZoneBar("Warm Up / Recovery (80 - 99 BPM)", "Gentle active mobility & cool down", Color(0xFF2196F3), 0.25f)
                    }
                }

                item {
                    GlassCard {
                        Text(
                            text = "Cardio Health Indicators",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = secondaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ESTIMATED VO2 MAX", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("48.5 ml/kg/min", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)
                                Text("Superior Range", fontSize = 11.sp, color = primaryColor)
                            }
                            Column {
                                Text("RESTING HEART RATE", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("58 BPM", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = secondaryColor)
                                Text("Athletic Baseline", fontSize = 11.sp, color = secondaryColor)
                            }
                            Column {
                                Text("METs CAPACITY", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("11.2 METs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                                Text("High Efficiency", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // TAB 3: ROUTINES & BODY METRICS PLANNER
            if (currentTab == WorkoutTab.ROUTINES) {
                item {
                    GlassCard {
                        Text(
                            text = "Preset Workout Programs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = secondaryColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val modes = listOf("Bodyweight", "Simple Equipment", "Full Equipment", "HIIT Fat Burn", "Core Shred")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(modes) { mode ->
                                GradientChip(
                                    selected = selectedEquipmentMode == mode,
                                    onClick = {
                                        selectedEquipmentMode = mode
                                        activeExercises = when (mode) {
                                            "Bodyweight" -> listOf(
                                                ExerciseStep(name = "Push-ups", target = "10 Reps", restSecs = 30, formTip = "Keep back straight and chest low."),
                                                ExerciseStep(name = "Sit-ups", target = "10 Reps", restSecs = 30, formTip = "Engage core, no neck pulling."),
                                                ExerciseStep(name = "Squats", target = "15 Reps", restSecs = 30, formTip = "Hips back, knees tracking toes."),
                                                ExerciseStep(name = "Pull-ups", target = "8 Reps", restSecs = 35, formTip = "Full extension at bottom."),
                                                ExerciseStep(name = "Plank Hold", target = "45 Secs", restSecs = 30, formTip = "Solid straight line.")
                                            )
                                            "Simple Equipment" -> listOf(
                                                ExerciseStep(name = "Dumbbell Press", target = "12 Reps", restSecs = 30, formTip = "Control weight down, push forcefully up."),
                                                ExerciseStep(name = "Resistance Rows", target = "15 Reps", restSecs = 30, formTip = "Squeeze shoulder blades."),
                                                ExerciseStep(name = "Kettlebell Swings", target = "20 Reps", restSecs = 35, formTip = "Hip hinge explosion."),
                                                ExerciseStep(name = "Goblet Squats", target = "15 Reps", restSecs = 30, formTip = "Hold weight at chest.")
                                            )
                                            "HIIT Fat Burn" -> listOf(
                                                ExerciseStep(name = "Burpees", target = "12 Reps", restSecs = 25, formTip = "Explosive jump at the top."),
                                                ExerciseStep(name = "Mountain Climbers", target = "30 Reps", restSecs = 20, formTip = "Drive knees fast to chest."),
                                                ExerciseStep(name = "Jumping Jacks", target = "40 Reps", restSecs = 20, formTip = "Stay light on balls of feet."),
                                                ExerciseStep(name = "High Knees", target = "30 Secs", restSecs = 25, formTip = "Pump arms rhythmically.")
                                            )
                                            "Core Shred" -> listOf(
                                                ExerciseStep(name = "Bicycle Crunches", target = "20 Reps", restSecs = 25, formTip = "Elbow to opposite knee."),
                                                ExerciseStep(name = "Leg Raises", target = "15 Reps", restSecs = 25, formTip = "Keep lower back glued down."),
                                                ExerciseStep(name = "Russian Twists", target = "25 Reps", restSecs = 25, formTip = "Rotate torso fully."),
                                                ExerciseStep(name = "Hollow Hold", target = "40 Secs", restSecs = 30, formTip = "Press lower back firmly down.")
                                            )
                                            else -> listOf(
                                                ExerciseStep(name = "Barbell Bench Press", target = "10 Reps", restSecs = 45, formTip = "Plant feet firmly, control descent."),
                                                ExerciseStep(name = "Lat Pulldowns", target = "12 Reps", restSecs = 35, formTip = "Pull bar to upper chest."),
                                                ExerciseStep(name = "Deadlifts", target = "8 Reps", restSecs = 60, formTip = "Keep bar tight to shins."),
                                                ExerciseStep(name = "Cable Flyes", target = "15 Reps", restSecs = 30, formTip = "Squeeze chest at center.")
                                            )
                                        }
                                    },
                                    label = mode,
                                    testTag = "mode_$mode"
                                )
                            }
                        }
                    }
                }

                item {
                    GlassCard {
                        Text(
                            text = "Body Metrics & Schedule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = heightInput,
                                onValueChange = { heightInput = it },
                                label = { Text("Height (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "BMI: ${String.format(Locale.US, "%.1f", calculatedBmi)} ($bmiCategory)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = primaryColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = workoutTime,
                                onValueChange = { workoutTime = it },
                                label = { Text("Daily Time (HH:mm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = durationMins,
                                onValueChange = { durationMins = it },
                                label = { Text("Duration (mins)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        GlassButton(
                            text = "Save Scheduled Plan",
                            onClick = {
                                val plan = WorkoutPlan(
                                    date = todayDate,
                                    weightKg = weightInput.toFloatOrNull() ?: 72f,
                                    heightCm = heightInput.toFloatOrNull() ?: 178f,
                                    bmi = calculatedBmi,
                                    equipmentMode = selectedEquipmentMode,
                                    targetSetsReps = activeExercises.joinToString(", ") { "${it.name}: ${it.target}" },
                                    workoutTime = workoutTime,
                                    durationMins = durationMins.toIntOrNull() ?: 30,
                                    isCompleted = todayPlan?.isCompleted ?: false,
                                    levelRank = rankTitle
                                )
                                sleeperRepo.saveWorkoutPlan(plan)
                                Toast.makeText(context, "Saved scheduled workout plan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "save_schedule_plan_btn"
                        )
                    }
                }
            }

            // TAB 4: HYDRATION & RECOVERY FUEL
            if (currentTab == WorkoutTab.HYDRATION) {
                item {
                    GlassCard {
                        Text(
                            text = "Hydration & Workout Fuel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Water progress cylinder
                        val targetWater = 3000
                        val progressFraction = (waterIntakeMl.toFloat() / targetWater).coerceIn(0f, 1f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$waterIntakeMl / $targetWater ml",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = primaryColor
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}% of daily hydration target",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = primaryColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { sleeperRepo.addWaterIntake(250) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+250ml Glass")
                            }
                            OutlinedButton(
                                onClick = { sleeperRepo.addWaterIntake(500) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+500ml Bottle")
                            }
                        }
                    }
                }
            }

            // TAB 5: LOGS & HISTORY AUDIT
            if (currentTab == WorkoutTab.LOGS) {
                item {
                    Text(
                        text = "Workout Tracker Audit & History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (workoutPlans.isEmpty()) {
                    item {
                        GlassCard {
                            Text(
                                text = "No workout logs found yet. Complete a guided session to populate your history.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(workoutPlans) { plan ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${plan.date} • ${plan.equipmentMode}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (plan.isCompleted) primaryColor else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (plan.isCompleted) {
                                        Text(
                                            text = "DONE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(primaryColor)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "PENDING / MISSED",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.error)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = plan.targetSetsReps,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "BMI: ${String.format(Locale.US, "%.1f", plan.bmi)} | ${plan.durationMins} mins | ${plan.levelRank}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Checkbox(
                                checked = plan.isCompleted,
                                onCheckedChange = { isChecked ->
                                    sleeperRepo.saveWorkoutPlan(plan.copy(isCompleted = isChecked))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                                modifier = Modifier.testTag("check_workout_${plan.date}")
                            )
                        }
                    }
                }
            }

            // Extra bottom space for floating elements
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // UPGRADED GLASSY WIDGET HOLDER (FLOATING)
        // Strictly implements user prompt:
        // "keep the floating music trigger, upgrade it, make it a premium Glassy widget holder,
        // which will contains widget of the added package (app)...
        // if it founds them show option to choose one from them even if only one option is available,
        // and if no if can't find any widget then it'll show a text, no widget founded.
        // then this button will trigger the app, or we select a widget then this button will open a the widget on a Premium glassy ui.
        // has a minus (➖)button to minimize the widget container. as previous, tap and hold triggers the package input option."
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            GlassyWidgetHolder(
                packageName = musicPkg,
                selectedWidgetClassName = selectedWidgetProvider,
                onPackageNameChange = { sleeperRepo.setMusicAppPackage(it) },
                onWidgetSelected = { sleeperRepo.setSelectedWidgetProvider(it) }
            )
        }

        // ADD CUSTOM EXERCISE STEP DIALOG
        if (showAddExerciseDialog) {
            AlertDialog(
                onDismissRequest = { showAddExerciseDialog = false },
                title = { Text("Add Step to Routine", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newExName,
                            onValueChange = { newExName = it },
                            label = { Text("Exercise Name (e.g. Diamond Push-ups)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newExTarget,
                            onValueChange = { newExTarget = it },
                            label = { Text("Target Reps/Secs (e.g. 12 Reps)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newExName.isNotBlank()) {
                                activeExercises = activeExercises + ExerciseStep(
                                    name = newExName.trim(),
                                    target = newExTarget.trim(),
                                    restSecs = 30,
                                    formTip = "Maintain disciplined cadence and engaged posture."
                                )
                                newExName = ""
                                showAddExerciseDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddExerciseDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    }
}

@Composable
fun FitbitActivityRingsCard(
    activeMins: Int,
    caloriesBurned: Int,
    exercisesDone: Int,
    totalExercises: Int,
    primaryColor: Color,
    secondaryColor: Color
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Activity Rings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calories: $caloriesBurned / 450 kcal", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Active: $activeMins / 35 mins", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(secondaryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exercises: $exercisesDone / $totalExercises", fontSize = 11.sp)
                }
            }

            // Triple concentric activity rings
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val centerOffset = Offset(size.width / 2, size.height / 2)

                    // Outer Ring (Calories)
                    val r1 = size.width / 2 - stroke / 2
                    drawCircle(
                        color = Color(0xFFE53935).copy(alpha = 0.2f),
                        radius = r1,
                        center = centerOffset,
                        style = Stroke(stroke)
                    )
                    drawArc(
                        color = Color(0xFFE53935),
                        startAngle = -90f,
                        sweepAngle = (caloriesBurned.toFloat() / 450f).coerceIn(0.05f, 1f) * 360f,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - r1, centerOffset.y - r1),
                        size = Size(r1 * 2, r1 * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )

                    // Middle Ring (Active Minutes)
                    val r2 = r1 - stroke - 3.dp.toPx()
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f),
                        radius = r2,
                        center = centerOffset,
                        style = Stroke(stroke)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = (activeMins.toFloat() / 35f).coerceIn(0.05f, 1f) * 360f,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - r2, centerOffset.y - r2),
                        size = Size(r2 * 2, r2 * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )

                    // Inner Ring (Exercises)
                    val r3 = r2 - stroke - 3.dp.toPx()
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.2f),
                        radius = r3,
                        center = centerOffset,
                        style = Stroke(stroke)
                    )
                    drawArc(
                        color = secondaryColor,
                        startAngle = -90f,
                        sweepAngle = (exercisesDone.toFloat() / totalExercises.coerceAtLeast(1).toFloat()).coerceIn(0.05f, 1f) * 360f,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - r3, centerOffset.y - r3),
                        size = Size(r3 * 2, r3 * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun CircularTimerIndicator(
    remainingSecs: Int,
    totalSecs: Int,
    color: Color
) {
    val progress = if (totalSecs > 0) remainingSecs.toFloat() / totalSecs else 0f
    Box(
        modifier = Modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawCircle(
                color = color.copy(alpha = 0.2f),
                style = Stroke(stroke)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${remainingSecs}s",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = color
            )
            Text(
                text = "REST",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CardioZoneBar(
    title: String,
    desc: String,
    color: Color,
    fraction: Float
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
            Text("${(fraction * 100).toInt()}% Intensity", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SessionMetricBadge(label: String, value: String, color: Color) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun parseMins(hhMm: String): Int {
    val parts = hhMm.split(":")
    if (parts.size == 2) {
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }
    return 0
}

private fun formatTimer(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
