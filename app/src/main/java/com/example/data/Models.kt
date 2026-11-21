package com.example.data

import android.net.Uri
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppConstants {
    const val OWNER_ADMIN_EMAIL = "ydharmendrakumar686@gmail.com"
    const val BOARD_CLASS_FIXED = "Class 10 Bihar Board"
    const val WHATSAPP_PHONE_DISPLAY = "+91 9905537456"
    const val WHATSAPP_BASE_URL = "https://wa.me/919905537456"
    const val DAILY_TEST_TARGET_QUESTIONS = 50

    fun buildVipWhatsAppUrl(
        studentName: String,
        studentUid: String,
        plan: VipPlan,
        transactionRef: String
    ): String {
        val msg = buildString {
            append("नमस्ते Roy Study Club! मुझे VIP Membership चाहिए।\n")
            append("Student: $studentName\n")
            append("UID: $studentUid\n")
            append("Plan: ${plan.title} (₹${plan.amountInr} / ${plan.durationLabel})\n")
            if (plan.discountPercent > 0L && plan.originalPriceInr > plan.amountInr) {
                append("Offer Applied: ${plan.discountPercent}% OFF (MRP ₹${plan.originalPriceInr}")
                if (plan.appliedOfferName.isNotBlank()) append(" • ${plan.appliedOfferName}")
                append(")\n")
            }
            if (transactionRef.isNotBlank()) {
                append("Payment Ref/UTR: $transactionRef\n")
            }
            append("मैं पेमेंट स्क्रीनशॉट भेज रहा/रही हूँ।")
        }
        return "$WHATSAPP_BASE_URL?text=${Uri.encode(msg)}"
    }

    fun buildWinnerVideoCallWhatsAppUrl(
        phone: String,
        winnerName: String,
        weekLabel: String
    ): String {
        val cleanDigits = phone.filter { it.isDigit() }
        val targetPhone = when {
            cleanDigits.length == 10 -> "91$cleanDigits"
            cleanDigits.length >= 11 -> cleanDigits
            else -> "919905537456"
        }
        val msg = "बधाई हो $winnerName! आप Roy Study Club ($weekLabel) के Weekly Champion बने हैं। वीडियो कॉल और पुरस्कार के लिए संपर्क किया जा रहा है।"
        return "https://wa.me/$targetPhone?text=${Uri.encode(msg)}"
    }
}

data class VipPlan(
    val id: String,
    val title: String,
    val titleHindi: String,
    val amountInr: Long,
    val durationDays: Long,
    val durationLabel: String,
    val hasSpecialGift: Boolean,
    val badge: String? = null,
    val originalPriceInr: Long = amountInr,
    val discountPercent: Long = 0L,
    val appliedOfferName: String = ""
)

val OFFICIAL_VIP_PLANS: List<VipPlan> = listOf(
    VipPlan(
        id = "PLAN_1M",
        title = "1 Month VIP",
        titleHindi = "1 महीना VIP प्लान",
        amountInr = 99L,
        durationDays = 30L,
        durationLabel = "1 Month (30 Days)",
        hasSpecialGift = false
    ),
    VipPlan(
        id = "PLAN_3M",
        title = "3 Months VIP",
        titleHindi = "3 महीने VIP प्लान",
        amountInr = 249L,
        durationDays = 90L,
        durationLabel = "3 Months (90 Days)",
        hasSpecialGift = false,
        badge = "Popular"
    ),
    VipPlan(
        id = "PLAN_6M",
        title = "6 Months VIP",
        titleHindi = "6 महीने VIP प्लान",
        amountInr = 449L,
        durationDays = 180L,
        durationLabel = "6 Months (180 Days)",
        hasSpecialGift = false,
        badge = "Best Value"
    ),
    VipPlan(
        id = "PLAN_12M",
        title = "12 Months VIP",
        titleHindi = "12 महीने VIP + स्पेशल गिफ्ट",
        amountInr = 999L,
        durationDays = 365L,
        durationLabel = "12 Months (365 Days)",
        hasSpecialGift = true,
        badge = "Special Gift"
    )
)

fun findVipPlan(planId: String): VipPlan? = OFFICIAL_VIP_PLANS.firstOrNull { it.id == planId }

enum class TestCategory(val code: String, val label: String, val labelHindi: String) {
    DAILY("DAILY", "Daily Test", "दैनिक टेस्ट"),
    CHAPTER_TEST("CHAPTER_TEST", "Chapter Test", "अध्याय टेस्ट"),
    WEEKLY_MEGA("WEEKLY_MEGA", "Weekly Mega Test", "साप्ताहिक मेगा टेस्ट"),
    VIP_TEST("VIP_TEST", "VIP Test", "VIP टेस्ट"),
    VIP_PRACTICE("VIP_PRACTICE", "VIP Practice Set", "VIP प्रैक्टिस सेट");

    companion object {
        fun fromCode(raw: String): TestCategory =
            entries.firstOrNull { it.code == raw } ?: DAILY
    }
}

enum class TestAvailabilityStatus(val label: String) {
    OPEN("Open Now"),
    UPCOMING("Upcoming"),
    CLOSED("Closed")
}

enum class PaymentStatus(val code: String, val label: String) {
    PENDING("PENDING", "Pending Verification"),
    APPROVED("APPROVED", "Approved"),
    REJECTED("REJECTED", "Rejected");

    companion object {
        fun fromCode(raw: String): PaymentStatus =
            entries.firstOrNull { it.code == raw } ?: PENDING
    }
}

enum class VideoCallStatus(val code: String, val label: String) {
    PENDING("PENDING", "Pending Call"),
    SCHEDULED("SCHEDULED", "Scheduled"),
    COMPLETED("COMPLETED", "Completed");

    companion object {
        fun fromCode(raw: String): VideoCallStatus =
            entries.firstOrNull { it.code == raw } ?: PENDING
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val label: String = "Owner Admin",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val district: String = "",
    val schoolName: String = "",
    val boardClass: String = AppConstants.BOARD_CLASS_FIXED,
    val vipActive: Boolean = false,
    val vipPlanId: String = "NONE",
    val vipExpiresAt: Timestamp? = null,
    val giftEligible: Boolean = false,
    val referralCode: String = "",
    val referredByUid: String = "",
    val referredByCode: String = "",
    val referralLocked: Boolean = false,
    val successfulReferrals: Long = 0L,
    val earnedVipDays: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun hasValidVipNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val expiry = vipExpiresAt?.toDate()?.time ?: return false
        return vipActive && expiry > nowMillis
    }

    fun remainingVipDays(nowMillis: Long = System.currentTimeMillis()): Long {
        val expiry = vipExpiresAt?.toDate()?.time ?: return 0L
        if (!vipActive || expiry <= nowMillis) return 0L
        return ((expiry - nowMillis) / (24L * 60L * 60L * 1000L)) + 1L
    }
}

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameHindi: String = "",
    val chapters: List<String> = emptyList(),
    val displayOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyTest(
    @DocumentId val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val chapterName: String = "",
    val testDate: String = "",
    val availableFrom: String = "",
    val closingDate: String = "",
    val durationMinutes: Long = 30L,
    val passingPercentage: Long = 0L,
    val marksPerQuestion: Long = 1L,
    val testCategory: String = TestCategory.DAILY.code,
    val weekKey: String = "",
    val questionCount: Long = 0L,
    val published: Boolean = false,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): TestCategory = TestCategory.fromCode(testCategory)

    fun effectiveMarksPerQuestion(): Long = marksPerQuestion.coerceIn(1L, 10L)

    fun totalMarks(): Long = questionCount * effectiveMarksPerQuestion()

    fun availabilityStatus(todayIso: String = DateUtils.todayIso()): TestAvailabilityStatus {
        val start = availableFrom.trim()
        val end = closingDate.trim()
        return when {
            start.isNotEmpty() && todayIso < start -> TestAvailabilityStatus.UPCOMING
            end.isNotEmpty() && todayIso > end -> TestAvailabilityStatus.CLOSED
            else -> TestAvailabilityStatus.OPEN
        }
    }
}

@Keep
@IgnoreExtraProperties
data class TestQuestion(
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctIndex: Int = 0, // 0=A, 1=B, 2=C, 3=D
    val explanation: String = ""
) {
    fun optionsList(): List<String> = listOf(optionA, optionB, optionC, optionD)

    fun toMap(): Map<String, Any> = mapOf(
        "questionText" to questionText.trim(),
        "optionA" to optionA.trim(),
        "optionB" to optionB.trim(),
        "optionC" to optionC.trim(),
        "optionD" to optionD.trim(),
        "correctIndex" to correctIndex.coerceIn(0, 3),
        "explanation" to explanation.trim()
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): TestQuestion = TestQuestion(
            questionText = map["questionText"] as? String ?: "",
            optionA = map["optionA"] as? String ?: "",
            optionB = map["optionB"] as? String ?: "",
            optionC = map["optionC"] as? String ?: "",
            optionD = map["optionD"] as? String ?: "",
            correctIndex = (map["correctIndex"] as? Number)?.toInt()?.coerceIn(0, 3) ?: 0,
            explanation = map["explanation"] as? String ?: ""
        )
    }
}

@Keep
@IgnoreExtraProperties
data class TestPaper(
    @DocumentId val id: String = "",
    val testId: String = "",
    val testCategory: String = TestCategory.DAILY.code,
    val published: Boolean = false,
    val questions: List<TestQuestion> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TestAttempt(
    @DocumentId val id: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val subjectName: String = "",
    val chapterName: String = "",
    val testCategory: String = TestCategory.DAILY.code,
    val weekKey: String = "",
    val userId: String = "",
    val studentName: String = "",
    val selectedAnswers: List<Long> = emptyList(), // -1 = skipped, 0..3 = option index
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val skippedCount: Long = 0L,
    val totalQuestions: Long = 0L,
    val marksPerQuestion: Long = 1L,
    val totalMarks: Long = 0L,
    val passingPercentage: Long = 0L,
    val score: Long = 0L,
    val timeTakenSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun effectiveMarksPerQuestion(): Long = marksPerQuestion.coerceIn(1L, 10L)

    fun effectiveTotalMarks(): Long =
        if (totalMarks > 0L) totalMarks else totalQuestions * effectiveMarksPerQuestion()

    fun accuracyPercent(): Int {
        val denom = effectiveTotalMarks()
        if (denom <= 0L) return 0
        return ((score * 100L) / denom).toInt().coerceIn(0, 100)
    }

    fun percentageFormatted(): String {
        val denom = effectiveTotalMarks()
        if (denom <= 0L) return "0.0%"
        val pct = (score.toDouble() * 100.0) / denom.toDouble()
        return String.format(Locale.US, "%.1f%%", pct)
    }

    /** Returns null if Pass/Fail is not configured (passingPercentage <= 0), else true=PASS, false=FAIL. */
    fun passStatus(): Boolean? {
        if (passingPercentage <= 0L) return null
        val denom = effectiveTotalMarks()
        if (denom <= 0L) return false
        return (score * 100L) >= (passingPercentage * denom)
    }
}

@Keep
@IgnoreExtraProperties
data class WeeklyLeaderboardEntry(
    @DocumentId val id: String = "",
    val weekKey: String = "",
    val userId: String = "",
    val displayName: String = "",
    val district: String = "",
    val vipBadge: Boolean = false,
    val totalCorrect: Long = 0L,
    val totalMarks: Long = 0L,
    val totalQuestionsAttempted: Long = 0L,
    val testsCount: Long = 0L,
    val totalTimeSeconds: Long = 0L,
    val countedTestIds: List<String> = emptyList(),
    val lastAttemptTestId: String = "",
    val publicVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WeeklyChampion(
    @DocumentId val id: String = "",
    val weekKey: String = "",
    val weekLabel: String = "",
    val winnerUserId: String = "",
    val winnerName: String = "",
    val winnerDistrict: String = "",
    val totalCorrect: Long = 0L,
    val totalMarks: Long = 0L,
    val totalTimeSeconds: Long = 0L,
    val testsCount: Long = 0L,
    val giftDetails: String = "",
    val specialOffer: String = "",
    val videoCallStatus: String = VideoCallStatus.PENDING.code,
    val announcementNote: String = "",
    val verifiedBy: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val studentPhone: String = "",
    val planId: String = "",
    val planName: String = "",
    val amountInr: Long = 0L,
    val originalPriceInr: Long = 0L,
    val discountPercent: Long = 0L,
    val appliedOfferName: String = "",
    val hasSpecialGift: Boolean = false,
    val durationDays: Long = 0L,
    val transactionRef: String = "",
    val note: String = "",
    val hasScreenshot: Boolean = false,
    val status: String = PaymentStatus.PENDING.code,
    val adminRemark: String = "",
    val reviewedBy: String = "",
    val reviewedAt: Timestamp? = null,
    val activatedUntil: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): PaymentStatus = PaymentStatus.fromCode(status)
}

@Keep
@IgnoreExtraProperties
data class VipNote(
    @DocumentId val id: String = "",
    val title: String = "",
    val subjectName: String = "",
    val chapterName: String = "",
    val summary: String = "",
    val published: Boolean = false,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VipNoteContent(
    @DocumentId val id: String = "",
    val noteId: String = "",
    val content: String = "",
    val keyPoints: List<String> = emptyList(),
    val published: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ReferralStatus(val code: String, val label: String) {
    PENDING("PENDING", "Pending"),
    SUCCESSFUL("SUCCESSFUL", "Successful"),
    REJECTED("REJECTED", "Rejected");

    companion object {
        fun fromCode(raw: String): ReferralStatus =
            entries.firstOrNull { it.code == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class ReferralConfig(
    @DocumentId val id: String = "settings",
    val enabled: Boolean = true,
    val rewardDays: Long = 7L,
    val maxSuccessfulReferrals: Long = 20L,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralCodeLookup(
    @DocumentId val id: String = "",
    val code: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referrerUid: String = "",
    val referrerName: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val referralCode: String = "",
    val status: String = ReferralStatus.PENDING.code,
    val firstDailyTestCompleted: Boolean = false,
    val firstDailyTestId: String = "",
    val firstDailyTestTitle: String = "",
    val firstDailyTestAt: Timestamp? = null,
    val rewardGranted: Boolean = false,
    val rewardDays: Long = 0L,
    val rejectionReason: String = "",
    val processedBy: String = "",
    val rewardedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): ReferralStatus = ReferralStatus.fromCode(status)
}

@Keep
@IgnoreExtraProperties
data class ReferralTransaction(
    @DocumentId val id: String = "",
    val referralId: String = "",
    val beneficiaryUid: String = "",
    val beneficiaryName: String = "",
    val counterpartyUid: String = "",
    val counterpartyName: String = "",
    val role: String = "REFERRER", // REFERRER or REFERRED
    val referralCode: String = "",
    val rewardDays: Long = 0L,
    val vipExpiresAfter: Timestamp? = null,
    val processedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FcmDeviceToken(
    @DocumentId val id: String = "",
    val userId: String = "",
    val token: String = "",
    val dailyTestAlertsEnabled: Boolean = true,
    val weeklyChampionAlertsEnabled: Boolean = true,
    val platform: String = "android",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationLog(
    @DocumentId val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val refId: String = "",
    val targetSummary: String = "",
    val status: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class BannerColorTheme(
    val key: String,
    val label: String,
    val startArgb: Long,
    val endArgb: Long,
    val textArgb: Long,
    val badgeBgArgb: Long,
    val badgeTextArgb: Long
) {
    FESTIVE_GOLD(
        key = "FESTIVE_GOLD",
        label = "Festive Gold",
        startArgb = 0xFFB45309L,
        endArgb = 0xFFF59E0BL,
        textArgb = 0xFFFFFFFFL,
        badgeBgArgb = 0xFFFFF7EDL,
        badgeTextArgb = 0xFF9A3412L
    ),
    ROYAL_BLUE(
        key = "ROYAL_BLUE",
        label = "Royal Blue",
        startArgb = 0xFF0A2E73L,
        endArgb = 0xFF1D4ED8L,
        textArgb = 0xFFFFFFFFL,
        badgeBgArgb = 0xFFFFD54FL,
        badgeTextArgb = 0xFF3E2723L
    ),
    CRIMSON_SALE(
        key = "CRIMSON_SALE",
        label = "Flash Crimson",
        startArgb = 0xFF991B1BL,
        endArgb = 0xFFDC2626L,
        textArgb = 0xFFFFFFFFL,
        badgeBgArgb = 0xFFFEF08AL,
        badgeTextArgb = 0xFF7F1D1DL
    ),
    EMERALD_DEAL(
        key = "EMERALD_DEAL",
        label = "Emerald Saver",
        startArgb = 0xFF065F46L,
        endArgb = 0xFF10B981L,
        textArgb = 0xFFFFFFFFL,
        badgeBgArgb = 0xFFECFDF5L,
        badgeTextArgb = 0xFF065F46L
    ),
    PURPLE_MEGA(
        key = "PURPLE_MEGA",
        label = "Mega Purple",
        startArgb = 0xFF4C1D95L,
        endArgb = 0xFF7C3AEDL,
        textArgb = 0xFFFFFFFFL,
        badgeBgArgb = 0xFFFDE68AL,
        badgeTextArgb = 0xFF4C1D95L
    ),
    SUNSET_ORANGE(
        key = "SUNSET_ORANGE",
        label = "Sunset Orange",
        startArgb = 0xFFC2410CL,
        endArgb = 0xFFF97316L,
        textArgb = 0xFFFFFFFFL,
        badgeBgArgb = 0xFFFFFFFFL,
        badgeTextArgb = 0xFF9A3412L
    );

    companion object {
        fun fromKey(raw: String): BannerColorTheme =
            entries.firstOrNull { it.key == raw } ?: FESTIVE_GOLD
    }
}

@Keep
@IgnoreExtraProperties
data class DynamicVipPlan(
    @DocumentId val id: String = "",
    val title: String = "",
    val titleHindi: String = "",
    val originalPriceInr: Long = 99L,
    val offerPriceInr: Long = 99L,
    val discountPercent: Long = 0L,
    val durationDays: Long = 30L,
    val durationLabel: String = "",
    val hasSpecialGift: Boolean = false,
    val badge: String = "",
    val offerName: String = "",
    val active: Boolean = true,
    val displayOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectivePrice(offerActiveNow: Boolean): Long {
        val safeOrig = originalPriceInr.coerceAtLeast(1L)
        val safeOffer = offerPriceInr.coerceIn(1L, safeOrig)
        return if (offerActiveNow) safeOffer else safeOrig
    }

    fun effectiveDiscountPercent(offerActiveNow: Boolean): Long {
        if (!offerActiveNow) return 0L
        return VipPricingUtils.calculateDiscountPercent(originalPriceInr, offerPriceInr)
    }

    fun hasActiveDiscount(offerActiveNow: Boolean): Boolean =
        effectiveDiscountPercent(offerActiveNow) > 0L

    fun toVipPlan(offerActiveNow: Boolean, globalOfferName: String = ""): VipPlan {
        val activePrice = effectivePrice(offerActiveNow)
        val activeDiscount = effectiveDiscountPercent(offerActiveNow)
        val resolvedOfferName = if (activeDiscount > 0L) {
            offerName.trim().ifBlank { globalOfferName.trim() }
        } else {
            ""
        }
        return VipPlan(
            id = id,
            title = title,
            titleHindi = titleHindi,
            amountInr = activePrice,
            durationDays = durationDays,
            durationLabel = durationLabel.ifBlank { "$durationDays Days" },
            hasSpecialGift = hasSpecialGift,
            badge = badge.ifBlank { null },
            originalPriceInr = originalPriceInr,
            discountPercent = activeDiscount,
            appliedOfferName = resolvedOfferName
        )
    }
}

@Keep
@IgnoreExtraProperties
data class VipOfferConfig(
    @DocumentId val id: String = "settings",
    val offersEnabled: Boolean = false,
    val offerName: String = "",
    val offerStartDate: String = "",
    val offerEndDate: String = "",
    val offerStartMillis: Long = 0L,
    val offerEndMillis: Long = 0L,
    val bannerEnabled: Boolean = false,
    val bannerHeading: String = "",
    val bannerDescription: String = "",
    val bannerOfferText: String = "",
    val bannerDiscountPercent: Long = 0L,
    val bannerColorKey: String = BannerColorTheme.FESTIVE_GOLD.key,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isOfferExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        offerEndMillis > 0L && nowMillis > offerEndMillis

    fun isOfferUpcoming(nowMillis: Long = System.currentTimeMillis()): Boolean =
        offerStartMillis > 0L && nowMillis < offerStartMillis

    fun isOfferActiveNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!offersEnabled) return false
        if (offerStartMillis > 0L && nowMillis < offerStartMillis) return false
        if (offerEndMillis > 0L && nowMillis > offerEndMillis) return false
        return true
    }

    fun shouldShowBanner(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!bannerEnabled) return false
        if (isOfferExpired(nowMillis)) return false
        if (isOfferUpcoming(nowMillis)) return false
        return bannerHeading.isNotBlank() || bannerDescription.isNotBlank() || bannerOfferText.isNotBlank()
    }

    fun colorTheme(): BannerColorTheme = BannerColorTheme.fromKey(bannerColorKey)
}

object VipPricingUtils {
    fun calculateDiscountPercent(originalPriceInr: Long, newPriceInr: Long): Long {
        if (originalPriceInr <= 0L || newPriceInr <= 0L || newPriceInr >= originalPriceInr) return 0L
        return (((originalPriceInr - newPriceInr) * 100L) / originalPriceInr).coerceIn(1L, 99L)
    }

    fun isoDateToStartOfDayMillis(isoDate: String): Long {
        val clean = isoDate.trim()
        if (clean.isEmpty()) return 0L
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = sdf.parse(clean) ?: return 0L
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis.coerceAtLeast(0L)
        } catch (_: Exception) {
            0L
        }
    }

    fun isoDateToEndOfDayMillis(isoDate: String): Long {
        val clean = isoDate.trim()
        if (clean.isEmpty()) return 0L
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = sdf.parse(clean) ?: return 0L
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            cal.timeInMillis.coerceAtLeast(0L)
        } catch (_: Exception) {
            0L
        }
    }

    fun sanitizePlanId(raw: String, fallbackPrefix: String = "PLAN_CUSTOM"): String {
        val clean = raw.trim().uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(48)
        return if (clean.length >= 2) clean else "${fallbackPrefix}_${System.currentTimeMillis() % 100000}"
    }

    fun defaultDynamicVipPlans(): List<DynamicVipPlan> = OFFICIAL_VIP_PLANS.mapIndexed { idx, p ->
        DynamicVipPlan(
            id = p.id,
            title = p.title,
            titleHindi = p.titleHindi,
            originalPriceInr = p.amountInr,
            offerPriceInr = p.amountInr,
            discountPercent = 0L,
            durationDays = p.durationDays,
            durationLabel = p.durationLabel,
            hasSpecialGift = p.hasSpecialGift,
            badge = p.badge.orEmpty(),
            offerName = "",
            active = true,
            displayOrder = (idx + 1).toLong()
        )
    }
}

object ReferralUtils {
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val CODE_REGEX = Regex("^[A-Z0-9]{6,16}$")

    fun normalizeCode(raw: String): String =
        raw.trim().uppercase(Locale.US).filter { it in 'A'..'Z' || it in '0'..'9' }.take(16)

    fun isValidCodeFormat(raw: String): Boolean {
        val clean = normalizeCode(raw)
        return CODE_REGEX.matches(clean)
    }

    fun generateUniqueReferralCode(seedUid: String, attempt: Int = 0): String {
        val input = "$seedUid:ROYSTUDY:$attempt"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder("ROY")
        for (i in 0 until 6) {
            val unsigned = digest[i].toInt() and 0xFF
            sb.append(CODE_ALPHABET[unsigned % CODE_ALPHABET.length])
        }
        return sb.toString()
    }

    fun buildInviteLink(code: String): String {
        val clean = normalizeCode(code)
        return "https://roystudyclub.app/invite?code=$clean"
    }

    fun buildShareMessage(code: String, rewardDays: Long): String {
        val clean = normalizeCode(code)
        val link = buildInviteLink(clean)
        return buildString {
            append("ROY STUDY CLUB (Class 10 Bihar Board) में जुड़ें!\n")
            append("मेरा Referral Code: $clean\n")
            append("रजिस्ट्रेशन के समय यह कोड डालें और अपना पहला Daily Test पूरा करें — हम दोनों को $rewardDays दिन की FREE VIP Membership मिलेगी!\n")
            append("Invite Link: $link")
        }
    }

    fun buildWhatsAppShareUrl(code: String, rewardDays: Long): String {
        val msg = buildShareMessage(code, rewardDays)
        val encoded = java.net.URLEncoder.encode(msg, "UTF-8").replace("+", "%20")
        return "https://wa.me/?text=$encoded"
    }

    fun escapeCsvField(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuoting) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    fun buildReferralsCsv(
        referrals: List<ReferralRecord>,
        transactions: List<ReferralTransaction>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("RecordType,ID,ReferralCode,ReferrerName,ReferrerUID,ReferredName,ReferredUID,Status,FirstDailyTestDone,FirstDailyTestTitle,RewardGranted,RewardDays,RejectionReason,ProcessedBy,CreatedAt,RewardedAt")
        for (ref in referrals) {
            val row = listOf(
                "REFERRAL",
                ref.id,
                ref.referralCode,
                ref.referrerName,
                ref.referrerUid,
                ref.referredName,
                ref.referredUid,
                ref.status,
                if (ref.firstDailyTestCompleted) "YES" else "NO",
                ref.firstDailyTestTitle,
                if (ref.rewardGranted) "YES" else "NO",
                ref.rewardDays.toString(),
                ref.rejectionReason,
                ref.processedBy,
                DateUtils.formatDateTime(ref.createdAt),
                DateUtils.formatDateTime(ref.rewardedAt)
            )
            sb.appendLine(row.joinToString(",") { escapeCsvField(it) })
        }
        if (transactions.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("RecordType,TransactionID,ReferralID,ReferralCode,BeneficiaryRole,BeneficiaryName,BeneficiaryUID,CounterpartyName,CounterpartyUID,RewardDays,VipExpiresAfter,ProcessedBy,CreatedAt")
            for (tx in transactions) {
                val row = listOf(
                    "REWARD_TX",
                    tx.id,
                    tx.referralId,
                    tx.referralCode,
                    tx.role,
                    tx.beneficiaryName,
                    tx.beneficiaryUid,
                    tx.counterpartyName,
                    tx.counterpartyUid,
                    tx.rewardDays.toString(),
                    DateUtils.formatDateTime(tx.vipExpiresAfter),
                    tx.processedBy,
                    DateUtils.formatDateTime(tx.createdAt)
                )
                sb.appendLine(row.joinToString(",") { escapeCsvField(it) })
            }
        }
        return sb.toString()
    }
}

object DateUtils {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val displayDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    fun todayIso(): String = isoDateFormat.format(Date())

    fun todayPlusDaysIso(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoDateFormat.format(cal.time)
    }

    fun currentWeekKey(): String {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    fun weekKeyFromIsoDate(isoDate: String): String {
        return try {
            val parsed = isoDateFormat.parse(isoDate) ?: return currentWeekKey()
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.minimalDaysInFirstWeek = 4
            cal.time = parsed
            val year = cal.get(Calendar.YEAR)
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            String.format(Locale.US, "%04d-W%02d", year, week)
        } catch (_: Exception) {
            currentWeekKey()
        }
    }

    fun formatTimestamp(ts: Timestamp?): String {
        val date = ts?.toDate() ?: return "—"
        return displayDateFormat.format(date)
    }

    fun formatDateTime(ts: Timestamp?): String {
        val date = ts?.toDate() ?: return "—"
        return displayDateTimeFormat.format(date)
    }

    fun formatDurationSeconds(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        val mins = safe / 60L
        val secs = safe % 60L
        return if (mins >= 60L) {
            val hrs = mins / 60L
            val remMins = mins % 60L
            String.format(Locale.US, "%dh %02dm %02ds", hrs, remMins, secs)
        } else {
            String.format(Locale.US, "%02dm %02ds", mins, secs)
        }
    }
}

// Official Bihar Board Class 10 (BSEB) Subject & Chapter Syllabus Catalog
// Used ONLY inside Admin Subject Management when Admin chooses to seed official syllabus subjects/chapters.
// Contains ZERO demo tests and ZERO demo questions.
data class SyllabusSubjectSeed(
    val id: String,
    val name: String,
    val nameHindi: String,
    val displayOrder: Long,
    val chapters: List<String>
)

val BIHAR_BOARD_CLASS_10_SYLLABUS: List<SyllabusSubjectSeed> = listOf(
    SyllabusSubjectSeed(
        id = "bseb_math",
        name = "Mathematics",
        nameHindi = "गणित",
        displayOrder = 1L,
        chapters = listOf(
            "1. वास्तविक संख्याएँ (Real Numbers)",
            "2. बहुपद (Polynomials)",
            "3. दो चर वाले रैखिक समीकरण युग्म",
            "4. द्विघात समीकरण (Quadratic Equations)",
            "5. समांतर श्रेढ़ियाँ (Arithmetic Progressions)",
            "6. त्रिभुज (Triangles)",
            "7. निर्देशांक ज्यामिति (Coordinate Geometry)",
            "8. त्रिकोणमिति का परिचय (Trigonometry)",
            "9. त्रिकोणमिति के कुछ अनुप्रयोग",
            "10. वृत्त (Circles)",
            "11. रचनाएँ (Constructions)",
            "12. वृत्तों से संबंधित क्षेत्रफल",
            "13. पृष्ठीय क्षेत्रफल और आयतन",
            "14. सांख्यिकी (Statistics)",
            "15. प्रायिकता (Probability)"
        )
    ),
    SyllabusSubjectSeed(
        id = "bseb_science",
        name = "Science",
        nameHindi = "विज्ञान",
        displayOrder = 2L,
        chapters = listOf(
            "1. रासायनिक अभिक्रियाएँ एवं समीकरण",
            "2. अम्ल, क्षारक एवं लवण",
            "3. धातु एवं अधातु",
            "4. कार्बन एवं उसके यौगिक",
            "5. तत्वों का आवर्त वर्गीकरण",
            "6. जैव प्रक्रम (Life Processes)",
            "7. नियंत्रण एवं समन्वय",
            "8. जीव जनन कैसे करते हैं",
            "9. आनुवंशिकता एवं जैव विकास",
            "10. प्रकाश - परावर्तन तथा अपवर्तन",
            "11. मानव नेत्र तथा रंगबिरंगा संसार",
            "12. विद्युत (Electricity)",
            "13. विद्युत धारा के चुंबकीय प्रभाव",
            "14. ऊर्जा के स्रोत",
            "15. हमारा पर्यावरण",
            "16. प्राकृतिक संसाधनों का प्रबंधन"
        )
    ),
    SyllabusSubjectSeed(
        id = "bseb_social_science",
        name = "Social Science",
        nameHindi = "सामाजिक विज्ञान",
        displayOrder = 3L,
        chapters = listOf(
            "इतिहास: यूरोप में राष्ट्रवाद",
            "इतिहास: समाजवाद एवं साम्यवाद",
            "इतिहास: हिंद-चीन में राष्ट्रवादी आंदोलन",
            "इतिहास: भारत में राष्ट्रवाद",
            "भूगोल: भारत - संसाधन एवं उपयोग",
            "भूगोल: कृषि एवं वन संसाधन",
            "राजनीति विज्ञान: लोकतंत्र में सत्ता की साझेदारी",
            "अर्थशास्त्र: अर्थव्यवस्था एवं इसके विकास का इतिहास",
            "आपदा प्रबंधन: प्राकृतिक आपदा एवं प्रबंधन"
        )
    ),
    SyllabusSubjectSeed(
        id = "bseb_hindi",
        name = "Hindi (Godhuli & Varnika)",
        nameHindi = "हिन्दी",
        displayOrder = 4L,
        chapters = listOf(
            "गोधूलि गद्य: श्रम विभाजन और जाति प्रथा",
            "गोधूलि गद्य: विष के दाँत",
            "गोधूलि गद्य: भारत से हम क्या सीखें",
            "गोधूलि गद्य: नाखून क्यों बढ़ते हैं",
            "गोधूलि काव्य: राम नाम बिनु बिरथे जगि जनमा",
            "वर्णिका: दही वाली मंगम्मा",
            "व्याकरण: संधि, समास, उपसर्ग एवं प्रत्यय"
        )
    ),
    SyllabusSubjectSeed(
        id = "bseb_sanskrit",
        name = "Sanskrit (Piyusham)",
        nameHindi = "संस्कृत",
        displayOrder = 5L,
        chapters = listOf(
            "1. मंगलम्",
            "2. पाटलिपुत्रवैभवम्",
            "3. अलसकथा",
            "4. संस्कृतसाहित्ये लेखिकाः",
            "5. भारतमहिमा",
            "6. भारतीयसंस्काराः",
            "7. नीतिश्लोकाः",
            "संस्कृत व्याकरण एवं अनुवाद"
        )
    ),
    SyllabusSubjectSeed(
        id = "bseb_english",
        name = "English (Panorama)",
        nameHindi = "अंग्रेजी",
        displayOrder = 6L,
        chapters = listOf(
            "1. The Pace for Living",
            "2. Me and the Ecology Bit",
            "3. Gillu",
            "4. What is Wrong with Indian Films",
            "5. Acceptance Speech",
            "Poetry: God Made the Country",
            "English Grammar: Tense, Voice & Narration"
        )
    )
)
