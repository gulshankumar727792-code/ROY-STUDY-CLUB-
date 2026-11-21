package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppConstants
import com.example.data.DateUtils
import com.example.data.FcmDeviceToken
import com.example.data.StudentProfile
import com.example.data.TestAttempt
import com.example.data.UiState
import com.example.ui.components.BoardHeaderBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrownGold
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.OnCrownGoldContainer
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun ProfileAndHistoryScreen(
    currentUserId: String,
    profileState: UiState<StudentProfile?>,
    attemptsState: UiState<List<TestAttempt>>,
    fcmTokenState: UiState<FcmDeviceToken?>,
    hasNotificationPermission: Boolean,
    isAdmin: Boolean,
    busy: Boolean,
    onSaveProfile: (fullName: String, phone: String, district: String, schoolName: String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onUpdateNotificationPreferences: (dailyEnabled: Boolean, championEnabled: Boolean) -> Unit,
    onReviewAttempt: (TestAttempt) -> Unit,
    onOpenReferAndEarn: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    val fcmToken = (fcmTokenState as? UiState.Success)?.data
    val dailyAlertsEnabled = fcmToken?.dailyTestAlertsEnabled ?: true
    val championAlertsEnabled = fcmToken?.weeklyChampionAlertsEnabled ?: true
    var fullName by remember(profile?.fullName) { mutableStateOf(profile?.fullName ?: "") }
    var phone by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }
    var district by remember(profile?.district) { mutableStateOf(profile?.district ?: "") }
    var schoolName by remember(profile?.schoolName) { mutableStateOf(profile?.schoolName ?: "") }

    val attempts = (attemptsState as? UiState.Success)?.data.orEmpty()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("profile_history_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BoardHeaderBanner(
                    title = profile?.fullName?.ifBlank { "Student Profile" } ?: "Student Profile",
                    subtitle = "${AppConstants.BOARD_CLASS_FIXED}\nUID: $currentUserId",
                    badge = if (profile?.hasValidVipNow() == true) "VIP Active (${profile.remainingVipDays()}d)" else "Standard Student"
                )
            }

            item {
                OutlinedButton(
                    onClick = onOpenReferAndEarn,
                    border = BorderStroke(1.5.dp, CrownGold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_open_refer_button")
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = OnCrownGoldContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (!profile?.referralCode.isNullOrBlank()) {
                            "Refer & Earn • My Code: ${profile?.referralCode}"
                        } else {
                            "Refer & Earn • Invite Friends for Free VIP"
                        }
                    )
                }
            }

            if (isAdmin) {
                item {
                    Button(
                        onClick = onOpenAdminPanel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrownGold,
                            contentColor = OnCrownGoldContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_open_admin_button")
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Owner Admin Panel")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("छात्र विवरण / Edit Student Profile", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "आपका फ़ोन नंबर और ईमेल गोपनीय (Private) रहता है और लीडरबोर्ड पर नहीं दिखाया जाता।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name / पूरा नाम") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile / WhatsApp Number (Private)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District / जिला (Bihar)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = schoolName,
                            onValueChange = { schoolName = it },
                            label = { Text("School Name / विद्यालय का नाम") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { onSaveProfile(fullName, phone, district, schoolName) },
                            enabled = !busy && fullName.trim().isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_profile_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (busy) "Saving..." else "Save Profile")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fcm_notification_settings_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = stringResource(R.string.notifications_settings_title),
                                    tint = RoyalBluePrimary
                                )
                                Text(
                                    text = stringResource(R.string.notifications_settings_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            StatusPill(
                                text = if (hasNotificationPermission) "Active" else "Permission Needed",
                                containerColor = if (hasNotificationPermission) SuccessGreenContainer else WarningAmberContainer,
                                contentColor = if (hasNotificationPermission) SuccessGreen else WarningAmber
                            )
                        }

                        Text(
                            text = "नया Daily Test पब्लिश होने और Weekly Champion घोषित होने पर तुरंत अलर्ट पाएं।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!hasNotificationPermission) {
                            Button(
                                onClick = onRequestNotificationPermission,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("enable_notifications_button")
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.notifications_enable_cta))
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Daily Test Alerts / डेली टेस्ट अलर्ट", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Instant notification when Owner Admin publishes a new Class 10 Daily Test",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = dailyAlertsEnabled,
                                enabled = !busy,
                                onCheckedChange = { checked ->
                                    onUpdateNotificationPreferences(checked, championAlertsEnabled)
                                },
                                modifier = Modifier.testTag("toggle_daily_test_alerts")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Weekly Champion Alerts / साप्ताहिक चैंपियन", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Direct congratulatory alert if you win + weekly winner announcements",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = championAlertsEnabled,
                                enabled = !busy,
                                onCheckedChange = { checked ->
                                    onUpdateNotificationPreferences(dailyAlertsEnabled, checked)
                                },
                                modifier = Modifier.testTag("toggle_weekly_champion_alerts")
                            )
                        }

                        val isTokenSynced = !fcmToken?.token.isNullOrBlank()
                        Text(
                            text = if (isTokenSynced) {
                                "FCM Device Status: Connected • Topics synced"
                            } else {
                                "FCM Device Status: Syncing device token..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTokenSynced) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "पिछले टेस्ट का इतिहास / Test History",
                    subtitle = "Tap any attempt to review correct answers, wrong answers & explanations"
                )
            }

            when (attemptsState) {
                is UiState.Loading -> item { LoadingCard() }
                is UiState.Error -> item { ErrorStateCard(attemptsState.message) }
                is UiState.Success -> {
                    if (attempts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.History,
                                title = "अभी कोई टेस्ट नहीं दिया गया है",
                                subtitle = "Your completed Daily Tests and VIP Tests will appear here."
                            )
                        }
                    } else {
                        items(attempts, key = { it.id }) { attempt ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onReviewAttempt(attempt) }
                                    .testTag("attempt_history_card_${attempt.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = attempt.testTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatusPill(
                                            text = "${attempt.score}/${attempt.totalQuestions} (${attempt.accuracyPercent()}%)",
                                            containerColor = SuccessGreenContainer,
                                            contentColor = SuccessGreen
                                        )
                                    }
                                    Text(
                                        text = "${attempt.subjectName} • ${attempt.chapterName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Correct: ${attempt.correctCount} • Wrong: ${attempt.wrongCount} • Time: ${DateUtils.formatDurationSeconds(attempt.timeTakenSeconds)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCrimson),
                    border = BorderStroke(1.dp, ErrorCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
