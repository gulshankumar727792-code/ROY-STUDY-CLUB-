package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.TestCategory
import com.example.data.TestQuestion
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.RoyalBlueHeroBrush
import com.example.ui.components.StatusPill
import com.example.ui.theme.CrownGold
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorCrimsonContainer
import com.example.ui.theme.OnCrownGoldContainer
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.viewmodel.ReviewSession

private val UnansweredGrey = Color(0xFF4B5563)
private val UnansweredGreyContainer = Color(0xFFE5E7EB)

private enum class AnalysisFilter(val label: String) {
    ALL("All"),
    CORRECT("Correct"),
    INCORRECT("Incorrect"),
    UNANSWERED("Unanswered")
}

private data class IndexedReviewQuestion(
    val originalIndex: Int,
    val question: TestQuestion,
    val chosenIndex: Int
) {
    val isUnanswered: Boolean get() = chosenIndex < 0
    val isCorrect: Boolean get() = chosenIndex == question.correctIndex
    val isIncorrect: Boolean get() = chosenIndex >= 0 && chosenIndex != question.correctIndex
}

/**
 * Enforces the 5-step post-submission workflow:
 * STEP 2 & 3: Always show Result Summary Page FIRST (with no question answers) + "View All Answers" CTA.
 * STEP 4 & 5: Only after tapping "View All Answers", display ordered Question-wise Analysis + Explanations.
 */
@Composable
fun TestResultReviewScreen(
    review: ReviewSession,
    onViewAllAnswers: () -> Unit,
    onBackToResult: () -> Unit,
    onClose: () -> Unit
) {
    if (!review.showAnswerAnalysis) {
        TestResultSummaryScreen(
            review = review,
            onViewAllAnswers = onViewAllAnswers,
            onClose = onClose
        )
    } else {
        TestAnswerAnalysisScreen(
            review = review,
            onBackToResult = onBackToResult
        )
    }
}

@Composable
private fun TestResultSummaryScreen(
    review: ReviewSession,
    onViewAllAnswers: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    val attempt = review.attempt
    val totalMarks = attempt.effectiveTotalMarks()
    val percentageText = attempt.percentageFormatted()
    val passStatus = attempt.passStatus()
    val categoryLabel = TestCategory.fromCode(attempt.testCategory).label

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("test_result_summary_screen"),
        topBar = {
            Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("result_summary_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Tests")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "टेस्ट परिणाम / Test Result",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = attempt.testTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    StatusPill(
                        text = categoryLabel,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Score & Percentage Card (NO answers shown here)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(RoyalBlueHeroBrush)
                            .padding(20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${attempt.subjectName} • ${attempt.chapterName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = percentageText,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.testTag("result_percentage_text")
                            )
                            Text(
                                text = "Obtained Marks: ${attempt.score} / $totalMarks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CrownGold,
                                modifier = Modifier.testTag("result_marks_text")
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (passStatus != null) {
                                    StatusPill(
                                        text = if (passStatus) {
                                            "PASS (Min ${attempt.passingPercentage}%)"
                                        } else {
                                            "FAIL (Min ${attempt.passingPercentage}%)"
                                        },
                                        containerColor = if (passStatus) SuccessGreenContainer else ErrorCrimsonContainer,
                                        contentColor = if (passStatus) SuccessGreen else ErrorCrimson,
                                        icon = if (passStatus) Icons.Filled.CheckCircle else Icons.Filled.Close
                                    )
                                }
                                StatusPill(
                                    text = DateUtils.formatDurationSeconds(attempt.timeTakenSeconds),
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White,
                                    icon = Icons.Filled.Timer
                                )
                            }
                        }
                    }
                }

                // Required Result Metrics: Total Questions, Total Marks, Obtained Marks
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            label = "Total Questions",
                            value = "${attempt.totalQuestions}",
                            accentColor = RoyalBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Total Marks",
                            value = "$totalMarks",
                            accentColor = RoyalBlueDark,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Obtained Marks",
                            value = "${attempt.score}",
                            accentColor = Color(0xFFB37200),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Required Result Metrics: Correct, Incorrect, Unattempted
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            label = "Correct",
                            value = "${attempt.correctCount}",
                            accentColor = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Incorrect",
                            value = "${attempt.wrongCount}",
                            accentColor = ErrorCrimson,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Unattempted",
                            value = "${attempt.skippedCount}",
                            accentColor = UnansweredGrey,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Detailed Breakdown Card
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
                            Text(
                                text = "परिणाम सारांश / Result Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            ResultSummaryRow("Total Questions", "${attempt.totalQuestions}")
                            ResultSummaryRow("Correct Answers", "${attempt.correctCount}", SuccessGreen)
                            ResultSummaryRow("Incorrect Answers", "${attempt.wrongCount}", ErrorCrimson)
                            ResultSummaryRow("Unattempted Questions", "${attempt.skippedCount}", UnansweredGrey)
                            HorizontalDivider()
                            ResultSummaryRow("Marks per Question", "${attempt.effectiveMarksPerQuestion()}")
                            ResultSummaryRow("Total Marks", "$totalMarks")
                            ResultSummaryRow("Obtained Marks", "${attempt.score}", RoyalBluePrimary)
                            ResultSummaryRow("Percentage", percentageText, RoyalBlueDark)
                            if (passStatus != null) {
                                ResultSummaryRow(
                                    label = "Status (Passing ${attempt.passingPercentage}%)",
                                    value = if (passStatus) "PASS" else "FAIL",
                                    valueColor = if (passStatus) SuccessGreen else ErrorCrimson
                                )
                            }
                            ResultSummaryRow("Time Taken", DateUtils.formatDurationSeconds(attempt.timeTakenSeconds))
                        }
                    }
                }

                // Permanent Lock Notice
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = RoyalBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "आपके उत्तर स्थायी रूप से लॉक हो चुके हैं। सभी प्रश्नों के सही उत्तर और व्याख्या देखने के लिए नीचे 'View All Answers' पर क्लिक करें।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // STEP 3: Prominent "View All Answers" Button
                item {
                    Button(
                        onClick = onViewAllAnswers,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalBluePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("view_all_answers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FactCheck,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "View All Answers (${attempt.totalQuestions} Questions)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("close_result_button")
                    ) {
                        Text("Back to Tests / टेस्ट सूची पर जाएं")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummaryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun TestAnswerAnalysisScreen(
    review: ReviewSession,
    onBackToResult: () -> Unit
) {
    BackHandler { onBackToResult() }

    val attempt = review.attempt
    val optionLabels = listOf("A", "B", "C", "D")
    var selectedFilter by remember { mutableStateOf(AnalysisFilter.ALL) }

    val indexedItems = remember(review.questions, attempt.selectedAnswers) {
        review.questions.mapIndexed { idx, q ->
            IndexedReviewQuestion(
                originalIndex = idx,
                question = q,
                chosenIndex = attempt.selectedAnswers.getOrElse(idx) { -1L }.toInt()
            )
        }
    }

    val filteredItems = remember(indexedItems, selectedFilter) {
        when (selectedFilter) {
            AnalysisFilter.ALL -> indexedItems
            AnalysisFilter.CORRECT -> indexedItems.filter { it.isCorrect }
            AnalysisFilter.INCORRECT -> indexedItems.filter { it.isIncorrect }
            AnalysisFilter.UNANSWERED -> indexedItems.filter { it.isUnanswered }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("test_answer_analysis_screen"),
        topBar = {
            Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToResult,
                        modifier = Modifier.testTag("analysis_back_to_result_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Result")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "सभी उत्तर व व्याख्या / All Answers",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${attempt.testTitle} • ${attempt.score}/${attempt.effectiveTotalMarks()} (${attempt.percentageFormatted()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .testTag("answer_analysis_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Legend + Filter Chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusPill(
                                text = "Correct: ${attempt.correctCount}",
                                containerColor = SuccessGreenContainer,
                                contentColor = SuccessGreen,
                                icon = Icons.Filled.CheckCircle
                            )
                            StatusPill(
                                text = "Wrong: ${attempt.wrongCount}",
                                containerColor = ErrorCrimsonContainer,
                                contentColor = ErrorCrimson,
                                icon = Icons.Filled.Close
                            )
                            StatusPill(
                                text = "Unanswered: ${attempt.skippedCount}",
                                containerColor = UnansweredGreyContainer,
                                contentColor = UnansweredGrey,
                                icon = Icons.Filled.RemoveCircleOutline
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AnalysisFilter.entries, key = { it.name }) { filter ->
                                val count = when (filter) {
                                    AnalysisFilter.ALL -> indexedItems.size
                                    AnalysisFilter.CORRECT -> attempt.correctCount.toInt()
                                    AnalysisFilter.INCORRECT -> attempt.wrongCount.toInt()
                                    AnalysisFilter.UNANSWERED -> attempt.skippedCount.toInt()
                                }
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text("${filter.label} ($count)") },
                                    modifier = Modifier.testTag("analysis_filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                if (review.questions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Quiz,
                            title = "Question solutions unavailable",
                            subtitle = "This test paper may have been unpublished or removed by Owner Admin."
                        )
                    }
                } else if (filteredItems.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Quiz,
                            title = "No questions in this filter",
                            subtitle = "Switch to 'All' to view all ${review.questions.size} questions in order."
                        )
                    }
                } else {
                    items(filteredItems, key = { it.originalIndex }) { item ->
                        val q = item.question
                        val chosen = item.chosenIndex
                        val cardBorderColor = when {
                            item.isCorrect -> SuccessGreen
                            item.isUnanswered -> UnansweredGrey
                            else -> ErrorCrimson
                        }
                        val cardBgColor = if (item.isUnanswered) {
                            Color(0xFFF9FAFB)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("analysis_question_card_${item.originalIndex + 1}"),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.2.dp, cardBorderColor),
                            shape = MaterialTheme.shapes.medium
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
                                    Text(
                                        text = "Q${item.originalIndex + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBluePrimary
                                    )
                                    StatusPill(
                                        text = when {
                                            item.isCorrect -> "Correct"
                                            item.isUnanswered -> "Unanswered"
                                            else -> "Incorrect"
                                        },
                                        containerColor = when {
                                            item.isCorrect -> SuccessGreenContainer
                                            item.isUnanswered -> UnansweredGreyContainer
                                            else -> ErrorCrimsonContainer
                                        },
                                        contentColor = when {
                                            item.isCorrect -> SuccessGreen
                                            item.isUnanswered -> UnansweredGrey
                                            else -> ErrorCrimson
                                        },
                                        icon = when {
                                            item.isCorrect -> Icons.Filled.CheckCircle
                                            item.isUnanswered -> Icons.Filled.RemoveCircleOutline
                                            else -> Icons.Filled.Close
                                        }
                                    )
                                }

                                Text(
                                    text = q.questionText,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                // All 4 Options with Green (Correct), Red (Incorrect Selected), Grey (Unanswered neutral)
                                q.optionsList().forEachIndexed { oIdx, optText ->
                                    val isRightOpt = oIdx == q.correctIndex
                                    val isChosenWrong = oIdx == chosen && !isRightOpt
                                    val isChosenRight = oIdx == chosen && isRightOpt

                                    val rowBg = when {
                                        isRightOpt -> SuccessGreenContainer
                                        isChosenWrong -> ErrorCrimsonContainer
                                        item.isUnanswered -> UnansweredGreyContainer.copy(alpha = 0.45f)
                                        else -> Color.Transparent
                                    }
                                    val labelColor = when {
                                        isRightOpt -> SuccessGreen
                                        isChosenWrong -> ErrorCrimson
                                        item.isUnanswered -> UnansweredGrey
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(rowBg)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${optionLabels[oIdx]}.",
                                            fontWeight = FontWeight.Bold,
                                            color = labelColor
                                        )
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                            color = if (isRightOpt || isChosenWrong) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else if (item.isUnanswered) {
                                                UnansweredGrey
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (isChosenRight) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Your Correct Answer",
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else if (isRightOpt) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Correct Answer",
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else if (isChosenWrong) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Your Incorrect Answer",
                                                tint = ErrorCrimson,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Explicit Student Answer vs Correct Answer Summary
                                val studentAnsLabel = if (chosen in 0..3) "Option ${optionLabels[chosen]}" else "Unanswered"
                                val correctAnsLabel = "Option ${optionLabels.getOrElse(q.correctIndex) { "A" }}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Your Answer: $studentAnsLabel",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            item.isCorrect -> SuccessGreen
                                            item.isUnanswered -> UnansweredGrey
                                            else -> ErrorCrimson
                                        }
                                    )
                                    Text(
                                        text = "Correct: $correctAnsLabel",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }

                                // STEP 5: Explanation below the correct answer (only if added by Admin)
                                if (q.explanation.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "Explanation",
                                                tint = RoyalBluePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "व्याख्या / Explanation: ${q.explanation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onBackToResult,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Back to Result Summary")
                    }
                }
            }
        }
    }
}
