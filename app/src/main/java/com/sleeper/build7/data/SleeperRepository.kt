package com.sleeper.build7.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SleeperRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sleeper_build7_prefs", Context.MODE_PRIVATE)

    // Secure encrypted storage for sensitive lock credentials and pins
    private val securePrefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "sleeper_build7_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for JVM environments or device Keystore fallback
        context.getSharedPreferences("sleeper_build7_secure_prefs_fallback", Context.MODE_PRIVATE)
    }

    init {
        // One-time migration: migrate any legacy unencrypted security data to EncryptedSharedPreferences
        if (prefs.contains("sec_enabled") && !securePrefs.contains("sec_enabled")) {
            val editor = securePrefs.edit()
            if (prefs.contains("sec_enabled")) editor.putBoolean("sec_enabled", prefs.getBoolean("sec_enabled", false))
            if (prefs.contains("sec_lock_type")) editor.putString("sec_lock_type", prefs.getString("sec_lock_type", "pin"))
            if (prefs.contains("sec_lock_hash")) editor.putString("sec_lock_hash", prefs.getString("sec_lock_hash", null))
            if (prefs.contains("sec_salt")) editor.putString("sec_salt", prefs.getString("sec_salt", null))
            if (prefs.contains("sec_question")) editor.putString("sec_question", prefs.getString("sec_question", null))
            if (prefs.contains("sec_answer_hash")) editor.putString("sec_answer_hash", prefs.getString("sec_answer_hash", null))
            if (prefs.contains("sec_app_lock_enabled")) editor.putBoolean("sec_app_lock_enabled", prefs.getBoolean("sec_app_lock_enabled", false))
            if (prefs.contains("sec_locked_sections")) editor.putStringSet("sec_locked_sections", prefs.getStringSet("sec_locked_sections", null))
            editor.apply()

            // Remove legacy credentials from unencrypted SharedPreferences
            prefs.edit()
                .remove("sec_enabled")
                .remove("sec_lock_type")
                .remove("sec_lock_hash")
                .remove("sec_salt")
                .remove("sec_question")
                .remove("sec_answer_hash")
                .remove("sec_app_lock_enabled")
                .remove("sec_locked_sections")
                .apply()
        }
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val fullBackupAdapter = moshi.adapter(FullBackupData::class.java)

    // State flows
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _todayPrayerRecord = MutableStateFlow(loadTodayPrayerRecord())
    val todayPrayerRecord: StateFlow<DailyPrayerRecord> = _todayPrayerRecord.asStateFlow()

    private val _allPrayerRecords = MutableStateFlow(loadAllPrayerRecords())
    val allPrayerRecords: StateFlow<List<DailyPrayerRecord>> = _allPrayerRecords.asStateFlow()
    val prayerRecords: StateFlow<List<DailyPrayerRecord>> get() = allPrayerRecords

    private val _workoutPlans = MutableStateFlow(loadWorkoutPlans())
    val workoutPlans: StateFlow<List<WorkoutPlan>> = _workoutPlans.asStateFlow()

    private val _studySessions = MutableStateFlow(loadStudySessions())
    val studySessions: StateFlow<List<StudySession>> = _studySessions.asStateFlow()

    private val _gainNotes = MutableStateFlow(loadGainNotes())
    val gainNotes: StateFlow<List<GainNote>> = _gainNotes.asStateFlow()

    private val _noFapStreak = MutableStateFlow(loadStreak("no_fap"))
    val noFapStreak: StateFlow<StreakData> = _noFapStreak.asStateFlow()

    private val _noSmokeStreak = MutableStateFlow(loadStreak("no_smoke"))
    val noSmokeStreak: StateFlow<StreakData> = _noSmokeStreak.asStateFlow()

    private val _musicAppPackage = MutableStateFlow(prefs.getString("music_app_pkg", "com.ongaku7.player") ?: "com.ongaku7.player")
    val musicAppPackage: StateFlow<String> = _musicAppPackage.asStateFlow()

    private val _selectedWidgetProvider = MutableStateFlow(prefs.getString("selected_widget_provider", null))
    val selectedWidgetProvider: StateFlow<String?> = _selectedWidgetProvider.asStateFlow()

    private val _waterIntakeMl = MutableStateFlow(prefs.getInt("water_intake_${getTodayDate()}", 1250))
    val waterIntakeMl: StateFlow<Int> = _waterIntakeMl.asStateFlow()

    private val _prayerAlarmsEnabled = MutableStateFlow(prefs.getBoolean("prayer_alarms_enabled", true))
    val prayerAlarmsEnabled: StateFlow<Boolean> = _prayerAlarmsEnabled.asStateFlow()

    // NO SMOKE DUAL-MODE & PERSISTENT COUNTER (Fix #4)
    private val _smokeModuleMode = MutableStateFlow(prefs.getString("smoke_module_mode", "angelic") ?: "angelic")
    val smokeModuleMode: StateFlow<String> = _smokeModuleMode.asStateFlow()

    private val _totalSmokeCount = MutableStateFlow(prefs.getInt("total_smoke_count", 0))
    val totalSmokeCount: StateFlow<Int> = _totalSmokeCount.asStateFlow()

    private val _todaySmokeCount = MutableStateFlow(prefs.getInt("smoke_today_${getTodayDate()}", 0))
    val todaySmokeCount: StateFlow<Int> = _todaySmokeCount.asStateFlow()

    private val _lastSmokeTimestamp = MutableStateFlow(prefs.getLong("last_smoke_timestamp", 0L))
    val lastSmokeTimestamp: StateFlow<Long> = _lastSmokeTimestamp.asStateFlow()

    fun setSmokeModuleMode(mode: String) {
        _smokeModuleMode.value = mode
        prefs.edit().putString("smoke_module_mode", mode).apply()
    }

    fun logCigarette() {
        val newTotal = _totalSmokeCount.value + 1
        _totalSmokeCount.value = newTotal

        val todayKey = "smoke_today_${getTodayDate()}"
        val newToday = prefs.getInt(todayKey, 0) + 1
        _todaySmokeCount.value = newToday

        val now = System.currentTimeMillis()
        _lastSmokeTimestamp.value = now

        prefs.edit()
            .putInt("total_smoke_count", newTotal)
            .putInt(todayKey, newToday)
            .putLong("last_smoke_timestamp", now)
            .apply()
    }

    fun undoCigarette() {
        if (_totalSmokeCount.value > 0) {
            val newTotal = _totalSmokeCount.value - 1
            _totalSmokeCount.value = newTotal

            val todayKey = "smoke_today_${getTodayDate()}"
            val currentToday = prefs.getInt(todayKey, 0)
            val newToday = (currentToday - 1).coerceAtLeast(0)
            _todaySmokeCount.value = newToday

            prefs.edit()
                .putInt("total_smoke_count", newTotal)
                .putInt(todayKey, newToday)
                .apply()
        }
    }

    // DASHBOARD CUSTOMIZATION & VISIBILITY TOGGLES (Fix #6)
    private fun loadDashboardSectionVisibility(): Map<String, Boolean> {
        val defaultKeys = listOf("prayer", "workout", "study", "gains", "nofap", "nosmoke")
        val map = mutableMapOf<String, Boolean>()
        for (key in defaultKeys) {
            map[key] = prefs.getBoolean("show_section_$key", true)
        }
        return map
    }

    private val _dashboardSectionVisibility = MutableStateFlow(loadDashboardSectionVisibility())
    val dashboardSectionVisibility: StateFlow<Map<String, Boolean>> = _dashboardSectionVisibility.asStateFlow()

    fun setSectionVisibility(sectionKey: String, isVisible: Boolean) {
        val current = _dashboardSectionVisibility.value.toMutableMap()
        current[sectionKey] = isVisible
        _dashboardSectionVisibility.value = current
        prefs.edit().putBoolean("show_section_$sectionKey", isVisible).apply()
    }

    // COMPREHENSIVE SECURITY SYSTEM (Fix #7)
    private fun hashWithSalt(input: String, salt: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + input).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private val _isSecurityEnabled = MutableStateFlow(securePrefs.getBoolean("sec_enabled", false))
    val isSecurityEnabled: StateFlow<Boolean> = _isSecurityEnabled.asStateFlow()

    private val _lockType = MutableStateFlow(securePrefs.getString("sec_lock_type", "pin") ?: "pin")
    val lockType: StateFlow<String> = _lockType.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(securePrefs.getBoolean("sec_app_lock_enabled", false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _securityQuestion = MutableStateFlow(securePrefs.getString("sec_question", "What was your first childhood pet's name?") ?: "What was your first childhood pet's name?")
    val securityQuestion: StateFlow<String> = _securityQuestion.asStateFlow()

    private val _lockedSections = MutableStateFlow(securePrefs.getStringSet("sec_locked_sections", emptySet()) ?: emptySet())
    val lockedSections: StateFlow<Set<String>> = _lockedSections.asStateFlow()

    fun setupSecurity(type: String, value: String, question: String, answer: String) {
        val salt = UUID.randomUUID().toString()
        val lockHash = hashWithSalt(value, salt)
        val answerHash = hashWithSalt(answer.trim().lowercase(), salt)

        securePrefs.edit()
            .putBoolean("sec_enabled", true)
            .putString("sec_lock_type", type)
            .putString("sec_lock_hash", lockHash)
            .putString("sec_salt", salt)
            .putString("sec_question", question)
            .putString("sec_answer_hash", answerHash)
            .apply()

        _isSecurityEnabled.value = true
        _lockType.value = type
        _securityQuestion.value = question
    }

    fun updateLock(newType: String, newValue: String) {
        val salt = UUID.randomUUID().toString()
        val lockHash = hashWithSalt(newValue, salt)
        securePrefs.edit()
            .putString("sec_lock_type", newType)
            .putString("sec_lock_hash", lockHash)
            .putString("sec_salt", salt)
            .apply()

        _lockType.value = newType
    }

    fun verifyLock(input: String): Boolean {
        if (!_isSecurityEnabled.value) return true
        val salt = securePrefs.getString("sec_salt", "") ?: ""
        val expectedHash = securePrefs.getString("sec_lock_hash", "") ?: ""
        if (salt.isEmpty() || expectedHash.isEmpty()) return true
        return hashWithSalt(input, salt) == expectedHash
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val salt = securePrefs.getString("sec_salt", "") ?: ""
        val expectedAnswerHash = securePrefs.getString("sec_answer_hash", "") ?: ""
        if (salt.isEmpty() || expectedAnswerHash.isEmpty()) return false
        return hashWithSalt(answer.trim().lowercase(), salt) == expectedAnswerHash
    }

    fun clearSecurity() {
        securePrefs.edit()
            .putBoolean("sec_enabled", false)
            .remove("sec_lock_type")
            .remove("sec_lock_hash")
            .remove("sec_salt")
            .remove("sec_question")
            .remove("sec_answer_hash")
            .putBoolean("sec_app_lock_enabled", false)
            .remove("sec_locked_sections")
            .apply()

        _isSecurityEnabled.value = false
        _isAppLockEnabled.value = false
        _lockedSections.value = emptySet()
    }

    fun setAppLockEnabled(enabled: Boolean) {
        _isAppLockEnabled.value = enabled
        securePrefs.edit().putBoolean("sec_app_lock_enabled", enabled).apply()
    }

    fun setSectionLock(route: String, isLocked: Boolean) {
        val current = _lockedSections.value.toMutableSet()
        if (isLocked) current.add(route) else current.remove(route)
        _lockedSections.value = current
        securePrefs.edit().putStringSet("sec_locked_sections", current).apply()
    }

    fun setSectionLocked(route: String, isLocked: Boolean) {
        setSectionLock(route, isLocked)
    }

    fun isSectionLocked(route: String): Boolean {
        return _isSecurityEnabled.value && _lockedSections.value.contains(route)
    }

    // ADVANCED CUSTOM TTS / RINGTONE ALARM SYSTEM (Fix #8)
    private val _audioAlarmMode = MutableStateFlow(prefs.getString("audio_alarm_mode", "tts") ?: "tts")
    val audioAlarmMode: StateFlow<String> = _audioAlarmMode.asStateFlow()

    private val _ttsVoiceProfile = MutableStateFlow(prefs.getString("tts_voice_profile", "girl") ?: "girl")
    val ttsVoiceProfile: StateFlow<String> = _ttsVoiceProfile.asStateFlow()

    private val _ttsCustomText = MutableStateFlow(
        prefs.getString("tts_custom_text", "Sleeper discipline alert! Time for your scheduled task, stay locked in.")
            ?: "Sleeper discipline alert! Time for your scheduled task, stay locked in."
    )
    val ttsCustomText: StateFlow<String> = _ttsCustomText.asStateFlow()

    fun setAudioAlarmMode(mode: String) {
        _audioAlarmMode.value = mode
        prefs.edit().putString("audio_alarm_mode", mode).apply()
    }

    fun setTtsVoiceProfile(profile: String) {
        _ttsVoiceProfile.value = profile
        prefs.edit().putString("tts_voice_profile", profile).apply()
    }

    fun setTtsCustomText(text: String) {
        _ttsCustomText.value = text
        prefs.edit().putString("tts_custom_text", text).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    // PRAYER
    private fun loadTodayPrayerRecord(): DailyPrayerRecord {
        val date = getTodayDate()
        val json = prefs.getString("prayer_rec_$date", null)
        return if (json != null) {
            try {
                moshi.adapter(DailyPrayerRecord::class.java).fromJson(json) ?: DailyPrayerRecord(date = date)
            } catch (e: Exception) {
                DailyPrayerRecord(date = date)
            }
        } else {
            DailyPrayerRecord(date = date)
        }
    }

    fun loadAllPrayerRecords(): List<DailyPrayerRecord> {
        val records = mutableListOf<DailyPrayerRecord>()
        val adapter = moshi.adapter(DailyPrayerRecord::class.java)
        val allPrefs = prefs.all
        for ((key, value) in allPrefs) {
            if (key.startsWith("prayer_rec_") && value is String) {
                try {
                    val record = adapter.fromJson(value)
                    if (record != null) records.add(record)
                } catch (e: Exception) { }
            }
        }
        return records.sortedByDescending { it.date }
    }

    fun updatePrayerCheck(prayerName: String, isChecked: Boolean) {
        val current = _todayPrayerRecord.value
        val updated = when (prayerName.lowercase()) {
            "fajr" -> current.copy(fajr = isChecked)
            "dhuhr" -> current.copy(dhuhr = isChecked)
            "asr" -> current.copy(asr = isChecked)
            "maghrib" -> current.copy(maghrib = isChecked)
            "isha" -> current.copy(isha = isChecked)
            else -> current
        }
        _todayPrayerRecord.value = updated
        val json = moshi.adapter(DailyPrayerRecord::class.java).toJson(updated)
        prefs.edit().putString("prayer_rec_${updated.date}", json).apply()
        _allPrayerRecords.value = loadAllPrayerRecords()
    }

    fun setPrayerAlarmsEnabled(enabled: Boolean) {
        _prayerAlarmsEnabled.value = enabled
        prefs.edit().putBoolean("prayer_alarms_enabled", enabled).apply()
    }

    // WORKOUT
    private fun loadWorkoutPlans(): List<WorkoutPlan> {
        val json = prefs.getString("workout_plans", null) ?: return emptyList()
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, WorkoutPlan::class.java)
            moshi.adapter<List<WorkoutPlan>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWorkoutPlan(plan: WorkoutPlan) {
        val current = _workoutPlans.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.date == plan.date }
        if (existingIndex >= 0) {
            current[existingIndex] = plan
        } else {
            current.add(0, plan)
        }
        _workoutPlans.value = current
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, WorkoutPlan::class.java)
        val json = moshi.adapter<List<WorkoutPlan>>(type).toJson(current)
        prefs.edit().putString("workout_plans", json).apply()
    }

    fun setMusicAppPackage(pkgName: String) {
        _musicAppPackage.value = pkgName
        prefs.edit().putString("music_app_pkg", pkgName).apply()
    }

    fun setSelectedWidgetProvider(providerClassName: String?) {
        _selectedWidgetProvider.value = providerClassName
        if (providerClassName != null) {
            prefs.edit().putString("selected_widget_provider", providerClassName).apply()
        } else {
            prefs.edit().remove("selected_widget_provider").apply()
        }
    }

    fun addWaterIntake(amountMl: Int) {
        val newAmount = (_waterIntakeMl.value + amountMl).coerceIn(0, 10000)
        _waterIntakeMl.value = newAmount
        prefs.edit().putInt("water_intake_${getTodayDate()}", newAmount).apply()
    }

    // STUDY
    private fun loadStudySessions(): List<StudySession> {
        val json = prefs.getString("study_sessions", null) ?: return emptyList()
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, StudySession::class.java)
            moshi.adapter<List<StudySession>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addStudySession(session: StudySession) {
        val current = _studySessions.value.toMutableList()
        current.add(0, session)
        _studySessions.value = current
        saveStudySessionsInternal(current)
    }

    fun saveStudySession(session: StudySession) = addStudySession(session)

    fun toggleStudySessionCompleted(sessionId: String) {
        val current = _studySessions.value.toMutableList()
        val index = current.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            current[index] = current[index].copy(isCompleted = !current[index].isCompleted)
            _studySessions.value = current
            saveStudySessionsInternal(current)
        }
    }

    private fun saveStudySessionsInternal(list: List<StudySession>) {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, StudySession::class.java)
        val json = moshi.adapter<List<StudySession>>(type).toJson(list)
        prefs.edit().putString("study_sessions", json).apply()
    }

    // GAINS NOTES
    private fun loadGainNotes(): List<GainNote> {
        val json = prefs.getString("gain_notes", null) ?: return emptyList()
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, GainNote::class.java)
            moshi.adapter<List<GainNote>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveGainNote(note: GainNote) {
        val current = _gainNotes.value.toMutableList()
        val index = current.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            current[index] = note
        } else {
            current.add(0, note)
        }
        _gainNotes.value = current
        saveGainNotesInternal(current)
    }

    fun deleteGainNote(noteId: String) {
        val current = _gainNotes.value.filter { it.id != noteId }
        _gainNotes.value = current
        saveGainNotesInternal(current)
    }

    private fun saveGainNotesInternal(list: List<GainNote>) {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, GainNote::class.java)
        val json = moshi.adapter<List<GainNote>>(type).toJson(list)
        prefs.edit().putString("gain_notes", json).apply()
    }

    // STREAKS (NoFap & NoSmoke)
    private fun loadStreak(key: String): StreakData {
        val json = prefs.getString("streak_$key", null) ?: return StreakData()
        return try {
            moshi.adapter(StreakData::class.java).fromJson(json) ?: StreakData()
        } catch (e: Exception) {
            StreakData()
        }
    }

    fun startStreakTimer(key: String) {
        val current = loadStreak(key)
        val now = System.currentTimeMillis()
        val updated = current.copy(
            isTimerStarted = true,
            lastRelapseTimestamp = now
        )
        if (key == "no_fap") _noFapStreak.value = updated
        if (key == "no_smoke") _noSmokeStreak.value = updated

        val json = moshi.adapter(StreakData::class.java).toJson(updated)
        prefs.edit().putString("streak_$key", json).apply()
    }

    fun resetStreak(key: String, note: String = "") {
        val current = loadStreak(key)
        val now = System.currentTimeMillis()
        val daysSucceeded = if (current.isTimerStarted && current.lastRelapseTimestamp > 0) {
            ((now - current.lastRelapseTimestamp) / (1000 * 60 * 60 * 24)).toInt()
        } else 0

        val newHighest = maxOf(current.highestStreakDays, daysSucceeded)

        val historyList = current.history.toMutableList()
        historyList.add(0, RelapseEntry(timestamp = now, daysSucceeded = daysSucceeded, note = note))

        val updated = StreakData(
            isTimerStarted = true,
            lastRelapseTimestamp = now,
            highestStreakDays = newHighest,
            totalRelapses = current.totalRelapses + 1,
            history = historyList
        )

        if (key == "no_fap") _noFapStreak.value = updated
        if (key == "no_smoke") _noSmokeStreak.value = updated

        val json = moshi.adapter(StreakData::class.java).toJson(updated)
        prefs.edit().putString("streak_$key", json).apply()
    }

    // ENCRYPTED IMPORT / EXPORT
    fun createFullBackup(): FullBackupData {
        return FullBackupData(
            prayerRecords = loadAllPrayerRecords(),
            workoutPlans = _workoutPlans.value,
            studySessions = _studySessions.value,
            gainNotes = _gainNotes.value,
            noFapStreak = _noFapStreak.value,
            noSmokeStreak = _noSmokeStreak.value,
            totalSmokeCount = _totalSmokeCount.value,
            smokeModuleMode = _smokeModuleMode.value,
            musicAppPackage = _musicAppPackage.value
        )
    }

    fun exportEncryptedBackup(pin: String): String {
        val backup = createFullBackup()
        val json = fullBackupAdapter.toJson(backup)
        return encryptAES(json, pin)
    }

    fun importEncryptedBackup(encryptedStr: String, pin: String): Boolean {
        return try {
            val json = decryptAES(encryptedStr, pin)
            val backup = fullBackupAdapter.fromJson(json) ?: return false

            if (backup.prayerRecords.isNotEmpty()) {
                val prayerAdapter = moshi.adapter(DailyPrayerRecord::class.java)
                val editor = prefs.edit()
                for (record in backup.prayerRecords) {
                    editor.putString("prayer_rec_${record.date}", prayerAdapter.toJson(record))
                }
                editor.apply()
                _allPrayerRecords.value = loadAllPrayerRecords()
                _todayPrayerRecord.value = loadTodayPrayerRecord()
            }

            if (backup.workoutPlans.isNotEmpty()) {
                _workoutPlans.value = backup.workoutPlans
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, WorkoutPlan::class.java)
                prefs.edit().putString("workout_plans", moshi.adapter<List<WorkoutPlan>>(type).toJson(backup.workoutPlans)).apply()
            }

            if (backup.studySessions.isNotEmpty()) {
                _studySessions.value = backup.studySessions
                saveStudySessionsInternal(backup.studySessions)
            }

            if (backup.gainNotes.isNotEmpty()) {
                _gainNotes.value = backup.gainNotes
                saveGainNotesInternal(backup.gainNotes)
            }

            _noFapStreak.value = backup.noFapStreak
            prefs.edit().putString("streak_no_fap", moshi.adapter(StreakData::class.java).toJson(backup.noFapStreak)).apply()

            _noSmokeStreak.value = backup.noSmokeStreak
            prefs.edit().putString("streak_no_smoke", moshi.adapter(StreakData::class.java).toJson(backup.noSmokeStreak)).apply()

            _totalSmokeCount.value = backup.totalSmokeCount
            prefs.edit().putInt("total_smoke_count", backup.totalSmokeCount).apply()
            setSmokeModuleMode(backup.smokeModuleMode)

            setMusicAppPackage(backup.musicAppPackage)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun encryptAES(plainText: String, pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    private fun decryptAES(encryptedBase64: String, pin: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
        val salt = ByteArray(16)
        val iv = ByteArray(16)
        val cipherText = ByteArray(combined.size - 32)

        System.arraycopy(combined, 0, salt, 0, 16)
        System.arraycopy(combined, 16, iv, 0, 16)
        System.arraycopy(cipherText, 0, combined, 32, cipherText.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        val plainBytes = cipher.doFinal(cipherText)

        return String(plainBytes, Charsets.UTF_8)
    }
}
