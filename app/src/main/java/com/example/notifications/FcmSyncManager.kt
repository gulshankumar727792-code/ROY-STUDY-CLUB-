package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.data.RoyStudyRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object FcmSyncManager {
    private const val TAG = "FcmSyncManager"

    suspend fun syncTokenAndTopics(
        context: Context,
        repository: RoyStudyRepository,
        userId: String,
        dailyTestAlertsEnabled: Boolean,
        weeklyChampionAlertsEnabled: Boolean
    ): Result<String> {
        RoyStudyNotificationHelper.ensureChannels(context)
        return try {
            val messaging = FirebaseMessaging.getInstance()
            val token = messaging.token.await()

            if (dailyTestAlertsEnabled) {
                messaging.subscribeToTopic(RoyStudyNotificationHelper.TOPIC_DAILY_TESTS).await()
            } else {
                messaging.unsubscribeFromTopic(RoyStudyNotificationHelper.TOPIC_DAILY_TESTS).await()
            }

            if (weeklyChampionAlertsEnabled) {
                messaging.subscribeToTopic(RoyStudyNotificationHelper.TOPIC_WEEKLY_CHAMPIONS).await()
            } else {
                messaging.unsubscribeFromTopic(RoyStudyNotificationHelper.TOPIC_WEEKLY_CHAMPIONS).await()
            }

            repository.upsertFcmToken(
                userId = userId,
                token = token,
                dailyTestAlertsEnabled = dailyTestAlertsEnabled,
                weeklyChampionAlertsEnabled = weeklyChampionAlertsEnabled
            ).getOrThrow()

            Result.success(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "FCM sync failed (may be offline or missing Play Services)", e)
            Result.failure(e)
        }
    }

    suspend fun clearOnSignOut(
        repository: RoyStudyRepository,
        userId: String
    ) {
        try {
            val messaging = FirebaseMessaging.getInstance()
            runCatching { messaging.unsubscribeFromTopic(RoyStudyNotificationHelper.TOPIC_DAILY_TESTS).await() }
            runCatching { messaging.unsubscribeFromTopic(RoyStudyNotificationHelper.TOPIC_WEEKLY_CHAMPIONS).await() }
            repository.deleteFcmToken(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear FCM token during sign-out", e)
        }
    }
}
