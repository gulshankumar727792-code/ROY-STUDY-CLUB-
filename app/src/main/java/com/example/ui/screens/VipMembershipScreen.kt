package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.AppConstants
import com.example.data.DateUtils
import com.example.data.DynamicVipPlan
import com.example.data.PaymentRequest
import com.example.data.PaymentStatus
import com.example.data.StudentProfile
import com.example.data.StudyTest
import com.example.data.TestAttempt
import com.example.data.TestCategory
import com.example.data.UiState
import com.example.data.VipNote
import com.example.data.VipNoteContent
import com.example.data.VipOfferConfig
import com.example.data.VipPlan
import com.example.data.VipPricingUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingCard
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
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun VipMembershipScreen(
    currentUserId: String,
    profileState: UiState<StudentProfile?>,
    testsState: UiState<List<StudyTest>>,
    attemptsState: UiState<List<TestAttempt>>,
    vipNotesState: UiState<List<VipNote>>,
    paymentsState: UiState<List<PaymentRequest>>,
    vipPlansState: UiState<List<DynamicVipPlan>>,
    vipOfferConfigState: UiState<VipOfferConfig>,
    busy: Boolean,
    onStartOrReviewTest: (StudyTest, TestAttempt?) -> Unit,
    onOpenVipNote: (VipNote) -> Unit,
    onSubmitPaymentRequest: (
        plan: VipPlan,
        transactionRef: String,
        studentPhone: String,
        note: String,
        screenshotUri: Uri?,
        onDone: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data
    val hasVip = profile?.hasValidVipNow() == true
    val allTests = (testsState as? UiState.Success)?.data.orEmpty()
    val vipTests = allTests.filter { it.categoryEnum() == TestCategory.VIP_TEST }
    val practiceSets = allTests.filter { it.categoryEnum() == TestCategory.VIP_PRACTICE }
    val attemptsMap = ((attemptsState as? UiState.Success)?.data.orEmpty()).associateBy { it.testId }
    val vipNotes = (vipNotesState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val offerConfig = (vipOfferConfigState as? UiState.Success)?.data ?: VipOfferConfig()
    val offerActiveNow = offerConfig.isOfferActiveNow()
    val showPromoBanner = offerConfig.shouldShowBanner()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPlanForPayment by remember { mutableStateOf<VipPlan?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("vip_membership_screen")
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Plans") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("VIP Tests") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Practice") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("VIP Notes") })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // VIP Status Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(RoyalBlueHeroBrush)
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hasVip) "VIP Membership Active" else "VIP सदस्यता / VIP Membership",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                VipCrownBadge(if (hasVip) "${profile?.remainingVipDays()} Days Left" else "CLASS 10 VIP")
                            }
                            Text(
                                text = if (hasVip) {
                                    "Valid until ${DateUtils.formatDateTime(profile?.vipExpiresAt)}" +
                                        if (profile?.giftEligible == true) " • 12-Month Special Gift Eligible!" else ""
                                } else {
                                    "Separate VIP Tests • Special Practice Sets • Special Notes • 12-Month Special Gift"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // Promotional Banner directly above VIP Plans (auto-hidden when OFF or expired)
                        if (showPromoBanner) {
                            item(key = "student_vip_promo_banner") {
                                VipPromotionalBannerCard(config = offerConfig)
                            }
                        }

                        item {
                            SectionHeader(
                                title = "VIP प्लान चुनें / Choose a VIP Plan",
                                subtitle = if (offerActiveNow && offerConfig.offerName.isNotBlank()) {
                                    "${offerConfig.offerName} सक्रिय • WhatsApp (${AppConstants.WHATSAPP_PHONE_DISPLAY})"
                                } else {
                                    "WhatsApp (${AppConstants.WHATSAPP_PHONE_DISPLAY}) पर भुगतान भेजें और अनुरोध सबमिट करें"
                                }
                            )
                        }

                        when (vipPlansState) {
                            is UiState.Loading -> item(key = "vip_plans_loading") { LoadingCard() }
                            is UiState.Error -> item(key = "vip_plans_error") { ErrorStateCard(vipPlansState.message) }
                            is UiState.Success -> {
                                val firestoreActivePlans = vipPlansState.data.filter { it.active }
                                val displayPlans = if (firestoreActivePlans.isNotEmpty()) {
                                    firestoreActivePlans
                                } else if (offerConfig.updatedBy.isBlank()) {
                                    // Initial fallback before Admin customizes /vip_plans in Firestore
                                    VipPricingUtils.defaultDynamicVipPlans()
                                } else {
                                    emptyList()
                                }

                                if (displayPlans.isEmpty()) {
                                    item(key = "vip_plans_empty") {
                                        EmptyStateCard(
                                            icon = Icons.Filled.WorkspacePremium,
                                            title = "अभी कोई सक्रिय VIP प्लान उपलब्ध नहीं है",
                                            subtitle = "No active VIP plans are available right now. Please check back shortly."
                                        )
                                    }
                                } else {
                                    items(displayPlans, key = { it.id }) { dynamicPlan ->
                                        val resolvedPlan = dynamicPlan.toVipPlan(
                                            offerActiveNow = offerActiveNow,
                                            globalOfferName = offerConfig.offerName
                                        )
                                        VipPlanCard(
                                            plan = resolvedPlan,
                                            onSelect = { selectedPlanForPayment = resolvedPlan }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(
                                title = "भुगतान इतिहास / Payment History",
                                subtitle = "Only after Owner Admin approval will VIP activate automatically"
                            )
                        }

                        when (paymentsState) {
                            is UiState.Loading -> item { LoadingCard() }
                            is UiState.Error -> item { ErrorStateCard(paymentsState.message) }
                            is UiState.Success -> {
                                if (payments.isEmpty()) {
                                    item {
                                        EmptyStateCard(
                                            icon = Icons.Filled.ReceiptLong,
                                            title = "कोई भुगतान अनुरोध नहीं है",
                                            subtitle = "Select any VIP plan above to contact WhatsApp and submit your payment proof."
                                        )
                                    }
                                } else {
                                    items(payments, key = { it.id }) { req ->
                                        PaymentHistoryCard(req)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            SectionHeader(
                                title = "VIP टेस्ट / Separate VIP Tests",
                                subtitle = if (hasVip) "Unlocked with your active VIP plan" else "Active VIP membership required"
                            )
                        }
                        if (vipTests.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.WorkspacePremium,
                                    title = "अभी कोई VIP टेस्ट प्रकाशित नहीं है",
                                    subtitle = "Owner Admin publishes exclusive VIP tests here."
                                )
                            }
                        } else {
                            items(vipTests, key = { it.id }) { test ->
                                val attempt = attemptsMap[test.id]
                                TestListCard(
                                    test = test,
                                    attempt = attempt,
                                    locked = !hasVip && attempt == null,
                                    onActionClick = {
                                        if (hasVip || attempt != null) {
                                            onStartOrReviewTest(test, attempt)
                                        } else {
                                            selectedTab = 0
                                        }
                                    }
                                )
                            }
                        }
                    }

                    2 -> {
                        item {
                            SectionHeader(
                                title = "विशेष प्रैक्टिस सेट / Special Practice Sets",
                                subtitle = if (hasVip) "Unlocked with your active VIP plan" else "Active VIP membership required"
                            )
                        }
                        if (practiceSets.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.WorkspacePremium,
                                    title = "अभी कोई VIP प्रैक्टिस सेट नहीं है",
                                    subtitle = "Owner Admin manually publishes special Class 10 Bihar Board practice sets here."
                                )
                            }
                        } else {
                            items(practiceSets, key = { it.id }) { test ->
                                val attempt = attemptsMap[test.id]
                                TestListCard(
                                    test = test,
                                    attempt = attempt,
                                    locked = !hasVip && attempt == null,
                                    onActionClick = {
                                        if (hasVip || attempt != null) {
                                            onStartOrReviewTest(test, attempt)
                                        } else {
                                            selectedTab = 0
                                        }
                                    }
                                )
                            }
                        }
                    }

                    3 -> {
                        item {
                            SectionHeader(
                                title = "VIP विशेष नोट्स / VIP Special Notes",
                                subtitle = "Admin-curated exam key points & summaries exclusively for active VIP members"
                            )
                        }
                        when (vipNotesState) {
                            is UiState.Loading -> item { LoadingCard() }
                            is UiState.Error -> item { ErrorStateCard(vipNotesState.message) }
                            is UiState.Success -> {
                                if (vipNotes.isEmpty()) {
                                    item {
                                        EmptyStateCard(
                                            icon = Icons.Filled.Description,
                                            title = "अभी कोई VIP विशेष नोट उपलब्ध नहीं है",
                                            subtitle = "Owner Admin publishes special VIP revision notes here."
                                        )
                                    }
                                } else {
                                    items(vipNotes, key = { it.id }) { note ->
                                        VipNoteCard(
                                            note = note,
                                            locked = !hasVip,
                                            onClick = {
                                                if (hasVip) onOpenVipNote(note) else selectedTab = 0
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // WhatsApp Payment Dialog
    selectedPlanForPayment?.let { plan ->
        WhatsAppPaymentDialog(
            plan = plan,
            studentName = profile?.fullName?.ifBlank { "Student" } ?: "Student",
            studentUid = currentUserId,
            defaultPhone = profile?.phone ?: "",
            busy = busy,
            onDismiss = { selectedPlanForPayment = null },
            onOpenWhatsApp = { ref ->
                val url = AppConstants.buildVipWhatsAppUrl(
                    studentName = profile?.fullName?.ifBlank { "Student" } ?: "Student",
                    studentUid = currentUserId,
                    plan = plan,
                    transactionRef = ref
                )
                openExternalUrl(context, url)
            },
            onSubmit = { ref, phone, note, uri ->
                onSubmitPaymentRequest(plan, ref, phone, note, uri) {
                    selectedPlanForPayment = null
                }
            }
        )
    }
}

@Composable
private fun VipPlanCard(
    plan: VipPlan,
    onSelect: () -> Unit
) {
    val hasDiscount = plan.discountPercent > 0L && plan.originalPriceInr > plan.amountInr
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vip_plan_card_${plan.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.hasSpecialGift) CrownGoldContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (plan.hasSpecialGift || hasDiscount) 2.dp else 1.dp,
            color = when {
                plan.hasSpecialGift -> CrownGold
                hasDiscount -> SuccessGreen
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (plan.titleHindi.isNotBlank()) {
                        Text(
                            text = plan.titleHindi,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "वैधता: ${plan.durationLabel} (${plan.durationDays} दिन)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (hasDiscount) {
                        StatusPill(
                            text = "${plan.discountPercent}% OFF",
                            containerColor = SuccessGreen,
                            contentColor = Color.White,
                            icon = Icons.Filled.LocalOffer
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "₹${plan.originalPriceInr}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Text(
                                text = "₹${plan.amountInr}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = RoyalBluePrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Text(
                            text = "₹${plan.amountInr}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = RoyalBluePrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (hasDiscount && plan.appliedOfferName.isNotBlank()) {
                StatusPill(
                    text = "${plan.appliedOfferName} • Save ₹${plan.originalPriceInr - plan.amountInr}",
                    containerColor = SuccessGreenContainer,
                    contentColor = SuccessGreen,
                    icon = Icons.Filled.LocalOffer
                )
            }

            if (plan.hasSpecialGift) {
                StatusPill(
                    text = "Includes Special Gift",
                    containerColor = CrownGold,
                    contentColor = OnCrownGoldContainer,
                    icon = Icons.Filled.CardGiftcard
                )
            } else if (!plan.badge.isNullOrBlank()) {
                StatusPill(
                    text = plan.badge,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("select_vip_plan_${plan.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasDiscount) "Pay via WhatsApp • ₹${plan.amountInr} (${plan.discountPercent}% OFF)"
                    else "Pay via WhatsApp • ₹${plan.amountInr}"
                )
            }
        }
    }
}

@Composable
private fun WhatsAppPaymentDialog(
    plan: VipPlan,
    studentName: String,
    studentUid: String,
    defaultPhone: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onOpenWhatsApp: (String) -> Unit,
    onSubmit: (ref: String, phone: String, note: String, uri: Uri?) -> Unit
) {
    var transactionRef by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf(defaultPhone) }
    var note by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        pickedUri = uri
    }

    val hasDiscount = plan.discountPercent > 0L && plan.originalPriceInr > plan.amountInr

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${plan.title} — ₹${plan.amountInr}")
                if (hasDiscount) {
                    Text(
                        text = "MRP ₹${plan.originalPriceInr} • ${plan.discountPercent}% OFF" +
                            if (plan.appliedOfferName.isNotBlank()) " (${plan.appliedOfferName})" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. WhatsApp (${AppConstants.WHATSAPP_PHONE_DISPLAY}) पर भुगतान विवरण और स्क्रीनशॉट भेजें।\n2. नीचे UTR / Transaction ID भरें व स्क्रीनशॉट चुनकर अनुरोध सबमिट करें।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { onOpenWhatsApp(transactionRef) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_whatsapp_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Contact on WhatsApp")
                }

                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text("UTR / Payment Ref Number *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_utr_input")
                )

                OutlinedTextField(
                    value = studentPhone,
                    onValueChange = { studentPhone = it },
                    label = { Text("Your WhatsApp / Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Note (e.g. UPI App used)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_payment_screenshot_button")
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (pickedUri != null) "Screenshot Attached (Change)" else "Attach Payment Screenshot")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(transactionRef, studentPhone, note, pickedUri) },
                enabled = !busy && transactionRef.trim().length >= 2,
                modifier = Modifier.testTag("submit_payment_request_button")
            ) {
                Text(if (busy) "Submitting..." else "Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PaymentHistoryCard(req: PaymentRequest) {
    val status = req.statusEnum()
    val (bg, fg) = when (status) {
        PaymentStatus.APPROVED -> SuccessGreenContainer to SuccessGreen
        PaymentStatus.REJECTED -> ErrorCrimsonContainer to ErrorCrimson
        PaymentStatus.PENDING -> WarningAmberContainer to WarningAmber
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
                Text("${req.planName} • ₹${req.amountInr}", style = MaterialTheme.typography.titleSmall)
                StatusPill(text = status.label, containerColor = bg, contentColor = fg)
            }
            Text(
                text = "Ref/UTR: ${req.transactionRef} • ${DateUtils.formatDateTime(req.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (req.activatedUntil != null && status == PaymentStatus.APPROVED) {
                Text(
                    text = "VIP Active Until: ${DateUtils.formatDateTime(req.activatedUntil)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            if (req.adminRemark.isNotBlank()) {
                Text(
                    text = "Admin Note: ${req.adminRemark}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun VipNoteCard(
    note: VipNote,
    locked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.Description,
                contentDescription = null,
                tint = if (locked) OnCrownGoldContainer else RoyalBluePrimary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(note.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${note.subjectName} • ${note.chapterName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (note.summary.isNotBlank()) {
                    Text(
                        text = note.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            VipCrownBadge(if (locked) "LOCKED" else "READ")
        }
    }
}

@Composable
fun VipNoteDetailScreen(
    note: VipNote,
    content: VipNoteContent,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(note.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            text = "${note.subjectName} • ${note.chapterName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    VipCrownBadge("VIP NOTE")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (content.keyPoints.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "मुख्य बिंदु / Key Exam Points",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            content.keyPoints.forEach { pt ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(pt, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = content.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        }
    }
}
