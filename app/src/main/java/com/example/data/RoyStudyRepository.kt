package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class RoyStudyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        uid: String,
        op: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    // --- ADMIN VERIFICATION ---

    suspend fun checkIsAdmin(userId: String): Boolean {
        val currentUser = auth.currentUser
        if (currentUser != null &&
            currentUser.uid == userId &&
            currentUser.email.equals(AppConstants.OWNER_ADMIN_EMAIL, ignoreCase = true) &&
            currentUser.isEmailVerified
        ) {
            return true
        }
        return try {
            val doc = db.collection("admins").document(userId).get().await()
            doc.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "admins/$userId")
            false
        }
    }

    suspend fun registerAdminUid(adminUid: String, email: String, label: String): Result<Unit> {
        val cleanUid = adminUid.trim()
        val path = "admins/$cleanUid"
        return try {
            requireUserId()
            require(cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) { "Invalid UID format" }
            require(email.trim().length in 3..160) { "Invalid email" }
            val payload = mapOf(
                "uid" to cleanUid,
                "email" to email.trim(),
                "label" to label.trim().ifBlank { "Owner Admin" }.take(80),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("admins").document(cleanUid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeAdminsForAdmin(): Flow<List<AdminRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("admins")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreErrors(uid, OperationType.LIST, "admins")
        )
    }

    // --- STUDENT PROFILE ---

    fun observeStudentProfile(userId: String): Flow<StudentProfile?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(uid, OperationType.GET, "users/$userId")
        )
    }

    suspend fun ensureStudentProfile(
        userId: String,
        displayName: String?,
        email: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Session UID mismatch" }
            val ref = db.collection("users").document(userId)
            val existing = ref.get().await()
            if (!existing.exists()) {
                val safeName = displayName?.trim()?.take(80)?.ifBlank { "Bihar Board Student" }
                    ?: "Bihar Board Student"
                val safeEmail = email?.trim()?.take(160)?.ifBlank { "$userId@student.local" }
                    ?: "$userId@student.local"
                val payload = mapOf(
                    "userId" to userId,
                    "fullName" to safeName,
                    "email" to safeEmail,
                    "phone" to "",
                    "district" to "",
                    "schoolName" to "",
                    "boardClass" to AppConstants.BOARD_CLASS_FIXED,
                    "vipActive" to false,
                    "vipPlanId" to "NONE",
                    "giftEligible" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudentProfile(
        userId: String,
        fullName: String,
        phone: String,
        district: String,
        schoolName: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cannot update another user's profile" }
            val trimmedName = fullName.trim()
            require(trimmedName.length in 1..80) { "Name must be 1 to 80 characters" }
            require(phone.trim().length <= 20) { "Phone number too long" }
            require(district.trim().length <= 80) { "District name too long" }
            require(schoolName.trim().length <= 120) { "School name too long" }
            db.collection("users").document(userId).update(
                mapOf(
                    "fullName" to trimmedName,
                    "phone" to phone.trim(),
                    "district" to district.trim(),
                    "schoolName" to schoolName.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAllStudentsForAdmin(): Flow<List<StudentProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "users")
        )
    }

    suspend fun getStudentProfileForAdmin(studentUid: String): Result<StudentProfile?> {
        val path = "users/$studentUid"
        return try {
            requireUserId()
            val doc = db.collection("users").document(studentUid).get().await()
            val profile = if (doc.exists()) {
                doc.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateStudentVip(
        studentUid: String,
        vipActive: Boolean,
        vipPlanId: String,
        vipExpiresAt: Timestamp?,
        giftEligible: Boolean
    ): Result<Unit> {
        val path = "users/$studentUid"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any?>(
                "vipActive" to vipActive,
                "vipPlanId" to vipPlanId,
                "vipExpiresAt" to vipExpiresAt,
                "giftEligible" to giftEligible,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(studentUid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- SUBJECTS & CHAPTERS ---

    fun observeActiveSubjects(): Flow<List<Subject>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("subjects")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Subject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayOrder }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "subjects")
        )
    }

    fun observeAllSubjectsForAdmin(): Flow<List<Subject>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("subjects")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Subject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayOrder }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "subjects")
        )
    }

    suspend fun saveSubject(
        subjectId: String?,
        name: String,
        nameHindi: String,
        chapters: List<String>,
        displayOrder: Long,
        active: Boolean
    ): Result<Unit> {
        var path = "subjects"
        return try {
            requireUserId()
            val trimmedName = name.trim()
            require(trimmedName.length in 1..80) { "Subject name must be 1–80 characters" }
            val cleanChapters = chapters.map { it.trim() }.filter { it.isNotEmpty() }.take(60)
            val docRef = if (subjectId.isNullOrBlank()) {
                db.collection("subjects").document()
            } else {
                db.collection("subjects").document(subjectId)
            }
            path = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "name" to trimmedName,
                        "nameHindi" to nameHindi.trim().take(80),
                        "chapters" to cleanChapters,
                        "displayOrder" to displayOrder.coerceIn(0L, 100L),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "name" to trimmedName,
                        "nameHindi" to nameHindi.trim().take(80),
                        "chapters" to cleanChapters,
                        "displayOrder" to displayOrder.coerceIn(0L, 100L),
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun seedOfficialBiharBoardSyllabus(): Result<Unit> {
        return try {
            requireUserId()
            val batch = db.batch()
            for (seed in BIHAR_BOARD_CLASS_10_SYLLABUS) {
                val ref = db.collection("subjects").document(seed.id)
                batch.set(
                    ref,
                    mapOf(
                        "name" to seed.name,
                        "nameHindi" to seed.nameHindi,
                        "chapters" to seed.chapters,
                        "displayOrder" to seed.displayOrder,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "subjects")
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        val path = "subjects/$subjectId"
        return try {
            requireUserId()
            db.collection("subjects").document(subjectId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- TESTS & QUESTION PAPERS ---

    fun observePublishedTests(): Flow<List<StudyTest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("tests")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyTest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<StudyTest> { it.testDate }
                                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .logFirestoreErrors(uid, OperationType.LIST, "tests")
        )
    }

    fun observeAllTestsForAdmin(): Flow<List<StudyTest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("tests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyTest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<StudyTest> { it.testDate }
                                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .logFirestoreErrors(uid, OperationType.LIST, "tests")
        )
    }

    suspend fun getTestPaper(testId: String): Result<TestPaper?> {
        val path = "test_papers/$testId"
        return try {
            requireUserId()
            val snap = db.collection("test_papers").document(testId).get().await()
            if (!snap.exists()) return Result.success(null)
            val data = snap.data ?: return Result.success(null)
            val rawQuestions = (data["questions"] as? List<*>) ?: emptyList<Any>()
            val parsedQuestions = rawQuestions.mapNotNull { entry ->
                @Suppress("UNCHECKED_CAST")
                (entry as? Map<String, Any?>)?.let { TestQuestion.fromMap(it) }
            }
            val paper = TestPaper(
                id = snap.id,
                testId = data["testId"] as? String ?: snap.id,
                testCategory = data["testCategory"] as? String ?: TestCategory.DAILY.code,
                published = data["published"] as? Boolean ?: false,
                questions = parsedQuestions,
                createdAt = snap.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = snap.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
            Result.success(paper)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveTestWithPaper(
        existingTestId: String?,
        title: String,
        subjectId: String,
        subjectName: String,
        chapterName: String,
        testDate: String,
        durationMinutes: Long,
        testCategory: TestCategory,
        weekKey: String,
        published: Boolean,
        questions: List<TestQuestion>,
        availableFrom: String = "",
        closingDate: String = "",
        passingPercentage: Long = 0L,
        marksPerQuestion: Long = 1L
    ): Result<String> {
        var path = "tests"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            require(cleanTitle.length in 2..140) { "Test title must be 2–140 characters" }
            require(subjectId.isNotBlank() && subjectName.isNotBlank()) { "Subject is required" }
            require(chapterName.trim().length in 1..140) { "Chapter name is required" }
            require(durationMinutes in 1L..360L) { "Duration must be 1–360 minutes" }
            val maxAllowed = if (testCategory == TestCategory.CHAPTER_TEST) {
                BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS
            } else {
                BulkQuestionParser.MAX_WEEKLY_MEGA_QUESTIONS
            }
            require(questions.size <= maxAllowed) {
                "Maximum $maxAllowed questions allowed for ${testCategory.label}"
            }
            if (published) {
                require(questions.isNotEmpty()) { "Add at least 1 valid question before publishing" }
            }

            // Validate every question & prevent duplicate question submissions
            val seenQuestions = HashSet<String>()
            questions.forEachIndexed { idx, q ->
                require(q.questionText.trim().isNotEmpty()) { "Question #${idx + 1} text cannot be empty" }
                require(
                    q.optionA.trim().isNotEmpty() &&
                        q.optionB.trim().isNotEmpty() &&
                        q.optionC.trim().isNotEmpty() &&
                        q.optionD.trim().isNotEmpty()
                ) { "Question #${idx + 1} must have all 4 options (A, B, C, D)" }
                require(q.correctIndex in 0..3) { "Question #${idx + 1} must have a valid answer key (A–D)" }
                val normKey = BulkQuestionParser.normalizeQuestionKey(q.questionText)
                require(seenQuestions.add(normKey)) {
                    "Duplicate question detected at Q${idx + 1}. Please remove duplicate questions before saving."
                }
            }

            val cleanAvailFrom = availableFrom.trim().take(20)
            val cleanClosing = closingDate.trim().take(20)
            if (cleanAvailFrom.isNotEmpty() && cleanClosing.isNotEmpty()) {
                require(cleanClosing >= cleanAvailFrom) {
                    "Closing date ($cleanClosing) cannot be earlier than availability date ($cleanAvailFrom)"
                }
            }
            val safePassPct = passingPercentage.coerceIn(0L, 100L)
            val safeMarksPerQ = marksPerQuestion.coerceIn(1L, 10L)

            val testRef = if (existingTestId.isNullOrBlank()) {
                db.collection("tests").document()
            } else {
                db.collection("tests").document(existingTestId)
            }
            val testId = testRef.id
            path = testRef.path
            val paperRef = db.collection("test_papers").document(testId)
            val existingTestSnap = testRef.get().await()
            val existingPaperSnap = paperRef.get().await()

            val effectiveDate = testDate.trim().ifBlank { DateUtils.todayIso() }
            val effectiveWeekKey = weekKey.trim().ifBlank { DateUtils.weekKeyFromIsoDate(effectiveDate) }

            val batch = db.batch()
            if (existingTestSnap.exists()) {
                val updateMap = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "subjectId" to subjectId,
                    "subjectName" to subjectName.trim().take(80),
                    "chapterName" to chapterName.trim().take(140),
                    "testDate" to effectiveDate,
                    "availableFrom" to cleanAvailFrom,
                    "closingDate" to cleanClosing,
                    "durationMinutes" to durationMinutes,
                    "passingPercentage" to safePassPct,
                    "marksPerQuestion" to safeMarksPerQ,
                    "testCategory" to testCategory.code,
                    "weekKey" to effectiveWeekKey,
                    "questionCount" to questions.size.toLong(),
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.update(testRef, updateMap)
            } else {
                val createMap = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "subjectId" to subjectId,
                    "subjectName" to subjectName.trim().take(80),
                    "chapterName" to chapterName.trim().take(140),
                    "testDate" to effectiveDate,
                    "availableFrom" to cleanAvailFrom,
                    "closingDate" to cleanClosing,
                    "durationMinutes" to durationMinutes,
                    "passingPercentage" to safePassPct,
                    "marksPerQuestion" to safeMarksPerQ,
                    "testCategory" to testCategory.code,
                    "weekKey" to effectiveWeekKey,
                    "questionCount" to questions.size.toLong(),
                    "published" to published,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(testRef, createMap)
            }

            val questionMaps = questions.map { it.toMap() }
            if (existingPaperSnap.exists()) {
                batch.update(
                    paperRef,
                    mapOf(
                        "testCategory" to testCategory.code,
                        "published" to published,
                        "questions" to questionMaps,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    paperRef,
                    mapOf(
                        "testId" to testId,
                        "testCategory" to testCategory.code,
                        "published" to published,
                        "questions" to questionMaps,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(testId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setTestPublished(testId: String, published: Boolean): Result<Unit> {
        val path = "tests/$testId"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("tests").document(testId),
                mapOf("published" to published, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.update(
                db.collection("test_papers").document(testId),
                mapOf("published" to published, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTest(testId: String): Result<Unit> {
        val path = "tests/$testId"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.delete(db.collection("tests").document(testId))
            batch.delete(db.collection("test_papers").document(testId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- TEST ATTEMPTS & ATOMIC WEEKLY LEADERBOARD SCORING ---

    fun observeMyAttempts(userId: String): Flow<List<TestAttempt>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("attempts")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TestAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "attempts")
        )
    }

    fun observeAllAttemptsForAdmin(): Flow<List<TestAttempt>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("attempts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TestAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "attempts")
        )
    }

    suspend fun submitTestAttempt(
        test: StudyTest,
        questions: List<TestQuestion>,
        selectedAnswers: List<Int>,
        timeTakenSeconds: Long,
        profile: StudentProfile?
    ): Result<TestAttempt> {
        val uid = auth.currentUser?.uid
        val attemptId = "${test.id}_${uid ?: "unauth"}"
        val path = "attempts/$attemptId"
        return try {
            val currentUid = requireUserId()
            require(questions.isNotEmpty()) { "Test has no questions" }

            var correct = 0L
            var wrong = 0L
            var skipped = 0L
            val normalizedAnswers = questions.indices.map { idx ->
                val chosen = selectedAnswers.getOrElse(idx) { -1 }
                when {
                    chosen < 0 -> {
                        skipped++
                        -1L
                    }
                    chosen == questions[idx].correctIndex -> {
                        correct++
                        chosen.toLong()
                    }
                    else -> {
                        wrong++
                        chosen.toLong()
                    }
                }
            }

            val total = questions.size.toLong()
            val marksPerQ = test.effectiveMarksPerQuestion()
            val totalMarks = total * marksPerQ
            val obtainedMarks = correct * marksPerQ
            val passPct = test.passingPercentage.coerceIn(0L, 100L)
            val safeDuration = timeTakenSeconds.coerceIn(0L, 21600L)
            val studentName = profile?.fullName?.trim()?.ifBlank { "Student" }?.take(80)
                ?: auth.currentUser?.displayName?.trim()?.ifBlank { "Student" }?.take(80)
                ?: "Student"
            val weekKey = test.weekKey.ifBlank { DateUtils.weekKeyFromIsoDate(test.testDate) }

            val attemptRef = db.collection("attempts").document("${test.id}_$currentUid")
            val attemptPayload = mapOf(
                "testId" to test.id,
                "testTitle" to test.title.take(140),
                "subjectName" to test.subjectName.take(80),
                "chapterName" to test.chapterName.take(140),
                "testCategory" to test.testCategory,
                "weekKey" to weekKey,
                "userId" to currentUid,
                "studentName" to studentName,
                "selectedAnswers" to normalizedAnswers,
                "correctCount" to correct,
                "wrongCount" to wrong,
                "skippedCount" to skipped,
                "totalQuestions" to total,
                "marksPerQuestion" to marksPerQ,
                "totalMarks" to totalMarks,
                "passingPercentage" to passPct,
                "score" to obtainedMarks,
                "timeTakenSeconds" to safeDuration,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(attemptRef, attemptPayload)

            // Eligible DAILY tests atomically update the public non-PII weekly leaderboard entry
            if (test.testCategory == TestCategory.DAILY.code) {
                val entryId = "${weekKey}_$currentUid"
                val entryRef = db.collection("weekly_entries").document(entryId)
                val existingEntrySnap = entryRef.get().await()
                val district = profile?.district?.trim()?.take(80) ?: ""
                val vipBadge = profile?.hasValidVipNow() == true

                if (existingEntrySnap.exists()) {
                    val existingEntry = existingEntrySnap.toObject(
                        WeeklyLeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (existingEntry != null && test.id !in existingEntry.countedTestIds) {
                        val updatedIds = existingEntry.countedTestIds + test.id
                        batch.update(
                            entryRef,
                            mapOf(
                                "displayName" to studentName,
                                "district" to district,
                                "vipBadge" to vipBadge,
                                "totalCorrect" to (existingEntry.totalCorrect + correct),
                                "totalMarks" to (existingEntry.totalMarks + correct),
                                "totalQuestionsAttempted" to (existingEntry.totalQuestionsAttempted + total),
                                "testsCount" to updatedIds.size.toLong(),
                                "totalTimeSeconds" to (existingEntry.totalTimeSeconds + safeDuration),
                                "countedTestIds" to updatedIds,
                                "lastAttemptTestId" to test.id,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                } else {
                    batch.set(
                        entryRef,
                        mapOf(
                            "weekKey" to weekKey,
                            "userId" to currentUid,
                            "displayName" to studentName,
                            "district" to district,
                            "vipBadge" to vipBadge,
                            "totalCorrect" to correct,
                            "totalMarks" to correct,
                            "totalQuestionsAttempted" to total,
                            "testsCount" to 1L,
                            "totalTimeSeconds" to safeDuration,
                            "countedTestIds" to listOf(test.id),
                            "lastAttemptTestId" to test.id,
                            "publicVisible" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // If this student joined via a referral code and hasn't completed their first Daily Test yet,
                // atomically mark firstDailyTestCompleted = true alongside the attempt creation.
                val inboundReferralRef = db.collection("referrals").document(currentUid)
                val inboundReferralSnap = try {
                    inboundReferralRef.get().await()
                } catch (_: Exception) {
                    null
                }
                if (inboundReferralSnap != null && inboundReferralSnap.exists()) {
                    val status = inboundReferralSnap.getString("status")
                    val alreadyDone = inboundReferralSnap.getBoolean("firstDailyTestCompleted") == true
                    val alreadyRewarded = inboundReferralSnap.getBoolean("rewardGranted") == true
                    if (status == ReferralStatus.PENDING.code && !alreadyDone && !alreadyRewarded) {
                        batch.update(
                            inboundReferralRef,
                            mapOf(
                                "firstDailyTestCompleted" to true,
                                "firstDailyTestId" to test.id,
                                "firstDailyTestTitle" to test.title.take(140),
                                "firstDailyTestAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }

            batch.commit().await()

            Result.success(
                TestAttempt(
                    id = "${test.id}_$currentUid",
                    testId = test.id,
                    testTitle = test.title,
                    subjectName = test.subjectName,
                    chapterName = test.chapterName,
                    testCategory = test.testCategory,
                    weekKey = weekKey,
                    userId = currentUid,
                    studentName = studentName,
                    selectedAnswers = normalizedAnswers,
                    correctCount = correct,
                    wrongCount = wrong,
                    skippedCount = skipped,
                    totalQuestions = total,
                    marksPerQuestion = marksPerQ,
                    totalMarks = totalMarks,
                    passingPercentage = passPct,
                    score = obtainedMarks,
                    timeTakenSeconds = safeDuration,
                    createdAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- WEEKLY LEADERBOARD & CHAMPIONS ---

    fun observeWeeklyLeaderboard(weekKey: String): Flow<List<WeeklyLeaderboardEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("weekly_entries")
                .whereEqualTo("publicVisible", true)
                .whereEqualTo("weekKey", weekKey)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WeeklyLeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<WeeklyLeaderboardEntry> { it.totalMarks }
                                .thenByDescending { it.totalCorrect }
                                .thenBy { it.totalTimeSeconds }
                        )
                }
                .logFirestoreErrors(uid, OperationType.LIST, "weekly_entries")
        )
    }

    fun observePublishedChampions(): Flow<List<WeeklyChampion>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("weekly_champions")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WeeklyChampion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.weekKey }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "weekly_champions")
        )
    }

    fun observeAllChampionsForAdmin(): Flow<List<WeeklyChampion>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("weekly_champions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(WeeklyChampion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.weekKey }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "weekly_champions")
        )
    }

    suspend fun saveWeeklyChampion(
        weekKey: String,
        weekLabel: String,
        winnerUserId: String,
        winnerName: String,
        winnerDistrict: String,
        totalCorrect: Long,
        totalMarks: Long,
        totalTimeSeconds: Long,
        testsCount: Long,
        giftDetails: String,
        specialOffer: String,
        videoCallStatus: VideoCallStatus,
        announcementNote: String,
        published: Boolean
    ): Result<Unit> {
        val cleanWeekKey = weekKey.trim()
        val path = "weekly_champions/$cleanWeekKey"
        return try {
            val adminUid = requireUserId()
            require(cleanWeekKey.matches(Regex("^[a-zA-Z0-9_\\-]{6,16}$"))) { "Invalid week key" }
            val ref = db.collection("weekly_champions").document(cleanWeekKey)
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "weekLabel" to weekLabel.trim().ifBlank { cleanWeekKey }.take(80),
                        "winnerUserId" to winnerUserId.trim().take(128),
                        "winnerName" to winnerName.trim().take(80),
                        "winnerDistrict" to winnerDistrict.trim().take(80),
                        "totalCorrect" to totalCorrect.coerceAtLeast(0L),
                        "totalMarks" to totalMarks.coerceAtLeast(0L),
                        "totalTimeSeconds" to totalTimeSeconds.coerceAtLeast(0L),
                        "testsCount" to testsCount.coerceIn(0L, 31L),
                        "giftDetails" to giftDetails.trim().take(300),
                        "specialOffer" to specialOffer.trim().take(300),
                        "videoCallStatus" to videoCallStatus.code,
                        "announcementNote" to announcementNote.trim().take(500),
                        "verifiedBy" to adminUid,
                        "published" to published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "weekKey" to cleanWeekKey,
                        "weekLabel" to weekLabel.trim().ifBlank { cleanWeekKey }.take(80),
                        "winnerUserId" to winnerUserId.trim().take(128),
                        "winnerName" to winnerName.trim().take(80),
                        "winnerDistrict" to winnerDistrict.trim().take(80),
                        "totalCorrect" to totalCorrect.coerceAtLeast(0L),
                        "totalMarks" to totalMarks.coerceAtLeast(0L),
                        "totalTimeSeconds" to totalTimeSeconds.coerceAtLeast(0L),
                        "testsCount" to testsCount.coerceIn(0L, 31L),
                        "giftDetails" to giftDetails.trim().take(300),
                        "specialOffer" to specialOffer.trim().take(300),
                        "videoCallStatus" to videoCallStatus.code,
                        "announcementNote" to announcementNote.trim().take(500),
                        "verifiedBy" to adminUid,
                        "published" to published,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- VIP WHATSAPP PAYMENTS & PROOFS ---

    fun observeMyPaymentRequests(userId: String): Flow<List<PaymentRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("payment_requests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "payment_requests")
        )
    }

    fun observeAllPaymentRequestsForAdmin(): Flow<List<PaymentRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("payment_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "payment_requests")
        )
    }

    suspend fun submitPaymentRequest(
        plan: VipPlan,
        studentName: String,
        studentPhone: String,
        transactionRef: String,
        note: String,
        screenshotBase64: String?
    ): Result<String> {
        var path = "payment_requests"
        return try {
            val uid = requireUserId()
            val cleanRef = transactionRef.trim()
            require(cleanRef.length in 2..80) { "Please enter a valid UTR / Transaction Reference (2–80 chars)" }
            val cleanName = studentName.trim().ifBlank { "Student" }.take(80)
            val cleanPhone = studentPhone.trim().take(20)
            val cleanNote = note.trim().take(300)
            val hasProof = !screenshotBase64.isNullOrBlank() && screenshotBase64.length <= 350000

            // Server-authoritative live pricing verification (never trust client-cached price alone)
            val livePlanSnap = try {
                db.collection("vip_plans").document(plan.id).get(Source.SERVER).await()
            } catch (_: Exception) {
                db.collection("vip_plans").document(plan.id).get().await()
            }
            val liveConfigSnap = try {
                db.collection("vip_offer_config").document("settings").get(Source.SERVER).await()
            } catch (_: Exception) {
                db.collection("vip_offer_config").document("settings").get().await()
            }
            val liveOfferConfig = if (liveConfigSnap.exists()) {
                liveConfigSnap.toObject(VipOfferConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: VipOfferConfig()
            } else {
                VipOfferConfig()
            }

            val verifiedPlanTitle: String
            val verifiedAmountInr: Long
            val verifiedOriginalPriceInr: Long
            val verifiedDiscountPercent: Long
            val verifiedDurationDays: Long
            val verifiedOfferName: String
            val verifiedHasSpecialGift: Boolean

            if (livePlanSnap.exists()) {
                val livePlan = livePlanSnap.toObject(
                    DynamicVipPlan::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Unable to verify VIP plan from server.")
                if (!livePlan.active) {
                    throw IllegalStateException("यह VIP प्लान अभी सक्रिय नहीं है / This VIP plan is currently inactive.")
                }
                val offerActiveNow = liveOfferConfig.isOfferActiveNow()
                val expectedAmount = livePlan.effectivePrice(offerActiveNow)
                val expectedDiscount = livePlan.effectiveDiscountPercent(offerActiveNow)
                if (expectedAmount != plan.amountInr || livePlan.durationDays != plan.durationDays) {
                    throw IllegalStateException(
                        "प्लान की कीमत या ऑफर अभी अपडेट हुआ है (नया मूल्य: ₹$expectedAmount)। कृपया पेज रिफ्रेश करके पुनः सबमिट करें।"
                    )
                }
                verifiedPlanTitle = livePlan.title.ifBlank { plan.title }.take(80)
                verifiedAmountInr = expectedAmount
                verifiedOriginalPriceInr = livePlan.originalPriceInr.coerceAtLeast(expectedAmount)
                verifiedDiscountPercent = expectedDiscount
                verifiedDurationDays = livePlan.durationDays
                verifiedOfferName = if (expectedDiscount > 0L) {
                    livePlan.offerName.trim().ifBlank { liveOfferConfig.offerName.trim() }.take(120)
                } else {
                    ""
                }
                verifiedHasSpecialGift = livePlan.hasSpecialGift
            } else {
                val legacyPlan = OFFICIAL_VIP_PLANS.firstOrNull { it.id == plan.id }
                    ?: throw IllegalStateException("अमान्य या निष्क्रिय VIP प्लान / Selected VIP plan is no longer available.")
                if (legacyPlan.amountInr != plan.amountInr || legacyPlan.durationDays != plan.durationDays) {
                    throw IllegalStateException("प्लान की कीमत मेल नहीं खाती (सही मूल्य: ₹${legacyPlan.amountInr})।")
                }
                verifiedPlanTitle = legacyPlan.title.take(80)
                verifiedAmountInr = legacyPlan.amountInr
                verifiedOriginalPriceInr = legacyPlan.amountInr
                verifiedDiscountPercent = 0L
                verifiedDurationDays = legacyPlan.durationDays
                verifiedOfferName = ""
                verifiedHasSpecialGift = legacyPlan.hasSpecialGift
            }

            val reqRef = db.collection("payment_requests").document()
            val reqId = reqRef.id
            path = reqRef.path

            val reqPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "studentName" to cleanName,
                "studentPhone" to cleanPhone,
                "planId" to plan.id,
                "planName" to verifiedPlanTitle,
                "amountInr" to verifiedAmountInr,
                "originalPriceInr" to verifiedOriginalPriceInr,
                "discountPercent" to verifiedDiscountPercent,
                "appliedOfferName" to verifiedOfferName,
                "hasSpecialGift" to verifiedHasSpecialGift,
                "durationDays" to verifiedDurationDays,
                "transactionRef" to cleanRef,
                "hasScreenshot" to hasProof,
                "status" to PaymentStatus.PENDING.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote.isNotEmpty()) {
                reqPayload["note"] = cleanNote
            }

            val batch = db.batch()
            batch.set(reqRef, reqPayload)
            if (hasProof && screenshotBase64 != null) {
                val proofRef = db.collection("payment_proofs").document(reqId)
                batch.set(
                    proofRef,
                    mapOf(
                        "requestId" to reqId,
                        "userId" to uid,
                        "imageBase64" to screenshotBase64,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(reqId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun getPaymentProofBase64(requestId: String): Result<String?> {
        val path = "payment_proofs/$requestId"
        return try {
            requireUserId()
            val snap = db.collection("payment_proofs").document(requestId).get().await()
            Result.success(snap.getString("imageBase64"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun approvePaymentRequest(
        request: PaymentRequest,
        adminRemark: String
    ): Result<Unit> {
        val path = "payment_requests/${request.id}"
        return try {
            val adminUid = requireUserId()
            val userRef = db.collection("users").document(request.userId)
            val userSnap = userRef.get().await()
            val existingProfile = if (userSnap.exists()) {
                userSnap.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null

            val nowMillis = System.currentTimeMillis()
            val baseMillis = if (existingProfile != null && existingProfile.hasValidVipNow(nowMillis)) {
                existingProfile.vipExpiresAt!!.toDate().time
            } else {
                nowMillis
            }
            val addedMillis = request.durationDays.coerceAtLeast(1L) * 24L * 60L * 60L * 1000L
            val expiryTimestamp = Timestamp(Date(baseMillis + addedMillis))
            val isGiftEligible = request.hasSpecialGift || request.planId == "PLAN_12M" || (existingProfile?.giftEligible == true)

            val batch = db.batch()
            batch.update(
                db.collection("payment_requests").document(request.id),
                mapOf(
                    "status" to PaymentStatus.APPROVED.code,
                    "adminRemark" to adminRemark.trim().ifBlank { "Payment verified & VIP activated" }.take(300),
                    "reviewedBy" to adminUid,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "activatedUntil" to expiryTimestamp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "vipActive" to true,
                    "vipPlanId" to request.planId,
                    "vipExpiresAt" to expiryTimestamp,
                    "giftEligible" to isGiftEligible,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectPaymentRequest(
        requestId: String,
        adminRemark: String
    ): Result<Unit> {
        val path = "payment_requests/$requestId"
        return try {
            val adminUid = requireUserId()
            db.collection("payment_requests").document(requestId).update(
                mapOf(
                    "status" to PaymentStatus.REJECTED.code,
                    "adminRemark" to adminRemark.trim().ifBlank { "Payment could not be verified" }.take(300),
                    "reviewedBy" to adminUid,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- VIP SPECIAL NOTES ---

    fun observePublishedVipNotes(): Flow<List<VipNote>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("vip_notes")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(VipNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "vip_notes")
        )
    }

    fun observeAllVipNotesForAdmin(): Flow<List<VipNote>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("vip_notes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(VipNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "vip_notes")
        )
    }

    suspend fun getVipNoteContent(noteId: String): Result<VipNoteContent?> {
        val path = "vip_note_contents/$noteId"
        return try {
            requireUserId()
            val snap = db.collection("vip_note_contents").document(noteId).get().await()
            val content = if (snap.exists()) {
                snap.toObject(VipNoteContent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(content)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveVipNoteWithContent(
        existingNoteId: String?,
        title: String,
        subjectName: String,
        chapterName: String,
        summary: String,
        content: String,
        keyPoints: List<String>,
        published: Boolean
    ): Result<String> {
        var path = "vip_notes"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanContent = content.trim()
            require(cleanTitle.length in 2..140) { "Note title must be 2–140 characters" }
            require(cleanContent.length in 1..20000) { "Note content is required" }

            val noteRef = if (existingNoteId.isNullOrBlank()) {
                db.collection("vip_notes").document()
            } else {
                db.collection("vip_notes").document(existingNoteId)
            }
            val noteId = noteRef.id
            path = noteRef.path
            val bodyRef = db.collection("vip_note_contents").document(noteId)
            val existingNoteSnap = noteRef.get().await()
            val existingBodySnap = bodyRef.get().await()
            val cleanPoints = keyPoints.map { it.trim() }.filter { it.isNotEmpty() }.take(30)

            val batch = db.batch()
            if (existingNoteSnap.exists()) {
                batch.update(
                    noteRef,
                    mapOf(
                        "title" to cleanTitle,
                        "subjectName" to subjectName.trim().take(80),
                        "chapterName" to chapterName.trim().take(140),
                        "summary" to summary.trim().take(300),
                        "published" to published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    noteRef,
                    mapOf(
                        "title" to cleanTitle,
                        "subjectName" to subjectName.trim().take(80),
                        "chapterName" to chapterName.trim().take(140),
                        "summary" to summary.trim().take(300),
                        "published" to published,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (existingBodySnap.exists()) {
                batch.update(
                    bodyRef,
                    mapOf(
                        "content" to cleanContent,
                        "keyPoints" to cleanPoints,
                        "published" to published,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    bodyRef,
                    mapOf(
                        "noteId" to noteId,
                        "content" to cleanContent,
                        "keyPoints" to cleanPoints,
                        "published" to published,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(noteId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setVipNotePublished(noteId: String, published: Boolean): Result<Unit> {
        val path = "vip_notes/$noteId"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("vip_notes").document(noteId),
                mapOf("published" to published, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.update(
                db.collection("vip_note_contents").document(noteId),
                mapOf("published" to published, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVipNote(noteId: String): Result<Unit> {
        val path = "vip_notes/$noteId"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.delete(db.collection("vip_notes").document(noteId))
            batch.delete(db.collection("vip_note_contents").document(noteId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- REFERRAL & EARN SYSTEM ---

    private suspend fun allocateUniqueReferralCode(userId: String): String {
        for (attempt in 0..15) {
            val candidate = ReferralUtils.generateUniqueReferralCode(userId, attempt)
            val snap = db.collection("referral_codes").document(candidate).get().await()
            if (!snap.exists() || snap.getString("ownerUid") == userId) {
                return candidate
            }
        }
        return ReferralUtils.generateUniqueReferralCode(userId, (System.currentTimeMillis() % 10000L).toInt())
    }

    fun observeReferralConfig(): Flow<ReferralConfig> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("referral_config").document("settings")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(ReferralConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: ReferralConfig()
                    } else {
                        ReferralConfig()
                    }
                }
                .logFirestoreErrors(uid, OperationType.GET, "referral_config/settings")
        )
    }

    suspend fun updateReferralConfig(
        enabled: Boolean,
        rewardDays: Long,
        maxSuccessfulReferrals: Long
    ): Result<Unit> {
        val path = "referral_config/settings"
        return try {
            val adminUid = requireUserId()
            val safeRewardDays = rewardDays.coerceIn(1L, 365L)
            val safeMax = maxSuccessfulReferrals.coerceIn(1L, 500L)
            db.collection("referral_config").document("settings").set(
                mapOf(
                    "enabled" to enabled,
                    "rewardDays" to safeRewardDays,
                    "maxSuccessfulReferrals" to safeMax,
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun lookupReferralCode(rawCode: String): Result<ReferralCodeLookup?> {
        val cleanCode = ReferralUtils.normalizeCode(rawCode)
        val path = "referral_codes/$cleanCode"
        return try {
            requireUserId()
            if (!ReferralUtils.isValidCodeFormat(cleanCode)) {
                return Result.failure(IllegalArgumentException("Invalid referral code format (6–16 letters/numbers)"))
            }
            val snap = db.collection("referral_codes").document(cleanCode).get().await()
            if (!snap.exists()) {
                return Result.success(null)
            }
            val lookup = snap.toObject(ReferralCodeLookup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (lookup == null || !lookup.active) {
                return Result.success(null)
            }
            Result.success(lookup)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun completeNewStudentRegistration(
        userId: String,
        fullName: String,
        email: String?,
        phone: String,
        district: String,
        schoolName: String,
        referralCodeInput: String
    ): Result<String> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Session UID mismatch" }

            val userRef = db.collection("users").document(userId)
            val existingUserSnap = userRef.get().await()
            if (existingUserSnap.exists()) {
                val existingCode = existingUserSnap.getString("referralCode").orEmpty()
                return Result.success(existingCode)
            }

            val cleanName = fullName.trim().take(80)
            require(cleanName.isNotEmpty()) { "कृपया अपना पूरा नाम लिखें / Please enter your full name." }
            val cleanEmail = email?.trim()?.take(160)?.ifBlank { "$userId@student.local" } ?: "$userId@student.local"
            val cleanPhone = phone.trim().take(20)
            val cleanDistrict = district.trim().take(80)
            val cleanSchool = schoolName.trim().take(120)

            val trimmedRefInput = ReferralUtils.normalizeCode(referralCodeInput)
            var matchedLookup: ReferralCodeLookup? = null
            if (trimmedRefInput.isNotEmpty()) {
                require(ReferralUtils.isValidCodeFormat(trimmedRefInput)) {
                    "अमान्य रेफरल कोड / Invalid referral code format."
                }
                val configSnap = db.collection("referral_config").document("settings").get().await()
                val referralsEnabled = !configSnap.exists() || configSnap.getBoolean("enabled") != false
                require(referralsEnabled) {
                    "रेफरल प्रोग्राम अभी बंद है / Referral program is currently paused by Owner Admin."
                }
                val codeSnap = db.collection("referral_codes").document(trimmedRefInput).get().await()
                require(codeSnap.exists()) {
                    "यह रेफरल कोड मौजूद नहीं है / Referral code not found."
                }
                val lookup = codeSnap.toObject(ReferralCodeLookup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                require(lookup != null && lookup.active) {
                    "यह रेफरल कोड सक्रिय नहीं है / Referral code is inactive."
                }
                require(lookup.ownerUid != userId) {
                    "आप अपना स्वयं का रेफरल कोड उपयोग नहीं कर सकते / Self-referral is not allowed."
                }
                matchedLookup = lookup
            }

            val myCode = allocateUniqueReferralCode(userId)
            val myCodeRef = db.collection("referral_codes").document(myCode)

            val userPayload = mutableMapOf<String, Any>(
                "userId" to userId,
                "fullName" to cleanName,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "district" to cleanDistrict,
                "schoolName" to cleanSchool,
                "boardClass" to AppConstants.BOARD_CLASS_FIXED,
                "vipActive" to false,
                "vipPlanId" to "NONE",
                "giftEligible" to false,
                "referralCode" to myCode,
                "referredByUid" to (matchedLookup?.ownerUid ?: ""),
                "referredByCode" to (matchedLookup?.code ?: ""),
                "referralLocked" to true,
                "successfulReferrals" to 0L,
                "earnedVipDays" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(userRef, userPayload)
            batch.set(
                myCodeRef,
                mapOf(
                    "code" to myCode,
                    "ownerUid" to userId,
                    "ownerName" to cleanName,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            if (matchedLookup != null) {
                val referralRef = db.collection("referrals").document(userId)
                batch.set(
                    referralRef,
                    mapOf(
                        "referrerUid" to matchedLookup.ownerUid,
                        "referrerName" to matchedLookup.ownerName.ifBlank { "Student" }.take(80),
                        "referredUid" to userId,
                        "referredName" to cleanName,
                        "referralCode" to matchedLookup.code,
                        "status" to ReferralStatus.PENDING.code,
                        "firstDailyTestCompleted" to false,
                        "rewardGranted" to false,
                        "rewardDays" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(myCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureLegacyReferralCode(profile: StudentProfile): Result<String> {
        val path = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Session UID mismatch" }
            if (profile.referralCode.isNotBlank()) {
                return Result.success(profile.referralCode)
            }
            val myCode = allocateUniqueReferralCode(uid)
            val userRef = db.collection("users").document(uid)
            val codeRef = db.collection("referral_codes").document(myCode)
            val existingCodeSnap = codeRef.get().await()

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "referralCode" to myCode,
                    "referralLocked" to true,
                    "successfulReferrals" to 0L,
                    "earnedVipDays" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (!existingCodeSnap.exists()) {
                batch.set(
                    codeRef,
                    mapOf(
                        "code" to myCode,
                        "ownerUid" to uid,
                        "ownerName" to profile.fullName.ifBlank { "Student" }.take(80),
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(myCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeMySentReferrals(userId: String): Flow<List<ReferralRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("referrals")
                .whereEqualTo("referrerUid", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "referrals")
        )
    }

    fun observeMyInboundReferral(userId: String): Flow<ReferralRecord?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("referrals").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(uid, OperationType.GET, "referrals/$userId")
        )
    }

    fun observeMyReferralTransactions(userId: String): Flow<List<ReferralTransaction>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("referral_transactions")
                .whereEqualTo("beneficiaryUid", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReferralTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "referral_transactions")
        )
    }

    fun observeAllReferralsForAdmin(): Flow<List<ReferralRecord>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("referrals")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "referrals")
        )
    }

    fun observeAllReferralTransactionsForAdmin(): Flow<List<ReferralTransaction>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("referral_transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReferralTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "referral_transactions")
        )
    }

    suspend fun approveReferralWithTransaction(
        referredUid: String,
        allowBeforeFirstDailyTest: Boolean = false
    ): Result<Long> {
        val path = "referrals/$referredUid"
        return try {
            val adminUid = requireUserId()
            val referralRef = db.collection("referrals").document(referredUid)
            val configRef = db.collection("referral_config").document("settings")
            val referrerTxRef = db.collection("referral_transactions").document("ref_${referredUid}_referrer")
            val referredTxRef = db.collection("referral_transactions").document("ref_${referredUid}_referred")

            val creditedDays = db.runTransaction { tx ->
                val refSnap = tx.get(referralRef)
                if (!refSnap.exists()) {
                    throw IllegalStateException("Referral record not found.")
                }
                val referral = refSnap.toObject(ReferralRecord::class.java)
                    ?: throw IllegalStateException("Corrupt referral record.")

                if (referral.rewardGranted || referral.status != ReferralStatus.PENDING.code) {
                    throw IllegalStateException("Referral already processed (${referral.status}). Duplicate reward blocked.")
                }
                if (referral.referrerUid == referral.referredUid) {
                    throw IllegalStateException("Self-referral cannot be rewarded.")
                }
                if (!allowBeforeFirstDailyTest && !referral.firstDailyTestCompleted) {
                    throw IllegalStateException("Referred student has not completed their first Daily Test yet.")
                }

                val configSnap = tx.get(configRef)
                val rewardDays = (configSnap.getLong("rewardDays") ?: 7L).coerceIn(1L, 365L)
                val maxSuccessful = (configSnap.getLong("maxSuccessfulReferrals") ?: 20L).coerceIn(1L, 500L)

                val referrerRef = db.collection("users").document(referral.referrerUid)
                val referredRef = db.collection("users").document(referral.referredUid)

                val referrerSnap = tx.get(referrerRef)
                val referredSnap = tx.get(referredRef)
                val referrerTxSnap = tx.get(referrerTxRef)
                val referredTxSnap = tx.get(referredTxRef)

                if (referrerTxSnap.exists() || referredTxSnap.exists()) {
                    throw IllegalStateException("Reward transaction already exists. Duplicate reward prevented.")
                }
                if (!referrerSnap.exists() || !referredSnap.exists()) {
                    throw IllegalStateException("Referrer or referred student profile not found.")
                }

                val referrerProfile = referrerSnap.toObject(StudentProfile::class.java)!!
                val referredProfile = referredSnap.toObject(StudentProfile::class.java)!!

                if (referrerProfile.successfulReferrals >= maxSuccessful) {
                    throw IllegalStateException("Referrer reached maximum limit ($maxSuccessful successful referrals).")
                }

                val nowMillis = System.currentTimeMillis()
                val addedMillis = rewardDays * 24L * 60L * 60L * 1000L

                val referrerBase = if (referrerProfile.hasValidVipNow(nowMillis)) {
                    referrerProfile.vipExpiresAt!!.toDate().time
                } else nowMillis
                val referrerExpiry = Timestamp(Date(referrerBase + addedMillis))
                val referrerPlan = if (referrerProfile.vipPlanId.isNotBlank() && referrerProfile.vipPlanId != "NONE") {
                    referrerProfile.vipPlanId
                } else "REFERRAL_BONUS"

                val referredBase = if (referredProfile.hasValidVipNow(nowMillis)) {
                    referredProfile.vipExpiresAt!!.toDate().time
                } else nowMillis
                val referredExpiry = Timestamp(Date(referredBase + addedMillis))
                val referredPlan = if (referredProfile.vipPlanId.isNotBlank() && referredProfile.vipPlanId != "NONE") {
                    referredProfile.vipPlanId
                } else "REFERRAL_BONUS"

                val referrerName = referrerProfile.fullName.ifBlank { referral.referrerName }.take(80)
                val referredName = referredProfile.fullName.ifBlank { referral.referredName }.take(80)

                tx.update(
                    referralRef,
                    mapOf(
                        "status" to ReferralStatus.SUCCESSFUL.code,
                        "rewardGranted" to true,
                        "rewardDays" to rewardDays,
                        "processedBy" to adminUid,
                        "rewardedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.update(
                    referrerRef,
                    mapOf(
                        "vipActive" to true,
                        "vipPlanId" to referrerPlan,
                        "vipExpiresAt" to referrerExpiry,
                        "successfulReferrals" to (referrerProfile.successfulReferrals + 1L),
                        "earnedVipDays" to (referrerProfile.earnedVipDays + rewardDays),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.update(
                    referredRef,
                    mapOf(
                        "vipActive" to true,
                        "vipPlanId" to referredPlan,
                        "vipExpiresAt" to referredExpiry,
                        "earnedVipDays" to (referredProfile.earnedVipDays + rewardDays),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    referrerTxRef,
                    mapOf(
                        "referralId" to referredUid,
                        "beneficiaryUid" to referral.referrerUid,
                        "beneficiaryName" to referrerName,
                        "counterpartyUid" to referral.referredUid,
                        "counterpartyName" to referredName,
                        "role" to "REFERRER",
                        "referralCode" to referral.referralCode,
                        "rewardDays" to rewardDays,
                        "vipExpiresAfter" to referrerExpiry,
                        "processedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    referredTxRef,
                    mapOf(
                        "referralId" to referredUid,
                        "beneficiaryUid" to referral.referredUid,
                        "beneficiaryName" to referredName,
                        "counterpartyUid" to referral.referrerUid,
                        "counterpartyName" to referrerName,
                        "role" to "REFERRED",
                        "referralCode" to referral.referralCode,
                        "rewardDays" to rewardDays,
                        "vipExpiresAfter" to referredExpiry,
                        "processedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                rewardDays
            }.await()

            Result.success(creditedDays)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectReferral(referredUid: String, reason: String): Result<Unit> {
        val path = "referrals/$referredUid"
        return try {
            val adminUid = requireUserId()
            val cleanReason = reason.trim().take(300)
            require(cleanReason.length >= 3) { "Please enter a clear rejection reason (at least 3 characters)." }

            val ref = db.collection("referrals").document(referredUid)
            val snap = ref.get().await()
            require(snap.exists()) { "Referral record not found." }
            require(snap.getBoolean("rewardGranted") != true) {
                "Cannot reject a referral whose reward has already been granted."
            }

            ref.update(
                mapOf(
                    "status" to ReferralStatus.REJECTED.code,
                    "rewardGranted" to false,
                    "rewardDays" to 0L,
                    "rejectionReason" to cleanReason,
                    "processedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FCM PUSH NOTIFICATIONS & SERVER DISPATCH LOGS ---

    fun observeFcmToken(userId: String): Flow<FcmDeviceToken?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("fcm_tokens").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(FcmDeviceToken::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(uid, OperationType.GET, "fcm_tokens/$userId")
        )
    }

    suspend fun getFcmTokenDoc(userId: String): Result<FcmDeviceToken?> {
        val path = "fcm_tokens/$userId"
        return try {
            requireUserId()
            val snap = db.collection("fcm_tokens").document(userId).get().await()
            val doc = if (snap.exists()) {
                snap.toObject(FcmDeviceToken::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun upsertFcmToken(
        userId: String,
        token: String,
        dailyTestAlertsEnabled: Boolean,
        weeklyChampionAlertsEnabled: Boolean
    ): Result<Unit> {
        val path = "fcm_tokens/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Cannot update another student's FCM device token" }
            val cleanToken = token.trim()
            require(cleanToken.length in 10..4096) { "Invalid FCM registration token" }

            val payload = mapOf(
                "userId" to userId,
                "token" to cleanToken,
                "dailyTestAlertsEnabled" to dailyTestAlertsEnabled,
                "weeklyChampionAlertsEnabled" to weeklyChampionAlertsEnabled,
                "platform" to "android",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("fcm_tokens").document(userId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFcmToken(userId: String): Result<Unit> {
        val path = "fcm_tokens/$userId"
        return try {
            requireUserId()
            db.collection("fcm_tokens").document(userId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeNotificationLogsForAdmin(): Flow<List<NotificationLog>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("notification_logs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NotificationLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, "notification_logs")
        )
    }

    // --- DYNAMIC VIP PRICING & OFFER / PROMOTIONAL BANNER MANAGEMENT ---

    fun observeActiveVipPlans(): Flow<List<DynamicVipPlan>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("vip_plans")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DynamicVipPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<DynamicVipPlan> { it.displayOrder }.thenBy { it.durationDays })
                }
                .logFirestoreErrors(uid, OperationType.LIST, "vip_plans")
        )
    }

    fun observeAllVipPlansForAdmin(): Flow<List<DynamicVipPlan>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("vip_plans")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DynamicVipPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<DynamicVipPlan> { it.displayOrder }.thenBy { it.durationDays })
                }
                .logFirestoreErrors(uid, OperationType.LIST, "vip_plans")
        )
    }

    fun observeVipOfferConfig(): Flow<VipOfferConfig> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("vip_offer_config")
                .document("settings")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(VipOfferConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: VipOfferConfig()
                    } else {
                        VipOfferConfig()
                    }
                }
                .logFirestoreErrors(uid, OperationType.GET, "vip_offer_config/settings")
        )
    }

    suspend fun saveVipPlan(
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
        displayOrder: Long
    ): Result<String> {
        val docId = existingPlanId?.takeIf { it.isNotBlank() }
            ?: VipPricingUtils.sanitizePlanId(requestedPlanId)
        val path = "vip_plans/$docId"
        return try {
            requireUserId()
            val cleanTitle = title.trim()
            require(cleanTitle.length in 2..80) { "Plan title must be 2–80 characters" }
            require(originalPriceInr in 1L..100000L) { "Old price (MRP) must be ₹1–₹100,000" }
            require(offerPriceInr in 1L..originalPriceInr) { "New offer price must be ₹1 to Old Price (₹$originalPriceInr)" }
            require(durationDays in 1L..1825L) { "Duration must be 1–1825 days" }
            val calcDiscount = VipPricingUtils.calculateDiscountPercent(originalPriceInr, offerPriceInr)
            val cleanLabel = durationLabel.trim().ifBlank { "$durationDays Days" }.take(60)

            val docRef = db.collection("vip_plans").document(docId)
            val existingSnap = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "title" to cleanTitle,
                "titleHindi" to titleHindi.trim().take(80),
                "originalPriceInr" to originalPriceInr,
                "offerPriceInr" to offerPriceInr,
                "discountPercent" to calcDiscount,
                "durationDays" to durationDays,
                "durationLabel" to cleanLabel,
                "hasSpecialGift" to hasSpecialGift,
                "badge" to badge.trim().take(80),
                "offerName" to offerName.trim().take(120),
                "active" to active,
                "displayOrder" to displayOrder.coerceIn(0L, 999L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingSnap.exists()) {
                val created = existingSnap.getTimestamp("createdAt")
                payload["createdAt"] = created ?: FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleVipPlanActive(planId: String, active: Boolean): Result<Unit> {
        val path = "vip_plans/$planId"
        return try {
            requireUserId()
            db.collection("vip_plans").document(planId).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVipPlan(planId: String): Result<Unit> {
        val path = "vip_plans/$planId"
        return try {
            requireUserId()
            db.collection("vip_plans").document(planId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultVipPlansToFirestore(): Result<Int> {
        val path = "vip_plans"
        return try {
            requireUserId()
            val defaults = VipPricingUtils.defaultDynamicVipPlans()
            val batch = db.batch()
            defaults.forEach { plan ->
                val ref = db.collection("vip_plans").document(plan.id)
                batch.set(
                    ref,
                    mapOf(
                        "title" to plan.title,
                        "titleHindi" to plan.titleHindi,
                        "originalPriceInr" to plan.originalPriceInr,
                        "offerPriceInr" to plan.offerPriceInr,
                        "discountPercent" to plan.discountPercent,
                        "durationDays" to plan.durationDays,
                        "durationLabel" to plan.durationLabel,
                        "hasSpecialGift" to plan.hasSpecialGift,
                        "badge" to plan.badge,
                        "offerName" to plan.offerName,
                        "active" to plan.active,
                        "displayOrder" to plan.displayOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(defaults.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveVipOfferConfig(
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
    ): Result<Unit> {
        val path = "vip_offer_config/settings"
        return try {
            val adminUid = requireUserId()
            val cleanStart = offerStartDate.trim()
            val cleanEnd = offerEndDate.trim()
            val startMillis = if (cleanStart.isNotEmpty()) {
                val parsed = VipPricingUtils.isoDateToStartOfDayMillis(cleanStart)
                require(parsed > 0L) { "Invalid Start Date format. Use YYYY-MM-DD (e.g. 2026-05-01)." }
                parsed
            } else 0L
            val endMillis = if (cleanEnd.isNotEmpty()) {
                val parsed = VipPricingUtils.isoDateToEndOfDayMillis(cleanEnd)
                require(parsed > 0L) { "Invalid End Date format. Use YYYY-MM-DD (e.g. 2026-05-31)." }
                parsed
            } else 0L
            if (startMillis > 0L && endMillis > 0L) {
                require(endMillis >= startMillis) { "Offer End Date must be on or after Offer Start Date." }
            }

            val safeColorKey = BannerColorTheme.fromKey(bannerColorKey).key
            val payload = mapOf(
                "offersEnabled" to offersEnabled,
                "offerName" to offerName.trim().take(120),
                "offerStartDate" to cleanStart.take(30),
                "offerEndDate" to cleanEnd.take(30),
                "offerStartMillis" to startMillis,
                "offerEndMillis" to endMillis,
                "bannerEnabled" to bannerEnabled,
                "bannerHeading" to bannerHeading.trim().take(120),
                "bannerDescription" to bannerDescription.trim().take(240),
                "bannerOfferText" to bannerOfferText.trim().take(100),
                "bannerDiscountPercent" to bannerDiscountPercent.coerceIn(0L, 99L),
                "bannerColorKey" to safeColorKey,
                "updatedBy" to adminUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("vip_offer_config").document("settings").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}

