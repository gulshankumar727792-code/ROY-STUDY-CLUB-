const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "student_alice_101";
const BOB_UID = "student_bob_202";
const OWNER_UID = "owner_admin_roy_001";
const OWNER_EMAIL = "ydharmendrakumar686@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

function futureDate(msAhead = 86400000) {
  return new Date(Date.now() + msAhead);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function ownerDb() {
  return testEnv
    .authenticatedContext(OWNER_UID, {
      email: OWNER_EMAIL,
      email_verified: true,
    })
    .firestore();
}

function spoofedOwnerDb() {
  return testEnv
    .authenticatedContext("spoof_attacker_999", {
      email: OWNER_EMAIL,
      email_verified: false,
    })
    .firestore();
}

function aliceDb() {
  return testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@student.test",
      email_verified: true,
    })
    .firestore();
}

function bobDb() {
  return testEnv
    .authenticatedContext(BOB_UID, {
      email: "bob@student.test",
      email_verified: true,
    })
    .firestore();
}

// 1. Unauthenticated Denial
test("Unauthenticated users are blocked from all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("tests").where("published", "==", true).get());
  await assertFails(db.collection("attempts").where("userId", "==", ALICE_UID).get());
  await assertFails(db.collection("payment_requests").where("userId", "==", ALICE_UID).get());
  await assertFails(db.collection("weekly_entries").where("publicVisible", "==", true).get());
  await assertFails(db.collection("vip_notes").where("published", "==", true).get());
});

// 2. Email Spoofing & Privilege Escalation Protection
test("Spoofed admin email with email_verified=false is denied admin writes", async () => {
  const spoofDb = spoofedOwnerDb();
  const now = pastDate();
  await assertFails(
    spoofDb.collection("subjects").doc("bseb_math").set({
      name: "Mathematics",
      nameHindi: "गणित",
      chapters: ["Real Numbers"],
      displayOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    spoofDb.collection("admins").doc("spoof_attacker_999").set({
      uid: "spoof_attacker_999",
      email: OWNER_EMAIL,
      label: "Fake Admin",
      createdAt: now,
    })
  );
});

// 3. Student Profile Creation, PII Isolation & VIP Tamper Protection
test("Student profile CRUD enforces PII isolation and blocks VIP self-activation", async () => {
  const aDb = aliceDb();
  const bDb = bobDb();
  const now = pastDate();

  // Valid Kotlin-aligned profile creation
  await assertSucceeds(
    aDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Kumari",
      email: "alice@student.test",
      phone: "9876543210",
      district: "Patna",
      schoolName: "Patna High School",
      boardClass: "Class 10 Bihar Board",
      vipActive: false,
      vipPlanId: "NONE",
      giftEligible: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid partial profile update
  await assertSucceeds(
    aDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice Verma",
      phone: "9876543211",
      district: "Gaya",
      schoolName: "Gaya High School",
      updatedAt: pastDate(1000),
    })
  );

  // Student CANNOT self-activate VIP
  await assertFails(
    aDb.collection("users").doc(ALICE_UID).update({
      vipActive: true,
      vipPlanId: "PLAN_12M",
      vipExpiresAt: futureDate(),
      updatedAt: pastDate(500),
    })
  );

  // Student CANNOT inject ghost fields (Shadow Update Test)
  await assertFails(
    aDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Cross-user PII isolation: Bob cannot read or list Alice's private profile
  await assertFails(bDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bDb.collection("users").get());
});

// 4. Owner Admin Subject, Test & Unpublished Content Protection
test("Owner Admin manages subjects/tests; unpublished tests stay hidden from students", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const now = pastDate();

  await assertSucceeds(
    oDb.collection("subjects").doc("bseb_science").set({
      name: "Science",
      nameHindi: "विज्ञान",
      chapters: ["Light - Reflection and Refraction"],
      displayOrder: 2,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Create UNPUBLISHED draft daily test
  await assertSucceeds(
    oDb.collection("tests").doc("daily_01").set({
      title: "Science Daily Test #1",
      subjectId: "bseb_science",
      subjectName: "Science",
      chapterName: "Light - Reflection and Refraction",
      testDate: "2026-11-20",
      durationMinutes: 45,
      testCategory: "DAILY",
      weekKey: "2026-W47",
      questionCount: 1,
      published: false,
      createdBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    oDb.collection("test_papers").doc("daily_01").set({
      testId: "daily_01",
      testCategory: "DAILY",
      published: false,
      questions: [
        {
          questionText: "प्रकाश की किरण गमन करती है?",
          optionA: "सीधी रेखा में",
          optionB: "टेढ़ी रेखा में",
          optionC: "किसी भी दिशा में",
          optionD: "इनमें से कोई नहीं",
          correctIndex: 0,
          explanation: "प्रकाश हमेशा सीधी रेखा में गमन करता है।",
        },
      ],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student CANNOT read unpublished test or paper, nor list unfiltered tests
  await assertFails(aDb.collection("tests").doc("daily_01").get());
  await assertFails(aDb.collection("test_papers").doc("daily_01").get());
  await assertFails(aDb.collection("tests").get());

  // Student CAN query published tests
  await assertSucceeds(aDb.collection("tests").where("published", "==", true).get());

  // Publish test & paper
  await assertSucceeds(
    oDb.collection("tests").doc("daily_01").update({
      published: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    oDb.collection("test_papers").doc("daily_01").update({
      published: true,
      updatedAt: pastDate(1000),
    })
  );

  // Now student CAN read published daily test & paper
  await assertSucceeds(aDb.collection("tests").doc("daily_01").get());
  await assertSucceeds(aDb.collection("test_papers").doc("daily_01").get());
});

// 5. Daily Test Single-Attempt, Atomic Weekly Leaderboard & Tamper Rejection
test("Daily test attempt is atomic with weekly leaderboard and enforces single attempt & score integrity", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const now = pastDate();

  await oDb.collection("tests").doc("daily_02").set({
    title: "Math Daily Test #2",
    subjectId: "bseb_math",
    subjectName: "Mathematics",
    chapterName: "Real Numbers",
    testDate: "2026-11-20",
    durationMinutes: 30,
    testCategory: "DAILY",
    weekKey: "2026-W47",
    questionCount: 2,
    published: true,
    createdBy: OWNER_UID,
    createdAt: now,
    updatedAt: now,
  });

  // Tampered score (score != correctCount) MUST FAIL
  await assertFails(
    aDb.collection("attempts").doc(`daily_02_${ALICE_UID}`).set({
      testId: "daily_02",
      testTitle: "Math Daily Test #2",
      subjectName: "Mathematics",
      chapterName: "Real Numbers",
      testCategory: "DAILY",
      weekKey: "2026-W47",
      userId: ALICE_UID,
      studentName: "Alice Kumari",
      selectedAnswers: [0, 1],
      correctCount: 1,
      wrongCount: 1,
      skippedCount: 0,
      totalQuestions: 2,
      score: 50, // tampered!
      timeTakenSeconds: 45,
      createdAt: now,
    })
  );

  // Valid atomic WriteBatch: attempt + weekly leaderboard entry
  const batch = aDb.batch();
  batch.set(aDb.collection("attempts").doc(`daily_02_${ALICE_UID}`), {
    testId: "daily_02",
    testTitle: "Math Daily Test #2",
    subjectName: "Mathematics",
    chapterName: "Real Numbers",
    testCategory: "DAILY",
    weekKey: "2026-W47",
    userId: ALICE_UID,
    studentName: "Alice Kumari",
    selectedAnswers: [0, 1],
    correctCount: 2,
    wrongCount: 0,
    skippedCount: 0,
    totalQuestions: 2,
    score: 2,
    timeTakenSeconds: 42,
    createdAt: now,
  });
  batch.set(aDb.collection("weekly_entries").doc(`2026-W47_${ALICE_UID}`), {
    weekKey: "2026-W47",
    userId: ALICE_UID,
    displayName: "Alice Kumari",
    district: "Patna",
    vipBadge: false,
    totalCorrect: 2,
    totalMarks: 2,
    totalQuestionsAttempted: 2,
    testsCount: 1,
    totalTimeSeconds: 42,
    countedTestIds: ["daily_02"],
    lastAttemptTestId: "daily_02",
    publicVisible: true,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Student CANNOT update/retake attempt
  await assertFails(
    aDb.collection("attempts").doc(`daily_02_${ALICE_UID}`).update({
      score: 2,
      timeTakenSeconds: 10,
    })
  );

  // Student CAN query own attempts & public weekly leaderboard
  await assertSucceeds(aDb.collection("attempts").where("userId", "==", ALICE_UID).get());
  await assertFails(aDb.collection("attempts").get());
  await assertSucceeds(
    aDb
      .collection("weekly_entries")
      .where("publicVisible", "==", true)
      .where("weekKey", "==", "2026-W47")
      .get()
  );
});

// 6. VIP WhatsApp Payment Approval, VIP Content Gating & Automatic Server-Side Expiry
test("VIP WhatsApp payment approval unlocks VIP tests/notes and auto-locks on expiry", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const bDb = bobDb();
  const now = pastDate();

  // Seed Alice profile (non-VIP)
  await aDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    fullName: "Alice Kumari",
    email: "alice@student.test",
    phone: "9876543210",
    district: "Patna",
    schoolName: "Patna High School",
    boardClass: "Class 10 Bihar Board",
    vipActive: false,
    vipPlanId: "NONE",
    giftEligible: false,
    createdAt: now,
    updatedAt: now,
  });

  // Owner creates published VIP test paper & VIP special note
  await oDb.collection("test_papers").doc("vip_test_1").set({
    testId: "vip_test_1",
    testCategory: "VIP_TEST",
    published: true,
    questions: [
      {
        questionText: "VIP Q1",
        optionA: "1",
        optionB: "2",
        optionC: "3",
        optionD: "4",
        correctIndex: 1,
        explanation: "Exp",
      },
    ],
    createdAt: now,
    updatedAt: now,
  });
  await oDb.collection("vip_note_contents").doc("vip_note_1").set({
    noteId: "vip_note_1",
    content: "Top Class 10 Bihar Board Science Formulas",
    keyPoints: ["V = IR", "F = ma"],
    published: true,
    createdAt: now,
    updatedAt: now,
  });

  // Non-VIP Alice CANNOT access VIP paper or VIP note content
  await assertFails(aDb.collection("test_papers").doc("vip_test_1").get());
  await assertFails(aDb.collection("vip_note_contents").doc("vip_note_1").get());

  // Alice submits invalid amount for PLAN_12M -> FAILS
  await assertFails(
    aDb.collection("payment_requests").doc("pay_bad").set({
      userId: ALICE_UID,
      studentName: "Alice Kumari",
      studentPhone: "9876543210",
      planId: "PLAN_12M",
      planName: "12 Months VIP",
      amountInr: 1, // tampered amount!
      durationDays: 365,
      transactionRef: "UTR99999",
      hasScreenshot: false,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice submits valid WhatsApp payment request + screenshot proof atomically
  const payBatch = aDb.batch();
  payBatch.set(aDb.collection("payment_requests").doc("pay_req_1"), {
    userId: ALICE_UID,
    studentName: "Alice Kumari",
    studentPhone: "9876543210",
    planId: "PLAN_12M",
    planName: "12 Months VIP",
    amountInr: 999,
    durationDays: 365,
    transactionRef: "UTR123456789",
    hasScreenshot: true,
    status: "PENDING",
    createdAt: now,
    updatedAt: now,
  });
  payBatch.set(aDb.collection("payment_proofs").doc("pay_req_1"), {
    requestId: "pay_req_1",
    userId: ALICE_UID,
    imageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk",
    createdAt: now,
  });
  await assertSucceeds(payBatch.commit());

  // Alice CANNOT approve her own payment; Bob CANNOT read Alice's payment proof
  await assertFails(
    aDb.collection("payment_requests").doc("pay_req_1").update({
      status: "APPROVED",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(bDb.collection("payment_proofs").doc("pay_req_1").get());

  // Owner Admin approves payment & activates 12-Month VIP + gift eligibility
  const approveBatch = oDb.batch();
  const validExpiry = futureDate(365 * 24 * 60 * 60 * 1000);
  approveBatch.update(oDb.collection("payment_requests").doc("pay_req_1"), {
    status: "APPROVED",
    adminRemark: "Verified on WhatsApp",
    reviewedBy: OWNER_UID,
    reviewedAt: pastDate(500),
    activatedUntil: validExpiry,
    updatedAt: pastDate(500),
  });
  approveBatch.update(oDb.collection("users").doc(ALICE_UID), {
    vipActive: true,
    vipPlanId: "PLAN_12M",
    vipExpiresAt: validExpiry,
    giftEligible: true,
    updatedAt: pastDate(500),
  });
  await assertSucceeds(approveBatch.commit());

  // Active VIP Alice CAN now read VIP test paper and VIP special note body!
  await assertSucceeds(aDb.collection("test_papers").doc("vip_test_1").get());
  await assertSucceeds(aDb.collection("vip_note_contents").doc("vip_note_1").get());

  // Simulate VIP expiry in the past -> Firestore rules AUTOMATICALLY revoke access!
  await assertSucceeds(
    oDb.collection("users").doc(ALICE_UID).update({
      vipExpiresAt: pastDate(60000),
      updatedAt: pastDate(200),
    })
  );
  await assertFails(aDb.collection("test_papers").doc("vip_test_1").get());
  await assertFails(aDb.collection("vip_note_contents").doc("vip_note_1").get());
});

// 7. Weekly Champion Announcement (Admin-Only Write, Zero PII Public Read)
test("Only Owner Admin can declare Weekly Champion; students can read published champions", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const now = pastDate();

  const champPayload = {
    weekKey: "2026-W47",
    weekLabel: "Week 2026-W47",
    winnerUserId: ALICE_UID,
    winnerName: "Alice Kumari",
    winnerDistrict: "Patna",
    totalCorrect: 48,
    totalMarks: 48,
    totalTimeSeconds: 1200,
    testsCount: 5,
    giftDetails: "Bihar Board Model Set Kit",
    specialOffer: "1 Month Free VIP",
    videoCallStatus: "SCHEDULED",
    announcementNote: "Congratulations to our Class 10 Champion!",
    verifiedBy: OWNER_UID,
    published: true,
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(aDb.collection("weekly_champions").doc("2026-W47").set(champPayload));
  await assertSucceeds(oDb.collection("weekly_champions").doc("2026-W47").set(champPayload));
  await assertSucceeds(
    aDb.collection("weekly_champions").where("published", "==", true).get()
  );
});

// 8. Referral Code Index Security & Legacy Profile Backfill
test("Referral codes allow single-doc lookup, deny student listing, and allow one-time legacy backfill", async () => {
  const aDb = aliceDb();
  const bDb = bobDb();
  const now = pastDate();

  // Alice creates legacy profile without referralCode
  await assertSucceeds(
    aDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Kumari",
      email: "alice@student.test",
      phone: "9876543210",
      district: "Patna",
      schoolName: "Patna High School",
      boardClass: "Class 10 Bihar Board",
      vipActive: false,
      vipPlanId: "NONE",
      giftEligible: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // One-time legacy backfill succeeds with 0 counters
  const backfillBatch = aDb.batch();
  backfillBatch.update(aDb.collection("users").doc(ALICE_UID), {
    referralCode: "ROYALICE1",
    referralLocked: true,
    successfulReferrals: 0,
    earnedVipDays: 0,
    updatedAt: pastDate(1000),
  });
  backfillBatch.set(aDb.collection("referral_codes").doc("ROYALICE1"), {
    code: "ROYALICE1",
    ownerUid: ALICE_UID,
    ownerName: "Alice Kumari",
    active: true,
    createdAt: now,
  });
  await assertSucceeds(backfillBatch.commit());

  // Bob CAN look up a specific code by exact ID, but CANNOT list all referral codes
  await assertSucceeds(bDb.collection("referral_codes").doc("ROYALICE1").get());
  await assertFails(bDb.collection("referral_codes").get());

  // Alice CANNOT tamper with her own earnedVipDays or successfulReferrals
  await assertFails(
    aDb.collection("users").doc(ALICE_UID).update({
      successfulReferrals: 5,
      earnedVipDays: 35,
      updatedAt: pastDate(500),
    })
  );
});

// 9. Referral Attribution Lock, Self-Referral Block, Duplicate Block & Disabled Toggle
test("Referrals enforce registration lock, prevent self-referral & duplicates, and honor disabled config", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const bDb = bobDb();
  const now = pastDate();

  // Alice has an active referral code
  await assertSucceeds(
    aDb.collection("referral_codes").doc("ROY4K9M2P").set({
      code: "ROY4K9M2P",
      ownerUid: ALICE_UID,
      ownerName: "Alice Kumari",
      active: true,
      createdAt: now,
    })
  );

  // Self-referral MUST FAIL
  await assertFails(
    aDb.collection("referrals").doc(ALICE_UID).set({
      referrerUid: ALICE_UID,
      referrerName: "Alice Kumari",
      referredUid: ALICE_UID,
      referredName: "Alice Kumari",
      referralCode: "ROY4K9M2P",
      status: "PENDING",
      firstDailyTestCompleted: false,
      rewardGranted: false,
      rewardDays: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin disables referrals -> Bob's referral registration MUST FAIL
  await assertSucceeds(
    oDb.collection("referral_config").doc("settings").set({
      enabled: false,
      rewardDays: 7,
      maxSuccessfulReferrals: 20,
      updatedBy: OWNER_UID,
      updatedAt: now,
    })
  );
  await assertFails(
    bDb.collection("referrals").doc(BOB_UID).set({
      referrerUid: ALICE_UID,
      referrerName: "Alice Kumari",
      referredUid: BOB_UID,
      referredName: "Bob Kumar",
      referralCode: "ROY4K9M2P",
      status: "PENDING",
      firstDailyTestCompleted: false,
      rewardGranted: false,
      rewardDays: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin re-enables referrals
  await assertSucceeds(
    oDb.collection("referral_config").doc("settings").update({
      enabled: true,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(500),
    })
  );

  // Bob registers atomically with Alice's referral code -> SUCCEEDS & locks attribution
  const regBatch = bDb.batch();
  regBatch.set(bDb.collection("users").doc(BOB_UID), {
    userId: BOB_UID,
    fullName: "Bob Kumar",
    email: "bob@student.test",
    phone: "9988776655",
    district: "Nalanda",
    schoolName: "Nalanda High School",
    boardClass: "Class 10 Bihar Board",
    vipActive: false,
    vipPlanId: "NONE",
    giftEligible: false,
    referralCode: "ROY7B8C9D",
    referredByUid: ALICE_UID,
    referredByCode: "ROY4K9M2P",
    referralLocked: true,
    successfulReferrals: 0,
    earnedVipDays: 0,
    createdAt: now,
    updatedAt: now,
  });
  regBatch.set(bDb.collection("referral_codes").doc("ROY7B8C9D"), {
    code: "ROY7B8C9D",
    ownerUid: BOB_UID,
    ownerName: "Bob Kumar",
    active: true,
    createdAt: now,
  });
  regBatch.set(bDb.collection("referrals").doc(BOB_UID), {
    referrerUid: ALICE_UID,
    referrerName: "Alice Kumari",
    referredUid: BOB_UID,
    referredName: "Bob Kumar",
    referralCode: "ROY4K9M2P",
    status: "PENDING",
    firstDailyTestCompleted: false,
    rewardGranted: false,
    rewardDays: 0,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(regBatch.commit());

  // Duplicate referral creation by Bob MUST FAIL (doc already exists & locked)
  await assertFails(
    bDb.collection("referrals").doc(BOB_UID).set({
      referrerUid: ALICE_UID,
      referrerName: "Alice Kumari",
      referredUid: BOB_UID,
      referredName: "Bob Kumar",
      referralCode: "ROY4K9M2P",
      status: "PENDING",
      firstDailyTestCompleted: false,
      rewardGranted: false,
      rewardDays: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
});

// 10. First Daily Test Milestone, Student Tamper Protection & Admin Transaction-Safe Rewarding
test("First Daily Test updates referral atomically; only Owner Admin can credit VIP & immutable transactions", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const bDb = bobDb();
  const now = pastDate();

  // Seed Alice & Bob profiles, referral code, referral record, and a published Daily Test
  await aDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    fullName: "Alice Kumari",
    email: "alice@student.test",
    phone: "",
    district: "Patna",
    schoolName: "",
    boardClass: "Class 10 Bihar Board",
    vipActive: false,
    vipPlanId: "NONE",
    giftEligible: false,
    referralCode: "ROY4K9M2P",
    referredByUid: "",
    referredByCode: "",
    referralLocked: true,
    successfulReferrals: 0,
    earnedVipDays: 0,
    createdAt: now,
    updatedAt: now,
  });
  await aDb.collection("referral_codes").doc("ROY4K9M2P").set({
    code: "ROY4K9M2P",
    ownerUid: ALICE_UID,
    ownerName: "Alice Kumari",
    active: true,
    createdAt: now,
  });
  const regBatch = bDb.batch();
  regBatch.set(bDb.collection("users").doc(BOB_UID), {
    userId: BOB_UID,
    fullName: "Bob Kumar",
    email: "bob@student.test",
    phone: "",
    district: "Gaya",
    schoolName: "",
    boardClass: "Class 10 Bihar Board",
    vipActive: false,
    vipPlanId: "NONE",
    giftEligible: false,
    referralCode: "ROY7B8C9D",
    referredByUid: ALICE_UID,
    referredByCode: "ROY4K9M2P",
    referralLocked: true,
    successfulReferrals: 0,
    earnedVipDays: 0,
    createdAt: now,
    updatedAt: now,
  });
  regBatch.set(bDb.collection("referrals").doc(BOB_UID), {
    referrerUid: ALICE_UID,
    referrerName: "Alice Kumari",
    referredUid: BOB_UID,
    referredName: "Bob Kumar",
    referralCode: "ROY4K9M2P",
    status: "PENDING",
    firstDailyTestCompleted: false,
    rewardGranted: false,
    rewardDays: 0,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(regBatch.commit());

  await oDb.collection("tests").doc("daily_ref_test").set({
    title: "Bihar Board Science Daily Test",
    subjectId: "bseb_science",
    subjectName: "Science",
    chapterName: "Electricity",
    testDate: "2026-11-20",
    durationMinutes: 30,
    testCategory: "DAILY",
    weekKey: "2026-W47",
    questionCount: 1,
    published: true,
    createdBy: OWNER_UID,
    createdAt: now,
    updatedAt: now,
  });

  // Bob CANNOT self-approve his referral or grant himself VIP days
  await assertFails(
    bDb.collection("referrals").doc(BOB_UID).update({
      status: "SUCCESSFUL",
      rewardGranted: true,
      rewardDays: 7,
      updatedAt: pastDate(500),
    })
  );

  // Bob CANNOT mark firstDailyTestCompleted without a real attempt document
  await assertFails(
    bDb.collection("referrals").doc(BOB_UID).update({
      firstDailyTestCompleted: true,
      firstDailyTestId: "daily_ref_test",
      firstDailyTestTitle: "Bihar Board Science Daily Test",
      firstDailyTestAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Bob submits real Daily Test attempt + marks firstDailyTestCompleted atomically -> SUCCEEDS
  const testBatch = bDb.batch();
  testBatch.set(bDb.collection("attempts").doc(`daily_ref_test_${BOB_UID}`), {
    testId: "daily_ref_test",
    testTitle: "Bihar Board Science Daily Test",
    subjectName: "Science",
    chapterName: "Electricity",
    testCategory: "DAILY",
    weekKey: "2026-W47",
    userId: BOB_UID,
    studentName: "Bob Kumar",
    selectedAnswers: [0],
    correctCount: 1,
    wrongCount: 0,
    skippedCount: 0,
    totalQuestions: 1,
    score: 1,
    timeTakenSeconds: 35,
    createdAt: now,
  });
  testBatch.update(bDb.collection("referrals").doc(BOB_UID), {
    firstDailyTestCompleted: true,
    firstDailyTestId: "daily_ref_test",
    firstDailyTestTitle: "Bihar Board Science Daily Test",
    firstDailyTestAt: pastDate(500),
    updatedAt: pastDate(500),
  });
  await assertSucceeds(testBatch.commit());

  // Students CANNOT forge /referral_transactions
  const sevenDaysFuture = futureDate(7 * 24 * 60 * 60 * 1000);
  await assertFails(
    aDb.collection("referral_transactions").doc(`ref_${BOB_UID}_referrer`).set({
      referralId: BOB_UID,
      beneficiaryUid: ALICE_UID,
      beneficiaryName: "Alice Kumari",
      counterpartyUid: BOB_UID,
      counterpartyName: "Bob Kumar",
      role: "REFERRER",
      referralCode: "ROY4K9M2P",
      rewardDays: 7,
      vipExpiresAfter: sevenDaysFuture,
      processedBy: ALICE_UID,
      createdAt: now,
    })
  );

  // Owner Admin processes reward atomically: updates referral, both user VIP expiries, and immutable transactions
  const rewardBatch = oDb.batch();
  rewardBatch.update(oDb.collection("referrals").doc(BOB_UID), {
    status: "SUCCESSFUL",
    rewardGranted: true,
    rewardDays: 7,
    processedBy: OWNER_UID,
    rewardedAt: pastDate(200),
    updatedAt: pastDate(200),
  });
  rewardBatch.update(oDb.collection("users").doc(ALICE_UID), {
    vipActive: true,
    vipPlanId: "REFERRAL_BONUS",
    vipExpiresAt: sevenDaysFuture,
    successfulReferrals: 1,
    earnedVipDays: 7,
    updatedAt: pastDate(200),
  });
  rewardBatch.update(oDb.collection("users").doc(BOB_UID), {
    vipActive: true,
    vipPlanId: "REFERRAL_BONUS",
    vipExpiresAt: sevenDaysFuture,
    earnedVipDays: 7,
    updatedAt: pastDate(200),
  });
  rewardBatch.set(oDb.collection("referral_transactions").doc(`ref_${BOB_UID}_referrer`), {
    referralId: BOB_UID,
    beneficiaryUid: ALICE_UID,
    beneficiaryName: "Alice Kumari",
    counterpartyUid: BOB_UID,
    counterpartyName: "Bob Kumar",
    role: "REFERRER",
    referralCode: "ROY4K9M2P",
    rewardDays: 7,
    vipExpiresAfter: sevenDaysFuture,
    processedBy: OWNER_UID,
    createdAt: pastDate(200),
  });
  rewardBatch.set(oDb.collection("referral_transactions").doc(`ref_${BOB_UID}_referred`), {
    referralId: BOB_UID,
    beneficiaryUid: BOB_UID,
    beneficiaryName: "Bob Kumar",
    counterpartyUid: ALICE_UID,
    counterpartyName: "Alice Kumari",
    role: "REFERRED",
    referralCode: "ROY4K9M2P",
    rewardDays: 7,
    vipExpiresAfter: sevenDaysFuture,
    processedBy: OWNER_UID,
    createdAt: pastDate(200),
  });
  await assertSucceeds(rewardBatch.commit());

  // Both Alice & Bob CAN read their own reward transaction history, NOT each other's
  await assertSucceeds(
    aDb.collection("referral_transactions").where("beneficiaryUid", "==", ALICE_UID).get()
  );
  await assertFails(
    aDb.collection("referral_transactions").doc(`ref_${BOB_UID}_referred`).get()
  );

  // Immutable audit trail: even Owner Admin CANNOT update or delete /referral_transactions
  await assertFails(
    oDb.collection("referral_transactions").doc(`ref_${BOB_UID}_referrer`).update({
      rewardDays: 30,
    })
  );
  await assertFails(
    oDb.collection("referral_transactions").doc(`ref_${BOB_UID}_referrer`).delete()
  );

  // Double-reward protection: Admin CANNOT re-update an already rewarded referral
  await assertFails(
    oDb.collection("referrals").doc(BOB_UID).update({
      rewardDays: 14,
      updatedAt: pastDate(100),
    })
  );
});

// 11. FCM Device Tokens (Owner Isolation & Schema Validation) + Server-Only Notification Logs
test("FCM tokens enforce strict owner isolation and notification logs deny all client writes", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const bDb = bobDb();
  const now = pastDate();

  const validToken = "fcm_token_valid_sample_1234567890_abcdef";

  // Alice CAN create, read, update, and delete her own FCM device token
  await assertSucceeds(
    aDb.collection("fcm_tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      token: validToken,
      dailyTestAlertsEnabled: true,
      weeklyChampionAlertsEnabled: true,
      platform: "android",
      updatedAt: now,
    })
  );
  await assertSucceeds(aDb.collection("fcm_tokens").doc(ALICE_UID).get());
  await assertSucceeds(
    aDb.collection("fcm_tokens").doc(ALICE_UID).update({
      dailyTestAlertsEnabled: false,
      updatedAt: pastDate(500),
    })
  );

  // Invalid schema rejected (short token, wrong platform, mismatched userId)
  await assertFails(
    aDb.collection("fcm_tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      token: "short",
      dailyTestAlertsEnabled: true,
      weeklyChampionAlertsEnabled: true,
      platform: "android",
      updatedAt: now,
    })
  );
  await assertFails(
    aDb.collection("fcm_tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      token: validToken,
      dailyTestAlertsEnabled: true,
      weeklyChampionAlertsEnabled: true,
      platform: "web",
      updatedAt: now,
    })
  );

  // Bob CANNOT read, write, list, or delete Alice's FCM token
  await assertFails(bDb.collection("fcm_tokens").doc(ALICE_UID).get());
  await assertFails(bDb.collection("fcm_tokens").get());
  await assertFails(
    bDb.collection("fcm_tokens").doc(ALICE_UID).set({
      userId: ALICE_UID,
      token: validToken,
      dailyTestAlertsEnabled: false,
      weeklyChampionAlertsEnabled: false,
      platform: "android",
      updatedAt: now,
    })
  );
  await assertFails(bDb.collection("fcm_tokens").doc(ALICE_UID).delete());

  // Owner Admin CAN read & list FCM tokens (e.g., diagnostics), and Alice CAN delete her own on sign-out
  await assertSucceeds(oDb.collection("fcm_tokens").doc(ALICE_UID).get());
  await assertSucceeds(oDb.collection("fcm_tokens").get());
  await assertSucceeds(aDb.collection("fcm_tokens").doc(ALICE_UID).delete());

  // Server-only /notification_logs: Admin can read/list, students CANNOT read/list, NO client (even Admin) can write
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("notification_logs").doc("daily_test_t1").set({
      type: "DAILY_TEST_PUBLISHED",
      title: "New Daily Test Live",
      body: "Science • Light Reflection",
      refId: "t1",
      targetSummary: "topic:daily_tests_class10",
      status: "SENT",
      createdAt: now,
    });
  });

  await assertSucceeds(oDb.collection("notification_logs").doc("daily_test_t1").get());
  await assertSucceeds(oDb.collection("notification_logs").get());
  await assertFails(aDb.collection("notification_logs").doc("daily_test_t1").get());
  await assertFails(aDb.collection("notification_logs").get());
  await assertFails(
    oDb.collection("notification_logs").doc("forged_log").set({
      type: "DAILY_TEST_PUBLISHED",
      title: "Forged",
      body: "Client write must fail",
      refId: "t2",
      targetSummary: "topic:daily_tests_class10",
      status: "SENT",
      createdAt: now,
    })
  );
  await assertFails(oDb.collection("notification_logs").doc("daily_test_t1").delete());
});

// 12. Bulk Chapter Test (up to 50 MCQs) & Weekly Mega Test (up to 200 MCQs) Workflow + Attempt Locking
test("Admin publishes 50-Q Chapter Test and 200-Q Weekly Mega Test; students read, submit once, and cannot tamper", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const now = pastDate();

  function buildQuestions(count) {
    const arr = [];
    for (let i = 1; i <= count; i++) {
      arr.push({
        questionText: `Bihar Board Q${i}: What is ${i} + ${i}?`,
        optionA: `${i * 2 - 1}`,
        optionB: `${i * 2}`,
        optionC: `${i * 2 + 1}`,
        optionD: `${i * 2 + 2}`,
        correctIndex: 1,
        explanation: `${i} + ${i} = ${i * 2}`,
      });
    }
    return arr;
  }

  // Student CANNOT create a Chapter Test or Weekly Mega Test
  await assertFails(
    aDb.collection("tests").doc("chapter_unauthorized").set({
      title: "Unauthorized Chapter Test",
      subjectId: "bseb_science",
      subjectName: "Science",
      chapterName: "Electricity",
      testDate: "2026-11-20",
      durationMinutes: 45,
      testCategory: "CHAPTER_TEST",
      weekKey: "2026-W47",
      questionCount: 5,
      published: true,
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin publishes 50-question CHAPTER_TEST
  const chapterBatch = oDb.batch();
  chapterBatch.set(oDb.collection("tests").doc("chapter_elec_50"), {
    title: "Electricity - Bulk Chapter Test",
    subjectId: "bseb_science",
    subjectName: "Science",
    chapterName: "12. विद्युत (Electricity)",
    testDate: "2026-11-20",
    durationMinutes: 50,
    passingPercentage: 40,
    marksPerQuestion: 1,
    testCategory: "CHAPTER_TEST",
    weekKey: "2026-W47",
    questionCount: 50,
    published: true,
    createdBy: OWNER_UID,
    createdAt: now,
    updatedAt: now,
  });
  chapterBatch.set(oDb.collection("test_papers").doc("chapter_elec_50"), {
    testId: "chapter_elec_50",
    testCategory: "CHAPTER_TEST",
    published: true,
    questions: buildQuestions(50),
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(chapterBatch.commit());

  // Admin publishes 200-question WEEKLY_MEGA test with availability/closing dates, 2 marks/Q, 35% passing
  const megaBatch = oDb.batch();
  megaBatch.set(oDb.collection("tests").doc("weekly_mega_200"), {
    title: "Bihar Board Weekly Mega Exam #1",
    subjectId: "ALL_SUBJECTS",
    subjectName: "All Subjects Combined",
    chapterName: "Full Weekly Mega Syllabus",
    testDate: "2026-11-20",
    availableFrom: "2026-11-20",
    closingDate: "2026-11-27",
    durationMinutes: 180,
    passingPercentage: 35,
    marksPerQuestion: 2,
    testCategory: "WEEKLY_MEGA",
    weekKey: "2026-W47",
    questionCount: 200,
    published: true,
    createdBy: OWNER_UID,
    createdAt: now,
    updatedAt: now,
  });
  megaBatch.set(oDb.collection("test_papers").doc("weekly_mega_200"), {
    testId: "weekly_mega_200",
    testCategory: "WEEKLY_MEGA",
    published: true,
    questions: buildQuestions(200),
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(megaBatch.commit());

  // Exceeding 200 questions MUST FAIL
  await assertFails(
    oDb.collection("test_papers").doc("weekly_mega_201").set({
      testId: "weekly_mega_201",
      testCategory: "WEEKLY_MEGA",
      published: true,
      questions: buildQuestions(201),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-VIP student CAN read both published CHAPTER_TEST and WEEKLY_MEGA metadata & 200-Q paper
  await assertSucceeds(aDb.collection("tests").doc("chapter_elec_50").get());
  await assertSucceeds(aDb.collection("test_papers").doc("chapter_elec_50").get());
  await assertSucceeds(aDb.collection("tests").doc("weekly_mega_200").get());
  await assertSucceeds(aDb.collection("test_papers").doc("weekly_mega_200").get());

  // Build 200 selected answers (150 correct, 30 wrong, 20 unattempted -> score = 150 * 2 = 300 / 400)
  const answers200 = [];
  for (let i = 0; i < 200; i++) {
    if (i < 150) answers200.push(1);
    else if (i < 180) answers200.push(0);
    else answers200.push(-1);
  }

  // Tampered score (score != correctCount * marksPerQuestion) MUST FAIL
  await assertFails(
    aDb.collection("attempts").doc(`weekly_mega_200_${ALICE_UID}`).set({
      testId: "weekly_mega_200",
      testTitle: "Bihar Board Weekly Mega Exam #1",
      subjectName: "All Subjects Combined",
      chapterName: "Full Weekly Mega Syllabus",
      testCategory: "WEEKLY_MEGA",
      weekKey: "2026-W47",
      userId: ALICE_UID,
      studentName: "Alice Kumari",
      selectedAnswers: answers200,
      correctCount: 150,
      wrongCount: 30,
      skippedCount: 20,
      totalQuestions: 200,
      marksPerQuestion: 2,
      totalMarks: 400,
      passingPercentage: 35,
      score: 400, // tampered!
      timeTakenSeconds: 5400,
      createdAt: now,
    })
  );

  // Valid 200-question Weekly Mega Test submission SUCCEEDS
  await assertSucceeds(
    aDb.collection("attempts").doc(`weekly_mega_200_${ALICE_UID}`).set({
      testId: "weekly_mega_200",
      testTitle: "Bihar Board Weekly Mega Exam #1",
      subjectName: "All Subjects Combined",
      chapterName: "Full Weekly Mega Syllabus",
      testCategory: "WEEKLY_MEGA",
      weekKey: "2026-W47",
      userId: ALICE_UID,
      studentName: "Alice Kumari",
      selectedAnswers: answers200,
      correctCount: 150,
      wrongCount: 30,
      skippedCount: 20,
      totalQuestions: 200,
      marksPerQuestion: 2,
      totalMarks: 400,
      passingPercentage: 35,
      score: 300,
      timeTakenSeconds: 5400,
      createdAt: now,
    })
  );

  // Submitted attempt is permanently locked: student CANNOT modify answers or re-submit
  await assertFails(
    aDb.collection("attempts").doc(`weekly_mega_200_${ALICE_UID}`).update({
      score: 350,
      correctCount: 175,
    })
  );
  await assertFails(
    aDb.collection("attempts").doc(`weekly_mega_200_${ALICE_UID}`).delete()
  );
});

// 13. Dynamic VIP Pricing, Offer Expiry, Promotional Banner & Historical Payment Safety
test("Dynamic VIP plans & offers enforce admin-only writes, active-only student reads, server-time offer pricing, and preserve past purchases", async () => {
  const oDb = ownerDb();
  const aDb = aliceDb();
  const bDb = bobDb();
  const now = pastDate();
  const nowMs = Date.now();

  // Seed Alice profile
  await assertSucceeds(
    aDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Kumari",
      email: "alice@student.test",
      phone: "9876543210",
      district: "Patna",
      schoolName: "Patna High School",
      boardClass: "Class 10 Bihar Board",
      vipActive: false,
      vipPlanId: "NONE",
      giftEligible: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student CANNOT create/edit /vip_plans or /vip_offer_config/settings
  await assertFails(
    aDb.collection("vip_plans").doc("PLAN_HACK").set({
      title: "Hacked ₹1 VIP",
      titleHindi: "1 रुपये",
      originalPriceInr: 1,
      offerPriceInr: 1,
      discountPercent: 0,
      durationDays: 365,
      durationLabel: "1 Year",
      hasSpecialGift: true,
      badge: "",
      offerName: "",
      active: true,
      displayOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aDb.collection("vip_offer_config").doc("settings").set({
      offersEnabled: true,
      offerName: "Fake Sale",
      offerStartDate: "2026-01-01",
      offerEndDate: "2026-12-31",
      offerStartMillis: nowMs - 86400000,
      offerEndMillis: nowMs + 86400000,
      bannerEnabled: true,
      bannerHeading: "Fake",
      bannerDescription: "Fake",
      bannerOfferText: "FAKE",
      bannerDiscountPercent: 90,
      bannerColorKey: "FESTIVE_GOLD",
      updatedBy: ALICE_UID,
      updatedAt: now,
    })
  );

  // Admin creates an Active Custom Plan (MRP ₹200, Offer ₹120 => 40% OFF, 60 days) and an Inactive Plan
  await assertSucceeds(
    oDb.collection("vip_plans").doc("PLAN_2M_BOARD").set({
      title: "2 Months Board Booster",
      titleHindi: "2 महीना बोर्ड बूस्टर",
      originalPriceInr: 200,
      offerPriceInr: 120,
      discountPercent: 40,
      durationDays: 60,
      durationLabel: "2 Months",
      hasSpecialGift: false,
      badge: "Exam Special",
      offerName: "Board Booster Sale",
      active: true,
      displayOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    oDb.collection("vip_plans").doc("PLAN_INACTIVE_TEST").set({
      title: "Hidden Draft Plan",
      titleHindi: "छिपा हुआ प्लान",
      originalPriceInr: 500,
      offerPriceInr: 250,
      discountPercent: 50,
      durationDays: 90,
      durationLabel: "3 Months",
      hasSpecialGift: false,
      badge: "",
      offerName: "",
      active: false,
      displayOrder: 2,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student CAN read active plans (filtered query + direct get), CANNOT read inactive plan
  await assertSucceeds(aDb.collection("vip_plans").where("active", "==", true).get());
  await assertSucceeds(aDb.collection("vip_plans").doc("PLAN_2M_BOARD").get());
  await assertFails(aDb.collection("vip_plans").doc("PLAN_INACTIVE_TEST").get());

  // Admin enables Global Offer + Promotional Banner with active window (yesterday -> tomorrow)
  await assertSucceeds(
    oDb.collection("vip_offer_config").doc("settings").set({
      offersEnabled: true,
      offerName: "Bihar Board Festival Offer",
      offerStartDate: "2026-01-01",
      offerEndDate: "2030-12-31",
      offerStartMillis: nowMs - 86400000,
      offerEndMillis: nowMs + 86400000,
      bannerEnabled: true,
      bannerHeading: "बिहार बोर्ड स्पेशल ऑफर!",
      bannerDescription: "सभी VIP प्लान पर 40% तक की छूट",
      bannerOfferText: "LIMITED TIME",
      bannerDiscountPercent: 40,
      bannerColorKey: "CRIMSON_SALE",
      updatedBy: OWNER_UID,
      updatedAt: now,
    })
  );
  await assertSucceeds(aDb.collection("vip_offer_config").doc("settings").get());

  // While Offer is ACTIVE:
  // (a) Purchasing inactive plan MUST FAIL
  await assertFails(
    aDb.collection("payment_requests").doc("pay_inactive_fail").set({
      userId: ALICE_UID,
      studentName: "Alice Kumari",
      studentPhone: "9876543210",
      planId: "PLAN_INACTIVE_TEST",
      planName: "Hidden Draft Plan",
      amountInr: 250,
      durationDays: 90,
      transactionRef: "UTRINACTIVE1",
      hasScreenshot: false,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // (b) Tampered underpaid amount (₹50 instead of ₹120) MUST FAIL
  await assertFails(
    aDb.collection("payment_requests").doc("pay_tampered_fail").set({
      userId: ALICE_UID,
      studentName: "Alice Kumari",
      studentPhone: "9876543210",
      planId: "PLAN_2M_BOARD",
      planName: "2 Months Board Booster",
      amountInr: 50,
      originalPriceInr: 200,
      discountPercent: 75,
      durationDays: 60,
      transactionRef: "UTRTAMPER50",
      hasScreenshot: false,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // (c) Exact live offer price (₹120, 60 days) SUCCEEDS
  await assertSucceeds(
    aDb.collection("payment_requests").doc("pay_offer_valid").set({
      userId: ALICE_UID,
      studentName: "Alice Kumari",
      studentPhone: "9876543210",
      planId: "PLAN_2M_BOARD",
      planName: "2 Months Board Booster",
      amountInr: 120,
      originalPriceInr: 200,
      discountPercent: 40,
      appliedOfferName: "Board Booster Sale",
      hasSpecialGift: false,
      durationDays: 60,
      transactionRef: "UTROFFER120",
      hasScreenshot: false,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin EXPIRES the offer window (offerEndMillis in the past) & raises plan price to ₹250
  await assertSucceeds(
    oDb.collection("vip_offer_config").doc("settings").update({
      offerEndMillis: nowMs - 60000,
      updatedAt: pastDate(500),
    })
  );

  // After Offer EXPIRES:
  // (a) Student trying to pay expired offer price (₹120) MUST FAIL
  await assertFails(
    bDb.collection("payment_requests").doc("pay_expired_offer_fail").set({
      userId: BOB_UID,
      studentName: "Bob Kumar",
      studentPhone: "9988776655",
      planId: "PLAN_2M_BOARD",
      planName: "2 Months Board Booster",
      amountInr: 120,
      durationDays: 60,
      transactionRef: "UTREXPIRED120",
      hasScreenshot: false,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // (b) Paying regular Old Price / MRP (₹200) after offer expiry SUCCEEDS
  await assertSucceeds(
    bDb.collection("payment_requests").doc("pay_regular_after_expiry").set({
      userId: BOB_UID,
      studentName: "Bob Kumar",
      studentPhone: "9988776655",
      planId: "PLAN_2M_BOARD",
      planName: "2 Months Board Booster",
      amountInr: 200,
      originalPriceInr: 200,
      discountPercent: 0,
      appliedOfferName: "",
      hasSpecialGift: false,
      durationDays: 60,
      transactionRef: "UTRREGULAR200",
      hasScreenshot: false,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin changes plan price to ₹299 and even deactivates the plan; approving Alice's EARLIER pending ₹120 request MUST STILL SUCCEED!
  await assertSucceeds(
    oDb.collection("vip_plans").doc("PLAN_2M_BOARD").update({
      originalPriceInr: 299,
      offerPriceInr: 299,
      discountPercent: 0,
      active: false,
      updatedAt: pastDate(300),
    })
  );

  const expiry60d = futureDate(60 * 24 * 60 * 60 * 1000);
  const approveBatch = oDb.batch();
  approveBatch.update(oDb.collection("payment_requests").doc("pay_offer_valid"), {
    status: "APPROVED",
    adminRemark: "Verified ₹120 offer payment",
    reviewedBy: OWNER_UID,
    reviewedAt: pastDate(200),
    activatedUntil: expiry60d,
    updatedAt: pastDate(200),
  });
  approveBatch.update(oDb.collection("users").doc(ALICE_UID), {
    vipActive: true,
    vipPlanId: "PLAN_2M_BOARD",
    vipExpiresAt: expiry60d,
    giftEligible: false,
    updatedAt: pastDate(200),
  });
  await assertSucceeds(approveBatch.commit());
});




