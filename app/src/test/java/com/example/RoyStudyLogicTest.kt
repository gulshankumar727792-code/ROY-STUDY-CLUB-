package com.example

import com.example.data.AppConstants
import com.example.data.DateUtils
import com.example.data.OFFICIAL_VIP_PLANS
import com.example.data.StudentProfile
import com.example.data.TestAttempt
import com.example.data.WeeklyLeaderboardEntry
import com.example.data.findVipPlan
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class RoyStudyLogicTest {

    @Test
    fun officialVipPlansMatchExactPricingAndDurations() {
        assertEquals(4, OFFICIAL_VIP_PLANS.size)

        val m1 = findVipPlan("PLAN_1M")
        assertNotNull(m1)
        assertEquals(99L, m1?.amountInr)
        assertEquals(30L, m1?.durationDays)
        assertFalse(m1!!.hasSpecialGift)

        val m3 = findVipPlan("PLAN_3M")
        assertEquals(249L, m3?.amountInr)
        assertEquals(90L, m3?.durationDays)

        val m6 = findVipPlan("PLAN_6M")
        assertEquals(449L, m6?.amountInr)
        assertEquals(180L, m6?.durationDays)

        val m12 = findVipPlan("PLAN_12M")
        assertEquals(999L, m12?.amountInr)
        assertEquals(365L, m12?.durationDays)
        assertTrue(m12!!.hasSpecialGift)
    }

    @Test
    fun vipExpiryEvaluatesAutomaticallyAgainstCurrentTime() {
        val now = 1_800_000_000_000L
        val activeVip = StudentProfile(
            userId = "u1",
            vipActive = true,
            vipPlanId = "PLAN_1M",
            vipExpiresAt = Timestamp(Date(now + 5 * 24 * 60 * 60 * 1000L))
        )
        assertTrue(activeVip.hasValidVipNow(now))
        assertTrue(activeVip.remainingVipDays(now) >= 5L)

        val expiredVip = StudentProfile(
            userId = "u2",
            vipActive = true,
            vipPlanId = "PLAN_1M",
            vipExpiresAt = Timestamp(Date(now - 60_000L))
        )
        assertFalse(expiredVip.hasValidVipNow(now))
        assertEquals(0L, expiredVip.remainingVipDays(now))
    }

    @Test
    fun weeklyLeaderboardTieBreakerPrefersLowerTime() {
        val entries = listOf(
            WeeklyLeaderboardEntry(userId = "a", displayName = "Slow", totalMarks = 45, totalCorrect = 45, totalTimeSeconds = 1800),
            WeeklyLeaderboardEntry(userId = "b", displayName = "Fast", totalMarks = 45, totalCorrect = 45, totalTimeSeconds = 1100),
            WeeklyLeaderboardEntry(userId = "c", displayName = "Top", totalMarks = 48, totalCorrect = 48, totalTimeSeconds = 1500)
        )
        val sorted = entries.sortedWith(
            compareByDescending<WeeklyLeaderboardEntry> { it.totalMarks }
                .thenByDescending { it.totalCorrect }
                .thenBy { it.totalTimeSeconds }
        )
        assertEquals("Top", sorted[0].displayName)
        assertEquals("Fast", sorted[1].displayName)
        assertEquals("Slow", sorted[2].displayName)
    }

    @Test
    fun attemptAccuracyAndDurationFormattingWorkCorrectly() {
        val attempt = TestAttempt(correctCount = 40, wrongCount = 5, skippedCount = 5, totalQuestions = 50, score = 40)
        assertEquals(80, attempt.accuracyPercent())
        assertEquals("02m 05s", DateUtils.formatDurationSeconds(125L))
        assertEquals("1h 01m 01s", DateUtils.formatDurationSeconds(3661L))
        assertEquals("+91 9905537456", AppConstants.WHATSAPP_PHONE_DISPLAY)
        assertEquals("https://wa.me/919905537456", AppConstants.WHATSAPP_BASE_URL)
    }

    @Test
    fun referralCodeGenerationProducesValidUniqueCodes() {
        val code1 = com.example.data.ReferralUtils.generateUniqueReferralCode("uid_student_101", 0)
        val code2 = com.example.data.ReferralUtils.generateUniqueReferralCode("uid_student_202", 0)
        val code1Alt = com.example.data.ReferralUtils.generateUniqueReferralCode("uid_student_101", 1)

        assertTrue(code1.startsWith("ROY"))
        assertEquals(9, code1.length)
        assertTrue(com.example.data.ReferralUtils.isValidCodeFormat(code1))
        assertTrue(code1 != code2)
        assertTrue(code1 != code1Alt)
        assertEquals("ROY4K9M2P", com.example.data.ReferralUtils.normalizeCode("  roy-4k9m2p  "))
    }

    @Test
    fun referralShareLinksAndCsvExportWorkProperly() {
        val link = com.example.data.ReferralUtils.buildInviteLink("roy4k9m2p")
        assertEquals("https://roystudyclub.app/invite?code=ROY4K9M2P", link)

        val waUrl = com.example.data.ReferralUtils.buildWhatsAppShareUrl("ROY4K9M2P", 7L)
        assertTrue(waUrl.startsWith("https://wa.me/?text="))
        assertTrue(waUrl.contains("ROY4K9M2P"))

        val ref = com.example.data.ReferralRecord(
            id = "uid_bob",
            referrerUid = "uid_alice",
            referrerName = "Alice, Kumari",
            referredUid = "uid_bob",
            referredName = "Bob \"Topper\" Kumar",
            referralCode = "ROY4K9M2P",
            status = "SUCCESSFUL",
            firstDailyTestCompleted = true,
            firstDailyTestTitle = "Science Daily Test #1",
            rewardGranted = true,
            rewardDays = 7L,
            processedBy = "admin_1"
        )
        val tx = com.example.data.ReferralTransaction(
            id = "ref_uid_bob_referrer",
            referralId = "uid_bob",
            beneficiaryUid = "uid_alice",
            beneficiaryName = "Alice, Kumari",
            counterpartyUid = "uid_bob",
            counterpartyName = "Bob \"Topper\" Kumar",
            role = "REFERRER",
            referralCode = "ROY4K9M2P",
            rewardDays = 7L,
            processedBy = "admin_1"
        )
        val csv = com.example.data.ReferralUtils.buildReferralsCsv(listOf(ref), listOf(tx))
        assertTrue(csv.contains("RecordType,ID,ReferralCode"))
        assertTrue(csv.contains("\"Alice, Kumari\""))
        assertTrue(csv.contains("\"Bob \"\"Topper\"\" Kumar\""))
        assertTrue(csv.contains("REWARD_TX,ref_uid_bob_referrer"))
    }

    @Test
    fun fcmNotificationRoutingResolvesDailyAndChampionTargets() {
        assertEquals(
            com.example.notifications.RoyStudyNotificationHelper.CHANNEL_DAILY_TESTS,
            com.example.notifications.RoyStudyNotificationHelper.resolveChannelId(
                com.example.notifications.RoyStudyNotificationHelper.TYPE_DAILY_TEST_PUBLISHED,
                null
            )
        )
        assertEquals(
            com.example.notifications.RoyStudyNotificationHelper.CHANNEL_WEEKLY_CHAMPION,
            com.example.notifications.RoyStudyNotificationHelper.resolveChannelId(
                com.example.notifications.RoyStudyNotificationHelper.TYPE_WEEKLY_CHAMPION_WINNER,
                null
            )
        )
        assertEquals(
            com.example.notifications.RoyStudyNotificationHelper.CHANNEL_WEEKLY_CHAMPION,
            com.example.notifications.RoyStudyNotificationHelper.resolveChannelId(
                com.example.notifications.RoyStudyNotificationHelper.TYPE_WEEKLY_CHAMPION_ANNOUNCED,
                null
            )
        )
        assertEquals(
            com.example.notifications.RoyStudyNotificationHelper.NAV_DAILY_TESTS,
            com.example.notifications.RoyStudyNotificationHelper.resolveNavTarget(
                com.example.notifications.RoyStudyNotificationHelper.TYPE_DAILY_TEST_PUBLISHED,
                null
            )
        )
        assertEquals(
            com.example.notifications.RoyStudyNotificationHelper.NAV_WEEKLY_CHAMPION,
            com.example.notifications.RoyStudyNotificationHelper.resolveNavTarget(
                com.example.notifications.RoyStudyNotificationHelper.TYPE_WEEKLY_CHAMPION_WINNER,
                null
            )
        )
        assertEquals(
            com.example.notifications.RoyStudyNotificationHelper.NAV_WEEKLY_CHAMPION,
            com.example.notifications.RoyStudyNotificationHelper.resolveNavTarget(
                null,
                com.example.notifications.RoyStudyNotificationHelper.NAV_WEEKLY_CHAMPION
            )
        )
    }

    @Test
    fun bulkQuestionParserParsesValidInvalidDuplicateAndMultilineQuestions() {
        val sample = """
            Q1. प्रकाश के परावर्तन के कितने नियम हैं?
            A. एक
            B. दो
            C. तीन
            D. चार
            ANSWER: B
            EXPLANATION: प्रकाश के परावर्तन के दो नियम होते हैं।

            Q2. Which gas is released during photosynthesis
            in green plants?
            A. Carbon Dioxide
            B. Nitrogen
            C. Oxygen
            D. Hydrogen
            Ans: C

            Q3. प्रकाश के परावर्तन के कितने नियम हैं?
            A. एक
            B. दो
            C. तीन
            D. चार
            ANSWER: B

            Q4. Broken question with missing option D and bad answer?
            A. One
            B. Two
            C. Three
            ANSWER: Z
        """.trimIndent()

        val result = com.example.data.BulkQuestionParser.parse(
            sample,
            com.example.data.BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS
        )
        assertEquals(4, result.totalCount)
        assertEquals(2, result.validCount)
        assertEquals(2, result.invalidCount)
        assertEquals(1, result.duplicateCount)
        assertEquals(0, result.truncatedCount)

        val q1 = result.items[0]
        assertTrue(q1.isValid)
        assertEquals(1, q1.detectedNumber)
        assertEquals(1, q1.question.correctIndex)
        assertTrue(q1.question.explanation.contains("दो नियम"))

        val q2 = result.items[1]
        assertTrue(q2.isValid)
        assertEquals(2, q2.question.correctIndex)
        assertTrue(q2.question.questionText.contains("in green plants?"))

        val q3 = result.items[2]
        assertFalse(q3.isValid)
        assertTrue(q3.isDuplicate)

        val q4 = result.items[3]
        assertFalse(q4.isValid)
        assertTrue(q4.validationErrors.any { it.contains("Option D") })
        assertTrue(q4.validationErrors.any { it.contains("ANSWER") })
    }

    @Test
    fun bulkQuestionParserHandles200WeeklyMegaQuestionsAndEnforcesMaxCap() {
        val sb = StringBuilder()
        for (i in 1..205) {
            sb.appendLine("Q$i. Class 10 Bihar Board Question Number $i?")
            sb.appendLine("A. Choice ${i}A")
            sb.appendLine("B. Choice ${i}B")
            sb.appendLine("C. Choice ${i}C")
            sb.appendLine("D. Choice ${i}D")
            val ansLetter = listOf("A", "B", "C", "D")[(i - 1) % 4]
            sb.appendLine("ANSWER: $ansLetter")
            sb.appendLine()
        }

        val megaResult = com.example.data.BulkQuestionParser.parse(
            sb.toString(),
            com.example.data.BulkQuestionParser.MAX_WEEKLY_MEGA_QUESTIONS
        )
        assertEquals(200, megaResult.totalCount)
        assertEquals(200, megaResult.validCount)
        assertEquals(0, megaResult.invalidCount)
        assertEquals(5, megaResult.truncatedCount)
        assertEquals(0, megaResult.items[0].question.correctIndex)
        assertEquals(3, megaResult.items[199].question.correctIndex)

        val chapterResult = com.example.data.BulkQuestionParser.parse(
            sb.toString(),
            com.example.data.BulkQuestionParser.MAX_CHAPTER_BULK_QUESTIONS
        )
        assertEquals(50, chapterResult.totalCount)
        assertEquals(50, chapterResult.validCount)
        assertEquals(155, chapterResult.truncatedCount)
    }

    @Test
    fun studyTestAvailabilityAndResultPassFailScoringWorkCorrectly() {
        val upcoming = com.example.data.StudyTest(
            availableFrom = "2026-12-01",
            closingDate = "2026-12-07"
        )
        assertEquals(
            com.example.data.TestAvailabilityStatus.UPCOMING,
            upcoming.availabilityStatus("2026-11-20")
        )
        assertEquals(
            com.example.data.TestAvailabilityStatus.OPEN,
            upcoming.availabilityStatus("2026-12-03")
        )
        assertEquals(
            com.example.data.TestAvailabilityStatus.CLOSED,
            upcoming.availabilityStatus("2026-12-08")
        )

        val passedAttempt = TestAttempt(
            correctCount = 140,
            wrongCount = 40,
            skippedCount = 20,
            totalQuestions = 200,
            marksPerQuestion = 2,
            totalMarks = 400,
            passingPercentage = 35,
            score = 280
        )
        assertEquals(400L, passedAttempt.effectiveTotalMarks())
        assertEquals("70.0%", passedAttempt.percentageFormatted())
        assertEquals(true, passedAttempt.passStatus())

        val failedAttempt = passedAttempt.copy(correctCount = 50, score = 100)
        assertEquals("25.0%", failedAttempt.percentageFormatted())
        assertEquals(false, failedAttempt.passStatus())

        val unconfiguredPassAttempt = passedAttempt.copy(passingPercentage = 0L)
        assertEquals(null, unconfiguredPassAttempt.passStatus())
    }

    @Test
    fun dynamicVipPricingCalculatesDiscountsAndAutoExpiresOffersAndBanner() {
        // Auto discount calculation
        assertEquals(40L, com.example.data.VipPricingUtils.calculateDiscountPercent(200L, 120L))
        assertEquals(50L, com.example.data.VipPricingUtils.calculateDiscountPercent(999L, 499L))
        assertEquals(0L, com.example.data.VipPricingUtils.calculateDiscountPercent(99L, 99L))
        assertEquals(0L, com.example.data.VipPricingUtils.calculateDiscountPercent(99L, 120L))
        assertEquals(0L, com.example.data.VipPricingUtils.calculateDiscountPercent(0L, 50L))

        val plan = com.example.data.DynamicVipPlan(
            id = "PLAN_3M",
            title = "3 Months VIP",
            titleHindi = "3 महीना VIP",
            originalPriceInr = 249L,
            offerPriceInr = 149L,
            discountPercent = 40L,
            durationDays = 90L,
            durationLabel = "3 Months",
            active = true
        )

        // When offers are active
        assertEquals(149L, plan.effectivePrice(offerActiveNow = true))
        assertEquals(40L, plan.effectiveDiscountPercent(offerActiveNow = true))
        assertTrue(plan.hasActiveDiscount(offerActiveNow = true))
        val discountedVipPlan = plan.toVipPlan(offerActiveNow = true, globalOfferName = "Board Exam Sale")
        assertEquals(149L, discountedVipPlan.amountInr)
        assertEquals(249L, discountedVipPlan.originalPriceInr)
        assertEquals(40L, discountedVipPlan.discountPercent)
        assertEquals("Board Exam Sale", discountedVipPlan.appliedOfferName)

        // When offers are OFF or expired -> reverts to regular Old Price & 0% discount
        assertEquals(249L, plan.effectivePrice(offerActiveNow = false))
        assertEquals(0L, plan.effectiveDiscountPercent(offerActiveNow = false))
        assertFalse(plan.hasActiveDiscount(offerActiveNow = false))
        val regularVipPlan = plan.toVipPlan(offerActiveNow = false, globalOfferName = "Board Exam Sale")
        assertEquals(249L, regularVipPlan.amountInr)
        assertEquals(0L, regularVipPlan.discountPercent)
        assertEquals("", regularVipPlan.appliedOfferName)

        // Offer window & Promotional Banner auto-expiry
        val now = 1_750_000_000_000L
        val activeConfig = com.example.data.VipOfferConfig(
            offersEnabled = true,
            offerName = "Festive Offer",
            offerStartMillis = now - 86_400_000L,
            offerEndMillis = now + 86_400_000L,
            bannerEnabled = true,
            bannerHeading = "Special Offer!",
            bannerDescription = "Save up to 40%",
            bannerDiscountPercent = 40L
        )
        assertTrue(activeConfig.isOfferActiveNow(now))
        assertFalse(activeConfig.isOfferExpired(now))
        assertTrue(activeConfig.shouldShowBanner(now))

        // After offerEndMillis passes -> both offer AND banner auto-hide
        val afterExpiry = now + 100_000_000L
        assertTrue(activeConfig.isOfferExpired(afterExpiry))
        assertFalse(activeConfig.isOfferActiveNow(afterExpiry))
        assertFalse(activeConfig.shouldShowBanner(afterExpiry))

        // When bannerEnabled = false -> banner stays hidden even during active offer
        assertFalse(activeConfig.copy(bannerEnabled = false).shouldShowBanner(now))
        // When offersEnabled = false -> offer is inactive
        assertFalse(activeConfig.copy(offersEnabled = false).isOfferActiveNow(now))
    }
}



