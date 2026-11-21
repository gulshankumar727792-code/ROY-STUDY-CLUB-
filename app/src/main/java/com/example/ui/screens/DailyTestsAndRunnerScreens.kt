package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.StudyTest
import com.example.data.Subject
import com.example.data.TestAttempt
import com.example.data.TestCategory
import com.example.data.UiState
import com.example.ui.components.BoardHeaderBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrownGold
import com.example.ui.theme.CrownGoldContainer
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorCrimsonContainer
import com.example.ui.theme.OnCrownGoldContainer
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.viewmodel.ActiveTestSession

enum class StudentTestSection(
    val label: String,
    val labelHindi: String,
    val category: TestCategory,
    val tag: String
) {
    DAILY("Daily Tests", "दैनिक टेस्ट", TestCategory.DAILY, "section_tab_daily"),
    CHAPTER_WISE("Chapter-wise", "अध्याय टेस्ट", TestCategory.CHAPTER_TEST, "section_tab_chapter"),
    WEEKLY_MEGA("Weekly Mega", "मेगा टेस्ट", TestCategory.WEEKLY_MEGA, "section_tab_mega")
}

private enum class NavigatorFilter(val label: String) {
    ALL("All"),
    UNANSWERED("Unanswered"),
    ANSWERED("Answered")
}

@Composable
fun DailyTestsScreen(
    selectedSection: StudentTestSection,
    onSelectSection: (StudentTestSection) -> Unit,
    subjectsState: UiState<List<Subject>>,
    testsState: UiState<List<StudyTest>>,
    attemptsState: UiState<List<TestAttempt>>,
    onStartOrReviewTest: (StudyTest, TestAttempt?) -> Unit
) {
    var selectedSubject by remember(selectedSection) { mutableStateOf<String?>(null) }
    val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()
    val allPublished = (testsState as? UiState.Success)?.data.orEmpty()
    val attemptsMap = ((attemptsState as? UiState.Success)?.data.orEmpty()).associateBy { it.testId }

    val dailyCount = allPublished.count { it.categoryEnum() == TestCategory.DAILY }
    val chapterCount = allPublished.count { it.categoryEnum() == TestCategory.CHAPTER_TEST }
    val megaCount = allPublished.count { it.categoryEnum() == TestCategory.WEEKLY_MEGA }

    val sectionTests = allPublished.filter { it.categoryEnum() == selectedSection.category }
    val filtered = if (selectedSubject == null) {
        sectionTests
    } else {
        sectionTests.filter { it.subjectName.equals(selectedSubject, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("daily_tests_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Adaptive Section Hero Banner
            item {
                when (selectedSection) {
                    StudentTestSection.DAILY -> BoardHeaderBanner(
                        title = "दैनिक टेस्ट / Daily Tests",
                        subtitle = "हर दिन नया Class 10 Bihar Board टेस्ट। प्रत्येक टेस्ट केवल एक बार दिया जा सकता है और साप्ताहिक चैंपियन में जुड़ता है।",
                        badge = "One Attempt Per Test"
                    )
                    StudentTestSection.CHAPTER_WISE -> BoardHeaderBanner(
                        title = "अध्याय-वार टेस्ट / Chapter-wise Tests",
                        subtitle = "बिहार बोर्ड कक्षा 10 के प्रत्येक विषय व अध्याय पर आधारित वस्तुनिष्ठ प्रश्न (50 MCQ तक)।",
                        badge = "Chapter Practice • Live Sync"
                    )
                    StudentTestSection.WEEKLY_MEGA -> BoardHeaderBanner(
                        title = "साप्ताहिक मेगा टेस्ट / Weekly Mega Test",
                        subtitle = "200 MCQ तक का पूर्ण साप्ताहिक मेगा टेस्ट। समय-सीमा के अंदर उत्तर दें, प्रश्न 1 से 200 तक आसानी से नेविगेट करें।",
                        badge = "Up to 200 MCQs • Timed Exam"
                    )
                }
            }

            // 3-Section Switcher: Daily Tests | Chapter-wise Tests | Weekly Mega Test
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudentTestSection.entries.forEach { section ->
                        val isSelected = selectedSection == section
                        val count = when (section) {
                            StudentTestSection.DAILY -> dailyCount
                            StudentTestSection.CHAPTER_WISE -> chapterCount
                            StudentTestSection.WEEKLY_MEGA -> megaCount
                        }
                        val bg = when {
                            isSelected && section == StudentTestSection.WEEKLY_MEGA -> CrownGold
                            isSelected -> RoyalBluePrimary
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val fg = when {
                            isSelected && section == StudentTestSection.WEEKLY_MEGA -> OnCrownGoldContainer
                            isSelected -> Color.White
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Surface(
                            onClick = { onSelectSection(section) },
                            shape = RoundedCornerShape(14.dp),
                            color = bg,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .testTag(section.tag)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = section.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = fg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$count Live",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Subject Filter Row
            if (subjects.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedSubject == null,
                                onClick = { selectedSubject = null },
                                label = { Text("All Subjects") }
                            )
                        }
                        if (selectedSection == StudentTestSection.WEEKLY_MEGA &&
                            sectionTests.any { it.subjectName.equals("All Subjects Combined", ignoreCase = true) }
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedSubject == "All Subjects Combined",
                                    onClick = {
                                        selectedSubject = if (selectedSubject == "All Subjects Combined") null else "All Subjects Combined"
                                    },
                                    label = { Text("All Subjects Combined") }
                                )
                            }
                        }
                        items(subjects, key = { it.id }) { subject ->
                            FilterChip(
                                selected = selectedSubject == subject.name,
                                onClick = {
                                    selectedSubject = if (selectedSubject == subject.name) null else subject.name
                                },
                                label = { Text(subject.name) }
                            )
                        }
                    }
                }
            }

            // Live Published Tests List
            when (testsState) {
                is UiState.Loading -> item { LoadingCard() }
                is UiState.Error -> item { ErrorStateCard(testsState.message) }
                is UiState.Success -> {
                    if (filtered.isEmpty()) {
                        item {
                            val (emptyIcon, emptyTitle, emptySubtitle) = when (selectedSection) {
                                StudentTestSection.DAILY -> Triple(
                                    Icons.Filled.Quiz,
                                    "कोई दैनिक टेस्ट उपलब्ध नहीं है",
                                    "No published Daily Test matches your filter. Owner Admin publishes fresh Class 10 Bihar Board tests here."
                                )
                                StudentTestSection.CHAPTER_WISE -> Triple(
                                    Icons.Filled.MenuBook,
                                    "अभी कोई अध्याय टेस्ट प्रकाशित नहीं है",
                                    "No published Chapter-wise Test matches your filter yet. Once Owner Admin publishes a chapter test, it appears here automatically."
                                )
                                StudentTestSection.WEEKLY_MEGA -> Triple(
                                    Icons.Filled.WorkspacePremium,
                                    "अभी कोई साप्ताहिक मेगा टेस्ट प्रकाशित नहीं है",
                                    "No published Weekly Mega Test yet. Up to 200-question Mega Tests published by Owner Admin sync here live."
                                )
                            }
                            EmptyStateCard(
                                icon = emptyIcon,
                                title = emptyTitle,
                                subtitle = emptySubtitle
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { test ->
                            val attempt = attemptsMap[test.id]
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
fun TestRunnerScreen(
    session: ActiveTestSession,
    onSelectAnswer: (Int, Int) -> Unit,
    onClearAnswer: (Int) -> Unit,
    onNavigateQuestion: (Int) -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit
) {
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showQuestionGrid by remember { mutableStateOf(false) }
    val stripState = rememberLazyListState()

    BackHandler { showExitDialog = true }

    // Keep the 1..200 question number palette scrolled cleanly to the active question
    LaunchedEffect(session.currentIndex) {
        val target = (session.currentIndex - 2).coerceAtLeast(0)
        stripState.animateScrollToItem(target)
    }

    val question = session.questions.getOrNull(session.currentIndex)
    val chosenIndex = session.selectedAnswers.getOrElse(session.currentIndex) { -1 }
    val totalCount = session.questions.size
    val answeredCount = session.selectedAnswers.count { it >= 0 }
    val unansweredCount = (totalCount - answeredCount).coerceAtLeast(0)
    val isLowTime = session.remainingSeconds <= 60L

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("test_runner_screen"),
        topBar = {
            Surface(shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Test")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.test.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Q ${session.currentIndex + 1}/$totalCount • Ans $answeredCount • Left $unansweredCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { showQuestionGrid = true },
                        modifier = Modifier.testTag("question_grid_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "All Questions Navigator (1 to $totalCount)",
                            tint = RoyalBluePrimary
                        )
                    }
                    StatusPill(
                        text = DateUtils.formatDurationSeconds(session.remainingSeconds),
                        containerColor = if (isLowTime) ErrorCrimsonContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isLowTime) ErrorCrimson else MaterialTheme.colorScheme.primary,
                        icon = Icons.Filled.Timer
                    )
                }
            }
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onNavigateQuestion(session.currentIndex - 1) },
                        enabled = session.currentIndex > 0 && !session.submitting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("prev_question_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Prev")
                    }

                    if (session.currentIndex < session.questions.lastIndex) {
                        Button(
                            onClick = { onNavigateQuestion(session.currentIndex + 1) },
                            enabled = !session.submitting,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("next_question_button")
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = { showSubmitDialog = true },
                            enabled = !session.submitting,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("submit_test_button")
                        ) {
                            if (session.submitting) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Submit")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // Auto-scrolling 1..200 Question Number Strip
            LazyRow(
                state = stripState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_number_strip")
            ) {
                itemsIndexed(session.questions, key = { idx, _ -> idx }) { idx, _ ->
                    val isCurrent = idx == session.currentIndex
                    val isAnswered = session.selectedAnswers.getOrElse(idx) { -1 } >= 0
                    val bg = when {
                        isCurrent -> RoyalBluePrimary
                        isAnswered -> SuccessGreenContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val fg = when {
                        isCurrent -> Color.White
                        isAnswered -> SuccessGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .clickable { onNavigateQuestion(idx) }
                            .testTag("question_chip_${idx + 1}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = fg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (question != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "प्रश्न ${session.currentIndex + 1} / $totalCount",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = RoyalBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (session.test.effectiveMarksPerQuestion() > 1L) {
                                    Text(
                                        text = "${session.test.effectiveMarksPerQuestion()} Marks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = question.questionText,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    val optionLabels = listOf("A", "B", "C", "D")
                    question.optionsList().forEachIndexed { optIdx, optText ->
                        val selected = chosenIndex == optIdx
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !session.submitting) {
                                    onSelectAnswer(session.currentIndex, optIdx)
                                }
                                .testTag("option_card_${session.currentIndex}_$optIdx"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = MaterialTheme.shapes.medium
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) RoyalBluePrimary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = optionLabels[optIdx],
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onClearAnswer(session.currentIndex) },
                            enabled = chosenIndex >= 0 && !session.submitting
                        ) {
                            Text("Clear Answer")
                        }
                        TextButton(
                            onClick = { showQuestionGrid = true },
                            enabled = !session.submitting
                        ) {
                            Text("Q Grid ($unansweredCount Left)", color = RoyalBlueDark)
                        }
                        TextButton(
                            onClick = { showSubmitDialog = true },
                            enabled = !session.submitting
                        ) {
                            Text("Submit", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    // 1..200 Question Navigator Grid Dialog (with All / Unanswered / Answered filters)
    if (showQuestionGrid) {
        var gridFilter by remember { mutableStateOf(NavigatorFilter.ALL) }
        val indices = remember(session.selectedAnswers, gridFilter, totalCount) {
            (0 until totalCount).filter { idx ->
                val isAns = session.selectedAnswers.getOrElse(idx) { -1 } >= 0
                when (gridFilter) {
                    NavigatorFilter.ALL -> true
                    NavigatorFilter.UNANSWERED -> !isAns
                    NavigatorFilter.ANSWERED -> isAns
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showQuestionGrid = false },
            title = {
                Text("Question Navigator (1–$totalCount)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusPill(
                            text = "Ans: $answeredCount",
                            containerColor = SuccessGreenContainer,
                            contentColor = SuccessGreen
                        )
                        StatusPill(
                            text = "Left: $unansweredCount",
                            containerColor = CrownGoldContainer,
                            contentColor = OnCrownGoldContainer
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(NavigatorFilter.entries, key = { it.name }) { f ->
                            val count = when (f) {
                                NavigatorFilter.ALL -> totalCount
                                NavigatorFilter.UNANSWERED -> unansweredCount
                                NavigatorFilter.ANSWERED -> answeredCount
                            }
                            FilterChip(
                                selected = gridFilter == f,
                                onClick = { gridFilter = f },
                                label = { Text("${f.label} ($count)") }
                            )
                        }
                    }
                    if (indices.isEmpty()) {
                        Text(
                            text = "No questions in this filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 340.dp)
                                .testTag("question_navigator_grid")
                        ) {
                            items(indices, key = { it }) { idx ->
                                val isCurrent = idx == session.currentIndex
                                val isAns = session.selectedAnswers.getOrElse(idx) { -1 } >= 0
                                val bg = when {
                                    isCurrent -> RoyalBluePrimary
                                    isAns -> SuccessGreenContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val fg = when {
                                    isCurrent -> Color.White
                                    isAns -> SuccessGreen
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .clickable {
                                            onNavigateQuestion(idx)
                                            showQuestionGrid = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = fg
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuestionGrid = false }) { Text("Close") }
            }
        )
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("टेस्ट सबमिट करें? / Submit Test?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total Questions: $totalCount")
                    Text("Answered: $answeredCount", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Unanswered: $unansweredCount",
                        color = if (unansweredCount > 0) ErrorCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "सबमिट करने के बाद आपके उत्तर स्थायी रूप से लॉक हो जाएंगे और परिणाम स्क्रीन खुलेगी।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        onSubmit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.testTag("confirm_submit_test_button")
                ) {
                    Text("Yes, Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) { Text("Review Questions") }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("टेस्ट छोड़ें? / Leave Test?") },
            text = { Text("यदि आप अभी बाहर निकलते हैं तो आपके चयनित उत्तर सहेजे नहीं जाएंगे।") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onExit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Continue Test") }
            }
        )
    }
}
