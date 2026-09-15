package com.sleeper.build7.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeper.build7.data.*
import com.sleeper.build7.ui.components.AnimatedGlassyBackground
import com.sleeper.build7.ui.components.GlassCard
import com.sleeper.build7.ui.components.SecurityLockScreen
import com.sleeper.build7.ui.screens.*
import com.sleeper.build7.ui.theme.NeonAmber
import com.sleeper.build7.ui.theme.NeonCyan
import com.sleeper.build7.ui.theme.NeonGreen

enum class HubSection(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badge: String
) {
    PRAYER(
        route = "prayer",
        title = "Prayer Times",
        subtitle = "5 daily prayers and timely azan notifications",
        icon = Icons.Default.Mosque,
        accentColor = Color(0xFF10B981),
        badge = "Active"
    ),
    WORKOUT(
        route = "workout",
        title = "Daily Workout",
        subtitle = "Daily exercise routines, sets and rest timer",
        icon = Icons.Default.FitnessCenter,
        accentColor = Color(0xFF06B6D4),
        badge = "Workout"
    ),
    STUDY(
        route = "study",
        title = "Schedules",
        subtitle = "Study & Others",
        icon = Icons.Default.Schedule,
        accentColor = Color(0xFF3B82F6),
        badge = "Timer"
    ),
    GAINS(
        route = "gains",
        title = "Daily Gains",
        subtitle = "Track coding progress and personal achievements",
        icon = Icons.Default.Code,
        accentColor = Color(0xFF22C55E),
        badge = "Progress"
    ),
    NO_FAP(
        route = "nofap",
        title = "NoFap",
        subtitle = "Track clean days and build mental discipline",
        icon = Icons.Default.Bolt,
        accentColor = Color(0xFFF59E0B),
        badge = "Streak"
    ),
    NO_SMOKE(
        route = "nosmoke",
        title = "No Smoke",
        subtitle = "Track smoke-free days and log daily habits",
        icon = Icons.Default.SmokeFree,
        accentColor = Color(0xFF14B8A6),
        badge = "Habits"
    ),
    SETTINGS(
        route = "settings",
        title = "Settings",
        subtitle = "App theme, security lock and preferences",
        icon = Icons.Default.Settings,
        accentColor = Color(0xFF8B5CF6),
        badge = "Settings"
    ),
    BACKUP(
        route = "backup",
        title = "Backup & Restore",
        subtitle = "Safely export and restore your encrypted data",
        icon = Icons.Default.Lock,
        accentColor = Color(0xFFEC4899),
        badge = "Backup"
    )
}

@Composable
fun MainContainer(
    sleeperRepo: SleeperRepository
) {
    val context = LocalContext.current
    var activeSection by remember { mutableStateOf<HubSection?>(null) }

    val isSecurityEnabled by sleeperRepo.isSecurityEnabled.collectAsState()
    val isAppLockEnabled by sleeperRepo.isAppLockEnabled.collectAsState()
    val lockedSections by sleeperRepo.lockedSections.collectAsState()

    var isAppUnlocked by remember { mutableStateOf(false) }
    var pendingSectionToUnlock by remember { mutableStateOf<HubSection?>(null) }
    val unlockedSectionsInSession = remember { mutableStateListOf<String>() }

    // Permission setup for Notifications
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Full App Lock Enforcement
    if (isSecurityEnabled && isAppLockEnabled && !isAppUnlocked) {
        SecurityLockScreen(
            sleeperRepo = sleeperRepo,
            title = "Sleeper Security Lock",
            subtitle = "Authenticate to access SleeperBuild",
            onUnlocked = { isAppUnlocked = true },
            onCancel = null
        )
        return
    }

    // Individual Section Lock Overlay
    if (pendingSectionToUnlock != null) {
        val targetSec = pendingSectionToUnlock!!
        SecurityLockScreen(
            sleeperRepo = sleeperRepo,
            title = "Locked: ${targetSec.title}",
            subtitle = "Authenticate to access this section",
            onUnlocked = {
                unlockedSectionsInSession.add(targetSec.route)
                activeSection = targetSec
                pendingSectionToUnlock = null
            },
            onCancel = {
                pendingSectionToUnlock = null
            }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(250)) { -it / 3 } + fadeOut(animationSpec = tween(250)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(250)) { it } + fadeOut(animationSpec = tween(250)))
                    }
                },
                label = "HubScreenNavigation"
            ) { section ->
                if (section == null) {
                    HomeDashboardHub(
                        sleeperRepo = sleeperRepo,
                        onSelectSection = { sec ->
                            if (isSecurityEnabled && lockedSections.contains(sec.route) && !unlockedSectionsInSession.contains(sec.route)) {
                                pendingSectionToUnlock = sec
                            } else {
                                activeSection = sec
                            }
                        }
                    )
                } else {
                    val onBack = { activeSection = null }
                    when (section) {
                        HubSection.PRAYER -> PrayerScreen(sleeperRepo = sleeperRepo, onBack = onBack)
                        HubSection.WORKOUT -> WorkoutScreen(sleeperRepo = sleeperRepo, onBack = onBack)
                        HubSection.STUDY -> StudyScreen(sleeperRepo = sleeperRepo, onBack = onBack)
                        HubSection.GAINS -> GainsScreen(sleeperRepo = sleeperRepo, onBack = onBack)
                        HubSection.NO_FAP -> NoFapScreen(sleeperRepo = sleeperRepo, onBack = onBack)
                        HubSection.NO_SMOKE -> NoSmokeScreen(sleeperRepo = sleeperRepo, onBack = onBack)
                        HubSection.SETTINGS -> SettingsScreen(sleeperRepo = sleeperRepo, onBack = onBack)
                        HubSection.BACKUP -> BackupScreen(sleeperRepo = sleeperRepo, onBack = onBack)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeDashboardHub(
    sleeperRepo: SleeperRepository,
    onSelectSection: (HubSection) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val prayerRecords by sleeperRepo.allPrayerRecords.collectAsState()
    val noFapStreak by sleeperRepo.noFapStreak.collectAsState()
    val noSmokeStreak by sleeperRepo.noSmokeStreak.collectAsState()
    val studySessions by sleeperRepo.studySessions.collectAsState()
    val sectionVisibility by sleeperRepo.dashboardSectionVisibility.collectAsState()
    val smokeMode by sleeperRepo.smokeModuleMode.collectAsState()

    val visibleSections = remember(sectionVisibility) {
        HubSection.values().filter { section ->
            when (section) {
                HubSection.SETTINGS, HubSection.BACKUP -> true
                else -> sectionVisibility[section.route] != false
            }
        }
    }

    val todayDate = sleeperRepo.getTodayDate()
    val todayPrayer = prayerRecords.find { it.date == todayDate }
    val prayersDone = if (todayPrayer != null) {
        listOf(todayPrayer.fajr, todayPrayer.dhuhr, todayPrayer.asr, todayPrayer.maghrib, todayPrayer.isha).count { it }
    } else 0

    val focusMinsToday = studySessions.filter { it.date == todayDate && it.isCompleted }.sumOf { it.durationMins }

    AnimatedGlassyBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HERO BRANDING & OVERVIEW HEADER
            item {
                GlassCard(borderColor = primaryColor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = primaryColor.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "SR7 MODS",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = primaryColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BUILD EVERYDAY",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = secondaryColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SleeperBuild",
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Master Physical, Mental & Spiritual Greatness",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Logo Shield Icon
                        Surface(
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "SleeperBuild Shield",
                                    tint = primaryColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // DAILY SNAPSHOT ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetricChip(
                            label = "Prayers",
                            value = "$prayersDone/5",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetricChip(
                            label = "Focus",
                            value = "${focusMinsToday}m",
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetricChip(
                            label = "NoFap",
                            value = if (noFapStreak.isTimerStarted) "${(noFapStreak.highestStreakDays)}d" else "Ready",
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Apps & Tools (${visibleSections.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap card to launch",
                        fontSize = 12.sp,
                        color = secondaryColor
                    )
                }
            }

            // DYNAMICALLY FILTERED SECTIONS LIST
            items(visibleSections) { section ->
                val isDemonicSmoke = section == HubSection.NO_SMOKE && smokeMode == "demonic"
                val itemTitle = if (isDemonicSmoke) "Smoke Counter" else section.title
                val itemSubtitle = if (isDemonicSmoke) "Active burn tracker & lifetime smoke log" else section.subtitle
                val itemIcon = if (isDemonicSmoke) Icons.Default.LocalFireDepartment else section.icon
                val itemAccent = if (isDemonicSmoke) Color(0xFFEF4444) else section.accentColor
                val itemBadge = if (isDemonicSmoke) "Active" else section.badge

                GlassCard(
                    borderColor = itemAccent,
                    onClick = { onSelectSection(section) },
                    modifier = Modifier.testTag("hub_item_${section.route}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Section Icon Box with Glowing Gradient
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = itemAccent.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, itemAccent.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = itemIcon,
                                    contentDescription = itemTitle,
                                    tint = itemAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = itemTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = itemAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = itemBadge,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = itemAccent,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = itemSubtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Sleek Chevron Arrow
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Open $itemTitle",
                            tint = itemAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun QuickMetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f)),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
