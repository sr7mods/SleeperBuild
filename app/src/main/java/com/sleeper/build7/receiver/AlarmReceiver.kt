package com.sleeper.build7.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sleeper.build7.R
import com.sleeper.build7.audio.SleeperAudioEngine

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "SleeperBuild Reminder"
        val message = intent.getStringExtra("message") ?: "Time for your scheduled discipline activity!"
        val audioMode = intent.getStringExtra("audio_mode") ?: "tts"
        val voiceProfile = intent.getStringExtra("voice_profile") ?: "girl"
        val customText = intent.getStringExtra("custom_tts_text") ?: message
        val channelId = "sleeper_build_channel"

        // Play custom Audio Alarm (TTS with voice profile or System Ringtone)
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
                description = "Channel for Prayer, Workout, and Schedule reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
