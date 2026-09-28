package com.sleeper.build7.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.sleeper.build7.audio.SleeperAudioEngine
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.ui.components.AnimatedGlassyBackground
import com.sleeper.build7.ui.components.GlassButton
import com.sleeper.build7.ui.components.GlassCard
import com.sleeper.build7.ui.components.GradientChip

@Composable
fun SettingsScreen(
    sleeperRepo: SleeperRepository,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currentThemeMode by sleeperRepo.themeMode.collectAsState()
    val musicPkg by sleeperRepo.musicAppPackage.collectAsState()
    val alarmsEnabled by sleeperRepo.prayerAlarmsEnabled.collectAsState()
    val globalAudioMode by sleeperRepo.audioAlarmMode.collectAsState()
    val globalTtsVoice by sleeperRepo.ttsVoiceProfile.collectAsState()
    val workoutAlarmEnabled by sleeperRepo.workoutAlarmEnabled.collectAsState()
    val workoutAlarmTime by sleeperRepo.workoutAlarmTime.collectAsState()
    val hydrationAlarmEnabled by sleeperRepo.hydrationAlarmEnabled.collectAsState()
    val sectionVisibility by sleeperRepo.dashboardSectionVisibility.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    var musicPackageInput by remember { mutableStateOf(musicPkg) }
    var showSecuritySetup by remember { mutableStateOf(false) }

    val isSecurityEnabled by sleeperRepo.isSecurityEnabled.collectAsState()
    val isAppLockEnabled by sleeperRepo.isAppLockEnabled.collectAsState()
    val activeLockType by sleeperRepo.lockType.collectAsState()
    val lockedSections by sleeperRepo.lockedSections.collectAsState()

    if (showSecuritySetup) {
        com.sleeper.build7.ui.components.SecuritySetupDialog(
            sleeperRepo = sleeperRepo,
            onDismiss = { showSecuritySetup = false }
        )
    }

    fun openDeepLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Hub",
                            tint = primaryColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = primaryColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Settings & Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Dashboard Customization, Dev Links & Preferences",
                            fontSize = 11.sp,
                            color = secondaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FIX #6: CUSTOMIZE HOME SCREEN PANEL (DYNAMIC MODULE TOGGLES)
                item {
                    GlassCard(borderColor = primaryColor) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Customize Dashboard",
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Customize Home Screen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Turn sections on or off to customize your home screen",
                                    fontSize = 12.sp,
                                    color = secondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val modules = listOf(
                            DashboardModuleToggle(
                                key = "prayer",
                                title = "Prayer Times",
                                description = "5 daily prayers and timely azan notifications",
                                icon = Icons.Default.Mosque,
                                accentColor = Color(0xFF10B981)
                            ),
                            DashboardModuleToggle(
                                key = "workout",
                                title = "Daily Workout",
                                description = "Daily exercises, workout routines and rest timer",
                                icon = Icons.Default.FitnessCenter,
                                accentColor = Color(0xFF06B6D4)
                            ),
                            DashboardModuleToggle(
                                key = "study",
                                title = "Schedules",
                                description = "Study & Others • Task schedules and focus timer",
                                icon = Icons.Default.Schedule,
                                accentColor = Color(0xFF3B82F6)
                            ),
                            DashboardModuleToggle(
                                key = "gains",
                                title = "Daily Gains",
                                description = "Software wins and project milestones",
                                icon = Icons.Default.Code,
                                accentColor = Color(0xFF22C55E)
                            ),
                            DashboardModuleToggle(
                                key = "nofap",
                                title = "NoFap",
                                description = "Track clean streak and build mental clarity",
                                icon = Icons.Default.Bolt,
                                accentColor = Color(0xFFF59E0B)
                            ),
                            DashboardModuleToggle(
                                key = "nosmoke",
                                title = "No Smoke",
                                description = "Track clean days and log smoke habits",
                                icon = Icons.Default.SmokeFree,
                                accentColor = Color(0xFF14B8A6)
                            )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            modules.forEach { mod ->
                                val isVisible = sectionVisibility[mod.key] != false
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (isVisible) mod.accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    color = if (isVisible) mod.accentColor.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = mod.accentColor.copy(alpha = 0.15f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = mod.icon,
                                                        contentDescription = mod.title,
                                                        tint = mod.accentColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = mod.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = mod.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = isVisible,
                                            onCheckedChange = { checked ->
                                                sleeperRepo.setSectionVisibility(mod.key, checked)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = mod.accentColor,
                                                checkedTrackColor = mod.accentColor.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.testTag("toggle_module_${mod.key}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // FIX #7: SECURITY & PRIVACY LOCK CONTROLS
                item {
                    GlassCard(borderColor = Color(0xFFEC4899)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEC4899).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.5f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Security",
                                        tint = Color(0xFFEC4899),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Security & Privacy Lock",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (isSecurityEnabled) "Active: ${activeLockType.uppercase()} Protected" else "Disabled • Tap to Set Up",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSecurityEnabled) Color(0xFF10B981) else secondaryColor
                                )
                            }

                            Button(
                                onClick = { showSecuritySetup = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEC4899)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("setup_security_btn")
                            ) {
                                Text(
                                    text = if (isSecurityEnabled) "Manage" else "Enable",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isSecurityEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFFEC4899).copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Lock entire app toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lock Entire App on Launch",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Prompt $activeLockType authentication when opening SleeperBuild",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAppLockEnabled,
                                    onCheckedChange = { sleeperRepo.setAppLockEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFEC4899),
                                        checkedTrackColor = Color(0xFFEC4899).copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("app_lock_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "LOCKED SECTIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = secondaryColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val sectionsToLock = listOf(
                                "prayer" to "Prayer Times",
                                "workout" to "Daily Workout",
                                "study" to "Schedules",
                                "gains" to "Daily Gains",
                                "nofap" to "NoFap",
                                "nosmoke" to "No Smoke",
                                "backup" to "Backup & Restore"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                sectionsToLock.forEach { (secKey, secLabel) ->
                                    val isLocked = lockedSections.contains(secKey)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isLocked) Color(0x20EC4899) else Color(0x10FFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isLocked) Color(0x60EC4899) else Color(0x15FFFFFF)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = null,
                                                    tint = if (isLocked) Color(0xFFEC4899) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = secLabel,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isLocked) FontWeight.Bold else FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }

                                            Checkbox(
                                                checked = isLocked,
                                                onCheckedChange = { checked ->
                                                    sleeperRepo.setSectionLocked(secKey, checked)
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFFEC4899)
                                                ),
                                                modifier = Modifier.testTag("lock_checkbox_$secKey")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // FIX #5: DEDICATED NATIVE "ABOUT DEVELOPER" SECTION
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        GlassCard(borderColor = Color(0xFF8B5CF6)) {
                            // Developer Header Info
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "SR7",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = Color(0xFFC084FC)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "SR7MODS",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "VERIFIED DEV",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFA78BFA),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "Professional Android, Web & Game Developer and Modder",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFC084FC)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Alex Sifat Rayhan (SR7MODS) develops high-performance Android apps, custom tools, and productivity utilities. SleeperBuild is built with Kotlin and Jetpack Compose, designed to keep all your daily habits, routines, and personal data private on your device.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "CONNECT & COMMUNITY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = secondaryColor
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 5 Integrated Deep-Link Action Buttons with Material Gradient Styling
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Telegram
                                DevLinkButton(
                                    title = "Telegram Channel",
                                    subtitle = "@sr7mods • Updates & Community",
                                    gradient = listOf(Color(0xFF0088CC), Color(0xFF229ED9)),
                                    icon = Icons.Default.Send,
                                    onClick = { openDeepLink("https://t.me/sr7mods") },
                                    testTag = "dev_link_telegram"
                                )

                                // Facebook
                                DevLinkButton(
                                    title = "Facebook Profile",
                                    subtitle = "Alex Sifat Rayhan",
                                    gradient = listOf(Color(0xFF1877F2), Color(0xFF0D53B7)),
                                    icon = Icons.Default.Person,
                                    onClick = { openDeepLink("https://m.facebook.com/sifatrayhan2007") },
                                    testTag = "dev_link_facebook"
                                )

                                // WhatsApp
                                DevLinkButton(
                                    title = "WhatsApp Direct",
                                    subtitle = "+8801318930997 • Quick Contact",
                                    gradient = listOf(Color(0xFF25D366), Color(0xFF128C7E)),
                                    icon = Icons.Default.Call,
                                    onClick = { openDeepLink("https://wa.me/+8801318930997") },
                                    testTag = "dev_link_whatsapp"
                                )

                                // GitHub Profile
                                DevLinkButton(
                                    title = "GitHub Profile",
                                    subtitle = "sr7mods • Open Source Works",
                                    gradient = listOf(Color(0xFF24292E), Color(0xFF4B5563)),
                                    icon = Icons.Default.Code,
                                    onClick = { openDeepLink("https://github.com/sr7mods") },
                                    testTag = "dev_link_github"
                                )

                                // Source Code Repository
                                DevLinkButton(
                                    title = "Source Code Repository",
                                    subtitle = "GitHub • sr7mods/SleeperBuild",
                                    gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
                                    icon = Icons.Default.Folder,
                                    onClick = { openDeepLink("https://github.com/sr7mods/SleeperBuild") },
                                    testTag = "dev_link_source_code"
                                )
                            }
                        }
                    }
                }

                // Color Scheme / Theme Selector
                item {
                    GlassCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Theme",
                                tint = secondaryColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Color Scheme & Theme",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val themeOptions = listOf(
                            "system" to "System Default",
                            "dark" to "Dark Mode",
                            "light" to "Light Mode"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themeOptions.forEach { (mode, label) ->
                                val isSelected = currentThemeMode.equals(mode, ignoreCase = true)
                                GradientChip(
                                    selected = isSelected,
                                    onClick = { sleeperRepo.setThemeMode(mode) },
                                    label = label,
                                    modifier = Modifier.weight(1f),
                                    testTag = "theme_$mode"
                                )
                            }
                        }
                    }
                }

                // Global Audio Alarms & TTS Voice Service Hub
                item {
                    GlassCard(borderColor = primaryColor) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Audio Service",
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Alarm & TTS Voice Service Hub",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Global voice profile & audio chime routing across all modules",
                                    fontSize = 12.sp,
                                    color = secondaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Audio Mode Selector (Voice Only, Ringtone, Both)
                        Text(
                            text = "ALARM PLAYBACK MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = secondaryColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val audioModes = listOf(
                            "tts" to "Voice (TTS)",
                            "ringtone" to "Ringtone",
                            "both" to "Both (Chime + Voice)"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            audioModes.forEach { (mode, label) ->
                                val isSelected = globalAudioMode.equals(mode, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) primaryColor else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { sleeperRepo.setAudioAlarmMode(mode) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Voice Pitch Profile Selector (Cute Girl, Cute Boy, Calm Voice)
                        Text(
                            text = "TTS VOICE PITCH & TIMBRE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = secondaryColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val voiceProfiles = listOf(
                            "girl" to "Cute Girl (~1.35x)",
                            "boy" to "Cute Boy (~0.90x)",
                            "calm" to "Calm Voice (~1.0x)"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            voiceProfiles.forEach { (profile, label) ->
                                val isSelected = globalTtsVoice.equals(profile, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) secondaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) secondaryColor else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { sleeperRepo.setTtsVoiceProfile(profile) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) secondaryColor else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Universal Test Voice & Chime Preview Button
                        OutlinedButton(
                            onClick = {
                                SleeperAudioEngine.playAlarmOrTts(
                                    context = context,
                                    mode = globalAudioMode,
                                    customText = "SleeperBuild discipline alert! Your audio alarm and text to speech services are operating with full precision.",
                                    voiceProfile = globalTtsVoice
                                )
                                Toast.makeText(context, "Testing Audio Alarm & Voice Service...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_global_audio_btn")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Global Audio & Voice Preview")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = primaryColor.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Subsystems Overview & Toggles
                        Text(
                            text = "ACTIVE ALARM MODULES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = secondaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 1. Prayer Alarms
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🕌 Salah & Jam'ah Reminders", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Aladhan precise timings with manual Jam'ah sync", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = alarmsEnabled,
                                onCheckedChange = { sleeperRepo.setPrayerAlarmsEnabled(it) },
                                modifier = Modifier.testTag("prayer_alarm_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Workout Alarm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🏋️ Daily Workout Alarm", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Scheduled at $workoutAlarmTime with audio coach voice", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = workoutAlarmEnabled,
                                onCheckedChange = { sleeperRepo.setWorkoutAlarmEnabled(it) },
                                modifier = Modifier.testTag("workout_alarm_switch_settings")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Hydration Alarm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("💧 Hydration Periodic Alerts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("2-hour regular water intake reminders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = hydrationAlarmEnabled,
                                onCheckedChange = { sleeperRepo.setHydrationAlarmEnabled(it) },
                                modifier = Modifier.testTag("hydration_alarm_switch_settings")
                            )
                        }
                    }
                }

                // Default Music/Workout App Target Package
                item {
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Music",
                                tint = secondaryColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Workout Music App Package",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Set the Android package name (e.g. com.ongaku7.player, com.spotify.music). The floating workout button launches this application directly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = musicPackageInput,
                            onValueChange = { musicPackageInput = it },
                            label = { Text("Package Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_music_package_input"),
                            singleLine = true
                        )

                        // Ongaku7 download button if not installed
                        val isOngakuInstalled = remember(musicPackageInput) {
                            try {
                                context.packageManager.getPackageInfo("com.ongaku7.player", 0)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }

                        if (!isOngakuInstalled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            GlassButton(
                                text = "Download Ongaku7 (GitHub)",
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sr7mods/Ongaku7/releases")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "settings_download_ongaku7_btn"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassButton(
                            text = "Update Music Package",
                            onClick = {
                                if (musicPackageInput.isNotBlank()) {
                                    sleeperRepo.setMusicAppPackage(musicPackageInput.trim())
                                    Toast.makeText(context, "Music package updated!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "save_music_package_btn"
                        )
                    }
                }

                // App Info Footer - Strictly "1" as required ("use 1 on the version on the settings section")
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SleeperBuild v1",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Version: 1 (Release 1)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = secondaryColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Package: com.sleeper.build7",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Encrypted Local Storage • Zero Cloud Tracking",
                                fontSize = 11.sp,
                                color = secondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DashboardModuleToggle(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
private fun DevLinkButton(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, gradient.first().copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Open Link",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
