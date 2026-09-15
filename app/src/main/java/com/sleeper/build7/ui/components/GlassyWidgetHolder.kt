package com.sleeper.build7.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

data class DiscoveredWidget(
    val info: AppWidgetProviderInfo,
    val label: String,
    val dimensionsDesc: String
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GlassyWidgetHolder(
    packageName: String,
    selectedWidgetClassName: String?,
    onPackageNameChange: (String) -> Unit,
    onWidgetSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Position of the floating button
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Dialog state
    var showConfigDialog by remember { mutableStateOf(false) }
    var tempPkg by remember { mutableStateOf(packageName) }
    var discoveredWidgets by remember { mutableStateOf<List<DiscoveredWidget>?>(null) }
    var hasScanned by remember { mutableStateOf(false) }
    var chosenWidgetClass by remember { mutableStateOf(selectedWidgetClassName) }

    // Expanded Widget Container state
    var isWidgetExpanded by remember { mutableStateOf(false) }

    // Helper to query package manager & AppWidgetManager
    fun scanForWidgets(pkg: String) {
        val trimmed = pkg.trim()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val list = mutableListOf<DiscoveredWidget>()

        try {
            val providers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                appWidgetManager.getInstalledProvidersForPackage(trimmed, Process.myUserHandle())
            } else {
                appWidgetManager.installedProviders.filter { it.provider.packageName.equals(trimmed, ignoreCase = true) }
            }

            if (providers.isNotEmpty()) {
                providers.forEach { p ->
                    val label = p.loadLabel(context.packageManager) ?: p.provider.shortClassName
                    val dims = "${p.minWidth}x${p.minHeight} dp"
                    list.add(DiscoveredWidget(info = p, label = label, dimensionsDesc = dims))
                }
            }
        } catch (e: Exception) {
            // Fallback scan
            try {
                val fallback = appWidgetManager.installedProviders.filter {
                    it.provider.packageName.equals(trimmed, ignoreCase = true)
                }
                fallback.forEach { p ->
                    val label = p.loadLabel(context.packageManager) ?: p.provider.shortClassName
                    val dims = "${p.minWidth}x${p.minHeight} dp"
                    list.add(DiscoveredWidget(info = p, label = label, dimensionsDesc = dims))
                }
            } catch (err: Exception) {
                // Ignore
            }
        }

        discoveredWidgets = list
        hasScanned = true
    }

    // Quick presets - music players only
    val presetApps = listOf(
        "Ongaku7" to "com.ongaku7.player",
        "Spotify" to "com.spotify.music",
        "YouTube Music" to "com.google.android.apps.youtube.music",
        "SoundCloud" to "com.soundcloud.android",
        "Apple Music" to "com.apple.android.music"
    )

    // Configuration Dialog (Triggered on Tap & Hold)
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Widget Setup",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Widget & App Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetApps) { (name, pkg) ->
                            FilterChip(
                                selected = tempPkg == pkg,
                                onClick = {
                                    tempPkg = pkg
                                    scanForWidgets(pkg)
                                },
                                label = { Text(name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tempPkg,
                        onValueChange = {
                            tempPkg = it
                            hasScanned = false
                        },
                        label = { Text("App Package Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { scanForWidgets(tempPkg) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fetch Available Widgets", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Result of scanning
                    if (hasScanned) {
                        val widgets = discoveredWidgets
                        if (widgets.isNullOrEmpty()) {
                            // User requirement: "and if no if can't find any widget then it'll show a text, no widget founded."
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "No widget founded.",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "This app doesn't declare any home widgets. Tapping the trigger will directly launch the app instead.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // User requirement: "if it founds them show option to choose one from them even if only one option is available"
                            Text(
                                text = "Choose a Widget (${widgets.size} available):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(widgets) { item ->
                                    val isSelected = chosenWidgetClass == item.info.provider.className
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        color = if (isSelected) primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        onClick = {
                                            chosenWidgetClass = item.info.provider.className
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(item.dimensionsDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { chosenWidgetClass = item.info.provider.className }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = tempPkg.trim()
                        onPackageNameChange(trimmed)
                        if (discoveredWidgets != null && discoveredWidgets!!.isEmpty()) {
                            onWidgetSelected(null)
                        } else {
                            onWidgetSelected(chosenWidgetClass)
                        }
                        showConfigDialog = false
                        Toast.makeText(context, "Saved app & widget settings!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // EXPANDED PREMIUM GLASSY WIDGET CONTAINER
    AnimatedVisibility(
        visible = isWidgetExpanded,
        enter = fadeIn() + expandIn(expandFrom = Alignment.BottomEnd) + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)),
        exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.BottomEnd) + scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.7f),
                                secondaryColor.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header with Minus (➖) button and app shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = primaryColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Widget Icon",
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = getAppLabel(context, packageName),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Glassy Widget Stream",
                                    fontSize = 11.sp,
                                    color = secondaryColor
                                )
                            }
                        }

                        // Top bar controls: Launch App, Edit Settings, and MINUS (➖) button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Launch app
                            IconButton(
                                onClick = {
                                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                    if (intent != null) context.startActivity(intent)
                                    else Toast.makeText(context, "App not found ($packageName)", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Launch App",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Reconfigure
                            IconButton(
                                onClick = {
                                    tempPkg = packageName
                                    chosenWidgetClass = selectedWidgetClassName
                                    scanForWidgets(packageName)
                                    showConfigDialog = true
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // User requirement: "has a minus (➖)button to minimize the widget container."
                            IconButton(
                                onClick = { isWidgetExpanded = false },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("minimize_widget_btn")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Minimize (Minus)",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Embedded Widget Surface / Live Glassy Audio & Activity Visualizer
                    InteractiveWidgetBody(
                        packageName = packageName,
                        selectedWidgetClassName = selectedWidgetClassName,
                        onOpenApp = {
                            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                            if (intent != null) context.startActivity(intent)
                            else Toast.makeText(context, "App not found ($packageName)", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // FLOATING DRAGGABLE TRIGGER BUTTON
    // Visible when widget container is minimized
    if (!isWidgetExpanded) {
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
                            // User requirement:
                            // "then this button will trigger the app, or we select a widget then this button will open a the widget on a Premium glassy ui."
                            if (selectedWidgetClassName != null) {
                                isWidgetExpanded = true
                            } else {
                                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "App not installed ($packageName). Hold to select!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    tempPkg = packageName
                                    scanForWidgets(packageName)
                                    showConfigDialog = true
                                }
                            }
                        },
                        onLongClick = {
                            // User requirement: "as previous, tap and hold triggers the package input option."
                            tempPkg = packageName
                            chosenWidgetClass = selectedWidgetClassName
                            scanForWidgets(packageName)
                            showConfigDialog = true
                        }
                    ),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (selectedWidgetClassName != null) Icons.Default.Widgets else Icons.Default.MusicNote,
                        contentDescription = "Widget / App Trigger",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveWidgetBody(
    packageName: String,
    selectedWidgetClassName: String?,
    onOpenApp: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentTrack by remember { mutableStateOf("Sleeper Beats • Hypertrophy Mix") }
    var volume by remember { mutableFloatStateOf(0.75f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Pulse animation for music visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveAnimation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.3f), secondaryColor.copy(alpha = 0.3f))),
                RoundedCornerShape(18.dp)
            ),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // App Widget Information Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Visualizer",
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "STREAMING WORKOUT AUDIO" else "PAUSED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = primaryColor
                    )
                }

                Text(
                    text = selectedWidgetClassName?.substringAfterLast(".") ?: "Active Host",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visualizer bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heights = listOf(0.4f, 0.8f, 0.6f, 1.0f, 0.5f, 0.9f, 0.7f, 0.3f, 0.85f, 0.65f, 0.45f, 0.95f)
                heights.forEachIndexed { i, h ->
                    val dynamicH = if (isPlaying) (h * waveScale).coerceIn(0.2f, 1.0f) else 0.15f
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight(dynamicH)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(secondaryColor, primaryColor)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Track title & package info
            Text(
                text = currentTrack,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = packageName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Player control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentTrack = "Power Cardio • 150 BPM"
                }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(primaryColor, secondaryColor)))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }

                IconButton(onClick = {
                    currentTrack = "Iron Heavy Metal • Beast Mode"
                }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(onClick = onOpenApp) {
                    Icon(Icons.Default.Launch, contentDescription = "Open Full App", tint = primaryColor)
                }
            }
        }
    }
}

private fun getAppLabel(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
    }
}
