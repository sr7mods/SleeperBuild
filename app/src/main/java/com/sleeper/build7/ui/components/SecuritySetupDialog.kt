package com.sleeper.build7.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeper.build7.data.SleeperRepository

val PRESET_SECURITY_QUESTIONS = listOf(
    "What was your first childhood pet's name?",
    "What city were you born in?",
    "What is your favorite personal motto?",
    "What was the name of your first school?",
    "What is your secret master keyword?"
)

/**
 * Dialog for creating, changing, or disabling security lock methods.
 * Enforces Previous Lock confirmation before allowing modifications.
 */
@Composable
fun SecuritySetupDialog(
    sleeperRepo: SleeperRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isSecurityEnabled by sleeperRepo.isSecurityEnabled.collectAsState()
    val activeLockType by sleeperRepo.lockType.collectAsState()

    var step by remember {
        mutableStateOf(if (isSecurityEnabled) "confirm_previous" else "choose_type")
    }

    var selectedType by remember { mutableStateOf(activeLockType) }
    var inputCode by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }
    var patternCode by remember { mutableStateOf("") }
    var confirmPatternCode by remember { mutableStateOf("") }

    var selectedQuestion by remember { mutableStateOf(PRESET_SECURITY_QUESTIONS.first()) }
    var questionAnswer by remember { mutableStateOf("") }
    var previousLockInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (step) {
                        "confirm_previous" -> "Confirm Previous Lock"
                        "choose_type" -> "Select Security Lock"
                        "enter_value" -> "Set $selectedType Lock"
                        "security_qa" -> "Emergency Password Recovery"
                        else -> "Security Settings"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = errorColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = errorColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                when (step) {
                    "confirm_previous" -> {
                        Text(
                            text = "To change or disable security, please enter your current $activeLockType to verify ownership.",
                            fontSize = 13.sp
                        )

                        if (activeLockType == "pattern") {
                            PatternLockView(
                                size = 240.dp,
                                activeColor = primaryColor,
                                onPatternComplete = { pat ->
                                    if (sleeperRepo.verifyLock(pat)) {
                                        step = "choose_type"
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Incorrect pattern. Please try again."
                                    }
                                }
                            )
                        } else {
                            OutlinedTextField(
                                value = previousLockInput,
                                onValueChange = { previousLockInput = it },
                                label = { Text("Current $activeLockType") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (activeLockType == "pin") KeyboardType.NumberPassword else KeyboardType.Password
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        if (sleeperRepo.verifyLock(previousLockInput)) {
                                            step = "choose_type"
                                            errorMessage = null
                                        } else {
                                            errorMessage = "Incorrect $activeLockType. Try again."
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Verify & Continue")
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (sleeperRepo.verifyLock(previousLockInput)) {
                                            sleeperRepo.clearSecurity()
                                            Toast.makeText(context, "Security disabled.", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            errorMessage = "Incorrect $activeLockType."
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = errorColor)
                                ) {
                                    Text("Disable")
                                }
                            }
                        }
                    }

                    "choose_type" -> {
                        Text(
                            text = "Choose your preferred authentication method:",
                            fontSize = 13.sp
                        )

                        val types = listOf(
                            Triple("pin", "Numeric PIN", Icons.Default.Pin),
                            Triple("password", "Alphanumeric Password", Icons.Default.Password),
                            Triple("pattern", "3x3 Pattern Lock", Icons.Default.Gesture)
                        )

                        types.forEach { (typeKey, title, icon) ->
                            val isSelected = selectedType == typeKey
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedType = typeKey },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) primaryColor else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                step = "enter_value"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Next: Set Credentials")
                        }
                    }

                    "enter_value" -> {
                        when (selectedType) {
                            "pattern" -> {
                                Text(
                                    text = if (patternCode.isEmpty()) "Draw your 3x3 pattern to register:" else "Draw the pattern again to confirm:",
                                    fontSize = 13.sp
                                )

                                PatternLockView(
                                    size = 240.dp,
                                    activeColor = primaryColor,
                                    onPatternComplete = { drawn ->
                                        if (patternCode.isEmpty()) {
                                            patternCode = drawn
                                            errorMessage = null
                                        } else {
                                            confirmPatternCode = drawn
                                            if (patternCode == confirmPatternCode) {
                                                errorMessage = null
                                                step = "security_qa"
                                            } else {
                                                errorMessage = "Patterns do not match. Please redraw."
                                                patternCode = ""
                                                confirmPatternCode = ""
                                            }
                                        }
                                    }
                                )
                            }

                            "pin" -> {
                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = { if (it.length <= 6) inputCode = it },
                                    label = { Text("Enter 4-6 Digit PIN") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = confirmCode,
                                    onValueChange = { if (it.length <= 6) confirmCode = it },
                                    label = { Text("Confirm 4-6 Digit PIN") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (inputCode.length < 4) {
                                            errorMessage = "PIN must be at least 4 digits."
                                        } else if (inputCode != confirmCode) {
                                            errorMessage = "PINs do not match."
                                        } else {
                                            errorMessage = null
                                            step = "security_qa"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Next: Emergency Question")
                                }
                            }

                            else -> {
                                // Password
                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = { inputCode = it },
                                    label = { Text("Enter Master Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = confirmCode,
                                    onValueChange = { confirmCode = it },
                                    label = { Text("Confirm Master Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (inputCode.length < 4) {
                                            errorMessage = "Password must be at least 4 characters."
                                        } else if (inputCode != confirmCode) {
                                            errorMessage = "Passwords do not match."
                                        } else {
                                            errorMessage = null
                                            step = "security_qa"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Next: Emergency Question")
                                }
                            }
                        }
                    }

                    "security_qa" -> {
                        Text(
                            text = "Set a secret security question & answer for emergency password recovery if you ever forget your credentials.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        PRESET_SECURITY_QUESTIONS.forEach { q ->
                            val isSel = selectedQuestion == q
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedQuestion = q }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = { selectedQuestion = q }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = q,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) primaryColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = questionAnswer,
                            onValueChange = { questionAnswer = it },
                            label = { Text("Secret Answer") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (questionAnswer.isBlank()) {
                                    errorMessage = "Please enter an answer for recovery."
                                } else {
                                    val finalValue = if (selectedType == "pattern") patternCode else inputCode
                                    sleeperRepo.setupSecurity(
                                        type = selectedType,
                                        value = finalValue,
                                        question = selectedQuestion,
                                        answer = questionAnswer
                                    )
                                    Toast.makeText(context, "$selectedType security lock configured!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save & Activate Security")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
