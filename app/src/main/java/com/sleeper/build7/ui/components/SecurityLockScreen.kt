package com.sleeper.build7.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeper.build7.data.SleeperRepository

/**
 * Full-screen Security Lock Screen Composable supporting PIN, Password, Pattern,
 * and emergency Security Question recovery.
 */
@Composable
fun SecurityLockScreen(
    sleeperRepo: SleeperRepository,
    title: String = "Sleeper Security Lock",
    subtitle: String = "Enter credentials to unlock",
    onUnlocked: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    BackHandler(enabled = onCancel != null) {
        onCancel?.invoke()
    }

    val context = LocalContext.current
    val lockType by sleeperRepo.lockType.collectAsState()
    val securityQuestion by sleeperRepo.securityQuestion.collectAsState()

    var pinInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Forgot Password / Recovery Dialog State
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    fun attemptUnlock(value: String) {
        errorMessage = null
        if (sleeperRepo.verifyLock(value)) {
            onUnlocked()
        } else {
            errorMessage = "Incorrect $lockType. Try again."
            pinInput = ""
            passwordInput = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617),
                        Color(0xFF000000)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onCancel != null) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("lock_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Surface(
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Lock",
                        tint = primaryColor,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Title & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = errorColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Interactive Lock Input depending on type
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (lockType) {
                    "pattern" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Draw your 3x3 pattern to unlock",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            PatternLockView(
                                size = 280.dp,
                                activeColor = primaryColor,
                                onPatternComplete = { patternStr ->
                                    attemptUnlock(patternStr)
                                }
                            )
                        }
                    }

                    "password" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Enter Master Password", color = Color.White.copy(alpha = 0.7f)) },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { attemptUnlock(passwordInput) }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lock_password_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            GlassButton(
                                text = "Unlock Now",
                                onClick = { attemptUnlock(passwordInput) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lock_password_submit_btn")
                            )
                        }
                    }

                    else -> {
                        // Numeric PIN Mode
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // PIN indicator dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 14.dp)
                            ) {
                                for (i in 0 until 4) {
                                    val isFilled = i < pinInput.length
                                    Surface(
                                        modifier = Modifier.size(16.dp),
                                        shape = CircleShape,
                                        color = if (isFilled) primaryColor else Color.White.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.5.dp,
                                            if (isFilled) primaryColor else Color.White.copy(alpha = 0.4f)
                                        )
                                    ) {}
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3x4 Numeric Keypad
                            NumericKeypad(
                                onDigit = { digit ->
                                    if (pinInput.length < 6) {
                                        val newPin = pinInput + digit
                                        pinInput = newPin
                                        if (newPin.length >= 4) {
                                            attemptUnlock(newPin)
                                        }
                                    }
                                },
                                onBackspace = {
                                    if (pinInput.isNotEmpty()) {
                                        pinInput = pinInput.dropLast(1)
                                    }
                                },
                                onClear = { pinInput = "" }
                            )
                        }
                    }
                }
            }

            // Forgot Password / Recovery Button
            TextButton(
                onClick = {
                    recoveryAnswerInput = ""
                    recoveryError = null
                    showRecoveryDialog = true
                },
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("forgot_password_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Forgot Credentials? (Recovery)",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Emergency Security Question & Password Recovery Dialog
    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.85f),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = primaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Security Recovery",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Answer your secret security question to reset and bypass all locks.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = securityQuestion,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = primaryColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = recoveryAnswerInput,
                        onValueChange = { recoveryAnswerInput = it },
                        label = { Text("Your Answer", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_answer_input")
                    )

                    if (recoveryError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = recoveryError!!,
                            color = errorColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sleeperRepo.verifySecurityAnswer(recoveryAnswerInput)) {
                            sleeperRepo.clearSecurity()
                            showRecoveryDialog = false
                            Toast.makeText(context, "Security reset successfully!", Toast.LENGTH_LONG).show()
                            onUnlocked()
                        } else {
                            recoveryError = "Incorrect answer. Please check your answer."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.testTag("submit_recovery_btn")
                ) {
                    Text("Verify & Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        digits.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable {
                                when (key) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    else -> onDigit(key)
                                }
                            }
                            .testTag("keypad_$key"),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key,
                                fontSize = if (key == "⌫") 20.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
