package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ReferralConfig
import com.example.data.RoyStudyRepository
import com.example.data.UiState
import com.example.data.signOutUser
import com.example.notifications.PushNavigationAction
import com.example.notifications.RoyStudyNotificationHelper
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DailyTestsScreen
import com.example.ui.screens.ProfileAndHistoryScreen
import com.example.ui.screens.ReferAndEarnScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudentDashboardScreen
import com.example.ui.screens.StudentRegistrationScreen
import com.example.ui.screens.StudentTestSection
import com.example.ui.screens.TestResultReviewScreen
import com.example.ui.screens.TestRunnerScreen
import com.example.ui.screens.VipMembershipScreen
import com.example.ui.screens.VipNoteDetailScreen
import com.example.ui.screens.WeeklyCompetitionScreen
import com.example.ui.theme.RoyStudyClubTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.StudentViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingPushAction = MutableStateFlow<PushNavigationAction?>(null)
    private val inviteCodeFlow = MutableStateFlow("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RoyStudyNotificationHelper.ensureChannels(this)
        handleIncomingIntent(intent)

        setContent {
            RoyStudyClubTheme {
                val inviteCode by inviteCodeFlow.collectAsStateWithLifecycle()
                val pushAction by pendingPushAction.collectAsStateWithLifecycle()
                RoyStudyClubRoot(
                    initialInviteCode = inviteCode,
                    pendingPushAction = pushAction,
                    onConsumePushAction = { pendingPushAction.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val code = intent?.data?.getQueryParameter("code").orEmpty()
        if (code.isNotBlank()) {
            inviteCodeFlow.value = code
        }
        val push = RoyStudyNotificationHelper.extractPushNavigationAction(intent)
        if (push != null) {
            pendingPushAction.value = push
        }
    }
}

private data class NavTabItem(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val STUDENT_TABS = listOf(
    NavTabItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavTabItem(R.string.nav_tests, Icons.Filled.Quiz, Icons.Outlined.Quiz, "nav_tests"),
    NavTabItem(R.string.nav_weekly, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_weekly"),
    NavTabItem(R.string.nav_vip, Icons.Filled.WorkspacePremium, Icons.Outlined.WorkspacePremium, "nav_vip"),
    NavTabItem(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun RoyStudyClubRoot(
    initialInviteCode: String = "",
    pendingPushAction: PushNavigationAction? = null,
    onConsumePushAction: () -> Unit = {}
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedRoyStudyApp(
                currentUserId = user.uid,
                displayName = user.displayName,
                email = user.email,
                initialInviteCode = initialInviteCode,
                pendingPushAction = pendingPushAction,
                onConsumePushAction = onConsumePushAction
            )
        }
    }
}

@Composable
private fun AuthenticatedRoyStudyApp(
    currentUserId: String,
    displayName: String?,
    email: String?,
    initialInviteCode: String,
    pendingPushAction: PushNavigationAction?,
    onConsumePushAction: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val studentViewModel: StudentViewModel = viewModel(
        key = "student_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudentViewModel(
                    repository = RoyStudyRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val isAdmin by studentViewModel.isAdmin.collectAsStateWithLifecycle()
    val profileState by studentViewModel.profileState.collectAsStateWithLifecycle()
    val subjectsState by studentViewModel.subjectsState.collectAsStateWithLifecycle()
    val testsState by studentViewModel.publishedTestsState.collectAsStateWithLifecycle()
    val attemptsState by studentViewModel.myAttemptsState.collectAsStateWithLifecycle()
    val leaderboardState by studentViewModel.weeklyLeaderboardState.collectAsStateWithLifecycle()
    val championsState by studentViewModel.championsState.collectAsStateWithLifecycle()
    val paymentsState by studentViewModel.myPaymentsState.collectAsStateWithLifecycle()
    val vipNotesState by studentViewModel.vipNotesState.collectAsStateWithLifecycle()
    val referralConfigState by studentViewModel.referralConfigState.collectAsStateWithLifecycle()
    val sentReferralsState by studentViewModel.mySentReferralsState.collectAsStateWithLifecycle()
    val inboundReferralState by studentViewModel.myInboundReferralState.collectAsStateWithLifecycle()
    val referralTxState by studentViewModel.myReferralTransactionsState.collectAsStateWithLifecycle()
    val fcmTokenState by studentViewModel.fcmTokenState.collectAsStateWithLifecycle()
    val vipPlansState by studentViewModel.vipPlansState.collectAsStateWithLifecycle()
    val vipOfferConfigState by studentViewModel.vipOfferConfigState.collectAsStateWithLifecycle()
    val codeVerification by studentViewModel.codeVerification.collectAsStateWithLifecycle()
    val selectedWeekKey by studentViewModel.selectedWeekKey.collectAsStateWithLifecycle()
    val bannerMessage by studentViewModel.bannerMessage.collectAsStateWithLifecycle()
    val activeTest by studentViewModel.activeTest.collectAsStateWithLifecycle()
    val reviewSession by studentViewModel.reviewSession.collectAsStateWithLifecycle()
    val openedVipNote by studentViewModel.openedVipNote.collectAsStateWithLifecycle()
    val busy by studentViewModel.busy.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedTestSectionIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTestSection = StudentTestSection.entries.getOrElse(selectedTestSectionIndex) { StudentTestSection.DAILY }
    var showAdminPanel by rememberSaveable { mutableStateOf(false) }
    var showReferAndEarn by rememberSaveable { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(RoyStudyNotificationHelper.hasNotificationPermission(context))
    }
    var permissionRequestedOnce by rememberSaveable(currentUserId) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || RoyStudyNotificationHelper.hasNotificationPermission(context)
        if (hasNotificationPermission) {
            studentViewModel.syncPushNotifications(context)
            studentViewModel.showMessage("पुश नोटिफिकेशन सक्रिय हो गए! अब नए Daily Test व Weekly Champion अलर्ट तुरंत मिलेंगे।")
        }
    }

    // Automatically sync FCM token + topic subscriptions and request POST_NOTIFICATIONS on Android 13+
    LaunchedEffect(currentUserId) {
        hasNotificationPermission = RoyStudyNotificationHelper.hasNotificationPermission(context)
        studentViewModel.syncPushNotifications(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission &&
            !permissionRequestedOnce
        ) {
            permissionRequestedOnce = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Handle FCM notification tap deep-link routing
    LaunchedEffect(pendingPushAction) {
        val action = pendingPushAction ?: return@LaunchedEffect
        showAdminPanel = false
        showReferAndEarn = false
        when (action.navTarget) {
            RoyStudyNotificationHelper.NAV_WEEKLY_CHAMPION -> {
                if (!action.weekKey.isNullOrBlank()) {
                    studentViewModel.setWeekKey(action.weekKey)
                }
                selectedTab = 2
            }
            RoyStudyNotificationHelper.NAV_DAILY_TESTS -> {
                selectedTestSectionIndex = StudentTestSection.DAILY.ordinal
                selectedTab = 1
            }
        }
        onConsumePushAction()
    }

    val referralConfig = (referralConfigState as? UiState.Success)?.data ?: ReferralConfig()

    LaunchedEffect(bannerMessage) {
        bannerMessage?.let {
            snackbarHostState.showSnackbar(it)
            studentViewModel.clearBanner()
        }
    }

    val performSignOut: () -> Unit = {
        scope.launch {
            studentViewModel.cleanupFcmBeforeSignOut()
            signOutUser(
                credentialManager = credentialManager,
                scope = scope
            )
        }
    }

    // New Student Registration Gate (allows entering optional referral code; locks attribution on submit)
    if (profileState is UiState.Success && (profileState as UiState.Success).data == null) {
        StudentRegistrationScreen(
            initialDisplayName = displayName.orEmpty(),
            initialEmail = email.orEmpty(),
            initialReferralCode = initialInviteCode,
            referralConfig = referralConfig,
            codeVerification = codeVerification,
            busy = busy,
            snackbarHostState = snackbarHostState,
            onVerifyCode = studentViewModel::verifyReferralCode,
            onResetCodeVerification = studentViewModel::resetCodeVerification,
            onCompleteRegistration = studentViewModel::completeRegistration,
            onSignOut = performSignOut
        )
        return
    }

    // Full-screen Owner Admin Panel (strictly gated behind verified Owner Admin status)
    if (showAdminPanel && isAdmin) {
        val adminViewModel: AdminViewModel = viewModel(
            key = "admin_$currentUserId",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    AdminViewModel(RoyStudyRepository(db))
                }
            }
        )
        AdminPanelScreen(
            adminViewModel = adminViewModel,
            currentUserUid = currentUserId,
            currentUserEmail = email.orEmpty(),
            onClose = { showAdminPanel = false }
        )
        return
    }

    // Full-screen Refer & Earn Page
    if (showReferAndEarn) {
        ReferAndEarnScreen(
            profileState = profileState,
            referralConfigState = referralConfigState,
            sentReferralsState = sentReferralsState,
            inboundReferralState = inboundReferralState,
            transactionsState = referralTxState,
            snackbarHostState = snackbarHostState,
            onShowMessage = studentViewModel::showMessage,
            onGoToDailyTests = {
                showReferAndEarn = false
                selectedTab = 1
            },
            onClose = { showReferAndEarn = false }
        )
        return
    }

    // Full-screen Timed Test Runner
    activeTest?.let { session ->
        TestRunnerScreen(
            session = session,
            onSelectAnswer = studentViewModel::selectAnswer,
            onClearAnswer = studentViewModel::clearAnswer,
            onNavigateQuestion = studentViewModel::navigateQuestion,
            onSubmit = studentViewModel::submitActiveTest,
            onExit = studentViewModel::cancelActiveTest
        )
        return
    }

    // Full-screen Test Result Summary (First) -> View All Answers (Second)
    reviewSession?.let { review ->
        TestResultReviewScreen(
            review = review,
            onViewAllAnswers = studentViewModel::openAnswerAnalysis,
            onBackToResult = studentViewModel::returnToResultSummary,
            onClose = studentViewModel::closeReview
        )
        return
    }

    // Full-screen VIP Special Note Viewer
    openedVipNote?.let { (note, content) ->
        VipNoteDetailScreen(
            note = note,
            content = content,
            onClose = studentViewModel::closeVipNote
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        STUDENT_TABS.forEachIndexed { idx, tab ->
                            val selected = selectedTab == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        STUDENT_TABS.forEachIndexed { idx, tab ->
                            val selected = selectedTab == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> StudentDashboardScreen(
                            profileState = profileState,
                            testsState = testsState,
                            attemptsState = attemptsState,
                            championsState = championsState,
                            isAdmin = isAdmin,
                            referralRewardDays = referralConfig.rewardDays,
                            onOpenDailyTests = {
                                selectedTestSectionIndex = StudentTestSection.DAILY.ordinal
                                selectedTab = 1
                            },
                            onOpenChapterTests = {
                                selectedTestSectionIndex = StudentTestSection.CHAPTER_WISE.ordinal
                                selectedTab = 1
                            },
                            onOpenWeeklyMegaTests = {
                                selectedTestSectionIndex = StudentTestSection.WEEKLY_MEGA.ordinal
                                selectedTab = 1
                            },
                            onOpenWeekly = { selectedTab = 2 },
                            onOpenVip = { selectedTab = 3 },
                            onOpenReferAndEarn = { showReferAndEarn = true },
                            onOpenAdminPanel = { if (isAdmin) showAdminPanel = true },
                            onStartOrReviewTest = studentViewModel::startTest
                        )

                        1 -> DailyTestsScreen(
                            selectedSection = selectedTestSection,
                            onSelectSection = { selectedTestSectionIndex = it.ordinal },
                            subjectsState = subjectsState,
                            testsState = testsState,
                            attemptsState = attemptsState,
                            onStartOrReviewTest = studentViewModel::startTest
                        )

                        2 -> WeeklyCompetitionScreen(
                            selectedWeekKey = selectedWeekKey,
                            currentUserId = currentUserId,
                            leaderboardState = leaderboardState,
                            championsState = championsState,
                            onWeekKeyChange = studentViewModel::setWeekKey
                        )

                        3 -> VipMembershipScreen(
                            currentUserId = currentUserId,
                            profileState = profileState,
                            testsState = testsState,
                            attemptsState = attemptsState,
                            vipNotesState = vipNotesState,
                            paymentsState = paymentsState,
                            vipPlansState = vipPlansState,
                            vipOfferConfigState = vipOfferConfigState,
                            busy = busy,
                            onStartOrReviewTest = studentViewModel::startTest,
                            onOpenVipNote = studentViewModel::openVipNote,
                            onSubmitPaymentRequest = { plan, ref, phone, note, uri, onDone ->
                                studentViewModel.submitVipPaymentRequest(
                                    context = context,
                                    plan = plan,
                                    transactionRef = ref,
                                    studentPhone = phone,
                                    note = note,
                                    screenshotUri = uri,
                                    onSubmitted = onDone
                                )
                            }
                        )

                        4 -> ProfileAndHistoryScreen(
                            currentUserId = currentUserId,
                            profileState = profileState,
                            attemptsState = attemptsState,
                            fcmTokenState = fcmTokenState,
                            hasNotificationPermission = hasNotificationPermission,
                            isAdmin = isAdmin,
                            busy = busy,
                            onSaveProfile = studentViewModel::updateProfile,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    studentViewModel.syncPushNotifications(context)
                                }
                            },
                            onUpdateNotificationPreferences = { dailyEnabled, championEnabled ->
                                studentViewModel.updateNotificationPreferences(
                                    context = context,
                                    dailyEnabled = dailyEnabled,
                                    championEnabled = championEnabled
                                )
                            },
                            onReviewAttempt = studentViewModel::openAttemptReview,
                            onOpenReferAndEarn = { showReferAndEarn = true },
                            onOpenAdminPanel = { if (isAdmin) showAdminPanel = true },
                            onSignOut = performSignOut
                        )
                    }
                }
            }
        }
    }
}

