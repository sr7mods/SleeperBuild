package com.sleeper.build7.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.sleeper.build7.data.GainNote
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.ui.components.AnimatedGlassyBackground
import com.sleeper.build7.ui.components.AnimatedProgressRing
import com.sleeper.build7.ui.components.GlassButton
import com.sleeper.build7.ui.components.GlassCard
import com.sleeper.build7.ui.components.MetricStatCard
import com.sleeper.build7.ui.components.WeeklyConsistencyHeatmap
import com.sleeper.build7.ui.theme.NeonCyan
import com.sleeper.build7.ui.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GainsScreen(
    sleeperRepo: SleeperRepository,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val gainNotes by sleeperRepo.gainNotes.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedNoteForDetail by remember { mutableStateOf<GainNote?>(null) }

    // Form inputs for new note
    var titleInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Software Modding") }
    var markdownInput by remember { mutableStateOf("## Progress Log\n- Built new custom feature\n- Refactored UI architecture\n\n```kt\nval status = \"BUILDING_FAST\"\n```") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var videoUri by remember { mutableStateOf<String?>(null) }
    var audioUri by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
            Toast.makeText(context, "Image attached!", Toast.LENGTH_SHORT).show()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            videoUri = uri.toString()
            Toast.makeText(context, "Video attached!", Toast.LENGTH_SHORT).show()
        }
    }

    // Weekly consistency computation
    val weeklyDays = remember(gainNotes) {
        val today = Calendar.getInstance()
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            val dayInitial = SimpleDateFormat("E", Locale.US).format(cal.time).take(1)
            val isDone = gainNotes.any {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.timestamp)) == dStr
            }
            dayInitial to isDone
        }
    }

    val softwareWinsCount = gainNotes.count { it.category.contains("Software", ignoreCase = true) || it.category.contains("Dev", ignoreCase = true) }
    val weeklyGoalTarget = 5f
    val recentNotesCount = weeklyDays.count { it.second }
    val progressFraction = (recentNotesCount / weeklyGoalTarget).coerceIn(0f, 1f)

    // Detail Dialog
    if (selectedNoteForDetail != null) {
        val note = selectedNoteForDetail!!
        AlertDialog(
            onDismissRequest = { selectedNoteForDetail = null },
            title = {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = NeonGreen
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        text = "Category: ${note.category} | ${
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(note.timestamp))
                        }",
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = note.contentMarkdown,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (note.imageUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            painter = rememberAsyncImagePainter(model = note.imageUri),
                            contentDescription = "Attached Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    if (note.videoUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🎥 Attached Video: ${note.videoUri}",
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNoteForDetail = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sleeperRepo.deleteGainNote(note.id)
                        selectedNoteForDetail = null
                        Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete Note", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Add Note Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log New Gains / Progress", fontWeight = FontWeight.Bold, color = NeonGreen) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Title / Project Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val categories = listOf("Software Modding", "Web Dev", "Daily Achievement", "General")
                    var expandedCat by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expandedCat = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Category: $categoryInput")
                    }
                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    categoryInput = cat
                                    expandedCat = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = markdownInput,
                        onValueChange = { markdownInput = it },
                        label = { Text("Markdown Notes & Achievements") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = "Add Image", tint = NeonGreen)
                        }
                        IconButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Add Video", tint = NeonCyan)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (titleInput.isNotBlank()) {
                            val newNote = GainNote(
                                title = titleInput,
                                category = categoryInput,
                                contentMarkdown = markdownInput,
                                imageUri = imageUri,
                                videoUri = videoUri,
                                audioUri = audioUri
                            )
                            sleeperRepo.saveGainNote(newNote)
                            showAddDialog = false
                            titleInput = ""
                            imageUri = null
                            videoUri = null
                            Toast.makeText(context, "Note added!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Note", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AnimatedGlassyBackground {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = NeonGreen,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_gain_note_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Gain Note")
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                            modifier = Modifier.testTag("gains_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Hub",
                                tint = NeonGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Daily Gains",
                            tint = NeonGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Gains",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // PREMIUM STATS OVERHAUL
                    item {
                        GlassCard(borderColor = NeonGreen) {
                            Text(
                                text = "Discipline & Growth Analytics",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AnimatedProgressRing(
                                    progress = progressFraction,
                                    size = 96.dp,
                                    strokeWidth = 10.dp,
                                    primaryColor = NeonGreen,
                                    secondaryColor = NeonCyan
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$recentNotesCount/5",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Week Goal",
                                            fontSize = 10.sp,
                                            color = NeonCyan,
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
                                            title = "Total Notes",
                                            value = "${gainNotes.size}",
                                            icon = Icons.Default.LibraryBooks,
                                            badgeText = "All-Time",
                                            accentColor = NeonGreen,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetricStatCard(
                                            title = "Tech Wins",
                                            value = "$softwareWinsCount",
                                            icon = Icons.Default.Code,
                                            badgeText = "Modding",
                                            accentColor = NeonCyan,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7-Day Consistency Heatmap
                    item {
                        WeeklyConsistencyHeatmap(
                            days = weeklyDays,
                            title = "7-Day Notes Tracker",
                            activeColor = NeonGreen
                        )
                    }

                    item {
                        Text(
                            text = "Logged Gains (${gainNotes.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (gainNotes.isEmpty()) {
                        item {
                            GlassCard {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No gains logged yet. Tap + to document your milestones!",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(gainNotes.chunked(2).size) { rowIndex ->
                            val chunk = gainNotes.chunked(2)[rowIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (note in chunk) {
                                    GlassCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedNoteForDetail = note }
                                            .testTag("gain_card_${note.id}")
                                    ) {
                                        Text(
                                            text = note.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.category,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeonGreen
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = note.contentMarkdown,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3
                                        )

                                        if (note.imageUri != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Image(
                                                painter = rememberAsyncImagePainter(model = note.imageUri),
                                                contentDescription = "Thumbnail",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(70.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
