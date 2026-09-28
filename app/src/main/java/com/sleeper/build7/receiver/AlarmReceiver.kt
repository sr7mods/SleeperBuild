package com.sleeper.build7.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sleeper.build7.MainActivity
import com.sleeper.build7.R
import com.sleeper.build7.audio.SleeperAudioEngine

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "SleeperBuild Reminder"
        val message = intent.getStringExtra("message") ?: "Time for your scheduled discipline activity!"
        val audioMode = intent.getStringExtra("audio_mode") ?: "tts"
        val voiceProfile = intent.getStringExtra("voice_profile") ?: "girl"
        val customText = intent.getStringExtra("custom_tts_text") ?: message
        val targetScreen = intent.getStringExtra("target_screen") ?: "hub"
        val channelId = "sleeper_build_channel"

        // Play custom Audio Alarm (TTS with voice profile, System Ringtone, or Both)
        SleeperAudioEngine.playAlarmOrTts(
            context = context,
            mode = audioMode,
            customText = customText,
            voiceProfile = voiceProfile
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SleeperBuild Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Prayer, Jam'ah, Workout, and Schedule reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", targetScreen)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(tapPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        try {
            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
