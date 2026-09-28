package com.sleeper.build7.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.ui.components.AnimatedGlassyBackground
import com.sleeper.build7.ui.components.GlassButton
import com.sleeper.build7.ui.components.GlassCard
import com.sleeper.build7.ui.theme.NeonCyan
import com.sleeper.build7.ui.theme.NeonGreen

@Composable
fun BackupScreen(
    sleeperRepo: SleeperRepository,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current

    var pinExportInput by remember { mutableStateOf("1234") }
    var isPinExportVisible by remember { mutableStateOf(false) }
    var encryptedExportResult by remember { mutableStateOf("") }

    var pinImportInput by remember { mutableStateOf("1234") }
    var isPinImportVisible by remember { mutableStateOf(false) }
    var encryptedImportInput by remember { mutableStateOf("") }

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
                        modifier = Modifier.testTag("backup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Hub",
                            tint = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Backup",
                        tint = NeonGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PIN Encrypted Backup & Restore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "AES-256 PBKDF2 Zero-Knowledge Security",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // EXPORT SECTION
                item {
                    GlassCard {
                        Text(
                            text = "1. Encrypted Export",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NeonGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pinExportInput,
                            onValueChange = { pinExportInput = it },
                            label = { Text("Set Encryption PIN Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = if (isPinExportVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (isPinExportVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                val description = if (isPinExportVisible) "Hide PIN" else "Show PIN"
                                IconButton(
                                    onClick = { isPinExportVisible = !isPinExportVisible },
                                    modifier = Modifier.testTag("export_pin_visibility_btn")
                                ) {
                                    Icon(imageVector = image, contentDescription = description, tint = NeonGreen)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_pin_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassButton(
                            text = "Generate Encrypted Backup String",
                            onClick = {
                                if (pinExportInput.length >= 4) {
                                    try {
                                        encryptedExportResult = sleeperRepo.exportEncryptedBackup(pinExportInput.trim())
                                        Toast.makeText(context, "Encrypted payload generated!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "generate_export_btn"
                        )

                        if (encryptedExportResult.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Encrypted Output (Copy & Store Safely):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = encryptedExportResult,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 4
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            GlassButton(
                                text = "Copy Payload to Clipboard",
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("SleeperBackup", encryptedExportResult)
                                    cm.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "copy_payload_btn"
                            )
                        }
                    }
                }

                // IMPORT SECTION
                item {
                    GlassCard {
                        Text(
                            text = "2. Encrypted Import & Restore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pinImportInput,
                            onValueChange = { pinImportInput = it },
                            label = { Text("Enter Encryption PIN Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = if (isPinImportVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (isPinImportVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                val description = if (isPinImportVisible) "Hide PIN" else "Show PIN"
                                IconButton(
                                    onClick = { isPinImportVisible = !isPinImportVisible },
                                    modifier = Modifier.testTag("import_pin_visibility_btn")
                                ) {
                                    Icon(imageVector = image, contentDescription = description, tint = NeonCyan)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_pin_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = encryptedImportInput,
                            onValueChange = { encryptedImportInput = it },
                            label = { Text("Paste Encrypted Payload String") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("import_payload_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassButton(
                            text = "Decrypt & Restore Data",
                            onClick = {
                                if (pinImportInput.isNotBlank() && encryptedImportInput.isNotBlank()) {
                                    val success = sleeperRepo.importEncryptedBackup(encryptedImportInput.trim(), pinImportInput.trim())
                                    if (success) {
                                        Toast.makeText(context, "🎉 Backup restored successfully!", Toast.LENGTH_LONG).show()
                                        encryptedImportInput = ""
                                    } else {
                                        Toast.makeText(context, "❌ Decryption failed. Incorrect PIN or corrupt string.", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter both PIN and Payload", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = NeonCyan,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            testTag = "restore_backup_btn"
                        )
                    }
                }
            }
        }
    }
}
