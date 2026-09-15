package com.sleeper.build7.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.ui.components.AnimatedProgressRing
import com.sleeper.build7.ui.components.GlassButton
import com.sleeper.build7.ui.components.GlassCard
import com.sleeper.build7.ui.components.MetricStatCard
import com.sleeper.build7.ui.components.StreakCounterBadge
import com.sleeper.build7.ui.theme.NeonAmber
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoFapScreen(
    sleeperRepo: SleeperRepository,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val streakData by sleeperRepo.noFapStreak.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showMotivationalOverlay by remember { mutableStateOf(false) }
    var resetNoteInput by remember { mutableStateOf("") }
    var currentMotivationalQuote by remember { mutableStateOf("") }

    val quotes = listOf(
        "\"The first and greatest victory is to conquer yourself.\" — Plato",
        "\"Discipline is choosing between what you want now and what you want most.\"",
        "\"A temporary pleasure is never worth a permanent setback.\"",
        "\"Fall seven times, stand up eight. Reset today and claim your glory!\"",
        "\"Your mind is a muscle. Train it to resist weakness.\""
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    val isStarted = streakData.isTimerStarted && streakData.lastRelapseTimestamp > 0L
    val diffMillis = if (isStarted) (currentTime - streakData.lastRelapseTimestamp).coerceAtLeast(0L) else 0L
    val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    val hours = ((diffMillis / (1000 * 60 * 60)) % 24).toInt()
    val minutes = ((diffMillis / (1000 * 60)) % 60).toInt()
    val seconds = ((diffMillis / 1000) % 60).toInt()

    val rankTitle = when {
        !isStarted -> "Timer Not Started"
        days >= 90 -> "Sleeper God (Lv 6)"
        days >= 30 -> "Titan Overlord (Lv 5)"
        days >= 14 -> "Monk Master (Lv 4)"
        days >= 7 -> "Iron Mind Warrior (Lv 3)"
        days >= 3 -> "Discipline Apprentice (Lv 2)"
        else -> "Initiate (Lv 1)"
    }

    // Milestone target computation
    val nextMilestoneDays = when {
        days < 3 -> 3
        days < 7 -> 7
        days < 14 -> 14
        days < 30 -> 30
        days < 90 -> 90
        else -> 180
    }
    val milestoneFraction = (days.toFloat() / nextMilestoneDays.toFloat()).coerceIn(0f, 1f)

    // Motivational Overlay
    if (showMotivationalOverlay) {
        AlertDialog(
            onDismissRequest = { showMotivationalOverlay = false },
            title = {
                Text(
                    text = "💪 Stand Up & Rebuild!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = primaryColor
                )
            },
            text = {
                Column {
                    Text(
                        text = currentMotivationalQuote,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your past record was $days days. Use this reset as fuel for your next ultimate streak!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMotivationalOverlay = false }) {
                    Text("I'm Ready To Lock In")
                }
            }
        )
    }

    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Confirm Relapse / Reset?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text("Resetting your streak will record your $days-day run and reset your timer. Are you sure?")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetNoteInput,
                        onValueChange = { resetNoteInput = it },
                        label = { Text("Trigger / Reflection Note (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sleeperRepo.resetStreak("no_fap", resetNoteInput)
                        showResetDialog = false
                        currentMotivationalQuote = quotes.random()
                        showMotivationalOverlay = true
                        resetNoteInput = ""
                    }
                ) {
                    Text("Confirm Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // NO ANIMATED BACKGROUND HERE (CRITICAL EXCEPTION: Keep custom theme background)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Bar with Back Navigation
        GlassCard(modifier = Modifier.padding(bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("nofap_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Hub",
                        tint = primaryColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "No Fap",
                    tint = primaryColor,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NoFap",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Live Counter & Rank Card
            item {
                GlassCard(borderColor = primaryColor) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = rankTitle.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = secondaryColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isStarted) {
                            Text(
                                text = "READY TO START",
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = secondaryColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Timer will not count until you manually tap Start below.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassButton(
                                text = "🚀 Start Discipline Counter Now",
                                onClick = {
                                    sleeperRepo.startStreakTimer("no_fap")
                                    Toast.makeText(context, "No Fap timer started! Lock in!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "start_nofap_timer_btn"
                            )
                        } else {
                            Text(
                                text = "$days DAYS",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 44.sp,
                                color = primaryColor
                            )

                            Text(
                                text = String.format(Locale.US, "%02dh : %02dm : %02ds", hours, minutes, seconds),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            GlassButton(
                                text = "⚠️ Relapse / Reset Timer",
                                onClick = { showResetDialog = true },
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "nofap_reset_btn"
                            )
                        }
                    }
                }
            }

            // PREMIUM STATS OVERHAUL: Milestone Ring & Key Metrics
            if (isStarted) {
                item {
                    GlassCard(borderColor = secondaryColor) {
                        Text(
                            text = "Dopamine & Milestone Analytics",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AnimatedProgressRing(
                                progress = milestoneFraction,
                                size = 96.dp,
                                strokeWidth = 10.dp,
                                primaryColor = NeonAmber,
                                secondaryColor = primaryColor
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(milestoneFraction * 100).toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${nextMilestoneDays}d Goal",
                                        fontSize = 10.sp,
                                        color = secondaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricStatCard(
                                        title = "Record",
                                        value = "${streakData.highestStreakDays}d",
                                        icon = Icons.Default.EmojiEvents,
                                        badgeText = "All-Time",
                                        accentColor = NeonAmber,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricStatCard(
                                        title = "Relapses",
                                        value = "${streakData.totalRelapses}",
                                        icon = Icons.Default.Shield,
                                        badgeText = "Total",
                                        accentColor = primaryColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    StreakCounterBadge(
                        currentStreakDays = days,
                        bestStreakDays = streakData.highestStreakDays,
                        label = "Mental Willpower Streak",
                        accentColor = NeonAmber
                    )
                }
            }

            // Benefits & Guidance Panel
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Benefits",
                            tint = secondaryColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Physiological & Mental Milestones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val benefits = listOf(
                        "🧠 Dopamine Sensitivity: Receptors begin upregulation after 7 days.",
                        "⚡ Baseline Vitality: Cortisol normalizes, testosterone surges around Day 7.",
                        "🎯 Focus & Composure: Brain fog clears and emotional stability returns.",
                        "🛡️ Social Confidence: Eye contact improves and social anxiety diminishes."
                    )

                    benefits.forEach { benefit ->
                        Text(
                            text = benefit,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Streak History Log
            item {
                Text(
                    text = "Streak Reset History (${streakData.history.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(streakData.history) { entry ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Streak: ${entry.daysSucceeded} Days",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = primaryColor
                            )
                            if (entry.note.isNotBlank()) {
                                Text(
                                    text = "Note: ${entry.note}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(entry.timestamp)),
                            fontSize = 12.sp,
                            color = secondaryColor
                        )
                    }
                }
            }
        }
    }
}
