package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.entity.CxTask
import com.example.data.entity.TaskPriority

object TatNotificationManager {

    const val CHANNEL_TAT_NEAR_BREACH = "cx_tat_near_breach"
    const val CHANNEL_TAT_BREACH = "cx_tat_breached"
    const val CHANNEL_TAT_GENERAL = "cx_general"
    const val CHANNEL_USER_ONBOARDING = "cx_user_onboarding"

    // Set of notified task IDs with their alert state to avoid spamming
    private val notifiedNearBreachTasks = mutableSetOf<Long>()
    private val notifiedBreachedTasks = mutableSetOf<Long>()

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. Channel for TAT Breached (Critical Red)
            val breachChannel = NotificationChannel(
                CHANNEL_TAT_BREACH,
                "🚨 CX SLA Breach Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts when customer experience SLA is breached"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 400)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 2. Channel for TAT Near Breach (Warning Amber)
            val nearBreachChannel = NotificationChannel(
                CHANNEL_TAT_NEAR_BREACH,
                "⚠️ CX SLA Near Breach Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Proactive warning alerts when customer tasks are approaching SLA deadline"
                enableLights(true)
                lightColor = Color.rgb(217, 119, 6) // Amber
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 150, 200)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 3. Channel for General CX Alerts
            val generalChannel = NotificationChannel(
                CHANNEL_TAT_GENERAL,
                "CX Tracker Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General task updates and department summaries"
            }

            // 4. Channel for Staff Onboarding & Email Triggers
            val onboardingChannel = NotificationChannel(
                CHANNEL_USER_ONBOARDING,
                "📧 Team Onboarding & Email Triggers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new team members joining CX units with email alerts to Ali Hassan & Unit Heads"
                enableLights(true)
                lightColor = Color.rgb(0, 130, 105)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(breachChannel, nearBreachChannel, generalChannel, onboardingChannel)
            )
        }
    }

    /**
     * Post a notification when a task has officially breached its SLA
     */
    fun sendTatBreachedNotification(context: Context, task: CxTask, unitName: String) {
        if (notifiedBreachedTasks.contains(task.id)) return
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_NAV_TAB", 1) // Tasks tab
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt() * 10 + 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val priorityText = when (task.priority) {
            TaskPriority.CRITICAL -> "🚨 CRITICAL"
            TaskPriority.HIGH -> "⚡ HIGH"
            TaskPriority.MEDIUM -> "🔹 MEDIUM"
            TaskPriority.LOW -> "▫️ LOW"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TAT_BREACH)
            .setSmallIcon(R.drawable.ic_cx_experience_logo)
            .setContentTitle("🚨 SLA BREACH: ${task.trackingNumber}")
            .setContentText("Task '${task.title}' has exceeded ${task.tatHours}h TAT ($unitName)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🚨 TAT SLA BREACH ALERT\n\n" +
                            "• Ticket: ${task.trackingNumber}\n" +
                            "• Title: ${task.title}\n" +
                            "• CX Unit: $unitName\n" +
                            "• Priority: $priorityText\n" +
                            "• SLA Limit: ${task.tatHours} Hours\n" +
                            (if (!task.customerAccountOrTicket.isNullOrBlank()) "• Customer Ref: ${task.customerAccountOrTicket}\n" else "") +
                            "Action Required: Please escalate and resolve immediately to mitigate CX penalty.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(0xFFDC2626.toInt()) // Red
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(task.id.toInt() * 10 + 2, notification)
            notifiedBreachedTasks.add(task.id)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    /**
     * Post a notification when a task is near to breach (<25% SLA remaining or <1 hour)
     */
    fun sendTatNearBreachNotification(
        context: Context,
        task: CxTask,
        unitName: String,
        remainingMinutes: Long
    ) {
        if (notifiedNearBreachTasks.contains(task.id) || notifiedBreachedTasks.contains(task.id)) return
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_NAV_TAB", 1) // Tasks tab
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt() * 10 + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val timeLeftText = if (remainingMinutes < 60) {
            "$remainingMinutes min"
        } else {
            val hours = remainingMinutes / 60
            val mins = remainingMinutes % 60
            "${hours}h ${mins}m"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TAT_NEAR_BREACH)
            .setSmallIcon(R.drawable.ic_cx_experience_logo)
            .setContentTitle("⚠️ TAT Alert: ${task.trackingNumber} near breach")
            .setContentText("$timeLeftText remaining for '${task.title}' ($unitName)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("⚠️ SLA NEAR BREACH WARNING\n\n" +
                            "• Ticket: ${task.trackingNumber}\n" +
                            "• Title: ${task.title}\n" +
                            "• Unit: $unitName\n" +
                            "• Time Remaining: $timeLeftText\n" +
                            "• Total SLA: ${task.tatHours} Hours\n" +
                            "Action Required: Complete resolution or update customer before SLA expires.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFD97706.toInt()) // Amber
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(task.id.toInt() * 10 + 1, notification)
            notifiedNearBreachTasks.add(task.id)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    /**
     * Trigger a demonstration test notification for Near-Breach or Breached state
     */
    fun sendTestNotification(context: Context, isBreach: Boolean) {
        if (!hasNotificationPermission(context)) return

        val notificationId = if (isBreach) 999991 else 999992
        val channelId = if (isBreach) CHANNEL_TAT_BREACH else CHANNEL_TAT_NEAR_BREACH

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (isBreach) {
            "🚨 TEST: TAT SLA Breached - CX-CMU-902"
        } else {
            "⚠️ TEST: TAT Near Breach (28m left) - CX-DCX-108"
        }

        val body = if (isBreach) {
            "Task 'ATM Dispense Failure' has breached its 24h SLA in Complaints Management Unit. Immediate action required."
        } else {
            "Task 'Instant Fund Transfer Reversal' has only 28 minutes remaining before SLA breach in Digital CX Unit."
        }

        val color = if (isBreach) 0xFFDC2626.toInt() else 0xFFD97706.toInt()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_cx_experience_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n\n$body\n\n[CX SLA Monitor Active]"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(color)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    /**
     * Trigger system notification when a new user registers, notifying Super Admin Ali Hassan and the Unit Head
     */
    fun sendUserOnboardedNotification(
        context: Context,
        userName: String,
        userEmail: String,
        userRole: String,
        unitName: String,
        unitHeadName: String
    ) {
        if (!hasNotificationPermission(context)) return

        val notificationId = (System.currentTimeMillis() % 100000).toInt() + 1000

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", 4) // Units & Staff tab
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = "📧 Staff Joined: $userName joined $unitName"
        val summary = "Email trigger sent to Ali Hassan & $unitHeadName for new $userRole ($userEmail)."

        val notification = NotificationCompat.Builder(context, CHANNEL_USER_ONBOARDING)
            .setSmallIcon(R.drawable.ic_cx_experience_logo)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("📧 TEAM ONBOARDING EMAIL TRIGGER\n\n" +
                            "• New Member: $userName ($userEmail)\n" +
                            "• Unit: $unitName\n" +
                            "• Role: $userRole\n" +
                            "• Dispatched To: Ali Hassan (malikalihassanarain397@gmail.com) & $unitHeadName\n\n" +
                            "User account is now active with role permissions configured.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF008269.toInt()) // Hbl Primary Green
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    /**
     * Clear notification and tracking when a task is completed or deleted
     */
    fun clearTaskAlert(context: Context, taskId: Long) {
        notifiedNearBreachTasks.remove(taskId)
        notifiedBreachedTasks.remove(taskId)
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(taskId.toInt() * 10 + 1)
            notificationManager.cancel(taskId.toInt() * 10 + 2)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun resetNotificationHistory() {
        notifiedNearBreachTasks.clear()
        notifiedBreachedTasks.clear()
    }

    fun cancelAllAlerts(context: Context) {
        resetNotificationHistory()
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
