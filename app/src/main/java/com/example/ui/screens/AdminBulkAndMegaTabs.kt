package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.BulkParseResult
import com.example.data.BulkQuestionParser
import com.example.data.DateUtils
import com.example.data.ParsedQuestionItem
import com.example.data.StudyTest
import com.example.data.Subject
import com.example.data.TestAvailabilityStatus
import com.example.data.TestCategory
import com.example.data.TestQuestion
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
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
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

enum class ParsedPreviewFilter(val label: String) {
    ALL("All"),
    VALID("Valid"),
    INVALID("Invalid / Duplicate")
}

data class BulkUploadFormState(
    val editingTestId: String? = null,
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterName: String = "",
    val durationMinutesStr: String = "30",
    val passingPercentStr: String = "33",
    val marksPerQuestionStr: String = "1",
    val availableFrom: String = DateUtils.todayIso(),
    val closingDate: String = DateUtils.todayPlusDaysIso(7),
    val rawPastedText: String = "",
    val parseResult: BulkParseResult? = null,
    val previewFilter: ParsedPreviewFilter = ParsedPreviewFilter.ALL
)

private val FORMAT_GUIDE_TEXT = """
Q1. Question text?
A. Option 1
B. Option 2
C. Option 3
D. Option 4
ANSWER: B
EXPLANATION: Optional explanation
""".trimIndent()

/**
 * PART 1 (Bulk Chapter Test Upload, up to 50 MCQs) and
 * PART 2 (Weekly Mega Test, up to 200 MCQs)
 * shared high-performance LazyListScope builder so 200-question previews scroll smoothly at 60fps.
 */
@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.adminBulkOrMegaTestTab(
    isWeeklyMega: Boolean,
    form: BulkUploadFormState,
    subjects: List<Subject>,
    existingTests: List<StudyTest>,
    busy: Boolean,
    onFormChange: (BulkUploadFormState) -> Unit,
    onParseNow: () -> Unit,
    onEditParsedQuestion: (Int, ParsedQuestionItem) -> Unit,
    onDeleteParsedQuestion: (Int) -> Unit,
    onRemoveAllInvalid: () -> Unit,
    onSaveOrPublish: (publishNow: Boolean) -> Unit,
    onLoadExistingTestIntoEditor: (StudyTest) -> Unit,
    onTogglePublishExisting: (StudyTest) -> Unit,
    onDeleteExisting: (StudyTest) -> Unit
) {
    val maxQuestions = if (isWeeklyMega) {
        BulkQuestionParser.MAX_WEEKLY_MEGA_QUESTIONS
    } else {
        BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS
    }
    val categoryTag = if (isWeeklyMega) "weekly_mega" else "bulk_chapter"
    val parsed = form.parseResult
    val filteredItems = parsed?.items.orEmpty().mapIndexed { idx, item -> idx to item }.filter { (_, item) ->
        when (form.previewFilter) {
            ParsedPreviewFilter.ALL -> true
            ParsedPreviewFilter.VALID -> item.isValid
            ParsedPreviewFilter.INVALID -> !item.isValid
        }
    }

    // Header Card
    item(key = "${categoryTag}_header") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isWeeklyMega) Color(0xFF062A78) else RoyalBluePrimary
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWeeklyMega) "Weekly Mega Test (Up to 200 MCQs)" else "Bulk Question Upload (Up to 50 MCQs)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        text = if (isWeeklyMega) "MAX 200 Qs" else "MAX 50 Qs",
                        containerColor = CrownGold,
                        contentColor = OnCrownGoldContainer
                    )
                }
                Text(
                    text = if (isWeeklyMega) {
                        "Paste up to 200 Class 10 Bihar Board MCQ questions, auto-parse & validate, set schedule/duration, and publish to all eligible students."
                    } else {
                        "Enter Subject, Chapter Name, and paste up to 50 MCQ questions. Auto-parse, preview, edit, validate, and publish instantly."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
                if (form.editingTestId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = "EDITING EXISTING TEST",
                            containerColor = WarningAmberContainer,
                            contentColor = WarningAmber
                        )
                        TextButton(
                            onClick = {
                                onFormChange(
                                    BulkUploadFormState(
                                        durationMinutesStr = if (isWeeklyMega) "120" else "30",
                                        passingPercentStr = "33",
                                        marksPerQuestionStr = "1"
                                    )
                                )
                            }
                        ) {
                            Text("Cancel Edit", color = CrownGold)
                        }
                    }
                }
            }
        }
    }

    // Step 1: Test Details & Subject / Chapter Form
    item(key = "${categoryTag}_form_card") {
        val activeSubject = subjects.firstOrNull { it.id == form.subjectId }
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
                    text = if (isWeeklyMega) "1. Weekly Mega Test Setup" else "1. Subject & Chapter Details",
                    style = MaterialTheme.typography.titleSmall,
                    color = RoyalBlueDark
                )

                // Subject Selection Chips
                Text(
                    text = "Select Subject / विषय चुनें",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isWeeklyMega) {
                        item {
                            FilterChip(
                                selected = form.subjectId == "all_subjects_mega",
                                onClick = {
                                    onFormChange(
                                        form.copy(
                                            subjectId = "all_subjects_mega",
                                            subjectName = "All Subjects (Combined)"
                                        )
                                    )
                                },
                                label = { Text("All Subjects (Combined)") },
                                modifier = Modifier.testTag("${categoryTag}_subject_all")
                            )
                        }
                    }
                    items(subjects, key = { it.id }) { sub ->
                        FilterChip(
                            selected = form.subjectId == sub.id,
                            onClick = {
                                onFormChange(
                                    form.copy(
                                        subjectId = sub.id,
                                        subjectName = sub.name
                                    )
                                )
                            },
                            label = { Text(sub.name) },
                            modifier = Modifier.testTag("${categoryTag}_subject_${sub.id}")
                        )
                    }
                }

                // Allow typing a Subject name if no subjects seeded yet
                if (subjects.isEmpty()) {
                    OutlinedTextField(
                        value = form.subjectName,
                        onValueChange = { value ->
                            onFormChange(
                                form.copy(
                                    subjectId = value.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").ifBlank { "general" },
                                    subjectName = value
                                )
                            )
                        },
                        label = { Text("Subject Name (e.g., Science, Mathematics)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${categoryTag}_subject_input")
                    )
                }

                if (isWeeklyMega) {
                    OutlinedTextField(
                        value = form.title,
                        onValueChange = { onFormChange(form.copy(title = it)) },
                        label = { Text("Weekly Mega Test Title *") },
                        placeholder = { Text("e.g., Class 10 Bihar Board Weekly Mega Test #1") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_mega_title_input")
                    )
                    OutlinedTextField(
                        value = form.chapterName,
                        onValueChange = { onFormChange(form.copy(chapterName = it)) },
                        label = { Text("Syllabus / Coverage (Optional, default Full Syllabus)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Chapter Name (Quick Pick from Subject + Editable Input)
                    if (activeSubject != null && activeSubject.chapters.isNotEmpty()) {
                        Text(
                            text = "Quick Pick Chapter (${activeSubject.name}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(activeSubject.chapters) { ch ->
                                FilterChip(
                                    selected = form.chapterName == ch,
                                    onClick = { onFormChange(form.copy(chapterName = ch)) },
                                    label = { Text(ch, maxLines = 1) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = form.chapterName,
                        onValueChange = { onFormChange(form.copy(chapterName = it)) },
                        label = { Text("Chapter Name / अध्याय का नाम *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_chapter_name_input")
                    )

                    OutlinedTextField(
                        value = form.title,
                        onValueChange = { onFormChange(form.copy(title = it)) },
                        label = { Text("Test Title (Optional — auto-generated from Chapter)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Duration, Marks per Question, and Pass %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = form.durationMinutesStr,
                        onValueChange = { onFormChange(form.copy(durationMinutesStr = it.filter { ch -> ch.isDigit() })) },
                        label = { Text("Duration (min)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${categoryTag}_duration_input")
                    )
                    OutlinedTextField(
                        value = form.passingPercentStr,
                        onValueChange = { onFormChange(form.copy(passingPercentStr = it.filter { ch -> ch.isDigit() })) },
                        label = { Text("Pass % (0=Off)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${categoryTag}_pass_percent_input")
                    )
                    OutlinedTextField(
                        value = form.marksPerQuestionStr,
                        onValueChange = { onFormChange(form.copy(marksPerQuestionStr = it.filter { ch -> ch.isDigit() })) },
                        label = { Text("Marks/Q") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                if (isWeeklyMega) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = form.availableFrom,
                            onValueChange = { onFormChange(form.copy(availableFrom = it)) },
                            label = { Text("Available From (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weekly_mega_available_from_input")
                        )
                        OutlinedTextField(
                            value = form.closingDate,
                            onValueChange = { onFormChange(form.copy(closingDate = it)) },
                            label = { Text("Closing Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weekly_mega_closing_date_input")
                        )
                    }
                }
            }
        }
    }

    // Step 2: Paste Questions Box + Format Guide + Parse Button
    item(key = "${categoryTag}_paste_card") {
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
                    text = "2. Paste MCQ Questions (Max $maxQuestions)",
                    style = MaterialTheme.typography.titleSmall,
                    color = RoyalBlueDark
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Supported Format:",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalBlueDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = FORMAT_GUIDE_TEXT,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = form.rawPastedText,
                    onValueChange = { onFormChange(form.copy(rawPastedText = it)) },
                    label = { Text("Paste up to $maxQuestions MCQ questions here...") },
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${categoryTag}_paste_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onParseNow,
                        enabled = !busy && form.rawPastedText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${categoryTag}_parse_button")
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Parsing...")
                        } else {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Parse & Validate (Max $maxQuestions)")
                        }
                    }

                    if (form.rawPastedText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onFormChange(form.copy(rawPastedText = "", parseResult = null)) },
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }

    // Step 3: Validation Summary, Actions & Question Preview List
    if (parsed != null) {
        item(key = "${categoryTag}_summary_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${categoryTag}_parse_summary"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (parsed.invalidCount == 0 && parsed.validCount > 0) SuccessGreen else WarningAmber
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. Parsed Questions Preview & Validation",
                        style = MaterialTheme.typography.titleSmall,
                        color = RoyalBlueDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile("Total", "${parsed.totalCount}", RoyalBluePrimary, Modifier.weight(1f))
                        MetricTile("Valid", "${parsed.validCount}", SuccessGreen, Modifier.weight(1f))
                        MetricTile("Invalid", "${parsed.invalidCount}", ErrorCrimson, Modifier.weight(1f))
                    }

                    if (parsed.truncatedCount > 0) {
                        Surface(
                            color = WarningAmberContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Limit reached: ${parsed.truncatedCount} extra question(s) beyond the maximum limit of $maxQuestions were trimmed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningAmber,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (parsed.invalidCount > 0) {
                        Surface(
                            color = ErrorCrimsonContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${parsed.invalidCount} question(s) have missing options, invalid answers, or duplicates. Edit or remove them before publishing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorCrimson
                                )
                                OutlinedButton(
                                    onClick = onRemoveAllInvalid,
                                    border = BorderStroke(1.dp, ErrorCrimson),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCrimson),
                                    modifier = Modifier.testTag("${categoryTag}_remove_invalid_button")
                                ) {
                                    Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Remove Invalid & Duplicates (${parsed.invalidCount})")
                                }
                            }
                        }
                    }

                    // Filter Chips
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ParsedPreviewFilter.entries.forEach { filter ->
                            val count = when (filter) {
                                ParsedPreviewFilter.ALL -> parsed.totalCount
                                ParsedPreviewFilter.VALID -> parsed.validCount
                                ParsedPreviewFilter.INVALID -> parsed.invalidCount
                            }
                            FilterChip(
                                selected = form.previewFilter == filter,
                                onClick = { onFormChange(form.copy(previewFilter = filter)) },
                                label = { Text("${filter.label} ($count)") }
                            )
                        }
                    }

                    // Publish / Save Draft Action Row
                    val canSave = !busy && parsed.validCount > 0 && parsed.invalidCount == 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSaveOrPublish(false) },
                            enabled = canSave,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("${categoryTag}_save_draft_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Draft")
                        }
                        Button(
                            onClick = { onSaveOrPublish(true) },
                            enabled = canSave,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("${categoryTag}_publish_button")
                        ) {
                            Icon(Icons.Filled.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Publish (${parsed.validCount} Qs)")
                        }
                    }
                }
            }
        }

        // Virtualized Question Preview List (handles 200 items smoothly!)
        items(filteredItems, key = { (_, item) -> item.id }) { (actualIndex, item) ->
            ParsedQuestionPreviewCard(
                displayIndex = actualIndex + 1,
                item = item,
                onEdit = { onEditParsedQuestion(actualIndex, item) },
                onDelete = { onDeleteParsedQuestion(actualIndex) }
            )
        }
    }

    // Existing Published / Draft Tests List for this section
    item(key = "${categoryTag}_existing_header") {
        Spacer(Modifier.height(6.dp))
        SectionHeader(
            title = if (isWeeklyMega) "Published & Draft Weekly Mega Tests (${existingTests.size})" else "Uploaded Chapter-wise Tests (${existingTests.size})",
            subtitle = "Synced live across all student devices via Firestore"
        )
    }

    if (existingTests.isEmpty()) {
        item(key = "${categoryTag}_existing_empty") {
            EmptyStateCard(
                icon = if (isWeeklyMega) Icons.Filled.WorkspacePremium else Icons.Filled.CloudUpload,
                title = if (isWeeklyMega) "No Weekly Mega Tests yet" else "No Chapter-wise Tests uploaded yet",
                subtitle = if (isWeeklyMega) {
                    "Paste up to 200 MCQ questions above to create and publish your first Weekly Mega Test."
                } else {
                    "Use Bulk Question Upload above to parse up to 50 MCQ questions and publish a chapter test."
                }
            )
        }
    } else {
        items(existingTests, key = { "${categoryTag}_existing_${it.id}" }) { test ->
            val avail = test.availabilityStatus()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${categoryTag}_test_item_${test.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        StatusPill(
                            text = test.subjectName,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isWeeklyMega) {
                                StatusPill(
                                    text = avail.label,
                                    containerColor = when (avail) {
                                        TestAvailabilityStatus.OPEN -> SuccessGreenContainer
                                        TestAvailabilityStatus.UPCOMING -> CrownGoldContainer
                                        TestAvailabilityStatus.CLOSED -> ErrorCrimsonContainer
                                    },
                                    contentColor = when (avail) {
                                        TestAvailabilityStatus.OPEN -> SuccessGreen
                                        TestAvailabilityStatus.UPCOMING -> OnCrownGoldContainer
                                        TestAvailabilityStatus.CLOSED -> ErrorCrimson
                                    }
                                )
                            }
                            StatusPill(
                                text = if (test.published) "PUBLISHED" else "DRAFT",
                                containerColor = if (test.published) SuccessGreenContainer else WarningAmberContainer,
                                contentColor = if (test.published) SuccessGreen else WarningAmber
                            )
                        }
                    }

                    Text(test.title, style = MaterialTheme.typography.titleSmall)
                    val scheduleText = if (isWeeklyMega && (test.availableFrom.isNotBlank() || test.closingDate.isNotBlank())) {
                        "\nWindow: ${test.availableFrom.ifBlank { "Now" }} → ${test.closingDate.ifBlank { "No cutoff" }}"
                    } else ""
                    val passText = if (test.passingPercentage > 0L) " • Pass ${test.passingPercentage}%" else ""
                    Text(
                        text = "${test.chapterName} • ${test.questionCount} Qs • ${test.durationMinutes} min • ${test.totalMarks()} Marks$passText$scheduleText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onLoadExistingTestIntoEditor(test) },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit")
                        }
                        Button(
                            onClick = { onTogglePublishExisting(test) },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (test.published) WarningAmber else SuccessGreen
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (test.published) "Unpublish" else "Publish")
                        }
                        IconButton(
                            onClick = { onDeleteExisting(test) },
                            enabled = !busy
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Test", tint = ErrorCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedQuestionPreviewCard(
    displayIndex: Int,
    item: ParsedQuestionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val optionLabels = listOf("A", "B", "C", "D")
    val q = item.question
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parsed_question_card_$displayIndex"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isValid) MaterialTheme.colorScheme.outlineVariant else ErrorCrimson
        ),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Q$displayIndex",
                        style = MaterialTheme.typography.titleSmall,
                        color = RoyalBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        text = if (item.isValid) {
                            "VALID • Ans ${optionLabels.getOrElse(q.correctIndex) { "?" }}"
                        } else if (item.isDuplicate) {
                            "DUPLICATE"
                        } else {
                            "INVALID"
                        },
                        containerColor = if (item.isValid) SuccessGreenContainer else ErrorCrimsonContainer,
                        contentColor = if (item.isValid) SuccessGreen else ErrorCrimson,
                        icon = if (item.isValid) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Q$displayIndex", tint = RoyalBluePrimary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Q$displayIndex", tint = ErrorCrimson)
                    }
                }
            }

            Text(
                text = q.questionText.ifBlank { "(Missing question text)" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (q.questionText.isBlank()) ErrorCrimson else MaterialTheme.colorScheme.onSurface
            )

            q.optionsList().forEachIndexed { optIdx, optText ->
                val isCorrect = optIdx == q.correctIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCorrect) SuccessGreenContainer else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${optionLabels[optIdx]}.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = optText.ifBlank { "(Missing Option ${optionLabels[optIdx]})" },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            optText.isBlank() -> ErrorCrimson
                            isCorrect -> SuccessGreen
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            if (q.explanation.isNotBlank()) {
                Text(
                    text = "Explanation: ${q.explanation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalBlueDark
                )
            }

            if (item.validationErrors.isNotEmpty()) {
                Surface(
                    color = ErrorCrimsonContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item.validationErrors.forEach { err ->
                            Text(
                                text = "• $err",
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorCrimson
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParsedQuestionEditDialog(
    questionNumber: Int,
    item: ParsedQuestionItem,
    onDismiss: () -> Unit,
    onSave: (ParsedQuestionItem) -> Unit
) {
    var qText by remember(item.id) { mutableStateOf(item.question.questionText) }
    var optA by remember(item.id) { mutableStateOf(item.question.optionA) }
    var optB by remember(item.id) { mutableStateOf(item.question.optionB) }
    var optC by remember(item.id) { mutableStateOf(item.question.optionC) }
    var optD by remember(item.id) { mutableStateOf(item.question.optionD) }
    var correctIndex by remember(item.id) { mutableIntStateOf(item.question.correctIndex.coerceIn(0, 3)) }
    var explanation by remember(item.id) { mutableStateOf(item.question.explanation) }
    val labels = listOf("A", "B", "C", "D")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Question #$questionNumber") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = qText,
                    onValueChange = { qText = it },
                    label = { Text("Question Text *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = optA,
                    onValueChange = { optA = it },
                    label = { Text("Option A *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = optB,
                    onValueChange = { optB = it },
                    label = { Text("Option B *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = optC,
                    onValueChange = { optC = it },
                    label = { Text("Option C *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = optD,
                    onValueChange = { optD = it },
                    label = { Text("Option D *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Correct Answer:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = correctIndex == idx,
                            onClick = { correctIndex = idx },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation (Optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        item.copy(
                            question = TestQuestion(
                                questionText = qText.trim(),
                                optionA = optA.trim(),
                                optionB = optB.trim(),
                                optionC = optC.trim(),
                                optionD = optD.trim(),
                                correctIndex = correctIndex,
                                explanation = explanation.trim()
                            ),
                            rawAnswerText = labels[correctIndex]
                        )
                    )
                }
            ) {
                Text("Save Question")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
