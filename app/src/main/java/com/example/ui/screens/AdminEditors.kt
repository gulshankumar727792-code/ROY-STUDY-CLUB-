package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppConstants
import com.example.data.DateUtils
import com.example.data.OFFICIAL_VIP_PLANS
import com.example.data.StudentProfile
import com.example.data.StudyTest
import com.example.data.Subject
import com.example.data.TestCategory
import com.example.data.TestQuestion
import com.example.data.VideoCallStatus
import com.example.data.VipNote
import com.example.data.VipNoteContent
import com.example.data.WeeklyChampion
import com.example.data.WeeklyLeaderboardEntry
import com.example.ui.components.StatusPill
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer

@Composable
fun AdminTestBuilderScreen(
    existingTest: StudyTest?,
    initialQuestions: List<TestQuestion>,
    subjects: List<Subject>,
    busy: Boolean,
    onClose: () -> Unit,
    onSave: (
        existingTestId: String?,
        title: String,
        subjectId: String,
        subjectName: String,
        chapterName: String,
        testDate: String,
        durationMinutes: Long,
        category: TestCategory,
        weekKey: String,
        published: Boolean,
        questions: List<TestQuestion>
    ) -> Unit
) {
    BackHandler { onClose() }

    var title by remember { mutableStateOf(existingTest?.title ?: "") }
    var selectedSubjectId by remember { mutableStateOf(existingTest?.subjectId ?: subjects.firstOrNull()?.id.orEmpty()) }
    var selectedSubjectName by remember { mutableStateOf(existingTest?.subjectName ?: subjects.firstOrNull()?.name.orEmpty()) }
    var chapterName by remember { mutableStateOf(existingTest?.chapterName ?: "") }
    var testDate by remember { mutableStateOf(existingTest?.testDate?.ifBlank { DateUtils.todayIso() } ?: DateUtils.todayIso()) }
    var durationStr by remember { mutableStateOf((existingTest?.durationMinutes ?: 45L).toString()) }
    var category by remember { mutableStateOf(existingTest?.categoryEnum() ?: TestCategory.DAILY) }
    var weekKey by remember { mutableStateOf(existingTest?.weekKey?.ifBlank { DateUtils.currentWeekKey() } ?: DateUtils.currentWeekKey()) }
    var published by remember { mutableStateOf(existingTest?.published ?: false) }

    var questions by remember { mutableStateOf(initialQuestions) }
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }

    val activeSubject = subjects.firstOrNull { it.id == selectedSubjectId }
    val optionLabels = listOf("A", "B", "C", "D")

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("admin_test_builder_screen"),
        topBar = {
            Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existingTest == null) "Create Manual Test" else "Edit Test",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${questions.size} / ${AppConstants.DAILY_TEST_TARGET_QUESTIONS} Questions added manually",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = {
                            val dur = durationStr.toLongOrNull()?.coerceIn(1L, 240L) ?: 30L
                            val safeSubId = selectedSubjectId.ifBlank { "custom_subject" }
                            val safeSubName = selectedSubjectName.ifBlank { "General" }
                            onSave(
                                existingTest?.id,
                                title,
                                safeSubId,
                                safeSubName,
                                chapterName,
                                testDate,
                                dur,
                                category,
                                weekKey.ifBlank { DateUtils.weekKeyFromIsoDate(testDate) },
                                published,
                                questions
                            )
                        },
                        enabled = !busy && title.trim().length >= 2 && chapterName.trim().isNotEmpty() && (!published || questions.isNotEmpty()),
                        modifier = Modifier.testTag("admin_save_test_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Test Details", style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Test Title *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_test_title_input")
                        )

                        Text("Category", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TestCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat.label) }
                                )
                            }
                        }

                        Text("Subject (Class 10 Bihar Board)", style = MaterialTheme.typography.labelMedium)
                        if (subjects.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(subjects, key = { it.id }) { sub ->
                                    FilterChip(
                                        selected = selectedSubjectId == sub.id,
                                        onClick = {
                                            selectedSubjectId = sub.id
                                            selectedSubjectName = sub.name
                                        },
                                        label = { Text(sub.name) }
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = selectedSubjectName,
                                onValueChange = {
                                    selectedSubjectName = it
                                    selectedSubjectId = "sub_${it.lowercase().filter { c -> c.isLetterOrDigit() }.ifBlank { "gen" }}"
                                },
                                label = { Text("Subject Name *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (activeSubject != null && activeSubject.chapters.isNotEmpty()) {
                            Text("Select Chapter from Syllabus", style = MaterialTheme.typography.labelMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(activeSubject.chapters) { ch ->
                                    FilterChip(
                                        selected = chapterName == ch,
                                        onClick = { chapterName = ch },
                                        label = { Text(ch, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = chapterName,
                            onValueChange = { chapterName = it },
                            label = { Text("Chapter Name *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_test_chapter_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = testDate,
                                onValueChange = {
                                    testDate = it
                                    weekKey = DateUtils.weekKeyFromIsoDate(it)
                                },
                                label = { Text("Date (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = durationStr,
                                onValueChange = { durationStr = it.filter { c -> c.isDigit() } },
                                label = { Text("Minutes") },
                                singleLine = true,
                                modifier = Modifier.weight(0.7f)
                            )
                        }

                        OutlinedTextField(
                            value = weekKey,
                            onValueChange = { weekKey = it },
                            label = { Text("Competition Week Key (e.g. ${DateUtils.currentWeekKey()})") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Publish Immediately", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Unpublished tests stay hidden from students",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = published, onCheckedChange = { published = it })
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manual Questions (${questions.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = {
                            editingQuestionIndex = null
                            showAddQuestionDialog = true
                        },
                        modifier = Modifier.testTag("admin_add_question_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Question")
                    }
                }
            }

            itemsIndexed(questions) { idx, q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            StatusPill(
                                text = "Q${idx + 1} • Ans: ${optionLabels.getOrElse(q.correctIndex) { "A" }}",
                                containerColor = SuccessGreenContainer,
                                contentColor = SuccessGreen
                            )
                            Row {
                                IconButton(onClick = {
                                    editingQuestionIndex = idx
                                    showAddQuestionDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Question", tint = RoyalBluePrimary)
                                }
                                IconButton(onClick = {
                                    questions = questions.toMutableList().apply { removeAt(idx) }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Question", tint = ErrorCrimson)
                                }
                            }
                        }
                        Text(q.questionText, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "A) ${q.optionA}   B) ${q.optionB}\nC) ${q.optionC}   D) ${q.optionD}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (q.explanation.isNotBlank()) {
                            Text(
                                text = "Exp: ${q.explanation}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBluePrimary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddQuestionDialog) {
        val initialQ = editingQuestionIndex?.let { questions.getOrNull(it) }
        ManualQuestionDialog(
            initial = initialQ,
            onDismiss = { showAddQuestionDialog = false },
            onSave = { newQ ->
                val idx = editingQuestionIndex
                questions = if (idx != null && idx in questions.indices) {
                    questions.toMutableList().apply { set(idx, newQ) }
                } else {
                    questions + newQ
                }
                showAddQuestionDialog = false
            }
        )
    }
}

@Composable
private fun ManualQuestionDialog(
    initial: TestQuestion?,
    onDismiss: () -> Unit,
    onSave: (TestQuestion) -> Unit
) {
    var questionText by remember { mutableStateOf(initial?.questionText ?: "") }
    var optA by remember { mutableStateOf(initial?.optionA ?: "") }
    var optB by remember { mutableStateOf(initial?.optionB ?: "") }
    var optC by remember { mutableStateOf(initial?.optionC ?: "") }
    var optD by remember { mutableStateOf(initial?.optionD ?: "") }
    var correctIndex by remember { mutableIntStateOf(initial?.correctIndex ?: 0) }
    var explanation by remember { mutableStateOf(initial?.explanation ?: "") }

    val valid = questionText.trim().isNotEmpty() &&
        optA.trim().isNotEmpty() && optB.trim().isNotEmpty() &&
        optC.trim().isNotEmpty() && optD.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Manual Question" else "Edit Question") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Question Text (Hindi / English) *") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Correct Answer", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("A", "B", "C", "D").forEachIndexed { idx, lbl ->
                        FilterChip(
                            selected = correctIndex == idx,
                            onClick = { correctIndex = idx },
                            label = { Text(lbl) }
                        )
                    }
                }

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation / व्याख्या") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        TestQuestion(
                            questionText = questionText.trim(),
                            optionA = optA.trim(),
                            optionB = optB.trim(),
                            optionC = optC.trim(),
                            optionD = optD.trim(),
                            correctIndex = correctIndex,
                            explanation = explanation.trim()
                        )
                    )
                },
                enabled = valid
            ) {
                Text("Save Question")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminSubjectEditorDialog(
    initial: Subject?,
    onDismiss: () -> Unit,
    onSave: (id: String?, name: String, nameHindi: String, chapters: List<String>, order: Long, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var nameHindi by remember { mutableStateOf(initial?.nameHindi ?: "") }
    var chaptersText by remember { mutableStateOf(initial?.chapters?.joinToString("\n") ?: "") }
    var orderStr by remember { mutableStateOf((initial?.displayOrder ?: 1L).toString()) }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Subject" else "Edit Subject") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name (English) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameHindi, onValueChange = { nameHindi = it }, label = { Text("Subject Name (Hindi)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = orderStr,
                    onValueChange = { orderStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Display Order") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chaptersText,
                    onValueChange = { chaptersText = it },
                    label = { Text("Chapters (one per line)") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active for Students")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chapters = chaptersText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(initial?.id, name, nameHindi, chapters, orderStr.toLongOrNull() ?: 1L, active)
                },
                enabled = name.trim().isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminVipNoteEditorDialog(
    initialNote: VipNote?,
    initialContent: VipNoteContent?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        subjectName: String,
        chapterName: String,
        summary: String,
        content: String,
        keyPoints: List<String>,
        published: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var subjectName by remember { mutableStateOf(initialNote?.subjectName ?: "") }
    var chapterName by remember { mutableStateOf(initialNote?.chapterName ?: "") }
    var summary by remember { mutableStateOf(initialNote?.summary ?: "") }
    var keyPointsText by remember { mutableStateOf(initialContent?.keyPoints?.joinToString("\n") ?: "") }
    var content by remember { mutableStateOf(initialContent?.content ?: "") }
    var published by remember { mutableStateOf(initialNote?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNote == null) "Add VIP Special Note" else "Edit VIP Special Note") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Note Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = chapterName, onValueChange = { chapterName = it }, label = { Text("Chapter *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Short Summary") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = keyPointsText, onValueChange = { keyPointsText = it }, label = { Text("Key Points (one per line)") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Full Revision Note Content *") }, minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish to VIP Members")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val points = keyPointsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(initialNote?.id, title, subjectName, chapterName, summary, content, points, published)
                },
                enabled = title.trim().length >= 2 && subjectName.trim().isNotEmpty() && chapterName.trim().isNotEmpty() && content.trim().isNotEmpty()
            ) { Text("Save Note") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminChampionDialog(
    weekKey: String,
    candidate: WeeklyLeaderboardEntry?,
    existing: WeeklyChampion?,
    onDismiss: () -> Unit,
    onSave: (
        weekLabel: String,
        giftDetails: String,
        specialOffer: String,
        videoCallStatus: VideoCallStatus,
        announcementNote: String,
        published: Boolean
    ) -> Unit
) {
    var weekLabel by remember { mutableStateOf(existing?.weekLabel ?: "Week $weekKey") }
    var giftDetails by remember { mutableStateOf(existing?.giftDetails ?: "") }
    var specialOffer by remember { mutableStateOf(existing?.specialOffer ?: "") }
    var callStatus by remember { mutableStateOf(VideoCallStatus.fromCode(existing?.videoCallStatus ?: "PENDING")) }
    var note by remember { mutableStateOf(existing?.announcementNote ?: "") }
    var published by remember { mutableStateOf(existing?.published ?: true) }

    val winnerName = existing?.winnerName ?: candidate?.displayName ?: ""
    val winnerMarks = existing?.totalMarks ?: candidate?.totalMarks ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify Weekly Champion") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Winner: $winnerName ($winnerMarks Marks)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(value = weekLabel, onValueChange = { weekLabel = it }, label = { Text("Week Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = giftDetails, onValueChange = { giftDetails = it }, label = { Text("Gift Awarded (e.g. Study Kit / Prize)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = specialOffer, onValueChange = { specialOffer = it }, label = { Text("Special Offer (e.g. Free 1 Month VIP)") }, modifier = Modifier.fillMaxWidth())

                Text("Video Call Status", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VideoCallStatus.entries.forEach { st ->
                        FilterChip(
                            selected = callStatus == st,
                            onClick = { callStatus = st },
                            label = { Text(st.label) }
                        )
                    }
                }

                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Public Announcement Note") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish Announcement")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(weekLabel, giftDetails, specialOffer, callStatus, note, published) }) {
                Text("Save & Announce")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminStudentVipDialog(
    student: StudentProfile,
    onDismiss: () -> Unit,
    onApply: (activate: Boolean, planId: String, daysFromNow: Long, giftEligible: Boolean) -> Unit
) {
    var selectedPlanId by remember { mutableStateOf(if (student.vipPlanId == "NONE") "PLAN_1M" else student.vipPlanId) }
    var daysStr by remember { mutableStateOf("30") }
    var giftEligible by remember { mutableStateOf(student.giftEligible) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage VIP — ${student.fullName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Email: ${student.email}\nPhone: ${student.phone.ifBlank { "Not set" }}\nDistrict: ${student.district.ifBlank { "Not set" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Select VIP Plan", style = MaterialTheme.typography.labelMedium)
                OFFICIAL_VIP_PLANS.forEach { plan ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPlanId = plan.id
                                daysStr = plan.durationDays.toString()
                                if (plan.hasSpecialGift) giftEligible = true
                            },
                        color = if (selectedPlanId == plan.id) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${plan.title} — ₹${plan.amountInr} (${plan.durationDays} days)",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedTextField(
                    value = daysStr,
                    onValueChange = { daysStr = it.filter { c -> c.isDigit() } },
                    label = { Text("VIP Validity (Days from today)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("12-Month Special Gift Eligible", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = giftEligible, onCheckedChange = { giftEligible = it })
                }

                if (student.vipActive) {
                    OutlinedButton(
                        onClick = { onApply(false, "NONE", 0L, false) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Revoke / Expire VIP Now")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysStr.toLongOrNull()?.coerceIn(1L, 730L) ?: 30L
                    onApply(true, selectedPlanId, days, giftEligible)
                }
            ) { Text("Activate / Extend VIP") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
