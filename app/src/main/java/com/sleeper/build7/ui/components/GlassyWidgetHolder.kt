package com.sleeper.build7.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val ONGAKU7_PACKAGE = "com.ongaku7.player"
private const val ONGAKU7_DOWNLOAD_URL = "https://github.com/sr7mods/Ongaku7/releases"

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GlassyWidgetHolder(
    packageName: String,
    selectedWidgetClassName: String? = null,
    onPackageNameChange: (String) -> Unit,
    onWidgetSelected: ((String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Draggable position offset for the floating trigger
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Dialog state for selecting music player / suggestions
    var showConfigDialog by remember { mutableStateOf(false) }
    var tempPkg by remember { mutableStateOf(packageName) }

    // Dialog state specifically prompted when clicking the button while Ongaku7 is not installed
    var showDownloadPromptDialog by remember { mutableStateOf(false) }

    // Quick suggestions / presets for music players
    val presetApps = listOf(
        "Ongaku7" to "com.ongaku7.player",
        "Spotify" to "com.spotify.music",
        "YouTube Music" to "com.google.android.apps.youtube.music",
        "SoundCloud" to "com.soundcloud.android",
        "Apple Music" to "com.apple.android.music"
    )

    // Configuration Dialog: Triggered by Tap & Hold (Long-click)
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music App Setup",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Workout Music Player", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Suggested Music Apps:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetApps) { (name, pkg) ->
                            val isSelected = tempPkg.equals(pkg, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    tempPkg = pkg
                                },
                                label = { Text(name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = tempPkg,
                        onValueChange = { tempPkg = it },
                        label = { Text("App Package Name") },
                        placeholder = { Text("e.g. com.ongaku7.player") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_package_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val appLabel = getAppLabel(context, tempPkg.trim())
                    val installed = isAppInstalled(context, tempPkg.trim())
                    val isOngaku7Selected = tempPkg.trim().equals(ONGAKU7_PACKAGE, ignoreCase = true)
                    val isOngaku7Installed = isAppInstalled(context, ONGAKU7_PACKAGE)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (installed) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (installed) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (installed) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (installed) "$appLabel is installed and ready" else "App label: $appLabel (Not installed)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Ongaku7 downloading button if not installed/available
                    if (!isOngaku7Installed || (!installed && isOngaku7Selected)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ONGAKU7_DOWNLOAD_URL)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_ongaku7_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Ongaku7",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Ongaku7",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = tempPkg.trim()
                        if (trimmed.isNotEmpty()) {
                            onPackageNameChange(trimmed)
                            onWidgetSelected?.invoke(null)
                            showConfigDialog = false
                            Toast.makeText(context, "Saved music player: ${getAppLabel(context, trimmed)}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Download Ongaku7 Prompt Dialog when tapping trigger and app is not available
    if (showDownloadPromptDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadPromptDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Ongaku7 Not Installed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Ongaku7 is not installed on this device. You can download and install the latest release directly from GitHub.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ONGAKU7_DOWNLOAD_URL)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(browserIntent)
                                showDownloadPromptDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_download_ongaku7_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Ongaku7 (GitHub)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDownloadPromptDialog = false
                        tempPkg = packageName
                        showConfigDialog = true
                    }
                ) {
                    Text("Change Player")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadPromptDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // FLOATING DRAGGABLE TRIGGER BUTTON
    // Single tap: directly triggers the configured app!
    // Long tap: opens the suggestions / configuration dialog.
    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .shadow(12.dp, CircleShape)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.6f),
                            primaryColor,
                            secondaryColor
                        )
                    ),
                    shape = CircleShape
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                )
                .combinedClickable(
                    onClick = {
                        // Directly trigger the app!
                        val targetPkg = packageName.trim().ifEmpty { ONGAKU7_PACKAGE }
                        val intent = context.packageManager.getLaunchIntentForPackage(targetPkg)
                        if (intent != null) {
                            try {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not launch app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (targetPkg.equals(ONGAKU7_PACKAGE, ignoreCase = true)) {
                                showDownloadPromptDialog = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "App not installed ($targetPkg). Hold to change music player!",
                                    Toast.LENGTH_LONG
                                ).show()
                                tempPkg = targetPkg
                                showConfigDialog = true
                            }
                        }
                    },
                    onLongClick = {
                        tempPkg = packageName
                        showConfigDialog = true
                    }
                )
                .testTag("floating_music_trigger_btn"),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Trigger Music App",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun getAppLabel(context: Context, packageName: String): String {
    if (packageName.isBlank()) return "Music Player"
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
    }
}

private fun isAppInstalled(context: Context, packageName: String): Boolean {
    if (packageName.isBlank()) return false
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
}
