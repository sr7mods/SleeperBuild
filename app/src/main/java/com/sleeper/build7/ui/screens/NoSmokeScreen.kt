package com.sleeper.build7.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.ui.components.*
import com.sleeper.build7.ui.theme.NeonAmber
import com.sleeper.build7.ui.theme.NeonCyan
import com.sleeper.build7.ui.theme.NeonGreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoSmokeScreen(
    sleeperRepo: SleeperRepository,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val streakData by sleeperRepo.noSmokeStreak.collectAsState()
    val smokeMode by sleeperRepo.smokeModuleMode.collectAsState()
    val totalSmoked by sleeperRepo.totalSmokeCount.collectAsState()
    val todaySmoked by sleeperRepo.todaySmokeCount.collectAsState()
    val lastSmokedTime by sleeperRepo.lastSmokeTimestamp.collectAsState()

    val isDemonic = smokeMode == "demonic"

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showResetDialog by remember { mutableStateOf(false) }
    var cigarettesPerDayInput by remember { mutableStateOf("10") }
    var packCostInput by remember { mutableStateOf("8.0") }

    // Flare trigger for demonic smoke logging
    var flareTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    // Dynamic animation for Uno Reverse button rotation
    val reverseRotation by animateFloatAsState(
        targetValue = if (isDemonic) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f),
        label = "uno_reverse_rotate"
    )

    // Angelic calculations
    val isStarted = streakData.isTimerStarted && streakData.lastRelapseTimestamp > 0L
    val diffMillis = if (isStarted) (currentTime - streakData.lastRelapseTimestamp).coerceAtLeast(0L) else 0L
    val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    val hours = ((diffMillis / (1000 * 60 * 60)) % 24).toInt()
    val minutes = ((diffMillis / (1000 * 60)) % 60).toInt()
    val seconds = ((diffMillis / 1000) % 60).toInt()

    val cigsPerDay = cigarettesPerDayInput.toDoubleOrNull() ?: 10.0
    val packCost = packCostInput.toDoubleOrNull() ?: 8.0

    val cigarettesAvoided = if (isStarted) (days + (hours / 24.0)) * cigsPerDay else 0.0
    val moneySaved = if (isStarted) (cigarettesAvoided / 20.0) * packCost else 0.0

    val rankTitle = when {
        !isStarted -> "Timer Not Started"
        days >= 365 -> "Clean Lungs Master (1 Year)"
        days >= 90 -> "Sleeper Vanguard (90 Days)"
        days >= 30 -> "Oxygen Titan (1 Month)"
        days >= 7 -> "Fresh Air Warrior (1 Week)"
        else -> "Smoke-Free Initiate"
    }

    // Health Recovery fractions for Angelic Mode
    val bpProgress = if (!isStarted) 0f else (diffMillis.toFloat() / (20 * 60 * 1000L)).coerceIn(0f, 1f)
    val coProgress = if (!isStarted) 0f else (diffMillis.toFloat() / (12 * 3600 * 1000L)).coerceIn(0f, 1f)
    val lungProgress = if (!isStarted) 0f else (days.toFloat() / 14f).coerceIn(0f, 1f)
    val heartProgress = if (!isStarted) 0f else (days.toFloat() / 365f).coerceIn(0f, 1f)
    val overallRecovery = (bpProgress * 0.15f + coProgress * 0.25f + lungProgress * 0.35f + heartProgress * 0.25f)

    // Demonic calculations
    val moneyBurned = (totalSmoked / 20.0) * packCost
    val totalMinutesLost = totalSmoked * 11 // ~11 minutes lost per cigarette
    val lostHours = totalMinutesLost / 60
    val lostRemainingMins = totalMinutesLost % 60

    val timeSinceLastSmokeStr = if (lastSmokedTime > 0L) {
        val diffMins = ((currentTime - lastSmokedTime) / (1000 * 60)).toInt()
        if (diffMins < 60) "${diffMins}m ago" else "${diffMins / 60}h ago"
    } else "Never"

    // Material You Backgrounds
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_shift"
    )

    val angelicBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF1FDF4),
            Color(0xFFF0F9FF),
            Color(0xFFFAF5FF),
            Color(0xFFF8FAFC)
        )
    )

    val demonicBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D0303),
            Color(0xFF1E0606),
            Color(0xFF120303),
            Color(0xFF2B0707)
        )
    )

    val activeBg = if (isDemonic) demonicBg else angelicBg
    val themeAccent = if (isDemonic) Color(0xFFEF4444) else Color(0xFF10B981)
    val themeSecondary = if (isDemonic) Color(0xFFF59E0B) else Color(0xFF06B6D4)
    val textColor = if (isDemonic) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val subTextColor = if (isDemonic) Color(0xFF94A3B8) else Color(0xFF475569)

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Confirm Smoke Relapse / Reset?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text("Resetting your smoke-free tracker will record your $days-day streak and restart your clean counter. Stand strong!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sleeperRepo.resetStreak("no_smoke")
                        showResetDialog = false
                        Toast.makeText(context, "Clean counter reset. Renew your commitment!", Toast.LENGTH_SHORT).show()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(activeBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // TOP BAR: Navigation + Dual-Mode Header + Prominent "Uno Reverse" Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        themeAccent.copy(alpha = 0.45f),
                        RoundedCornerShape(16.dp)
                    ),
                color = if (isDemonic) Color(0x301C0505) else Color(0x50FFFFFF)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("nosmoke_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Hub",
                                tint = themeAccent
                            )
                        }

                        Icon(
                            imageVector = if (isDemonic) Icons.Default.LocalFireDepartment else Icons.Default.SmokeFree,
                            contentDescription = if (isDemonic) "Demonic Smoke Mode" else "Angelic Clean Mode",
                            tint = themeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDemonic) "Smoke Counter" else "No Smoke",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = textColor
                        )
                    }

                    // PROMINENT "UNO REVERSE" BUTTON
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                val nextMode = if (isDemonic) "angelic" else "demonic"
                                sleeperRepo.setSmokeModuleMode(nextMode)
                                Toast.makeText(
                                    context,
                                    if (nextMode == "demonic") "😈 Switched to Demonic Mode (Smoke Counter)"
                                    else "😇 Switched to Angelic Mode (Clean Tracker)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .testTag("uno_reverse_toggle_btn"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDemonic) Color(0x40DC2626) else Color(0x3010B981),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isDemonic) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = "Uno Reverse Toggle",
                                tint = if (isDemonic) Color(0xFFFDE047) else Color(0xFF047857),
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(reverseRotation)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "UNO REVERSE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = if (isDemonic) Color(0xFFFDE047) else Color(0xFF047857)
                                )
                                Text(
                                    text = if (isDemonic) "Switch Angelic" else "Switch Demonic",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ITEM 1: ARTISTIC LUNGS CANVAS VISUALIZER
                item {
                    LungsVisualizer(
                        isDemonic = isDemonic,
                        flareTrigger = flareTrigger,
                        daysClean = days,
                        totalSmoked = totalSmoked
                    )
                }

                // ITEM 2: MAIN COUNTER & ACTION CARD (Consistent Position Across Both Modes)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, themeAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        color = if (isDemonic) Color(0x281C0505) else Color(0x60FFFFFF)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isDemonic) "SMOKING LOG • TOTAL SMOKED" else rankTitle.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp,
                                color = themeSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (!isDemonic) {
                                // ANGELIC MODE DISPLAY
                                if (!isStarted) {
                                    Text(
                                        text = "READY TO START",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap below to begin recording your clean days.",
                                        fontSize = 13.sp,
                                        color = subTextColor
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    GlassButton(
                                        text = "🚭 Start Smoke-Free Tracker Now",
                                        onClick = {
                                            sleeperRepo.startStreakTimer("no_smoke")
                                            Toast.makeText(context, "Smoke-Free timer started!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "start_nosmoke_timer_btn"
                                    )
                                } else {
                                    Text(
                                        text = "$days DAYS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 44.sp,
                                        color = themeAccent
                                    )

                                    Text(
                                        text = String.format(Locale.US, "%02dh : %02dm : %02ds", hours, minutes, seconds),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp,
                                        color = textColor
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    GlassButton(
                                        text = "⚠️ Record Relapse / Reset Timer",
                                        onClick = { showResetDialog = true },
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "nosmoke_reset_btn"
                                    )
                                }
                            } else {
                                // DEMONIC MODE DISPLAY
                                Text(
                                    text = "$totalSmoked SMOKED",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 42.sp,
                                    color = Color(0xFFEF4444)
                                )

                                Text(
                                    text = "$todaySmoked Today • Last: $timeSinceLastSmokeStr",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFF59E0B)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Prominent Demonic Add Smoke Button with Flare Trigger
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            sleeperRepo.logCigarette()
                                            flareTrigger = System.currentTimeMillis()
                                            Toast.makeText(context, "🔥 Cigarette logged (+1). Flaring lungs!", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("demonic_log_smoke_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDE047))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFFB91C1C),
                                                        Color(0xFFDC2626),
                                                        Color(0xFFF97316),
                                                        Color(0xFFEA580C)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = Color(0xFFFDE047),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "🚬 LOG 1 CIGARETTE SMOKED (+1)",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                if (totalSmoked > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            sleeperRepo.undoCigarette()
                                            Toast.makeText(context, "Undid 1 cigarette (-1)", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("demonic_undo_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Undo,
                                            contentDescription = "Undo",
                                            tint = subTextColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Undo Last Cigarette (-1)",
                                            fontSize = 12.sp,
                                            color = subTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ITEM 3: HEALTH & WEALTH ANALYTICS (Consistent Metrics Position)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, themeSecondary.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                        color = if (isDemonic) Color(0x281C0505) else Color(0x60FFFFFF)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isDemonic) "Health Impact & Money Spent" else "Health & Money Saved",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AnimatedProgressRing(
                                    progress = if (isDemonic) ((todaySmoked.toFloat() / 20f).coerceIn(0f, 1f)) else overallRecovery,
                                    size = 96.dp,
                                    strokeWidth = 10.dp,
                                    primaryColor = if (isDemonic) Color(0xFFEF4444) else NeonGreen,
                                    secondaryColor = if (isDemonic) Color(0xFFF59E0B) else NeonCyan
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isDemonic) "$todaySmoked" else "${(overallRecovery * 100).toInt()}%",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = if (isDemonic) "Today" else "Restored",
                                            fontSize = 10.sp,
                                            color = if (isDemonic) Color(0xFFEF4444) else NeonGreen,
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
                                            title = if (isDemonic) "Life Lost" else "Avoided",
                                            value = if (isDemonic) "${lostHours}h ${lostRemainingMins}m" else "${cigarettesAvoided.toInt()}",
                                            icon = if (isDemonic) Icons.Default.Warning else Icons.Default.HealthAndSafety,
                                            badgeText = if (isDemonic) "Estimated" else "Cigarettes",
                                            accentColor = if (isDemonic) Color(0xFFEF4444) else NeonGreen,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetricStatCard(
                                            title = if (isDemonic) "Burned" else "Saved",
                                            value = "$${String.format(Locale.US, "%.1f", if (isDemonic) moneyBurned else moneySaved)}",
                                            icon = Icons.Default.AttachMoney,
                                            badgeText = if (isDemonic) "Wasted" else "Cash",
                                            accentColor = if (isDemonic) Color(0xFFF59E0B) else NeonAmber,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ITEM 4: TIMELINE PROGRESS BARS (Recovery vs Toxicity)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, themeAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                        color = if (isDemonic) Color(0x281C0505) else Color(0x60FFFFFF)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isDemonic) "Toxicity & Tissue Impact Timeline" else "Cardiovascular & Pulmonary Timeline",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = themeAccent
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (!isDemonic) {
                                AnimatedStatProgressBar(
                                    progress = bpProgress,
                                    label = "Pulse & Blood Pressure Normalization (20m)",
                                    startColor = NeonGreen,
                                    endColor = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                AnimatedStatProgressBar(
                                    progress = coProgress,
                                    label = "Carbon Monoxide Purge (12h)",
                                    startColor = NeonCyan,
                                    endColor = NeonGreen
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                AnimatedStatProgressBar(
                                    progress = lungProgress,
                                    label = "Circulation & Lung Function (14d)",
                                    startColor = NeonAmber,
                                    endColor = NeonGreen
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                AnimatedStatProgressBar(
                                    progress = heartProgress,
                                    label = "Coronary Risk Cut in Half (1yr)",
                                    startColor = NeonGreen,
                                    endColor = NeonAmber
                                )
                            } else {
                                AnimatedStatProgressBar(
                                    progress = (totalSmoked.toFloat() / 100f).coerceIn(0f, 1f),
                                    label = "Tar Inhaled (${totalSmoked * 10} mg in Lung Alveoli)",
                                    startColor = Color(0xFFEF4444),
                                    endColor = Color(0xFF7F1D1D)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                AnimatedStatProgressBar(
                                    progress = (todaySmoked.toFloat() / 20f).coerceIn(0f, 1f),
                                    label = "Carbon Monoxide Hemoglobin Saturation",
                                    startColor = Color(0xFFF59E0B),
                                    endColor = Color(0xFFEF4444)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                AnimatedStatProgressBar(
                                    progress = ((todaySmoked * 1.5f) / 15f).coerceIn(0.1f, 1f),
                                    label = "Cilia Immobility & Bronchial Irritation",
                                    startColor = Color(0xFFF97316),
                                    endColor = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }

                // ITEM 5: COST & CALCULATOR SETTINGS
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, themeAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                        color = if (isDemonic) Color(0x281C0505) else Color(0x60FFFFFF)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Smoke Calculator Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = themeAccent
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = cigarettesPerDayInput,
                                    onValueChange = { cigarettesPerDayInput = it },
                                    label = { Text("Cigs Per Day") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cigs_input"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = packCostInput,
                                    onValueChange = { packCostInput = it },
                                    label = { Text("Cost Per Pack ($)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cost_input"),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // ITEM 6: RUN HISTORY / LIFETIME TALLY
                if (!isDemonic) {
                    item {
                        Text(
                            text = "Clean Streak History (${streakData.history.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textColor,
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
                                        text = "Smoke Free: ${entry.daysSucceeded} Days",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = themeAccent
                                    )
                                    if (entry.note.isNotBlank()) {
                                        Text(
                                            text = "Note: ${entry.note}",
                                            fontSize = 12.sp,
                                            color = subTextColor
                                        )
                                    }
                                }
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(entry.timestamp)),
                                    fontSize = 12.sp,
                                    color = themeSecondary
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            color = Color(0x281C0505)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Demonic Mode Persistence Guarantee",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFDE047)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your lifetime smoke counter ($totalSmoked cigarettes) is permanently stored in device storage and never gets wiped, even if you switch back to Angelic Mode. Face the truth, master your mind, and make the choice to heal.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
