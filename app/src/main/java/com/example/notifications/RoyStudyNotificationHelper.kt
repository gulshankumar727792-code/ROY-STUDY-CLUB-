package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

data class PushNavigationAction(
    val navTarget: String,
    val testId: String? = null,
    val weekKey: String? = null
)

object RoyStudyNotificationHelper {
    const val TOPIC_DAILY_TESTS = "daily_tests_class10"
    const val TOPIC_WEEKLY_CHAMPIONS = "weekly_champions_class10"

    const val CHANNEL_DAILY_TESTS = "daily_tests_channel"
    const val CHANNEL_WEEKLY_CHAMPION = "weekly_champion_channel"

    const val TYPE_DAILY_TEST_PUBLISHED = "DAILY_TEST_PUBLISHED"
    const val TYPE_WEEKLY_CHAMPION_WINNER = "WEEKLY_CHAMPION_WINNER"
    const val TYPE_WEEKLY_CHAMPION_ANNOUNCED = "WEEKLY_CHAMPION_ANNOUNCED"

    const val EXTRA_NAV_TARGET = "navTarget"
    const val EXTRA_TEST_ID = "testId"
    const val EXTRA_WEEK_KEY = "weekKey"
    const val EXTRA_TYPE = "type"

    const val NAV_DAILY_TESTS = "daily_tests"
    const val NAV_WEEKLY_CHAMPION = "weekly_champion"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val dailyChannel = NotificationChannel(
            CHANNEL_DAILY_TESTS,
            context.getString(R.string.fcm_channel_daily_tests_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.fcm_channel_daily_tests_desc)
            enableVibration(true)
        }

        val championChannel = NotificationChannel(
            CHANNEL_WEEKLY_CHAMPION,
            context.getString(R.string.fcm_channel_weekly_champion_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.fcm_channel_weekly_champion_desc)
            enableVibration(true)
        }

        manager.createNotificationChannel(dailyChannel)
        manager.createNotificationChannel(championChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun resolveChannelId(type: String?, explicitChannelId: String?): String {
        if (explicitChannelId == CHANNEL_WEEKLY_CHAMPION || explicitChannelId == CHANNEL_DAILY_TESTS) {
            return explicitChannelId
        }
        return when (type) {
            TYPE_WEEKLY_CHAMPION_WINNER, TYPE_WEEKLY_CHAMPION_ANNOUNCED -> CHANNEL_WEEKLY_CHAMPION
            else -> CHANNEL_DAILY_TESTS
        }
    }

    fun resolveNavTarget(type: String?, explicitNavTarget: String?): String {
        if (explicitNavTarget == NAV_WEEKLY_CHAMPION || explicitNavTarget == NAV_DAILY_TESTS) {
            return explicitNavTarget
        }
        return when (type) {
            TYPE_WEEKLY_CHAMPION_WINNER, TYPE_WEEKLY_CHAMPION_ANNOUNCED -> NAV_WEEKLY_CHAMPION
            else -> NAV_DAILY_TESTS
        }
    }

    fun extractPushNavigationAction(intent: Intent?): PushNavigationAction? {
        val extras = intent?.extras ?: return null
        val explicitTarget = extras.getString(EXTRA_NAV_TARGET)
        val type = extras.getString(EXTRA_TYPE)
        if (explicitTarget.isNullOrBlank() && type.isNullOrBlank()) {
            return null
        }
        val target = resolveNavTarget(type, explicitTarget)
        val testId = extras.getString(EXTRA_TEST_ID)
        val weekKey = extras.getString(EXTRA_WEEK_KEY)
        return PushNavigationAction(
            navTarget = target,
            testId = testId,
            weekKey = weekKey
        )
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String,
        navTarget: String,
        testId: String? = null,
        weekKey: String? = null
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV_TARGET, navTarget)
            if (!testId.isNullOrBlank()) putExtra(EXTRA_TEST_ID, testId)
            if (!weekKey.isNullOrBlank()) putExtra(EXTRA_WEEK_KEY, weekKey)
        }

        val notificationId = ((navTarget + (testId ?: weekKey ?: title)).hashCode() and 0x7FFFFFFF)
            .coerceAtLeast(1000)

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_roy_study)
            .setColor(ContextCompat.getColor(context, R.color.roy_notification_blue))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify
        }
    }
}
