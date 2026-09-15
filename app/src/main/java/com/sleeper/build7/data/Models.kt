package com.sleeper.build7.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PrayerItem(
    val name: String,
    val time: String,
    val isCompleted: Boolean = false,
    val alarmEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class DailyPrayerRecord(
    val date: String, // YYYY-MM-DD
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ExerciseStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val target: String, // e.g., "10 Reps", "45 Secs"
    val restSecs: Int = 30,
    val formTip: String = "Maintain control, keep core engaged and breathe rhythmically.",
    val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class WorkoutPlan(
    val date: String,
    val weightKg: Float = 70f,
    val heightCm: Float = 175f,
    val bmi: Float = 22.8f,
    val equipmentMode: String = "Bodyweight", // Bodyweight, Simple Equipment, Full Equipment
    val targetSetsReps: String = "Push-ups: 4x15, Pull-ups: 3x8, Squats: 4x20",
    val workoutTime: String = "17:00",
    val durationMins: Int = 30,
    val isCompleted: Boolean = false,
    val levelRank: String = "Sleeper Initiate (Lv 1)"
)

@JsonClass(generateAdapter = true)
data class StudySession(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val startTime: String, // HH:mm
    val durationMins: Int,
    val isCompleted: Boolean = false,
    val date: String // YYYY-MM-DD
)

@JsonClass(generateAdapter = true)
data class GainNote(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val category: String, // Software Modding, Web Dev, Daily Achievement, General
    val contentMarkdown: String,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val audioUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class RelapseEntry(
    val timestamp: Long,
    val daysSucceeded: Int,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class StreakData(
    val isTimerStarted: Boolean = false,
    val lastRelapseTimestamp: Long = System.currentTimeMillis(),
    val highestStreakDays: Int = 0,
    val totalRelapses: Int = 0,
    val history: List<RelapseEntry> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FullBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val prayerRecords: List<DailyPrayerRecord> = emptyList(),
    val workoutPlans: List<WorkoutPlan> = emptyList(),
    val studySessions: List<StudySession> = emptyList(),
    val gainNotes: List<GainNote> = emptyList(),
    val noFapStreak: StreakData = StreakData(),
    val noSmokeStreak: StreakData = StreakData(),
    val totalSmokeCount: Int = 0,
    val smokeModuleMode: String = "angelic",
    val musicAppPackage: String = "com.spotify.music"
)
