package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRecord
import com.example.data.BulkParseResult
import com.example.data.BulkQuestionParser
import com.example.data.DateUtils
import com.example.data.DynamicVipPlan
import com.example.data.NotificationLog
import com.example.data.PaymentRequest
import com.example.data.ReferralConfig
import com.example.data.ReferralRecord
import com.example.data.ReferralTransaction
import com.example.data.ReferralUtils
import com.example.data.RoyStudyRepository
import com.example.data.StudentProfile
import com.example.data.StudyTest
import com.example.data.Subject
import com.example.data.TestAttempt
import com.example.data.TestCategory
import com.example.data.TestQuestion
import com.example.data.UiState
import com.example.data.VideoCallStatus
import com.example.data.VipNote
import com.example.data.VipNoteContent
import com.example.data.VipOfferConfig
import com.example.data.WeeklyChampion
import com.example.data.WeeklyLeaderboardEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val repository: RoyStudyRepository
) : ViewModel() {

    private fun <T> Flow<T>.toSafeAdminState(tag: String, fallback: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "$tag admin stream error", root)
                emit(UiState.Error(root.message ?: fallback))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _selectedWeekKey = MutableStateFlow(DateUtils.currentWeekKey())
    val selectedWeekKey: StateFlow<String> = _selectedWeekKey.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _loadedProofBase64 = MutableStateFlow<Pair<String, String?>?>(null)
    val loadedProofBase64: StateFlow<Pair<String, String?>?> = _loadedProofBase64.asStateFlow()

    val subjectsState: StateFlow<UiState<List<Subject>>> =
        repository.observeAllSubjectsForAdmin().toSafeAdminState("subjects", "Failed to load subjects")

    val testsState: StateFlow<UiState<List<StudyTest>>> =
        repository.observeAllTestsForAdmin().toSafeAdminState("tests", "Failed to load tests")

    val studentsState: StateFlow<UiState<List<StudentProfile>>> =
        repository.observeAllStudentsForAdmin().toSafeAdminState("students", "Failed to load students")

    val paymentsState: StateFlow<UiState<List<PaymentRequest>>> =
        repository.observeAllPaymentRequestsForAdmin().toSafeAdminState("payments", "Failed to load payments")

    val attemptsState: StateFlow<UiState<List<TestAttempt>>> =
        repository.observeAllAttemptsForAdmin().toSafeAdminState("attempts", "Failed to load results")

    val weeklyEntriesState: StateFlow<UiState<List<WeeklyLeaderboardEntry>>> =
        _selectedWeekKey.flatMapLatest { week ->
            repository.observeWeeklyLeaderboard(week)
        }.toSafeAdminState("weekly", "Failed to load weekly leaderboard")

    val championsState: StateFlow<UiState<List<WeeklyChampion>>> =
        repository.observeAllChampionsForAdmin().toSafeAdminState("champions", "Failed to load champions")

    val vipNotesState: StateFlow<UiState<List<VipNote>>> =
        repository.observeAllVipNotesForAdmin().toSafeAdminState("vipNotes", "Failed to load VIP notes")

    val adminsState: StateFlow<UiState<List<AdminRecord>>> =
        repository.observeAdminsForAdmin().toSafeAdminState("admins", "Failed to load admins")

    val referralConfigState: StateFlow<UiState<ReferralConfig>> =
        repository.observeReferralConfig().toSafeAdminState("referralConfig", "Failed to load referral settings")

    val allReferralsState: StateFlow<UiState<List<ReferralRecord>>> =
        repository.observeAllReferralsForAdmin().toSafeAdminState("allReferrals", "Failed to load referrals")

    val allReferralTransactionsState: StateFlow<UiState<List<ReferralTransaction>>> =
        repository.observeAllReferralTransactionsForAdmin().toSafeAdminState("allReferralTx", "Failed to load referral reward history")

    val notificationLogsState: StateFlow<UiState<List<NotificationLog>>> =
        repository.observeNotificationLogsForAdmin().toSafeAdminState("notificationLogs", "Failed to load FCM notification logs")

    val allVipPlansState: StateFlow<UiState<List<DynamicVipPlan>>> =
        repository.observeAllVipPlansForAdmin().toSafeAdminState("vipPlansAdmin", "Failed to load dynamic VIP plans")

    val vipOfferConfigState: StateFlow<UiState<VipOfferConfig>> =
        repository.observeVipOfferConfig().toSafeAdminState("vipOfferConfigAdmin", "Failed to load VIP offer & banner config")

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun setWeekKey(weekKey: String) {
        if (weekKey.isNotBlank()) _selectedWeekKey.value = weekKey.trim()
    }

    // --- ADMIN UID REGISTRY ---
    fun addAdminUid(uid: String, email: String, label: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.registerAdminUid(uid, email, label)
                .onSuccess { _statusMessage.value = "Owner Admin UID saved." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to add Admin UID" }
            _busy.value = false
        }
    }

    // --- SUBJECTS & CHAPTERS ---
    fun saveSubject(
        subjectId: String?,
        name: String,
        nameHindi: String,
        chapters: List<String>,
        displayOrder: Long,
        active: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            repository.saveSubject(subjectId, name, nameHindi, chapters, displayOrder, active)
                .onSuccess {
                    _statusMessage.value = "Subject saved successfully."
                    onDone()
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save subject" }
            _busy.value = false
        }
    }

    fun seedOfficialSyllabus() {
        viewModelScope.launch {
            _busy.value = true
            repository.seedOfficialBiharBoardSyllabus()
                .onSuccess { _statusMessage.value = "Class 10 Bihar Board subjects & chapters added." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to seed syllabus" }
            _busy.value = false
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.deleteSubject(subjectId)
                .onSuccess { _statusMessage.value = "Subject deleted." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete subject" }
            _busy.value = false
        }
    }

    // --- TESTS & MANUAL QUESTION BUILDER ---
    fun loadTestQuestions(testId: String, onLoaded: (List<TestQuestion>) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val res = repository.getTestPaper(testId)
            _busy.value = false
            res.onSuccess { paper -> onLoaded(paper?.questions ?: emptyList()) }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to load questions" }
        }
    }

    fun parseBulkQuestionsAsync(
        rawText: String,
        maxLimit: Int,
        onParsed: (BulkParseResult) -> Unit
    ) {
        viewModelScope.launch {
            _busy.value = true
            val parsed = withContext(Dispatchers.Default) {
                BulkQuestionParser.parse(rawText, maxLimit)
            }
            _busy.value = false
            onParsed(parsed)
        }
    }

    fun saveTestWithQuestions(
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
        questions: List<TestQuestion>,
        availableFrom: String = "",
        closingDate: String = "",
        passingPercentage: Long = 0L,
        marksPerQuestion: Long = 1L,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            repository.saveTestWithPaper(
                existingTestId = existingTestId,
                title = title,
                subjectId = subjectId,
                subjectName = subjectName,
                chapterName = chapterName,
                testDate = testDate,
                durationMinutes = durationMinutes,
                testCategory = category,
                weekKey = weekKey,
                published = published,
                questions = questions,
                availableFrom = availableFrom,
                closingDate = closingDate,
                passingPercentage = passingPercentage,
                marksPerQuestion = marksPerQuestion
            ).onSuccess {
                _statusMessage.value = if (published) {
                    "${category.label} saved & published to all eligible students (${questions.size} Qs)!"
                } else {
                    "${category.label} draft saved (${questions.size} Qs)."
                }
                onDone()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save test"
            }
            _busy.value = false
        }
    }

    fun toggleTestPublish(test: StudyTest) {
        if (!test.published && test.questionCount <= 0L) {
            _statusMessage.value = "Add at least 1 manual question before publishing."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            repository.setTestPublished(test.id, !test.published)
                .onSuccess {
                    _statusMessage.value = if (!test.published) "Test published." else "Test unpublished."
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update publish status" }
            _busy.value = false
        }
    }

    fun deleteTest(testId: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.deleteTest(testId)
                .onSuccess { _statusMessage.value = "Test deleted." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete test" }
            _busy.value = false
        }
    }

    // --- VIP PAYMENTS & STUDENT MEMBERSHIP EXPIRY ---
    fun loadPaymentProof(requestId: String) {
        viewModelScope.launch {
            _busy.value = true
            val res = repository.getPaymentProofBase64(requestId)
            _busy.value = false
            res.onSuccess { base64 -> _loadedProofBase64.value = requestId to base64 }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to load screenshot" }
        }
    }

    fun clearLoadedProof() {
        _loadedProofBase64.value = null
    }

    fun approvePayment(request: PaymentRequest, remark: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.approvePaymentRequest(request, remark)
                .onSuccess { _statusMessage.value = "Payment approved & VIP activated for ${request.studentName}!" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Approval failed" }
            _busy.value = false
        }
    }

    fun rejectPayment(requestId: String, remark: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.rejectPaymentRequest(requestId, remark)
                .onSuccess { _statusMessage.value = "Payment request rejected." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Rejection failed" }
            _busy.value = false
        }
    }

    fun setStudentVipDays(
        student: StudentProfile,
        activate: Boolean,
        planId: String,
        daysFromNow: Long,
        giftEligible: Boolean
    ) {
        viewModelScope.launch {
            _busy.value = true
            val expiry = if (activate && daysFromNow > 0L) {
                Timestamp(Date(System.currentTimeMillis() + daysFromNow * 24L * 60L * 60L * 1000L))
            } else {
                null
            }
            repository.adminUpdateStudentVip(
                studentUid = student.userId,
                vipActive = activate,
                vipPlanId = if (activate) planId else "NONE",
                vipExpiresAt = expiry,
                giftEligible = if (activate) giftEligible else false
            ).onSuccess {
                _statusMessage.value = if (activate) {
                    "VIP updated for ${student.fullName} ($daysFromNow days)."
                } else {
                    "VIP revoked for ${student.fullName}."
                }
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update VIP status"
            }
            _busy.value = false
        }
    }

    // --- WEEKLY CHAMPION DECLARATION & WINNER CONTACT ---
    fun declareWeeklyChampion(
        weekKey: String,
        weekLabel: String,
        entry: WeeklyLeaderboardEntry,
        giftDetails: String,
        specialOffer: String,
        videoCallStatus: VideoCallStatus,
        announcementNote: String,
        published: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            repository.saveWeeklyChampion(
                weekKey = weekKey,
                weekLabel = weekLabel,
                winnerUserId = entry.userId,
                winnerName = entry.displayName,
                winnerDistrict = entry.district,
                totalCorrect = entry.totalCorrect,
                totalMarks = entry.totalMarks,
                totalTimeSeconds = entry.totalTimeSeconds,
                testsCount = entry.testsCount,
                giftDetails = giftDetails,
                specialOffer = specialOffer,
                videoCallStatus = videoCallStatus,
                announcementNote = announcementNote,
                published = published
            ).onSuccess {
                _statusMessage.value = "Weekly Champion verified & announced!"
                onDone()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to declare champion"
            }
            _busy.value = false
        }
    }

    fun updateExistingChampion(
        champion: WeeklyChampion,
        giftDetails: String,
        specialOffer: String,
        videoCallStatus: VideoCallStatus,
        announcementNote: String,
        published: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            repository.saveWeeklyChampion(
                weekKey = champion.weekKey,
                weekLabel = champion.weekLabel,
                winnerUserId = champion.winnerUserId,
                winnerName = champion.winnerName,
                winnerDistrict = champion.winnerDistrict,
                totalCorrect = champion.totalCorrect,
                totalMarks = champion.totalMarks,
                totalTimeSeconds = champion.totalTimeSeconds,
                testsCount = champion.testsCount,
                giftDetails = giftDetails,
                specialOffer = specialOffer,
                videoCallStatus = videoCallStatus,
                announcementNote = announcementNote,
                published = published
            ).onSuccess {
                _statusMessage.value = "Champion details updated."
                onDone()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update champion"
            }
            _busy.value = false
        }
    }

    fun fetchWinnerPrivateContact(winnerUid: String, onResult: (StudentProfile?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val res = repository.getStudentProfileForAdmin(winnerUid)
            _busy.value = false
            res.onSuccess { onResult(it) }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to load winner contact" }
        }
    }

    // --- VIP SPECIAL NOTES ---
    fun loadVipNoteContent(noteId: String, onLoaded: (VipNoteContent?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val res = repository.getVipNoteContent(noteId)
            _busy.value = false
            res.onSuccess { onLoaded(it) }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to load note content" }
        }
    }

    fun saveVipNote(
        existingNoteId: String?,
        title: String,
        subjectName: String,
        chapterName: String,
        summary: String,
        content: String,
        keyPoints: List<String>,
        published: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            repository.saveVipNoteWithContent(
                existingNoteId = existingNoteId,
                title = title,
                subjectName = subjectName,
                chapterName = chapterName,
                summary = summary,
                content = content,
                keyPoints = keyPoints,
                published = published
            ).onSuccess {
                _statusMessage.value = "VIP Special Note saved."
                onDone()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save VIP note"
            }
            _busy.value = false
        }
    }

    fun toggleVipNotePublish(note: VipNote) {
        viewModelScope.launch {
            _busy.value = true
            repository.setVipNotePublished(note.id, !note.published)
                .onSuccess {
                    _statusMessage.value = if (!note.published) "VIP Note published." else "VIP Note unpublished."
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update note status" }
            _busy.value = false
        }
    }

    fun deleteVipNote(noteId: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.deleteVipNote(noteId)
                .onSuccess { _statusMessage.value = "VIP Note deleted." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete note" }
            _busy.value = false
        }
    }

    // --- REFERRAL & EARN MANAGEMENT ---
    fun saveReferralConfig(enabled: Boolean, rewardDays: Long, maxSuccessfulReferrals: Long) {
        viewModelScope.launch {
            _busy.value = true
            repository.updateReferralConfig(enabled, rewardDays, maxSuccessfulReferrals)
                .onSuccess {
                    _statusMessage.value = "Referral settings updated (Active: ${if (enabled) "ON" else "OFF"}, +${rewardDays}d, Max $maxSuccessfulReferrals)."
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save referral settings" }
            _busy.value = false
        }
    }

    fun approveReferral(referral: ReferralRecord, allowBeforeFirstDailyTest: Boolean = false) {
        viewModelScope.launch {
            _busy.value = true
            repository.approveReferralWithTransaction(
                referredUid = referral.referredUid,
                allowBeforeFirstDailyTest = allowBeforeFirstDailyTest
            ).onSuccess { days ->
                _statusMessage.value = "Referral approved! +$days VIP days credited to both ${referral.referrerName} & ${referral.referredName}."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to approve referral"
            }
            _busy.value = false
        }
    }

    fun rejectReferral(referredUid: String, reason: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.rejectReferral(referredUid, reason)
                .onSuccess { _statusMessage.value = "Referral rejected & logged." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to reject referral" }
            _busy.value = false
        }
    }

    fun postMessage(message: String) {
        _statusMessage.value = message
    }

    fun buildReferralReportCsv(): String {
        val referrals = (allReferralsState.value as? UiState.Success)?.data.orEmpty()
        val transactions = (allReferralTransactionsState.value as? UiState.Success)?.data.orEmpty()
        return ReferralUtils.buildReferralsCsv(referrals, transactions)
    }

    // --- DYNAMIC VIP PRICING & OFFER / PROMOTIONAL BANNER MANAGEMENT ---
    fun saveVipPlan(
        existingPlanId: String?,
        requestedPlanId: String,
        title: String,
        titleHindi: String,
        originalPriceInr: Long,
        offerPriceInr: Long,
        durationDays: Long,
        durationLabel: String,
        hasSpecialGift: Boolean,
        badge: String,
        offerName: String,
        active: Boolean,
        displayOrder: Long,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            repository.saveVipPlan(
                existingPlanId = existingPlanId,
                requestedPlanId = requestedPlanId,
                title = title,
                titleHindi = titleHindi,
                originalPriceInr = originalPriceInr,
                offerPriceInr = offerPriceInr,
                durationDays = durationDays,
                durationLabel = durationLabel,
                hasSpecialGift = hasSpecialGift,
                badge = badge,
                offerName = offerName,
                active = active,
                displayOrder = displayOrder
            ).onSuccess { savedId ->
                _statusMessage.value = "VIP प्लान ($savedId) Firebase में सेव हो गया! सभी छात्रों को तुरंत दिखेगा।"
                onDone()
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save VIP plan"
            }
            _busy.value = false
        }
    }

    fun toggleVipPlanActive(plan: DynamicVipPlan) {
        viewModelScope.launch {
            _busy.value = true
            val nextActive = !plan.active
            repository.toggleVipPlanActive(plan.id, nextActive)
                .onSuccess {
                    _statusMessage.value = if (nextActive) {
                        "${plan.title} अब Active है और छात्रों को दिखेगा।"
                    } else {
                        "${plan.title} Inactive कर दिया गया (छात्रों से छिपा दिया गया)।"
                    }
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to toggle plan status" }
            _busy.value = false
        }
    }

    fun deleteVipPlan(plan: DynamicVipPlan) {
        viewModelScope.launch {
            _busy.value = true
            repository.deleteVipPlan(plan.id)
                .onSuccess {
                    _statusMessage.value = "${plan.title} हटाया गया (मौजूदा VIP सदस्यों पर कोई प्रभाव नहीं पड़ेगा)।"
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete VIP plan" }
            _busy.value = false
        }
    }

    fun seedDefaultVipPlans() {
        viewModelScope.launch {
            _busy.value = true
            repository.seedDefaultVipPlansToFirestore()
                .onSuccess { count ->
                    _statusMessage.value = "$count डिफ़ॉल्ट VIP प्लान Firebase में लोड हो गए! अब आप उनकी कीमत बदल सकते हैं।"
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to initialize default VIP plans" }
            _busy.value = false
        }
    }

    fun saveVipOfferConfig(
        offersEnabled: Boolean,
        offerName: String,
        offerStartDate: String,
        offerEndDate: String,
        bannerEnabled: Boolean,
        bannerHeading: String,
        bannerDescription: String,
        bannerOfferText: String,
        bannerDiscountPercent: Long,
        bannerColorKey: String
    ) {
        viewModelScope.launch {
            _busy.value = true
            repository.saveVipOfferConfig(
                offersEnabled = offersEnabled,
                offerName = offerName,
                offerStartDate = offerStartDate,
                offerEndDate = offerEndDate,
                bannerEnabled = bannerEnabled,
                bannerHeading = bannerHeading,
                bannerDescription = bannerDescription,
                bannerOfferText = bannerOfferText,
                bannerDiscountPercent = bannerDiscountPercent,
                bannerColorKey = bannerColorKey
            ).onSuccess {
                _statusMessage.value = "VIP ऑफर और प्रमोशनल बैनर सेटिंग्स Firebase में लाइव सेव हो गईं!"
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to save offer & banner settings"
            }
            _busy.value = false
        }
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}
