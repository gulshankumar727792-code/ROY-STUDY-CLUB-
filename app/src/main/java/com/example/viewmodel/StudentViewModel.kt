package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DateUtils
import com.example.data.DynamicVipPlan
import com.example.data.FcmDeviceToken
import com.example.data.PaymentRequest
import com.example.data.ReferralCodeLookup
import com.example.data.ReferralConfig
import com.example.data.ReferralRecord
import com.example.data.ReferralTransaction
import com.example.data.ReferralUtils
import com.example.data.RoyStudyRepository
import com.example.data.StudentProfile
import com.example.data.StudyTest
import com.example.data.Subject
import com.example.data.TestAttempt
import com.example.data.TestAvailabilityStatus
import com.example.data.TestQuestion
import com.example.data.UiState
import com.example.data.VipNote
import com.example.data.VipNoteContent
import com.example.data.VipOfferConfig
import com.example.data.VipPlan
import com.example.data.WeeklyChampion
import com.example.data.WeeklyLeaderboardEntry
import com.example.notifications.FcmSyncManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.io.ByteArrayOutputStream

data class ActiveTestSession(
    val test: StudyTest,
    val questions: List<TestQuestion>,
    val selectedAnswers: List<Int>,
    val currentIndex: Int = 0,
    val remainingSeconds: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val submitting: Boolean = false
)

data class ReviewSession(
    val attempt: TestAttempt,
    val questions: List<TestQuestion>,
    val showAnswerAnalysis: Boolean = false
)

sealed interface ReferralCodeVerification {
    data object Idle : ReferralCodeVerification
    data object Checking : ReferralCodeVerification
    data class Valid(val lookup: ReferralCodeLookup) : ReferralCodeVerification
    data class Invalid(val message: String) : ReferralCodeVerification
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudentViewModel(
    private val repository: RoyStudyRepository,
    val currentUserId: String,
    val initialDisplayName: String?,
    val initialEmail: String?
) : ViewModel() {

    private fun <T> Flow<T>.toSafeUiState(tag: String, fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "$tag stream error", rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _selectedWeekKey = MutableStateFlow(DateUtils.currentWeekKey())
    val selectedWeekKey: StateFlow<String> = _selectedWeekKey.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _activeTest = MutableStateFlow<ActiveTestSession?>(null)
    val activeTest: StateFlow<ActiveTestSession?> = _activeTest.asStateFlow()

    private val _reviewSession = MutableStateFlow<ReviewSession?>(null)
    val reviewSession: StateFlow<ReviewSession?> = _reviewSession.asStateFlow()

    private val _openedVipNote = MutableStateFlow<Pair<VipNote, VipNoteContent>?>(null)
    val openedVipNote: StateFlow<Pair<VipNote, VipNoteContent>?> = _openedVipNote.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _codeVerification = MutableStateFlow<ReferralCodeVerification>(ReferralCodeVerification.Idle)
    val codeVerification: StateFlow<ReferralCodeVerification> = _codeVerification.asStateFlow()

    private var timerJob: Job? = null
    private var legacyBackfillAttempted = false

    val profileState: StateFlow<UiState<StudentProfile?>> =
        repository.observeStudentProfile(currentUserId)
            .toSafeUiState("profile", "Failed to load student profile")

    val subjectsState: StateFlow<UiState<List<Subject>>> =
        repository.observeActiveSubjects()
            .toSafeUiState("subjects", "Failed to load subjects")

    val publishedTestsState: StateFlow<UiState<List<StudyTest>>> =
        repository.observePublishedTests()
            .toSafeUiState("tests", "Failed to load tests")

    val myAttemptsState: StateFlow<UiState<List<TestAttempt>>> =
        repository.observeMyAttempts(currentUserId)
            .toSafeUiState("attempts", "Failed to load test history")

    val weeklyLeaderboardState: StateFlow<UiState<List<WeeklyLeaderboardEntry>>> =
        _selectedWeekKey.flatMapLatest { week ->
            repository.observeWeeklyLeaderboard(week)
        }.toSafeUiState("weeklyLeaderboard", "Failed to load weekly leaderboard")

    val championsState: StateFlow<UiState<List<WeeklyChampion>>> =
        repository.observePublishedChampions()
            .toSafeUiState("champions", "Failed to load weekly champions")

    val myPaymentsState: StateFlow<UiState<List<PaymentRequest>>> =
        repository.observeMyPaymentRequests(currentUserId)
            .toSafeUiState("payments", "Failed to load payment history")

    val vipNotesState: StateFlow<UiState<List<VipNote>>> =
        repository.observePublishedVipNotes()
            .toSafeUiState("vipNotes", "Failed to load VIP notes")

    val referralConfigState: StateFlow<UiState<ReferralConfig>> =
        repository.observeReferralConfig()
            .toSafeUiState("referralConfig", "Failed to load referral settings")

    val mySentReferralsState: StateFlow<UiState<List<ReferralRecord>>> =
        repository.observeMySentReferrals(currentUserId)
            .toSafeUiState("sentReferrals", "Failed to load your referrals")

    val myInboundReferralState: StateFlow<UiState<ReferralRecord?>> =
        repository.observeMyInboundReferral(currentUserId)
            .toSafeUiState("inboundReferral", "Failed to load referral attribution")

    val myReferralTransactionsState: StateFlow<UiState<List<ReferralTransaction>>> =
        repository.observeMyReferralTransactions(currentUserId)
            .toSafeUiState("referralTx", "Failed to load referral reward history")

    val fcmTokenState: StateFlow<UiState<FcmDeviceToken?>> =
        repository.observeFcmToken(currentUserId)
            .toSafeUiState("fcmToken", "Failed to load push notification settings")

    val vipPlansState: StateFlow<UiState<List<DynamicVipPlan>>> =
        repository.observeActiveVipPlans()
            .toSafeUiState("vipPlans", "VIP प्लान लोड करने में समस्या आई / Failed to load VIP plans")

    val vipOfferConfigState: StateFlow<UiState<VipOfferConfig>> =
        repository.observeVipOfferConfig()
            .toSafeUiState("vipOfferConfig", "ऑफर जानकारी लोड करने में समस्या आई / Failed to load VIP offers")

    init {
        viewModelScope.launch {
            _isAdmin.value = repository.checkIsAdmin(currentUserId)
        }
        viewModelScope.launch {
            profileState.collect { state ->
                val profile = (state as? UiState.Success)?.data
                if (profile != null && profile.referralCode.isBlank() && !legacyBackfillAttempted) {
                    legacyBackfillAttempted = true
                    repository.ensureLegacyReferralCode(profile)
                }
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun showMessage(msg: String) {
        _bannerMessage.value = msg
    }

    fun resetCodeVerification() {
        _codeVerification.value = ReferralCodeVerification.Idle
    }

    fun verifyReferralCode(rawCode: String) {
        val clean = ReferralUtils.normalizeCode(rawCode)
        if (clean.isEmpty()) {
            _codeVerification.value = ReferralCodeVerification.Idle
            return
        }
        if (!ReferralUtils.isValidCodeFormat(clean)) {
            _codeVerification.value = ReferralCodeVerification.Invalid("अमान्य कोड फॉर्मेट / Enter 6–16 letters or digits")
            return
        }
        viewModelScope.launch {
            _codeVerification.value = ReferralCodeVerification.Checking
            val res = repository.lookupReferralCode(clean)
            res.onSuccess { lookup ->
                when {
                    lookup == null -> {
                        _codeVerification.value = ReferralCodeVerification.Invalid("यह रेफरल कोड नहीं मिला / Code not found")
                    }
                    lookup.ownerUid == currentUserId -> {
                        _codeVerification.value = ReferralCodeVerification.Invalid("अपना स्वयं का कोड उपयोग नहीं कर सकते / Self-referral blocked")
                    }
                    else -> {
                        _codeVerification.value = ReferralCodeVerification.Valid(lookup)
                    }
                }
            }.onFailure { err ->
                _codeVerification.value = ReferralCodeVerification.Invalid(err.localizedMessage ?: "Verification failed")
            }
        }
    }

    fun completeRegistration(
        fullName: String,
        phone: String,
        district: String,
        schoolName: String,
        referralCodeInput: String
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = repository.completeNewStudentRegistration(
                userId = currentUserId,
                fullName = fullName,
                email = initialEmail,
                phone = phone,
                district = district,
                schoolName = schoolName,
                referralCodeInput = referralCodeInput
            )
            _busy.value = false
            res.onSuccess { myCode ->
                _codeVerification.value = ReferralCodeVerification.Idle
                val refUsed = ReferralUtils.normalizeCode(referralCodeInput).isNotEmpty()
                _bannerMessage.value = if (refUsed) {
                    "रजिस्ट्रेशन पूर्ण! रेफरल कोड लॉक हो गया। अपना पहला Daily Test पूरा करके VIP दिन पाएं! आपका कोड: $myCode"
                } else {
                    "रजिस्ट्रेशन पूर्ण! आपका रेफरल कोड: $myCode"
                }
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Registration failed"
            }
        }
    }

    fun setWeekKey(weekKey: String) {
        if (weekKey.isNotBlank()) {
            _selectedWeekKey.value = weekKey.trim()
        }
    }

    fun updateProfile(fullName: String, phone: String, district: String, schoolName: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.updateStudentProfile(currentUserId, fullName, phone, district, schoolName)
                .onSuccess { _bannerMessage.value = "प्रोफ़ाइल सफलतापूर्वक सेव हुई / Profile saved" }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to update profile" }
            _busy.value = false
        }
    }

    fun syncPushNotifications(
        context: Context,
        dailyEnabled: Boolean? = null,
        championEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            val existing = (fcmTokenState.value as? UiState.Success)?.data
                ?: repository.getFcmTokenDoc(currentUserId).getOrNull()
            val finalDaily = dailyEnabled ?: existing?.dailyTestAlertsEnabled ?: true
            val finalChampion = championEnabled ?: existing?.weeklyChampionAlertsEnabled ?: true
            FcmSyncManager.syncTokenAndTopics(
                context = context.applicationContext,
                repository = repository,
                userId = currentUserId,
                dailyTestAlertsEnabled = finalDaily,
                weeklyChampionAlertsEnabled = finalChampion
            )
        }
    }

    fun updateNotificationPreferences(
        context: Context,
        dailyEnabled: Boolean,
        championEnabled: Boolean
    ) {
        viewModelScope.launch {
            _busy.value = true
            val res = FcmSyncManager.syncTokenAndTopics(
                context = context.applicationContext,
                repository = repository,
                userId = currentUserId,
                dailyTestAlertsEnabled = dailyEnabled,
                weeklyChampionAlertsEnabled = championEnabled
            )
            _busy.value = false
            res.onSuccess {
                _bannerMessage.value = "नोटिफिकेशन सेटिंग्स सेव हुईं / Notification preferences updated"
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Unable to sync FCM notifications right now"
            }
        }
    }

    suspend fun cleanupFcmBeforeSignOut() {
        FcmSyncManager.clearOnSignOut(repository, currentUserId)
    }

    fun startTest(test: StudyTest, existingAttempt: TestAttempt?) {
        if (existingAttempt != null) {
            openAttemptReview(existingAttempt)
            return
        }
        when (test.availabilityStatus()) {
            TestAvailabilityStatus.UPCOMING -> {
                _bannerMessage.value = "यह टेस्ट ${test.availableFrom} से शुरू होगा / Test opens on ${test.availableFrom}."
                return
            }
            TestAvailabilityStatus.CLOSED -> {
                _bannerMessage.value = "इस टेस्ट की अंतिम तिथि (${test.closingDate}) समाप्त हो चुकी है / Test closed on ${test.closingDate}."
                return
            }
            TestAvailabilityStatus.OPEN -> Unit
        }
        viewModelScope.launch {
            _busy.value = true
            val result = repository.getTestPaper(test.id)
            _busy.value = false
            result.onSuccess { paper ->
                if (paper == null || paper.questions.isEmpty()) {
                    _bannerMessage.value = "इस टेस्ट में अभी प्रश्न उपलब्ध नहीं हैं / No questions found in this test."
                } else {
                    val totalSecs = (test.durationMinutes.coerceAtLeast(1L)) * 60L
                    _activeTest.value = ActiveTestSession(
                        test = test,
                        questions = paper.questions,
                        selectedAnswers = List(paper.questions.size) { -1 },
                        currentIndex = 0,
                        remainingSeconds = totalSecs,
                        elapsedSeconds = 0L
                    )
                    startCountdownTimer()
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Unable to open test"
                _bannerMessage.value = if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                    "यह VIP टेस्ट है — सक्रिय VIP प्लान आवश्यक है / Active VIP plan required."
                } else msg
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val session = _activeTest.value ?: break
                if (session.submitting) break
                val nextRemaining = (session.remainingSeconds - 1L).coerceAtLeast(0L)
                val nextElapsed = session.elapsedSeconds + 1L
                _activeTest.value = session.copy(
                    remainingSeconds = nextRemaining,
                    elapsedSeconds = nextElapsed
                )
                if (nextRemaining == 0L) {
                    submitActiveTest()
                    break
                }
            }
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val session = _activeTest.value ?: return
        if (session.submitting || questionIndex !in session.selectedAnswers.indices) return
        val updated = session.selectedAnswers.toMutableList()
        updated[questionIndex] = optionIndex
        _activeTest.value = session.copy(selectedAnswers = updated)
    }

    fun clearAnswer(questionIndex: Int) {
        selectAnswer(questionIndex, -1)
    }

    fun navigateQuestion(index: Int) {
        val session = _activeTest.value ?: return
        if (index in session.questions.indices) {
            _activeTest.value = session.copy(currentIndex = index)
        }
    }

    fun cancelActiveTest() {
        timerJob?.cancel()
        _activeTest.value = null
    }

    fun submitActiveTest() {
        val session = _activeTest.value ?: return
        if (session.submitting) return
        timerJob?.cancel()
        _activeTest.value = session.copy(submitting = true)

        val profile = (profileState.value as? UiState.Success)?.data
        viewModelScope.launch {
            val res = repository.submitTestAttempt(
                test = session.test,
                questions = session.questions,
                selectedAnswers = session.selectedAnswers,
                timeTakenSeconds = session.elapsedSeconds,
                profile = profile
            )
            res.onSuccess { attempt ->
                _activeTest.value = null
                _reviewSession.value = ReviewSession(
                    attempt = attempt,
                    questions = session.questions,
                    showAnswerAnalysis = false
                )
                _bannerMessage.value = "टेस्ट सफलतापूर्वक सबमिट हुआ! अंक: ${attempt.score}/${attempt.effectiveTotalMarks()} (${attempt.percentageFormatted()})"
            }.onFailure { err ->
                _activeTest.value = session.copy(submitting = false)
                _bannerMessage.value = err.localizedMessage ?: "Test submission failed"
            }
        }
    }

    fun openAttemptReview(attempt: TestAttempt) {
        viewModelScope.launch {
            _busy.value = true
            val paperRes = repository.getTestPaper(attempt.testId)
            _busy.value = false
            val questions = paperRes.getOrNull()?.questions ?: emptyList()
            _reviewSession.value = ReviewSession(
                attempt = attempt,
                questions = questions,
                showAnswerAnalysis = false
            )
        }
    }

    fun openAnswerAnalysis() {
        val current = _reviewSession.value ?: return
        _reviewSession.value = current.copy(showAnswerAnalysis = true)
    }

    fun returnToResultSummary() {
        val current = _reviewSession.value ?: return
        _reviewSession.value = current.copy(showAnswerAnalysis = false)
    }

    fun closeReview() {
        _reviewSession.value = null
    }

    fun openVipNote(note: VipNote) {
        viewModelScope.launch {
            _busy.value = true
            val res = repository.getVipNoteContent(note.id)
            _busy.value = false
            res.onSuccess { body ->
                if (body == null) {
                    _bannerMessage.value = "नोट सामग्री उपलब्ध नहीं है / Note content unavailable."
                } else {
                    _openedVipNote.value = note to body
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Cannot open VIP note"
                _bannerMessage.value = if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                    "VIP नोट्स केवल सक्रिय VIP सदस्यों के लिए हैं / Active VIP membership required."
                } else msg
            }
        }
    }

    fun closeVipNote() {
        _openedVipNote.value = null
    }

    fun submitVipPaymentRequest(
        context: Context,
        plan: VipPlan,
        transactionRef: String,
        studentPhone: String,
        note: String,
        screenshotUri: Uri?,
        onSubmitted: () -> Unit
    ) {
        val profile = (profileState.value as? UiState.Success)?.data
        val studentName = profile?.fullName?.ifBlank { "Student" } ?: "Student"
        val effectivePhone = studentPhone.trim().ifBlank { profile?.phone ?: "" }
        viewModelScope.launch {
            _busy.value = true
            val encodedScreenshot = if (screenshotUri != null) {
                encodeCompressedProof(context, screenshotUri)
            } else null

            val res = repository.submitPaymentRequest(
                plan = plan,
                studentName = studentName,
                studentPhone = effectivePhone,
                transactionRef = transactionRef,
                note = note,
                screenshotBase64 = encodedScreenshot
            )
            _busy.value = false
            res.onSuccess {
                _bannerMessage.value = "भुगतान अनुरोध सबमिट हुआ! एडमिन सत्यापन के बाद VIP सक्रिय होगा।"
                onSubmitted()
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to submit payment request"
            }
        }
    }

    private suspend fun encodeCompressedProof(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                val maxWidth = 640
                val scaled = if (original.width > maxWidth) {
                    val ratio = maxWidth.toFloat() / original.width.toFloat()
                    val targetHeight = (original.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(original, maxWidth, targetHeight, true)
                } else {
                    original
                }
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 55, baos)
                if (scaled !== original) scaled.recycle()
                original.recycle()
                val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                if (encoded.length <= 340000) encoded else null
            } catch (e: Exception) {
                Log.w(TAG, "Screenshot compression failed", e)
                null
            }
        }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "StudentVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
