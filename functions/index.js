const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-roystudy-f0042adf-9b4c-4146-902a-eabb8f575c25";
const db = getFirestore(DATABASE_ID);
const TOPIC_DAILY_TESTS = "daily_tests_class10";
const TOPIC_WEEKLY_CHAMPIONS = "weekly_champions_class10";

/**
 * Trusted server-side trigger: fires whenever a student submits a TestAttempt.
 * If the attempt is a DAILY test and the student has a PENDING, unrewarded referral,
 * executes a transaction-safe reward grant giving both referrer and referred student
 * the configured VIP days (default 7) and writes immutable audit records to /referral_transactions.
 */
exports.processReferralOnFirstDailyTest = onDocumentCreated(
  {
    document: "attempts/{attemptId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const attempt = event.data?.data();
    if (!attempt || attempt.testCategory !== "DAILY") {
      return;
    }

    const referredUid = attempt.userId;
    if (!referredUid) {
      return;
    }

    const referralRef = db.collection("referrals").doc(referredUid);
    const configRef = db.collection("referral_config").doc("settings");

    await db.runTransaction(async (tx) => {
      const referralSnap = await tx.get(referralRef);
      if (!referralSnap.exists) return;

      const referral = referralSnap.data();
      // Guard against duplicate reward processing, rejected/fraudulent referrals, or self-referrals
      if (
        referral.status !== "PENDING" ||
        referral.rewardGranted === true ||
        referral.referrerUid === referral.referredUid
      ) {
        return;
      }

      const configSnap = await tx.get(configRef);
      const config = configSnap.exists
        ? configSnap.data()
        : { enabled: true, rewardDays: 7, maxSuccessfulReferrals: 20 };

      if (config.enabled === false) {
        // Still record first Daily Test completion so Admin can review later if re-enabled
        tx.update(referralRef, {
          firstDailyTestCompleted: true,
          firstDailyTestId: attempt.testId || "",
          firstDailyTestTitle: attempt.testTitle || "",
          firstDailyTestAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        });
        return;
      }

      const rewardDays = Math.max(1, Math.min(365, Number(config.rewardDays || 7)));
      const maxSuccessful = Math.max(1, Number(config.maxSuccessfulReferrals || 20));

      const referrerRef = db.collection("users").doc(referral.referrerUid);
      const referredUserRef = db.collection("users").doc(referredUid);
      const referrerTxRef = db.collection("referral_transactions").doc(`ref_${referredUid}_referrer`);
      const referredTxRef = db.collection("referral_transactions").doc(`ref_${referredUid}_referred`);

      const [referrerSnap, referredUserSnap, existingReferrerTx, existingReferredTx] =
        await Promise.all([
          tx.get(referrerRef),
          tx.get(referredUserRef),
          tx.get(referrerTxRef),
          tx.get(referredTxRef),
        ]);

      if (existingReferrerTx.exists || existingReferredTx.exists) {
        return;
      }
      if (!referrerSnap.exists || !referredUserSnap.exists) {
        return;
      }

      const referrerData = referrerSnap.data();
      const referredData = referredUserSnap.data();
      const currentSuccessful = Number(referrerData.successfulReferrals || 0);

      if (currentSuccessful >= maxSuccessful) {
        tx.update(referralRef, {
          firstDailyTestCompleted: true,
          firstDailyTestId: attempt.testId || "",
          firstDailyTestTitle: attempt.testTitle || "",
          firstDailyTestAt: FieldValue.serverTimestamp(),
          status: "REJECTED",
          rejectionReason: `Referrer reached maximum limit of ${maxSuccessful} successful referrals`,
          processedBy: "CLOUD_FUNCTION",
          updatedAt: FieldValue.serverTimestamp(),
        });
        return;
      }

      const nowMs = Date.now();
      const bonusMs = rewardDays * 24 * 60 * 60 * 1000;

      const computeNextExpiry = (profile) => {
        const existingMs = profile.vipExpiresAt?.toMillis?.() || 0;
        const baseMs = profile.vipActive && existingMs > nowMs ? existingMs : nowMs;
        return Timestamp.fromMillis(baseMs + bonusMs);
      };

      const referrerNextExpiry = computeNextExpiry(referrerData);
      const referredNextExpiry = computeNextExpiry(referredData);

      // 1. Mark referral as SUCCESSFUL & rewarded
      tx.update(referralRef, {
        status: "SUCCESSFUL",
        firstDailyTestCompleted: true,
        firstDailyTestId: attempt.testId || "",
        firstDailyTestTitle: attempt.testTitle || "",
        firstDailyTestAt: FieldValue.serverTimestamp(),
        rewardGranted: true,
        rewardDays,
        processedBy: "CLOUD_FUNCTION",
        rewardedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });

      // 2. Credit Referrer VIP + counters
      tx.update(referrerRef, {
        vipActive: true,
        vipPlanId: referrerData.vipPlanId && referrerData.vipPlanId !== "NONE"
          ? referrerData.vipPlanId
          : "REFERRAL_BONUS",
        vipExpiresAt: referrerNextExpiry,
        successfulReferrals: currentSuccessful + 1,
        earnedVipDays: Number(referrerData.earnedVipDays || 0) + rewardDays,
        updatedAt: FieldValue.serverTimestamp(),
      });

      // 3. Credit Referred Student VIP + counter
      tx.update(referredUserRef, {
        vipActive: true,
        vipPlanId: referredData.vipPlanId && referredData.vipPlanId !== "NONE"
          ? referredData.vipPlanId
          : "REFERRAL_BONUS",
        vipExpiresAt: referredNextExpiry,
        earnedVipDays: Number(referredData.earnedVipDays || 0) + rewardDays,
        updatedAt: FieldValue.serverTimestamp(),
      });

      // 4. Write two immutable auditable reward transactions
      tx.set(referrerTxRef, {
        referralId: referredUid,
        beneficiaryUid: referral.referrerUid,
        beneficiaryName: referral.referrerName || referrerData.fullName || "Student",
        counterpartyUid: referredUid,
        counterpartyName: referral.referredName || referredData.fullName || "Student",
        role: "REFERRER",
        referralCode: referral.referralCode,
        rewardDays,
        vipExpiresAfter: referrerNextExpiry,
        processedBy: "CLOUD_FUNCTION",
        createdAt: FieldValue.serverTimestamp(),
      });

      tx.set(referredTxRef, {
        referralId: referredUid,
        beneficiaryUid: referredUid,
        beneficiaryName: referral.referredName || referredData.fullName || "Student",
        counterpartyUid: referral.referrerUid,
        counterpartyName: referral.referrerName || referrerData.fullName || "Student",
        role: "REFERRED",
        referralCode: referral.referralCode,
        rewardDays,
        vipExpiresAfter: referredNextExpiry,
        processedBy: "CLOUD_FUNCTION",
        createdAt: FieldValue.serverTimestamp(),
      });
    });
  }
);

/**
 * FCM Push Trigger #1: Fires when a Daily Test transitions to published = true.
 * Broadcasts a high-priority notification to all Class 10 Bihar Board students
 * subscribed to the `daily_tests_class10` topic and logs dispatch idempotently.
 */
exports.notifyOnDailyTestPublished = onDocumentWritten(
  {
    document: "tests/{testId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!after || after.published !== true || after.testCategory !== "DAILY") {
      return;
    }
    // Only send when transitioning from draft/non-existent to published
    if (before && before.published === true) {
      return;
    }

    const testId = event.params.testId;
    const logRef = db.collection("notification_logs").doc(`daily_test_${testId}`);
    const existingLog = await logRef.get();
    if (existingLog.exists) {
      return;
    }

    const title = `नया दैनिक टेस्ट लाइव है! (${after.subjectName || "Class 10"})`;
    const body = `${after.title || "Daily Test"} • ${after.chapterName || ""} (${after.questionCount || 0} Qs, ${after.durationMinutes || 30} min). अभी टेस्ट दें!`;

    await getMessaging().send({
      topic: TOPIC_DAILY_TESTS,
      notification: { title, body },
      data: {
        type: "DAILY_TEST_PUBLISHED",
        testId: String(testId),
        subjectName: String(after.subjectName || ""),
        chapterName: String(after.chapterName || ""),
        navTarget: "daily_tests",
        channelId: "daily_tests_channel",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "daily_tests_channel",
          color: "#0A43B8",
          sound: "default",
        },
      },
    });

    await logRef.set({
      type: "DAILY_TEST_PUBLISHED",
      title,
      body,
      refId: testId,
      targetSummary: `Topic: ${TOPIC_DAILY_TESTS}`,
      status: "SENT",
      createdAt: FieldValue.serverTimestamp(),
    });
  }
);

/**
 * FCM Push Trigger #2: Fires when Owner Admin declares and publishes a Weekly Champion.
 * 1. Sends a personalized direct congratulatory FCM push to the declared winner's device token.
 * 2. Broadcasts a non-PII winner announcement to the `weekly_champions_class10` topic.
 */
exports.notifyOnWeeklyChampionDeclared = onDocumentWritten(
  {
    document: "weekly_champions/{weekKey}",
    database: DATABASE_ID,
  },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!after || after.published !== true || !after.winnerUserId) {
      return;
    }
    // Only fire on first publication or if the winner UID changed
    if (before && before.published === true && before.winnerUserId === after.winnerUserId) {
      return;
    }

    const weekKey = event.params.weekKey;
    const winnerUid = after.winnerUserId;
    const logRef = db.collection("notification_logs").doc(`champion_${weekKey}_${winnerUid}`);
    const existingLog = await logRef.get();
    if (existingLog.exists) {
      return;
    }

    const winnerName = after.winnerName || "Student";
    const weekLabel = after.weekLabel || weekKey;
    const giftHint = after.giftDetails ? ` गिफ्ट: ${after.giftDetails}. ` : " ";
    const personalTitle = `बधाई हो ${winnerName}! आप Weekly Champion बने 🏆`;
    const personalBody = `${weekLabel} में ${after.totalMarks || 0} अंक के साथ आप नंबर 1 रहे!${giftHint}डिटेल्स देखने के लिए टैप करें।`;

    let directSent = false;
    const tokenRef = db.collection("fcm_tokens").doc(winnerUid);
    const tokenSnap = await tokenRef.get();
    if (tokenSnap.exists) {
      const tokenDoc = tokenSnap.data();
      if (tokenDoc.token && tokenDoc.weeklyChampionAlertsEnabled !== false) {
        try {
          await getMessaging().send({
            token: tokenDoc.token,
            notification: {
              title: personalTitle,
              body: personalBody,
            },
            data: {
              type: "WEEKLY_CHAMPION_WINNER",
              weekKey: String(weekKey),
              winnerUserId: String(winnerUid),
              navTarget: "weekly_champion",
              channelId: "weekly_champion_channel",
            },
            android: {
              priority: "high",
              notification: {
                channelId: "weekly_champion_channel",
                color: "#0A43B8",
                sound: "default",
              },
            },
          });
          directSent = true;
        } catch (err) {
          if (
            err?.code === "messaging/registration-token-not-registered" ||
            err?.code === "messaging/invalid-registration-token"
          ) {
            await tokenRef.delete().catch(() => {});
          } else {
            throw err;
          }
        }
      }
    }

    // Non-PII community announcement (never includes phone or email)
    const broadcastTitle = `साप्ताहिक चैंपियन घोषित! 🏆 (${weekLabel})`;
    const broadcastBody = `${winnerName} ने ${after.totalMarks || 0} अंकों के साथ ${weekLabel} का खिताब जीता! लीडरबोर्ड देखें।`;
    await getMessaging().send({
      topic: TOPIC_WEEKLY_CHAMPIONS,
      notification: {
        title: broadcastTitle,
        body: broadcastBody,
      },
      data: {
        type: "WEEKLY_CHAMPION_ANNOUNCED",
        weekKey: String(weekKey),
        navTarget: "weekly_champion",
        channelId: "weekly_champion_channel",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "weekly_champion_channel",
          color: "#0A43B8",
          sound: "default",
        },
      },
    });

    await logRef.set({
      type: "WEEKLY_CHAMPION_DECLARED",
      title: personalTitle,
      body: personalBody,
      refId: `${weekKey}_${winnerUid}`,
      targetSummary: directSent
        ? `Winner Direct Push + Topic: ${TOPIC_WEEKLY_CHAMPIONS}`
        : `Topic: ${TOPIC_WEEKLY_CHAMPIONS} (Winner token unavailable)`,
      status: "SENT",
      createdAt: FieldValue.serverTimestamp(),
    });
  }
);
