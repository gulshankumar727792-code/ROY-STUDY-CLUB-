package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DateUtils
import com.example.data.StudentProfile
import com.example.data.StudyTest
import com.example.data.TestAttempt
import com.example.data.TestAvailabilityStatus
import com.example.data.TestCategory
import com.example.data.UiState
import com.example.data.WeeklyChampion
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.MetricTile
import com.example.ui.components.RoyalBlueHeroBrush
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.VipCrownBadge
import com.example.ui.theme.CrownGold
import com.example.ui.theme.CrownGoldContainer
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorCrimsonContainer
import com.example.ui.theme.OnCrownGoldContainer
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun StudentDashboardScreen(
    profileState: UiState<StudentProfile?>,
    testsState: UiState<List<StudyTest>>,
    attemptsState: UiState<List<TestAttempt>>,
    championsState: UiState<List<WeeklyChampion>>,
    isAdmin: Boolean,
    referralRewardDays: Long = 7L,
    onOpenDailyTests: () -> Unit,
    onOpenChapterTests: () -> Unit,
    onOpenWeeklyMegaTests: () -> Unit,
    onOpenWeekly: () -> Unit,
    onOpenVip: () -> Unit,
    onOpenReferAndEarn: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onStartOrReviewTest: (StudyTest, TestAttempt?) -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    val allTests = (testsState as? UiState.Success)?.data.orEmpty()
    val dailyTests = allTests.filter { it.categoryEnum() == TestCategory.DAILY }
    val chapterTests = allTests.filter { it.categoryEnum() == TestCategory.CHAPTER_TEST }
    val weeklyMegaTests = allTests.filter { it.categoryEnum() == TestCategory.WEEKLY_MEGA }
    val attempts = (attemptsState as? UiState.Success)?.data.orEmpty()
    val attemptsByTestId = attempts.associateBy { it.testId }
    val champions = (championsState as? UiState.Success)?.data.orEmpty()
    val latestChampion = champions.firstOrNull()
    val hasVip = profile?.hasValidVipNow() == true

    val totalAttempted = attempts.size
    val avgAccuracy = if (attempts.isEmpty()) 0 else attempts.map { it.accuracyPercent() }.average().toInt()
    val totalScore = attempts.sumOf { it.score }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("student_dashboard_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(RoyalBlueHeroBrush)
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.roy_study_club_logo),
                                    contentDescription = "Roy Study Club",
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "नमस्ते, ${profile?.fullName?.ifBlank { "Student" } ?: "Student"}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Class 10 • Bihar Board (BSEB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                            if (hasVip) {
                                VipCrownBadge(label = "VIP ${profile?.remainingVipDays()}d")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenDailyTests,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = RoyalBlueDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_daily_test_cta")
                            ) {
                                Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("दैनिक टेस्ट", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenVip,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrownGold,
                                    contentColor = OnCrownGoldContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_vip_cta")
                            ) {
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (hasVip) "VIP ज़ोन" else "VIP प्लान", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Owner Admin Panel Banner (Visible ONLY to Verified Owner Admin)
            if (isAdmin) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAdminPanel() }
                            .testTag("open_admin_panel_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF062A78)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CrownGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AdminPanelSettings,
                                    contentDescription = "Owner Admin Panel",
                                    tint = OnCrownGoldContainer
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Owner Admin Panel",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Add tests & questions, approve WhatsApp VIP payments, declare Weekly Champions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            StatusPill(
                                text = "OPEN",
                                containerColor = CrownGold,
                                contentColor = OnCrownGoldContainer
                            )
                        }
                    }
                }
            }

            // Student Performance Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Tests Attempted",
                        value = "$totalAttempted",
                        accentColor = RoyalBluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Avg Accuracy",
                        value = "$avgAccuracy%",
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Total Marks",
                        value = "$totalScore",
                        accentColor = Color(0xFFB37200),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Dedicated Student Sections: Chapter-wise Tests & Weekly Mega Test
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenChapterTests() }
                            .testTag("dashboard_chapter_tests_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, RoyalBluePrimary.copy(alpha = 0.45f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = "Chapter-wise Tests",
                                        tint = RoyalBluePrimary
                                    )
                                }
                                StatusPill(
                                    text = "${chapterTests.size} Live",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = RoyalBlueDark
                                )
                            }
                            Text(
                                text = "Chapter-wise Tests",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlueDark
                            )
                            Text(
                                text = "अध्याय टेस्ट • Up to 50 MCQs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenWeeklyMegaTests() }
                            .testTag("dashboard_weekly_mega_card"),
                        colors = CardDefaults.cardColors(containerColor = CrownGoldContainer.copy(alpha = 0.55f)),
                        border = BorderStroke(1.5.dp, CrownGold),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CrownGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WorkspacePremium,
                                        contentDescription = "Weekly Mega Test",
                                        tint = OnCrownGoldContainer
                                    )
                                }
                                StatusPill(
                                    text = "${weeklyMegaTests.size} Live",
                                    containerColor = CrownGold,
                                    contentColor = OnCrownGoldContainer
                                )
                            }
                            Text(
                                text = "Weekly Mega Test",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnCrownGoldContainer
                            )
                            Text(
                                text = "मेगा टेस्ट • Up to 200 MCQs",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnCrownGoldContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Highlight Live Weekly Mega Tests directly on Dashboard if published
            if (weeklyMegaTests.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "साप्ताहिक मेगा टेस्ट / Weekly Mega Test",
                        subtitle = "Up to 200 MCQs • Timed Exam",
                        trailing = {
                            TextButton(onClick = onOpenWeeklyMegaTests) {
                                Text("View All")
                            }
                        }
                    )
                }
                items(weeklyMegaTests.take(2), key = { "dash_mega_${it.id}" }) { mega ->
                    val attempt = attemptsByTestId[mega.id]
                    TestListCard(
                        test = mega,
                        attempt = attempt,
                        locked = false,
                        onActionClick = { onStartOrReviewTest(mega, attempt) }
                    )
                }
            }

            // Refer & Earn Promo Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenReferAndEarn() }
                        .testTag("dashboard_refer_and_earn_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, CrownGold),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CrownGoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = "Refer and Earn",
                                tint = OnCrownGoldContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Refer & Earn • मुफ्त VIP दिन पाएं",
                                style = MaterialTheme.typography.titleSmall,
                                color = RoyalBlueDark
                            )
                            Text(
                                text = "Invite friends — both get +$referralRewardDays VIP days after their 1st Daily Test!" +
                                    if (!profile?.referralCode.isNullOrBlank()) " Code: ${profile?.referralCode}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            text = "+${referralRewardDays}d VIP",
                            containerColor = CrownGold,
                            contentColor = OnCrownGoldContainer
                        )
                    }
                }
            }

            // Latest Weekly Champion Spotlight
            if (latestChampion != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenWeekly() },
                        colors = CardDefaults.cardColors(containerColor = CrownGoldContainer),
                        border = BorderStroke(1.dp, CrownGold),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Weekly Champion",
                                tint = OnCrownGoldContainer,
                                modifier = Modifier.size(36.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "साप्ताहिक चैंपियन • ${latestChampion.weekLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnCrownGoldContainer
                                )
                                Text(
                                    text = latestChampion.winnerName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OnCrownGoldContainer
                                )
                                Text(
                                    text = "${latestChampion.totalMarks} Marks • ${latestChampion.testsCount} Tests • ${DateUtils.formatDurationSeconds(latestChampion.totalTimeSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnCrownGoldContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // Published Daily Tests Section
            item {
                SectionHeader(
                    title = "दैनिक टेस्ट / Daily Tests",
                    subtitle = "Class 10 Bihar Board • Attempt once for Weekly Champion",
                    trailing = {
                        TextButton(onClick = onOpenDailyTests) {
                            Text("View All")
                        }
                    }
                )
            }

            when (testsState) {
                is UiState.Loading -> item { LoadingCard() }
                is UiState.Error -> item { ErrorStateCard(testsState.message) }
                is UiState.Success -> {
                    if (dailyTests.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Quiz,
                                title = "अभी कोई दैनिक टेस्ट प्रकाशित नहीं है",
                                subtitle = "No published Daily Test yet. Owner Admin adds fresh Class 10 Bihar Board tests manually."
                            )
                        }
                    } else {
                        items(dailyTests.take(5), key = { it.id }) { test ->
                            val attempt = attemptsByTestId[test.id]
                            TestListCard(
                                test = test,
                                attempt = attempt,
                                locked = false,
                                onActionClick = { onStartOrReviewTest(test, attempt) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestListCard(
    test: StudyTest,
    attempt: TestAttempt?,
    locked: Boolean,
    onActionClick: () -> Unit
) {
    val availability = test.availabilityStatus()
    val hasScheduleWindow = test.availableFrom.isNotBlank() || test.closingDate.isNotBlank()
    val isMega = test.categoryEnum() == TestCategory.WEEKLY_MEGA

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("test_card_${test.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isMega) 1.5.dp else 1.dp,
            color = if (isMega) CrownGold else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    text = "${test.subjectName} • ${test.categoryEnum().label}",
                    containerColor = if (isMega) CrownGoldContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isMega) OnCrownGoldContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (attempt != null) {
                    StatusPill(
                        text = "${attempt.score}/${attempt.effectiveTotalMarks()} (${attempt.percentageFormatted()})",
                        containerColor = SuccessGreenContainer,
                        contentColor = SuccessGreen,
                        icon = Icons.Filled.CheckCircle
                    )
                } else if (availability == TestAvailabilityStatus.UPCOMING) {
                    StatusPill(
                        text = "Upcoming",
                        containerColor = WarningAmberContainer,
                        contentColor = WarningAmber,
                        icon = Icons.Filled.Schedule
                    )
                } else if (availability == TestAvailabilityStatus.CLOSED) {
                    StatusPill(
                        text = "Closed",
                        containerColor = ErrorCrimsonContainer,
                        contentColor = ErrorCrimson
                    )
                } else {
                    Text(
                        text = test.testDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = test.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (test.chapterName.isNotBlank()) {
                Text(
                    text = "अध्याय / Chapter: ${test.chapterName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasScheduleWindow) {
                val windowText = buildString {
                    if (test.availableFrom.isNotBlank()) append("Opens: ${test.availableFrom}")
                    if (test.availableFrom.isNotBlank() && test.closingDate.isNotBlank()) append("  •  ")
                    if (test.closingDate.isNotBlank()) append("Closes: ${test.closingDate}")
                }
                Text(
                    text = windowText,
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalBlueDark
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val metaText = buildString {
                    append("${test.questionCount} Qs • ${test.totalMarks()} Marks • ${test.durationMinutes} min")
                    if (test.passingPercentage > 0L) {
                        append(" • Pass ${test.passingPercentage}%")
                    }
                }
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))

                if (attempt != null) {
                    OutlinedButton(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("review_test_btn_${test.id}")
                    ) {
                        Text("View Result")
                    }
                } else {
                    val isBlockedByDate = availability != TestAvailabilityStatus.OPEN
                    Button(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                locked -> CrownGold
                                isBlockedByDate -> MaterialTheme.colorScheme.surfaceVariant
                                else -> RoyalBluePrimary
                            },
                            contentColor = when {
                                locked -> OnCrownGoldContainer
                                isBlockedByDate -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> Color.White
                            }
                        ),
                        modifier = Modifier.testTag("start_test_btn_${test.id}")
                    ) {
                        Icon(
                            imageVector = when {
                                locked -> Icons.Filled.WorkspacePremium
                                isBlockedByDate -> Icons.Filled.Schedule
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when {
                                locked -> "Unlock VIP"
                                availability == TestAvailabilityStatus.UPCOMING -> "Upcoming"
                                availability == TestAvailabilityStatus.CLOSED -> "Closed"
                                else -> "Start Test"
                            }
                        )
                    }
                }
            }
        }
    }
}
