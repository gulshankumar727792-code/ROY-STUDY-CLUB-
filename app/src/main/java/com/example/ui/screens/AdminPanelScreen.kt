package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppConstants
import com.example.data.BulkQuestionParser
import com.example.data.DateUtils
import com.example.data.DynamicVipPlan
import com.example.data.ParsedQuestionItem
import com.example.data.PaymentRequest
import com.example.data.PaymentStatus
import com.example.data.ReferralConfig
import com.example.data.ReferralRecord
import com.example.data.StudentProfile
import com.example.data.StudyTest
import com.example.data.Subject
import com.example.data.TestCategory
import com.example.data.TestQuestion
import com.example.data.UiState
import com.example.data.VipNote
import com.example.data.VipNoteContent
import com.example.data.WeeklyChampion
import com.example.data.WeeklyLeaderboardEntry
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.VipCrownBadge
import com.example.ui.components.decodeBase64Bitmap
import com.example.ui.components.openExternalUrl
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
import com.example.viewmodel.AdminViewModel

@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    currentUserUid: String,
    currentUserEmail: String,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val statusMessage by adminViewModel.statusMessage.collectAsStateWithLifecycle()
    val busy by adminViewModel.busy.collectAsStateWithLifecycle()
    val subjectsState by adminViewModel.subjectsState.collectAsStateWithLifecycle()
    val testsState by adminViewModel.testsState.collectAsStateWithLifecycle()
    val studentsState by adminViewModel.studentsState.collectAsStateWithLifecycle()
    val paymentsState by adminViewModel.paymentsState.collectAsStateWithLifecycle()
    val attemptsState by adminViewModel.attemptsState.collectAsStateWithLifecycle()
    val weeklyState by adminViewModel.weeklyEntriesState.collectAsStateWithLifecycle()
    val championsState by adminViewModel.championsState.collectAsStateWithLifecycle()
    val vipNotesState by adminViewModel.vipNotesState.collectAsStateWithLifecycle()
    val adminsState by adminViewModel.adminsState.collectAsStateWithLifecycle()
    val referralConfigState by adminViewModel.referralConfigState.collectAsStateWithLifecycle()
    val allReferralsState by adminViewModel.allReferralsState.collectAsStateWithLifecycle()
    val allReferralTxState by adminViewModel.allReferralTransactionsState.collectAsStateWithLifecycle()
    val notificationLogsState by adminViewModel.notificationLogsState.collectAsStateWithLifecycle()
    val allVipPlansState by adminViewModel.allVipPlansState.collectAsStateWithLifecycle()
    val vipOfferConfigState by adminViewModel.vipOfferConfigState.collectAsStateWithLifecycle()
    val selectedWeekKey by adminViewModel.selectedWeekKey.collectAsStateWithLifecycle()
    val loadedProof by adminViewModel.loadedProofBase64.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearMessage()
        }
    }

    val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()
    val tests = (testsState as? UiState.Success)?.data.orEmpty()
    val students = (studentsState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val attempts = (attemptsState as? UiState.Success)?.data.orEmpty()
    val weeklyEntries = (weeklyState as? UiState.Success)?.data.orEmpty()
    val champions = (championsState as? UiState.Success)?.data.orEmpty()
    val vipNotes = (vipNotesState as? UiState.Success)?.data.orEmpty()
    val admins = (adminsState as? UiState.Success)?.data.orEmpty()
    val referralConfig = (referralConfigState as? UiState.Success)?.data ?: ReferralConfig()
    val allReferrals = (allReferralsState as? UiState.Success)?.data.orEmpty()
    val allReferralTransactions = (allReferralTxState as? UiState.Success)?.data.orEmpty()
    val notificationLogs = (notificationLogsState as? UiState.Success)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) }
    var referralFilter by remember { mutableStateOf(AdminReferralFilter.ALL) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = adminViewModel.buildReferralReportCsv()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                adminViewModel.postMessage("Referral CSV report exported successfully!")
            } catch (e: Exception) {
                adminViewModel.postMessage("CSV export failed: ${e.localizedMessage ?: "I/O error"}")
            }
        }
    }

    // Full-screen Test & Manual Question Builder state
    var builderTest by remember { mutableStateOf<Pair<StudyTest?, List<TestQuestion>>?>(null) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var showNewSubjectDialog by remember { mutableStateOf(false) }
    var editingVipNote by remember { mutableStateOf<Pair<VipNote?, VipNoteContent?>?>(null) }
    var reviewingPayment by remember { mutableStateOf<Pair<PaymentRequest, Boolean>?>(null) } // true=approve, false=reject
    var managingStudentVip by remember { mutableStateOf<StudentProfile?>(null) }
    var declaringChampionCandidate by remember { mutableStateOf<WeeklyLeaderboardEntry?>(null) }
    var editingExistingChampion by remember { mutableStateOf<WeeklyChampion?>(null) }
    var approvingReferral by remember { mutableStateOf<ReferralRecord?>(null) }
    var rejectingReferral by remember { mutableStateOf<ReferralRecord?>(null) }
    var showNewVipPlanDialog by remember { mutableStateOf(false) }
    var editingVipPlan by remember { mutableStateOf<DynamicVipPlan?>(null) }
    var deletingVipPlan by remember { mutableStateOf<DynamicVipPlan?>(null) }

    // Bulk Chapter Test Upload (max 50) & Weekly Mega Test (max 200) states
    var bulkChapterForm by remember {
        mutableStateOf(
            BulkUploadFormState(
                durationMinutesStr = "30",
                passingPercentStr = "33",
                marksPerQuestionStr = "1"
            )
        )
    }
    var weeklyMegaForm by remember {
        mutableStateOf(
            BulkUploadFormState(
                subjectId = "all_subjects_mega",
                subjectName = "All Subjects (Combined)",
                chapterName = "Full Class 10 Syllabus",
                durationMinutesStr = "120",
                passingPercentStr = "33",
                marksPerQuestionStr = "1",
                availableFrom = DateUtils.todayIso(),
                closingDate = DateUtils.todayPlusDaysIso(7)
            )
        )
    }
    // Triple(isWeeklyMega, indexInList, ParsedQuestionItem)
    var editingParsedItem by remember { mutableStateOf<Triple<Boolean, Int, ParsedQuestionItem>?>(null) }

    builderTest?.let { (testToEdit, qs) ->
        AdminTestBuilderScreen(
            existingTest = testToEdit,
            initialQuestions = qs,
            subjects = subjects,
            busy = busy,
            onClose = { builderTest = null },
            onSave = { id, title, subId, subName, ch, date, dur, cat, wk, pub, questions ->
                adminViewModel.saveTestWithQuestions(
                    id, title, subId, subName, ch, date, dur, cat, wk, pub, questions
                ) { builderTest = null }
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("owner_admin_panel_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = RoyalBlueDark, contentColor = Color.White, shadowElevation = 4.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Admin Panel", tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Owner Admin Panel", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Roy Study Club • Class 10 Bihar Board", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                        StatusPill(text = "VERIFIED OWNER", containerColor = CrownGold, contentColor = OnCrownGoldContainer)
                    }
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = RoyalBlueDark,
                        contentColor = Color.White,
                        edgePadding = 8.dp
                    ) {
                        listOf(
                            "Overview & Results",
                            "Subjects",
                            "Tests & Qs",
                            "Bulk Question Upload",
                            "Weekly Mega Test",
                            "Payments & Students",
                            "Champion & VIP Notes",
                            "Referrals",
                            "VIP Plan & Offer"
                        ).forEachIndexed { idx, label ->
                            Tab(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                text = { Text(label, color = Color.White) },
                                modifier = Modifier.testTag("admin_tab_$idx")
                            )
                        }
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
            when (selectedTab) {
                // TAB 0: OVERVIEW, ADMIN UID REGISTRY & ALL STUDENT TEST RESULTS
                0 -> {
                    val activeVipCount = students.count { it.hasValidVipNow() }
                    val pendingPayCount = payments.count { it.statusEnum() == PaymentStatus.PENDING }
                    val publishedTestsCount = tests.count { it.published }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile("Students", "${students.size}", RoyalBluePrimary, Modifier.weight(1f))
                            MetricTile("Active VIP", "$activeVipCount", Color(0xFFB37200), Modifier.weight(1f))
                            MetricTile("Pending Pay", "$pendingPayCount", WarningAmber, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile("Published Tests", "$publishedTestsCount", SuccessGreen, Modifier.weight(1f))
                            MetricTile("Total Attempts", "${attempts.size}", RoyalBluePrimary, Modifier.weight(1f))
                        }
                    }

                    item {
                        var newAdminUid by remember { mutableStateOf("") }
                        var newAdminEmail by remember { mutableStateOf("") }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Owner Admin UID Authorization", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Your UID: $currentUserUid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (admins.none { it.uid == currentUserUid }) {
                                    OutlinedButton(
                                        onClick = { adminViewModel.addAdminUid(currentUserUid, currentUserEmail, "Primary Owner") },
                                        enabled = !busy
                                    ) { Text("Register My UID in /admins") }
                                }
                                OutlinedTextField(
                                    value = newAdminUid,
                                    onValueChange = { newAdminUid = it },
                                    label = { Text("Add Verified Owner UID") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newAdminEmail,
                                    onValueChange = { newAdminEmail = it },
                                    label = { Text("Admin Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        adminViewModel.addAdminUid(newAdminUid, newAdminEmail, "Co-Owner Admin")
                                        newAdminUid = ""
                                        newAdminEmail = ""
                                    },
                                    enabled = !busy && newAdminUid.trim().isNotEmpty() && newAdminEmail.trim().contains("@")
                                ) { Text("Authorize UID") }
                            }
                        }
                    }

                    item {
                        SectionHeader("Student Test Results (${attempts.size})", "Recent submissions across all tests")
                    }
                    if (attempts.isEmpty()) {
                        item { EmptyStateCard(Icons.Filled.EmojiEvents, "No test submissions yet", "Student attempts will appear here automatically.") }
                    } else {
                        items(attempts.take(30), key = { it.id }) { att ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${att.studentName} — ${att.testTitle}", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${att.subjectName} • ${att.weekKey} • Time ${DateUtils.formatDurationSeconds(att.timeTakenSeconds)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(
                                        text = "${att.score}/${att.totalQuestions}",
                                        containerColor = SuccessGreenContainer,
                                        contentColor = SuccessGreen
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "FCM Push Notifications Log (${notificationLogs.size})",
                            subtitle = "Automatic push alerts sent when Daily Tests publish or Weekly Champions are declared"
                        )
                    }
                    if (notificationLogs.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.NotificationsActive,
                                title = "No push notifications dispatched yet",
                                subtitle = "Publishing a Daily Test or declaring a Weekly Champion automatically triggers FCM notifications via Firebase Cloud Functions."
                            )
                        }
                    } else {
                        items(notificationLogs.take(20), key = { it.id }) { log ->
                            val isChampionLog = log.type.contains("CHAMPION", ignoreCase = true)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_fcm_log_${log.id}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.title.ifBlank { log.type },
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatusPill(
                                            text = log.status.ifBlank { "SENT" },
                                            containerColor = if (isChampionLog) CrownGoldContainer else SuccessGreenContainer,
                                            contentColor = if (isChampionLog) OnCrownGoldContainer else SuccessGreen
                                        )
                                    }
                                    if (log.body.isNotBlank()) {
                                        Text(
                                            text = log.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${log.type} • ${log.targetSummary} • ${DateUtils.formatDateTime(log.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB 1: SUBJECTS & CHAPTERS MANAGEMENT
                1 -> {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showNewSubjectDialog = true },
                                modifier = Modifier.weight(1f).testTag("admin_add_subject_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Subject")
                            }
                            OutlinedButton(
                                onClick = { adminViewModel.seedOfficialSyllabus() },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).testTag("admin_seed_syllabus_button")
                            ) {
                                Icon(Icons.Filled.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("BSEB Syllabus")
                            }
                        }
                    }
                    if (subjects.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.LibraryAdd,
                                title = "No subjects configured",
                                subtitle = "Add Class 10 Bihar Board subjects manually or tap 'BSEB Syllabus' to add official Class 10 subjects & chapters."
                            )
                        }
                    } else {
                        items(subjects, key = { it.id }) { sub ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("${sub.name} (${sub.nameHindi})", style = MaterialTheme.typography.titleSmall)
                                            Text("${sub.chapters.size} Chapters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row {
                                            IconButton(onClick = { editingSubject = sub }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit Subject", tint = RoyalBluePrimary)
                                            }
                                            IconButton(onClick = { adminViewModel.deleteSubject(sub.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete Subject", tint = ErrorCrimson)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: TESTS & MANUAL QUESTIONS MANAGEMENT
                2 -> {
                    item {
                        Button(
                            onClick = { builderTest = null to emptyList() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_create_test_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create New Test (Daily / VIP / Practice)")
                        }
                    }
                    if (tests.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Add,
                                title = "No tests created yet",
                                subtitle = "Create your first Class 10 Bihar Board Daily Test or VIP Test manually."
                            )
                        }
                    } else {
                        items(tests, key = { it.id }) { test ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(
                                            text = "${test.categoryEnum().label} • ${test.subjectName}",
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        StatusPill(
                                            text = if (test.published) "PUBLISHED" else "DRAFT",
                                            containerColor = if (test.published) SuccessGreenContainer else WarningAmberContainer,
                                            contentColor = if (test.published) SuccessGreen else WarningAmber
                                        )
                                    }
                                    Text(test.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${test.chapterName} • ${test.testDate} • ${test.questionCount} Qs • ${test.durationMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                adminViewModel.loadTestQuestions(test.id) { loaded ->
                                                    builderTest = test to loaded
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Edit & Questions") }
                                        Button(
                                            onClick = { adminViewModel.toggleTestPublish(test) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (test.published) WarningAmber else SuccessGreen
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) { Text(if (test.published) "Unpublish" else "Publish") }
                                        IconButton(onClick = { adminViewModel.deleteTest(test.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete Test", tint = ErrorCrimson)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 3: BULK CHAPTER TEST UPLOAD (UP TO 50 MCQs)
                3 -> {
                    val chapterTests = tests.filter { it.categoryEnum() == TestCategory.CHAPTER_TEST }
                    adminBulkOrMegaTestTab(
                        isWeeklyMega = false,
                        form = bulkChapterForm,
                        subjects = subjects,
                        existingTests = chapterTests,
                        busy = busy,
                        onFormChange = { bulkChapterForm = it },
                        onParseNow = {
                            adminViewModel.parseBulkQuestionsAsync(
                                rawText = bulkChapterForm.rawPastedText,
                                maxLimit = BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS
                            ) { parsed ->
                                bulkChapterForm = bulkChapterForm.copy(
                                    parseResult = parsed,
                                    previewFilter = ParsedPreviewFilter.ALL
                                )
                                adminViewModel.postMessage(
                                    "Parsed ${parsed.totalCount} Qs: ${parsed.validCount} valid, ${parsed.invalidCount} invalid."
                                )
                            }
                        },
                        onEditParsedQuestion = { idx, item ->
                            editingParsedItem = Triple(false, idx, item)
                        },
                        onDeleteParsedQuestion = { idx ->
                            val current = bulkChapterForm.parseResult ?: return@adminBulkOrMegaTestTab
                            val remaining = current.items.toMutableList().apply {
                                if (idx in indices) removeAt(idx)
                            }
                            bulkChapterForm = bulkChapterForm.copy(
                                parseResult = BulkQuestionParser.revalidateItems(
                                    remaining,
                                    BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS,
                                    current.truncatedCount
                                )
                            )
                        },
                        onRemoveAllInvalid = {
                            val current = bulkChapterForm.parseResult ?: return@adminBulkOrMegaTestTab
                            val validOnly = current.items.filter { it.isValid }
                            bulkChapterForm = bulkChapterForm.copy(
                                parseResult = BulkQuestionParser.revalidateItems(
                                    validOnly,
                                    BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS,
                                    current.truncatedCount
                                ),
                                previewFilter = ParsedPreviewFilter.ALL
                            )
                        },
                        onSaveOrPublish = { publishNow ->
                            val parsed = bulkChapterForm.parseResult
                            val chosenSubName = bulkChapterForm.subjectName.trim()
                                .ifBlank { subjects.firstOrNull()?.name.orEmpty() }
                            val chosenSubId = bulkChapterForm.subjectId.trim()
                                .ifBlank { subjects.firstOrNull()?.id ?: "custom_subject" }
                            val cleanChapter = bulkChapterForm.chapterName.trim()
                            when {
                                chosenSubName.isBlank() -> adminViewModel.postMessage("Please select or enter a Subject.")
                                cleanChapter.isBlank() -> adminViewModel.postMessage("Please enter Chapter Name.")
                                parsed == null || parsed.validCount == 0 -> adminViewModel.postMessage("Parse at least 1 valid MCQ question first.")
                                parsed.invalidCount > 0 -> adminViewModel.postMessage("Fix or remove ${parsed.invalidCount} invalid/duplicate questions before saving.")
                                else -> {
                                    val autoTitle = bulkChapterForm.title.trim()
                                        .ifBlank { "$chosenSubName — $cleanChapter" }
                                        .take(140)
                                    val dur = bulkChapterForm.durationMinutesStr.toLongOrNull()?.coerceIn(1L, 360L) ?: 30L
                                    val passPct = bulkChapterForm.passingPercentStr.toLongOrNull()?.coerceIn(0L, 100L) ?: 33L
                                    val marksQ = bulkChapterForm.marksPerQuestionStr.toLongOrNull()?.coerceIn(1L, 10L) ?: 1L
                                    val today = DateUtils.todayIso()
                                    adminViewModel.saveTestWithQuestions(
                                        existingTestId = bulkChapterForm.editingTestId,
                                        title = autoTitle,
                                        subjectId = chosenSubId,
                                        subjectName = chosenSubName,
                                        chapterName = cleanChapter,
                                        testDate = today,
                                        durationMinutes = dur,
                                        category = TestCategory.CHAPTER_TEST,
                                        weekKey = DateUtils.weekKeyFromIsoDate(today),
                                        published = publishNow,
                                        questions = parsed.validQuestions,
                                        passingPercentage = passPct,
                                        marksPerQuestion = marksQ
                                    ) {
                                        bulkChapterForm = BulkUploadFormState(
                                            subjectId = chosenSubId,
                                            subjectName = chosenSubName,
                                            durationMinutesStr = "30",
                                            passingPercentStr = passPct.toString(),
                                            marksPerQuestionStr = marksQ.toString()
                                        )
                                    }
                                }
                            }
                        },
                        onLoadExistingTestIntoEditor = { existing ->
                            adminViewModel.loadTestQuestions(existing.id) { loaded ->
                                val items = loaded.mapIndexed { idx, q ->
                                    ParsedQuestionItem(
                                        id = "loaded_${existing.id}_$idx",
                                        detectedNumber = idx + 1,
                                        question = q,
                                        rawAnswerText = listOf("A", "B", "C", "D").getOrElse(q.correctIndex) { "A" }
                                    )
                                }
                                bulkChapterForm = BulkUploadFormState(
                                    editingTestId = existing.id,
                                    title = existing.title,
                                    subjectId = existing.subjectId,
                                    subjectName = existing.subjectName,
                                    chapterName = existing.chapterName,
                                    durationMinutesStr = existing.durationMinutes.toString(),
                                    passingPercentStr = existing.passingPercentage.toString(),
                                    marksPerQuestionStr = existing.effectiveMarksPerQuestion().toString(),
                                    parseResult = BulkQuestionParser.revalidateItems(
                                        items,
                                        BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS
                                    )
                                )
                            }
                        },
                        onTogglePublishExisting = { adminViewModel.toggleTestPublish(it) },
                        onDeleteExisting = { adminViewModel.deleteTest(it.id) }
                    )
                }

                // TAB 4: WEEKLY MEGA TEST (UP TO 200 MCQs)
                4 -> {
                    val megaTests = tests.filter { it.categoryEnum() == TestCategory.WEEKLY_MEGA }
                    adminBulkOrMegaTestTab(
                        isWeeklyMega = true,
                        form = weeklyMegaForm,
                        subjects = subjects,
                        existingTests = megaTests,
                        busy = busy,
                        onFormChange = { weeklyMegaForm = it },
                        onParseNow = {
                            adminViewModel.parseBulkQuestionsAsync(
                                rawText = weeklyMegaForm.rawPastedText,
                                maxLimit = BulkQuestionParser.MAX_WEEKLY_MEGA_QUESTIONS
                            ) { parsed ->
                                weeklyMegaForm = weeklyMegaForm.copy(
                                    parseResult = parsed,
                                    previewFilter = ParsedPreviewFilter.ALL
                                )
                                adminViewModel.postMessage(
                                    "Parsed ${parsed.totalCount} Mega Test Qs: ${parsed.validCount} valid, ${parsed.invalidCount} invalid."
                                )
                            }
                        },
                        onEditParsedQuestion = { idx, item ->
                            editingParsedItem = Triple(true, idx, item)
                        },
                        onDeleteParsedQuestion = { idx ->
                            val current = weeklyMegaForm.parseResult ?: return@adminBulkOrMegaTestTab
                            val remaining = current.items.toMutableList().apply {
                                if (idx in indices) removeAt(idx)
                            }
                            weeklyMegaForm = weeklyMegaForm.copy(
                                parseResult = BulkQuestionParser.revalidateItems(
                                    remaining,
                                    BulkQuestionParser.MAX_WEEKLY_MEGA_QUESTIONS,
                                    current.truncatedCount
                                )
                            )
                        },
                        onRemoveAllInvalid = {
                            val current = weeklyMegaForm.parseResult ?: return@adminBulkOrMegaTestTab
                            val validOnly = current.items.filter { it.isValid }
                            weeklyMegaForm = weeklyMegaForm.copy(
                                parseResult = BulkQuestionParser.revalidateItems(
                                    validOnly,
                                    BulkQuestionParser.MAX_WEEKLY_MEGA_QUESTIONS,
                                    current.truncatedCount
                                ),
                                previewFilter = ParsedPreviewFilter.ALL
                            )
                        },
                        onSaveOrPublish = { publishNow ->
                            val parsed = weeklyMegaForm.parseResult
                            val cleanTitle = weeklyMegaForm.title.trim()
                            val chosenSubName = weeklyMegaForm.subjectName.trim().ifBlank { "All Subjects (Combined)" }
                            val chosenSubId = weeklyMegaForm.subjectId.trim().ifBlank { "all_subjects_mega" }
                            val availFrom = weeklyMegaForm.availableFrom.trim()
                            val closeDate = weeklyMegaForm.closingDate.trim()
                            when {
                                cleanTitle.length < 2 -> adminViewModel.postMessage("Please enter a Weekly Mega Test Title.")
                                parsed == null || parsed.validCount == 0 -> adminViewModel.postMessage("Parse at least 1 valid MCQ question first.")
                                parsed.invalidCount > 0 -> adminViewModel.postMessage("Fix or remove ${parsed.invalidCount} invalid/duplicate questions before publishing.")
                                availFrom.isNotEmpty() && closeDate.isNotEmpty() && closeDate < availFrom -> {
                                    adminViewModel.postMessage("Closing Date cannot be earlier than Availability Date.")
                                }
                                else -> {
                                    val dur = weeklyMegaForm.durationMinutesStr.toLongOrNull()?.coerceIn(1L, 360L) ?: 120L
                                    val passPct = weeklyMegaForm.passingPercentStr.toLongOrNull()?.coerceIn(0L, 100L) ?: 33L
                                    val marksQ = weeklyMegaForm.marksPerQuestionStr.toLongOrNull()?.coerceIn(1L, 10L) ?: 1L
                                    val refDate = availFrom.ifBlank { DateUtils.todayIso() }
                                    adminViewModel.saveTestWithQuestions(
                                        existingTestId = weeklyMegaForm.editingTestId,
                                        title = cleanTitle.take(140),
                                        subjectId = chosenSubId,
                                        subjectName = chosenSubName,
                                        chapterName = weeklyMegaForm.chapterName.trim().ifBlank { "Full Class 10 Syllabus" },
                                        testDate = refDate,
                                        durationMinutes = dur,
                                        category = TestCategory.WEEKLY_MEGA,
                                        weekKey = DateUtils.weekKeyFromIsoDate(refDate),
                                        published = publishNow,
                                        questions = parsed.validQuestions,
                                        availableFrom = availFrom,
                                        closingDate = closeDate,
                                        passingPercentage = passPct,
                                        marksPerQuestion = marksQ
                                    ) {
                                        weeklyMegaForm = BulkUploadFormState(
                                            subjectId = chosenSubId,
                                            subjectName = chosenSubName,
                                            chapterName = "Full Class 10 Syllabus",
                                            durationMinutesStr = "120",
                                            passingPercentStr = passPct.toString(),
                                            marksPerQuestionStr = marksQ.toString(),
                                            availableFrom = DateUtils.todayIso(),
                                            closingDate = DateUtils.todayPlusDaysIso(7)
                                        )
                                    }
                                }
                            }
                        },
                        onLoadExistingTestIntoEditor = { existing ->
                            adminViewModel.loadTestQuestions(existing.id) { loaded ->
                                val items = loaded.mapIndexed { idx, q ->
                                    ParsedQuestionItem(
                                        id = "loaded_mega_${existing.id}_$idx",
                                        detectedNumber = idx + 1,
                                        question = q,
                                        rawAnswerText = listOf("A", "B", "C", "D").getOrElse(q.correctIndex) { "A" }
                                    )
                                }
                                weeklyMegaForm = BulkUploadFormState(
                                    editingTestId = existing.id,
                                    title = existing.title,
                                    subjectId = existing.subjectId,
                                    subjectName = existing.subjectName,
                                    chapterName = existing.chapterName,
                                    durationMinutesStr = existing.durationMinutes.toString(),
                                    passingPercentStr = existing.passingPercentage.toString(),
                                    marksPerQuestionStr = existing.effectiveMarksPerQuestion().toString(),
                                    availableFrom = existing.availableFrom.ifBlank { existing.testDate },
                                    closingDate = existing.closingDate,
                                    parseResult = BulkQuestionParser.revalidateItems(
                                        items,
                                        BulkQuestionParser.MAX_WEEKLY_MEGA_QUESTIONS
                                    )
                                )
                            }
                        },
                        onTogglePublishExisting = { adminViewModel.toggleTestPublish(it) },
                        onDeleteExisting = { adminViewModel.deleteTest(it.id) }
                    )
                }

                // TAB 5: VIP WHATSAPP PAYMENTS & STUDENT MANAGEMENT
                5 -> {
                    item { SectionHeader("WhatsApp VIP Payment Requests (${payments.size})", "Verify screenshot & UTR before activating VIP") }
                    if (payments.isEmpty()) {
                        item { EmptyStateCard(Icons.Filled.Check, "No payment requests", "Student WhatsApp VIP requests will appear here.") }
                    } else {
                        items(payments, key = { it.id }) { req ->
                            val st = req.statusEnum()
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${req.studentName} • ${req.planName} (₹${req.amountInr})", style = MaterialTheme.typography.titleSmall)
                                        StatusPill(
                                            text = st.code,
                                            containerColor = when (st) {
                                                PaymentStatus.APPROVED -> SuccessGreenContainer
                                                PaymentStatus.REJECTED -> ErrorCrimsonContainer
                                                PaymentStatus.PENDING -> WarningAmberContainer
                                            },
                                            contentColor = when (st) {
                                                PaymentStatus.APPROVED -> SuccessGreen
                                                PaymentStatus.REJECTED -> ErrorCrimson
                                                PaymentStatus.PENDING -> WarningAmber
                                            }
                                        )
                                    }
                                    Text(
                                        text = "UTR/Ref: ${req.transactionRef} • Phone: ${req.studentPhone.ifBlank { "N/A" }} • ${DateUtils.formatDateTime(req.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (req.note.isNotBlank()) {
                                        Text("Student Note: ${req.note}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (req.hasScreenshot) {
                                            OutlinedButton(onClick = { adminViewModel.loadPaymentProof(req.id) }) {
                                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Screenshot")
                                            }
                                        }
                                        if (st == PaymentStatus.PENDING) {
                                            Button(
                                                onClick = { reviewingPayment = req to true },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Approve")
                                            }
                                            OutlinedButton(
                                                onClick = { reviewingPayment = req to false },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCrimson)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Reject")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { SectionHeader("Manage Students & VIP Expiry (${students.size})", "Tap Manage VIP to extend or revoke membership") }
                    items(students, key = { it.id }) { st ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(st.fullName, style = MaterialTheme.typography.titleSmall)
                                        if (st.hasValidVipNow()) VipCrownBadge("VIP ${st.remainingVipDays()}d")
                                    }
                                    Text(
                                        text = "${st.email} • ${st.phone.ifBlank { "No phone" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (st.vipExpiresAt != null) {
                                        Text(
                                            text = "Expiry: ${DateUtils.formatDateTime(st.vipExpiresAt)}" + if (st.giftEligible) " • 12M Gift" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RoyalBluePrimary
                                        )
                                    }
                                }
                                OutlinedButton(onClick = { managingStudentVip = st }) {
                                    Text("VIP")
                                }
                            }
                        }
                    }
                }

                // TAB 6: WEEKLY CHAMPION MANAGEMENT & VIP SPECIAL NOTES
                6 -> {
                    item {
                        var weekInput by remember(selectedWeekKey) { mutableStateOf(selectedWeekKey) }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weekInput,
                                onValueChange = { weekInput = it },
                                label = { Text("Competition Week Key") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { adminViewModel.setWeekKey(weekInput) }) { Text("Load") }
                        }
                    }

                    item { SectionHeader("Top Weekly Candidates ($selectedWeekKey)", "Verify and declare Weekly Champion") }
                    if (weeklyEntries.isEmpty()) {
                        item { EmptyStateCard(Icons.Filled.EmojiEvents, "No leaderboard entries for $selectedWeekKey", "Students appear here after completing Daily Tests.") }
                    } else {
                        itemsIndexed(weeklyEntries.take(10), key = { _, e -> e.id }) { idx, entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, if (idx == 0) CrownGold else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("#${idx + 1} ${entry.displayName}", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${entry.totalMarks} Marks • ${entry.testsCount} Tests • ${DateUtils.formatDurationSeconds(entry.totalTimeSeconds)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(onClick = { declaringChampionCandidate = entry }) {
                                        Text("Declare Winner")
                                    }
                                }
                            }
                        }
                    }

                    item { SectionHeader("Declared Champions & Winner Contact", "Private winner contact is visible ONLY to Owner Admin") }
                    items(champions, key = { it.id }) { champ ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CrownGoldContainer.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, CrownGold)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${champ.weekLabel} — ${champ.winnerName}", style = MaterialTheme.typography.titleSmall)
                                Text("Marks: ${champ.totalMarks} • Video Call: ${champ.videoCallStatus} • Gift: ${champ.giftDetails.ifBlank { "None" }}", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            adminViewModel.fetchWinnerPrivateContact(champ.winnerUserId) { winnerProfile ->
                                                val phone = winnerProfile?.phone.orEmpty()
                                                val url = AppConstants.buildWinnerVideoCallWhatsAppUrl(
                                                    phone = phone,
                                                    winnerName = champ.winnerName,
                                                    weekLabel = champ.weekLabel
                                                )
                                                openExternalUrl(context, url)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Icon(Icons.Filled.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Video Call / WhatsApp")
                                    }
                                    OutlinedButton(onClick = { editingExistingChampion = champ }) {
                                        Text("Edit Gift/Offer")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(
                            title = "VIP Special Notes (${vipNotes.size})",
                            subtitle = "Admin-authored exam revision notes for active VIP members",
                            trailing = {
                                Button(onClick = { editingVipNote = null to null }) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Note")
                                }
                            }
                        )
                    }
                    items(vipNotes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(note.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    StatusPill(
                                        text = if (note.published) "PUBLISHED" else "DRAFT",
                                        containerColor = if (note.published) SuccessGreenContainer else WarningAmberContainer,
                                        contentColor = if (note.published) SuccessGreen else WarningAmber
                                    )
                                }
                                Text("${note.subjectName} • ${note.chapterName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        adminViewModel.loadVipNoteContent(note.id) { content ->
                                            editingVipNote = note to content
                                        }
                                    }) { Text("Edit") }
                                    OutlinedButton(onClick = { adminViewModel.toggleVipNotePublish(note) }) {
                                        Text(if (note.published) "Unpublish" else "Publish")
                                    }
                                    IconButton(onClick = { adminViewModel.deleteVipNote(note.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Note", tint = ErrorCrimson)
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 7: REFERRAL & EARN MANAGEMENT
                7 -> {
                    adminReferralTabContent(
                        config = referralConfig,
                        referrals = allReferrals,
                        transactions = allReferralTransactions,
                        activeFilter = referralFilter,
                        busy = busy,
                        onFilterChange = { referralFilter = it },
                        onSaveConfig = adminViewModel::saveReferralConfig,
                        onExportCsvSave = {
                            csvExportLauncher.launch("roy_study_club_referrals_${DateUtils.todayIso()}.csv")
                        },
                        onExportCsvShare = {
                            val csv = adminViewModel.buildReferralReportCsv()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Roy Study Club Referrals Report (${DateUtils.todayIso()})")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Referral CSV Report"))
                        },
                        onRequestApprove = { approvingReferral = it },
                        onRequestReject = { rejectingReferral = it }
                    )
                }

                // TAB 8: DYNAMIC VIP PRICING & OFFER / PROMOTIONAL BANNER MANAGEMENT
                8 -> {
                    adminVipPricingAndOffersTab(
                        plansState = allVipPlansState,
                        offerConfigState = vipOfferConfigState,
                        busy = busy,
                        onSeedDefaultPlans = adminViewModel::seedDefaultVipPlans,
                        onAddNewPlan = { showNewVipPlanDialog = true },
                        onEditPlan = { editingVipPlan = it },
                        onTogglePlanActive = adminViewModel::toggleVipPlanActive,
                        onDeletePlan = { deletingVipPlan = it },
                        onSaveOfferConfig = adminViewModel::saveVipOfferConfig
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showNewSubjectDialog || editingSubject != null) {
        AdminSubjectEditorDialog(
            initial = editingSubject,
            onDismiss = {
                showNewSubjectDialog = false
                editingSubject = null
            },
            onSave = { id, name, nameHindi, chapters, order, active ->
                adminViewModel.saveSubject(id, name, nameHindi, chapters, order, active) {
                    showNewSubjectDialog = false
                    editingSubject = null
                }
            }
        )
    }

    editingVipNote?.let { (note, content) ->
        AdminVipNoteEditorDialog(
            initialNote = note,
            initialContent = content,
            onDismiss = { editingVipNote = null },
            onSave = { id, title, sub, ch, sum, body, pts, pub ->
                adminViewModel.saveVipNote(id, title, sub, ch, sum, body, pts, pub) {
                    editingVipNote = null
                }
            }
        )
    }

    reviewingPayment?.let { (req, isApprove) ->
        var remark by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reviewingPayment = null },
            title = { Text(if (isApprove) "Approve VIP Payment?" else "Reject Payment Request?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${req.studentName} • ${req.planName} (₹${req.amountInr})\nUTR: ${req.transactionRef}")
                    OutlinedTextField(
                        value = remark,
                        onValueChange = { remark = it },
                        label = { Text("Admin Remark (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isApprove) adminViewModel.approvePayment(req, remark)
                        else adminViewModel.rejectPayment(req.id, remark)
                        reviewingPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isApprove) SuccessGreen else ErrorCrimson)
                ) { Text(if (isApprove) "Approve & Activate VIP" else "Reject") }
            },
            dismissButton = { TextButton(onClick = { reviewingPayment = null }) { Text("Cancel") } }
        )
    }

    loadedProof?.let { (_, base64) ->
        val bmp = remember(base64) { decodeBase64Bitmap(base64) }
        AlertDialog(
            onDismissRequest = { adminViewModel.clearLoadedProof() },
            title = { Text("Payment Screenshot Proof") },
            text = {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Payment Screenshot Proof",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Screenshot image could not be decoded or was not uploaded.")
                }
            },
            confirmButton = {
                TextButton(onClick = { adminViewModel.clearLoadedProof() }) { Text("Close") }
            }
        )
    }

    managingStudentVip?.let { student ->
        AdminStudentVipDialog(
            student = student,
            onDismiss = { managingStudentVip = null },
            onApply = { activate, planId, days, gift ->
                adminViewModel.setStudentVipDays(student, activate, planId, days, gift)
                managingStudentVip = null
            }
        )
    }

    declaringChampionCandidate?.let { candidate ->
        AdminChampionDialog(
            weekKey = selectedWeekKey,
            candidate = candidate,
            existing = null,
            onDismiss = { declaringChampionCandidate = null },
            onSave = { label, gift, offer, callStatus, note, pub ->
                adminViewModel.declareWeeklyChampion(
                    selectedWeekKey, label, candidate, gift, offer, callStatus, note, pub
                ) { declaringChampionCandidate = null }
            }
        )
    }

    editingExistingChampion?.let { existing ->
        AdminChampionDialog(
            weekKey = existing.weekKey,
            candidate = null,
            existing = existing,
            onDismiss = { editingExistingChampion = null },
            onSave = { _, gift, offer, callStatus, note, pub ->
                adminViewModel.updateExistingChampion(existing, gift, offer, callStatus, note, pub) {
                    editingExistingChampion = null
                }
            }
        )
    }

    approvingReferral?.let { ref ->
        AdminApproveReferralDialog(
            referral = ref,
            rewardDays = referralConfig.rewardDays,
            onDismiss = { approvingReferral = null },
            onConfirm = {
                adminViewModel.approveReferral(ref)
                approvingReferral = null
            }
        )
    }

    rejectingReferral?.let { ref ->
        AdminRejectReferralDialog(
            referral = ref,
            onDismiss = { rejectingReferral = null },
            onConfirmReject = { reason ->
                adminViewModel.rejectReferral(ref.referredUid, reason)
                rejectingReferral = null
            }
        )
    }

    editingParsedItem?.let { (isMega, index, item) ->
        ParsedQuestionEditDialog(
            questionNumber = index + 1,
            item = item,
            onDismiss = { editingParsedItem = null },
            onSave = { updated ->
                if (isMega) {
                    val current = weeklyMegaForm.parseResult
                    if (current != null && index in current.items.indices) {
                        val nextList = current.items.toMutableList().apply { set(index, updated) }
                        weeklyMegaForm = weeklyMegaForm.copy(
                            parseResult = BulkQuestionParser.revalidateItems(
                                nextList,
                                BulkQuestionParser.MAX_WEEKLY_MEGA_QUESTIONS,
                                current.truncatedCount
                            )
                        )
                    }
                } else {
                    val current = bulkChapterForm.parseResult
                    if (current != null && index in current.items.indices) {
                        val nextList = current.items.toMutableList().apply { set(index, updated) }
                        bulkChapterForm = bulkChapterForm.copy(
                            parseResult = BulkQuestionParser.revalidateItems(
                                nextList,
                                BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS,
                                current.truncatedCount
                            )
                        )
                    }
                }
                editingParsedItem = null
            }
        )
    }

    if (showNewVipPlanDialog || editingVipPlan != null) {
        val currentPlansCount = ((allVipPlansState as? UiState.Success)?.data?.size ?: 0).toLong()
        AdminVipPlanEditorDialog(
            initial = editingVipPlan,
            nextDisplayOrder = currentPlansCount + 1L,
            busy = busy,
            onDismiss = {
                showNewVipPlanDialog = false
                editingVipPlan = null
            },
            onSave = { existingId, reqId, title, titleHindi, oldPrice, newPrice, days, label, gift, badge, offerName, active, order ->
                adminViewModel.saveVipPlan(
                    existingPlanId = existingId,
                    requestedPlanId = reqId,
                    title = title,
                    titleHindi = titleHindi,
                    originalPriceInr = oldPrice,
                    offerPriceInr = newPrice,
                    durationDays = days,
                    durationLabel = label,
                    hasSpecialGift = gift,
                    badge = badge,
                    offerName = offerName,
                    active = active,
                    displayOrder = order
                ) {
                    showNewVipPlanDialog = false
                    editingVipPlan = null
                }
            }
        )
    }

    deletingVipPlan?.let { planToDelete ->
        AlertDialog(
            onDismissRequest = { deletingVipPlan = null },
            title = { Text("VIP प्लान हटाएँ?") },
            text = {
                Text(
                    "क्या आप '${planToDelete.title}' (ID: ${planToDelete.id}) को हटाना चाहते हैं?\n\nनोट: पहले से VIP खरीद चुके छात्रों की वैधता या भुगतान इतिहास पर इसका कोई असर नहीं पड़ेगा।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteVipPlan(planToDelete)
                        deletingVipPlan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson)
                ) { Text("Delete Plan") }
            },
            dismissButton = {
                TextButton(onClick = { deletingVipPlan = null }) { Text("Cancel") }
            }
        )
    }
}
