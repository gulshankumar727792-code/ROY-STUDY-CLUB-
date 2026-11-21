package com.example.notifications

import android.util.Log
import com.example.data.RoyStudyRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RoyStudyMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = Firebase.auth.currentUser?.uid ?: return
        serviceScope.launch {
            try {
                val repo = RoyStudyRepository(applicationContext)
                val existing = repo.getFcmTokenDoc(uid).getOrNull()
                repo.upsertFcmToken(
                    userId = uid,
                    token = token,
                    dailyTestAlertsEnabled = existing?.dailyTestAlertsEnabled ?: true,
                    weeklyChampionAlertsEnabled = existing?.weeklyChampionAlertsEnabled ?: true
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist refreshed FCM token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data[RoyStudyNotificationHelper.EXTRA_TYPE]
        val channelId = RoyStudyNotificationHelper.resolveChannelId(
            type = type,
            explicitChannelId = data["channelId"]
        )
        val navTarget = RoyStudyNotificationHelper.resolveNavTarget(
            type = type,
            explicitNavTarget = data[RoyStudyNotificationHelper.EXTRA_NAV_TARGET]
        )

        val defaultTitle = if (channelId == RoyStudyNotificationHelper.CHANNEL_WEEKLY_CHAMPION) {
            "साप्ताहिक चैंपियन अपडेट 🏆"
        } else {
            "नया दैनिक टेस्ट उपलब्ध है!"
        }
        val title = message.notification?.title?.takeIf { it.isNotBlank() }
            ?: data["title"]?.takeIf { it.isNotBlank() }
            ?: defaultTitle
        val body = message.notification?.body?.takeIf { it.isNotBlank() }
            ?: data["body"]?.takeIf { it.isNotBlank() }
            ?: "Roy Study Club खोलें और तुरंत देखें।"

        RoyStudyNotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = channelId,
            navTarget = navTarget,
            testId = data[RoyStudyNotificationHelper.EXTRA_TEST_ID],
            weekKey = data[RoyStudyNotificationHelper.EXTRA_WEEK_KEY]
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "RoyStudyFCM"
    }
}
