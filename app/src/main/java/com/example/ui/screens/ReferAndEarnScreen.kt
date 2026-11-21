package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppConstants
import com.example.data.DateUtils
import com.example.data.ReferralConfig
import com.example.data.ReferralRecord
import com.example.data.ReferralStatus
import com.example.data.ReferralTransaction
import com.example.data.ReferralUtils
import com.example.data.StudentProfile
import com.example.data.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.MetricTile
import com.example.ui.components.RoyalBlueHeroBrush
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.VipCrownBadge
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
import com.example.viewmodel.ReferralCodeVerification

@Composable
fun StudentRegistrationScreen(
    initialDisplayName: String,
    initialEmail: String,
    initialReferralCode: String,
    referralConfig: ReferralConfig,
    codeVerification: ReferralCodeVerification,
    busy: Boolean,
    snackbarHostState: SnackbarHostState,
    onVerifyCode: (String) -> Unit,
    onResetCodeVerification: () -> Unit,
    onCompleteRegistration: (
        fullName: String,
        phone: String,
        district: String,
        schoolName: String,
        referralCodeInput: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var fullName by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
    var phone by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var referralCode by remember(initialReferralCode) {
        mutableStateOf(ReferralUtils.normalizeCode(initialReferralCode))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("student_registration_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = RoyalBlueDark, contentColor = Color.White, shadowElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(38.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.roy_study_club_logo),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.referral_registration_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = AppConstants.BOARD_CLASS_FIXED,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("registration_sign_out_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.sign_out),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(RoyalBlueHeroBrush)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "रॉय स्टडी क्लब में आपका स्वागत है!",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Signed in as $initialEmail\nअपनी जानकारी भरें और यदि आपके पास किसी मित्र का रेफरल कोड है तो नीचे डालें।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "छात्र विवरण / Student Details",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name / पूरा नाम *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_full_name_input")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile / WhatsApp Number (Private)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_phone_input")
                        )
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District / जिला (Bihar)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_district_input")
                        )
                        OutlinedTextField(
                            value = schoolName,
                            onValueChange = { schoolName = it },
                            label = { Text("School Name / विद्यालय का नाम") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_school_input")
                        )
                    }
                }

                // Referral Code Card (Optional during Registration — Locked afterwards)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CrownGoldContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.5.dp, CrownGold),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = OnCrownGoldContainer
                            )
                            Text(
                                text = "Referral Code (Optional)",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnCrownGoldContainer
                            )
                        }

                        if (!referralConfig.enabled) {
                            StatusPill(
                                text = "Referral program is currently paused by Admin",
                                containerColor = WarningAmberContainer,
                                contentColor = WarningAmber
                            )
                        } else {
                            Text(
                                text = "यदि किसी दोस्त ने आपको आमंत्रित किया है, तो उनका Referral Code डालें। आपका पहला Daily Test पूरा होने पर आप दोनों को +${referralConfig.rewardDays} दिन की FREE VIP Membership मिलेगी!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = referralCode,
                                    onValueChange = {
                                        referralCode = ReferralUtils.normalizeCode(it)
                                        onResetCodeVerification()
                                    },
                                    label = { Text("e.g. ROY4K9M2P") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_referral_code_input")
                                )
                                OutlinedButton(
                                    onClick = { onVerifyCode(referralCode) },
                                    enabled = !busy && referralCode.length >= 6 &&
                                        codeVerification !is ReferralCodeVerification.Checking,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("reg_verify_code_button")
                                ) {
                                    if (codeVerification is ReferralCodeVerification.Checking) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Verify")
                                    }
                                }
                            }

                            when (codeVerification) {
                                is ReferralCodeVerification.Valid -> {
                                    StatusPill(
                                        text = "Verified! Invited by ${codeVerification.lookup.ownerName} (+${referralConfig.rewardDays} VIP Days)",
                                        containerColor = SuccessGreenContainer,
                                        contentColor = SuccessGreen,
                                        icon = Icons.Filled.Verified
                                    )
                                }
                                is ReferralCodeVerification.Invalid -> {
                                    StatusPill(
                                        text = codeVerification.message,
                                        containerColor = ErrorCrimsonContainer,
                                        contentColor = ErrorCrimson
                                    )
                                }
                                else -> {}
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Referral attribution is permanently locked after registration.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onCompleteRegistration(
                            fullName,
                            phone,
                            district,
                            schoolName,
                            if (referralConfig.enabled) referralCode else ""
                        )
                    },
                    enabled = !busy && fullName.trim().isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("complete_registration_button")
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Creating Student Account...")
                    } else {
                        Text(
                            text = "Complete Registration & Enter Club",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReferAndEarnScreen(
    profileState: UiState<StudentProfile?>,
    referralConfigState: UiState<ReferralConfig>,
    sentReferralsState: UiState<List<ReferralRecord>>,
    inboundReferralState: UiState<ReferralRecord?>,
    transactionsState: UiState<List<ReferralTransaction>>,
    snackbarHostState: SnackbarHostState,
    onShowMessage: (String) -> Unit,
    onGoToDailyTests: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    val context = LocalContext.current

    val profile = (profileState as? UiState.Success)?.data
    val config = (referralConfigState as? UiState.Success)?.data ?: ReferralConfig()
    val sentReferrals = (sentReferralsState as? UiState.Success)?.data.orEmpty()
    val inboundReferral = (inboundReferralState as? UiState.Success)?.data
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()

    val myCode = profile?.referralCode.orEmpty()
    val totalReferrals = sentReferrals.size
    val pendingReferrals = sentReferrals.count { it.statusEnum() == ReferralStatus.PENDING }
    val successfulReferrals = maxOf(
        profile?.successfulReferrals ?: 0L,
        sentReferrals.count { it.statusEnum() == ReferralStatus.SUCCESSFUL }.toLong()
    )
    val earnedVipDays = maxOf(
        profile?.earnedVipDays ?: 0L,
        transactions.sumOf { it.rewardDays }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("refer_and_earn_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = RoyalBlueDark, contentColor = Color.White, shadowElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("refer_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.refer_and_earn),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.refer_and_earn_hindi),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    VipCrownBadge("+${config.rewardDays} VIP Days")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .testTag("refer_and_earn_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Reward Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(RoyalBlueHeroBrush)
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(CrownGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.CardGiftcard,
                                        contentDescription = null,
                                        tint = OnCrownGoldContainer,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "मित्रों को आमंत्रित करें, मुफ्त VIP पाएं!",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Both you & your friend earn +${config.rewardDays} VIP days when they complete their 1st Daily Test.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            if (!config.enabled) {
                                StatusPill(
                                    text = "Referral Program Currently Paused by Owner Admin",
                                    containerColor = WarningAmberContainer,
                                    contentColor = WarningAmber
                                )
                            }
                        }
                    }
                }

                // Unique Referral Code + Copy / WhatsApp / Share Link Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, CrownGold),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "आपका यूनिक रेफरल कोड / Your Referral Code",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                color = CrownGoldContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CrownGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = myCode.ifBlank { "Generating..." },
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        letterSpacing = 3.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = RoyalBlueDark,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(vertical = 14.dp, horizontal = 12.dp)
                                        .testTag("my_referral_code_text")
                                )
                            }

                            val codeReady = myCode.isNotBlank()

                            // Action Buttons: Copy Code, Share on WhatsApp, Share Link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        copyToClipboard(context, "Roy Study Club Referral Code", myCode)
                                        onShowMessage("Referral code $myCode copied!")
                                    },
                                    enabled = codeReady,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("copy_referral_code_button")
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.copy_referral_code), maxLines = 1)
                                }

                                Button(
                                    onClick = {
                                        val shareText = ReferralUtils.buildShareMessage(myCode, config.rewardDays)
                                        launchSystemShare(context, shareText)
                                    },
                                    enabled = codeReady,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_referral_link_button")
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.share_referral_link), maxLines = 1)
                                }
                            }

                            Button(
                                onClick = {
                                    val waUrl = ReferralUtils.buildWhatsAppShareUrl(myCode, config.rewardDays)
                                    openExternalUrl(context, waUrl)
                                },
                                enabled = codeReady,
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("share_referral_whatsapp_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.share_on_whatsapp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2x2 Referral Metrics Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile(
                                label = "Total Referrals",
                                value = "$totalReferrals",
                                accentColor = RoyalBluePrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Pending",
                                value = "$pendingReferrals",
                                accentColor = WarningAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile(
                                label = "Successful (Max ${config.maxSuccessfulReferrals})",
                                value = "$successfulReferrals / ${config.maxSuccessfulReferrals}",
                                accentColor = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Earned VIP Days",
                                value = "$earnedVipDays Days",
                                accentColor = Color(0xFFB37200),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Inbound Referral Attribution Card (if this student joined with a referral code)
                if (inboundReferral != null) {
                    item {
                        InboundReferralStatusCard(
                            inbound = inboundReferral,
                            rewardDays = config.rewardDays,
                            onGoToDailyTests = onGoToDailyTests
                        )
                    }
                } else if (profile?.referralLocked == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Registration completed without a referrer code (attribution locked).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Reward Rules Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "नियम / Reward Rules",
                                style = MaterialTheme.typography.titleSmall,
                                color = RoyalBlueDark
                            )
                            Text(
                                text = "• नया छात्र रजिस्ट्रेशन के समय आपका Referral Code डाले।\n" +
                                    "• रेफरल तभी सफल माना जाता है जब नया छात्र अपना पहला Daily Test पूरा करे।\n" +
                                    "• प्रत्येक सफल रेफरल पर दोनों छात्रों को +${config.rewardDays} VIP दिन मिलते हैं।\n" +
                                    "• अधिकतम ${config.maxSuccessfulReferrals} सफल रेफरल प्रति छात्र मान्य हैं। Self-referral व duplicate referral अमान्य हैं।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // My Sent Referrals Section
                item {
                    SectionHeader(
                        title = "आपके रेफरल / Invited Friends ($totalReferrals)",
                        subtitle = "Track first Daily Test completion & approval status"
                    )
                }

                when (sentReferralsState) {
                    is UiState.Loading -> item { LoadingCard() }
                    is UiState.Error -> item { ErrorStateCard(sentReferralsState.message) }
                    is UiState.Success -> {
                        if (sentReferrals.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.GroupAdd,
                                    title = "अभी कोई रेफरल नहीं है",
                                    subtitle = "Share your code on WhatsApp with Class 10 Bihar Board friends to earn free VIP days!"
                                )
                            }
                        } else {
                            items(sentReferrals, key = { it.id }) { ref ->
                                SentReferralCard(ref)
                            }
                        }
                    }
                }

                // Auditable Reward Transaction History
                item {
                    SectionHeader(
                        title = "VIP रिवॉर्ड इतिहास / Referral Reward History (${transactions.size})",
                        subtitle = "Verified VIP days credited to your account"
                    )
                }

                when (transactionsState) {
                    is UiState.Loading -> item { LoadingCard() }
                    is UiState.Error -> item { ErrorStateCard(transactionsState.message) }
                    is UiState.Success -> {
                        if (transactions.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.History,
                                    title = "अभी कोई रिवॉर्ड क्रेडिट नहीं हुआ",
                                    subtitle = "Once an invited friend completes their first Daily Test and the reward is processed, it appears here."
                                )
                            }
                        } else {
                            items(transactions, key = { it.id }) { tx ->
                                ReferralTransactionCard(tx)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboundReferralStatusCard(
    inbound: ReferralRecord,
    rewardDays: Long,
    onGoToDailyTests: () -> Unit
) {
    val st = inbound.statusEnum()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Invited By ${inbound.referrerName}", style = MaterialTheme.typography.titleSmall)
                StatusPill(
                    text = "LOCKED • ${inbound.referralCode}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = Icons.Filled.Lock
                )
            }
            when (st) {
                ReferralStatus.SUCCESSFUL -> {
                    StatusPill(
                        text = "Reward Credited: +${inbound.rewardDays} VIP Days!",
                        containerColor = SuccessGreenContainer,
                        contentColor = SuccessGreen,
                        icon = Icons.Filled.CheckCircle
                    )
                }
                ReferralStatus.REJECTED -> {
                    Text(
                        text = "Status: Rejected — ${inbound.rejectionReason.ifBlank { "Marked ineligible by Owner Admin" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorCrimson
                    )
                }
                ReferralStatus.PENDING -> {
                    if (!inbound.firstDailyTestCompleted) {
                        Text(
                            text = "अपना पहला Daily Test पूरा करें ताकि आपको और ${inbound.referrerName} को +$rewardDays VIP दिन मिल सकें।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onGoToDailyTests,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Take First Daily Test")
                        }
                    } else {
                        StatusPill(
                            text = "First Daily Test Done! Reward Pending Verification",
                            containerColor = WarningAmberContainer,
                            contentColor = WarningAmber,
                            icon = Icons.Filled.HourglassTop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SentReferralCard(ref: ReferralRecord) {
    val st = ref.statusEnum()
    val (bg, fg, label) = when (st) {
        ReferralStatus.SUCCESSFUL -> Triple(SuccessGreenContainer, SuccessGreen, "Successful (+${ref.rewardDays}d)")
        ReferralStatus.REJECTED -> Triple(ErrorCrimsonContainer, ErrorCrimson, "Rejected")
        ReferralStatus.PENDING -> if (ref.firstDailyTestCompleted) {
            Triple(CrownGoldContainer, OnCrownGoldContainer, "1st Test Done • Processing")
        } else {
            Triple(WarningAmberContainer, WarningAmber, "Waiting 1st Daily Test")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ref.referredName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(text = label, containerColor = bg, contentColor = fg)
            }
            Text(
                text = "Joined: ${DateUtils.formatDateTime(ref.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ref.firstDailyTestCompleted && ref.firstDailyTestTitle.isNotBlank()) {
                Text(
                    text = "1st Daily Test: ${ref.firstDailyTestTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen
                )
            }
            if (st == ReferralStatus.REJECTED && ref.rejectionReason.isNotBlank()) {
                Text(
                    text = "Reason: ${ref.rejectionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorCrimson
                )
            }
        }
    }
}

@Composable
private fun ReferralTransactionCard(tx: ReferralTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SuccessGreenContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (tx.role == "REFERRER") {
                        "Referral Bonus • Invited ${tx.counterpartyName}"
                    } else {
                        "Welcome Bonus • Invited by ${tx.counterpartyName}"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "VIP valid until: ${DateUtils.formatDateTime(tx.vipExpiresAfter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Code ${tx.referralCode} • ${DateUtils.formatDateTime(tx.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                text = "+${tx.rewardDays}d VIP",
                containerColor = CrownGold,
                contentColor = OnCrownGoldContainer
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun launchSystemShare(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join Roy Study Club — Class 10 Bihar Board")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Referral Link"))
}
