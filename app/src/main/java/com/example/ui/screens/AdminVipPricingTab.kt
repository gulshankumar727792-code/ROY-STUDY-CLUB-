package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.BannerColorTheme
import com.example.data.DateUtils
import com.example.data.DynamicVipPlan
import com.example.data.UiState
import com.example.data.VipOfferConfig
import com.example.data.VipPricingUtils
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
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

/**
 * Compact, attractive promotional banner rendered directly above VIP Plans in the Student App
 * and as a live preview inside the Admin Panel's "VIP Plan & Offer Management" tab.
 */
@Composable
fun VipPromotionalBannerCard(
    config: VipOfferConfig,
    modifier: Modifier = Modifier
) {
    val theme = config.colorTheme()
    val startColor = Color(theme.startArgb.toInt())
    val endColor = Color(theme.endArgb.toInt())
    val textColor = Color(theme.textArgb.toInt())
    val badgeBg = Color(theme.badgeBgArgb.toInt())
    val badgeText = Color(theme.badgeTextArgb.toInt())

    val gradientBrush = Brush.linearGradient(listOf(startColor, endColor))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(gradientBrush)
            .border(
                width = 1.dp,
                color = textColor.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("vip_promotional_banner")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = "Promotional Offer Banner",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = config.bannerOfferText.ifBlank {
                            config.offerName.ifBlank { "SPECIAL VIP OFFER" }
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (config.bannerDiscountPercent > 0L) {
                    Surface(
                        color = badgeBg,
                        contentColor = badgeText,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalOffer,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "UP TO ${config.bannerDiscountPercent}% OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            if (config.bannerHeading.isNotBlank()) {
                Text(
                    text = config.bannerHeading,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (config.bannerDescription.isNotBlank()) {
                Text(
                    text = config.bannerDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.92f)
                )
            }

            if (config.offerEndDate.isNotBlank()) {
                Text(
                    text = "Offer valid till: ${config.offerEndDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.adminVipPricingAndOffersTab(
    plansState: UiState<List<DynamicVipPlan>>,
    offerConfigState: UiState<VipOfferConfig>,
    busy: Boolean,
    onSeedDefaultPlans: () -> Unit,
    onAddNewPlan: () -> Unit,
    onEditPlan: (DynamicVipPlan) -> Unit,
    onTogglePlanActive: (DynamicVipPlan) -> Unit,
    onDeletePlan: (DynamicVipPlan) -> Unit,
    onSaveOfferConfig: (
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
    ) -> Unit
) {
    val plans = (plansState as? UiState.Success)?.data.orEmpty()
    val config = (offerConfigState as? UiState.Success)?.data ?: VipOfferConfig()
    val offerActiveNow = config.isOfferActiveNow()

    item(key = "vip_mgmt_header") {
        SectionHeader(
            title = "VIP Plan & Offer Management",
            subtitle = "Firebase से डायनामिक कीमत, ऑफर व प्रमोशनल बैनर अपडेट करें — छात्रों को APK अपडेट नहीं करना पड़ेगा"
        )
    }

    item(key = "vip_mgmt_metrics") {
        val activeCount = plans.count { it.active }
        val offerStatusLabel = when {
            !config.offersEnabled -> "OFF"
            config.isOfferExpired() -> "EXPIRED"
            config.isOfferUpcoming() -> "UPCOMING"
            else -> "LIVE"
        }
        val offerStatusColor = when {
            offerActiveNow -> SuccessGreen
            config.isOfferUpcoming() -> RoyalBluePrimary
            config.isOfferExpired() -> ErrorCrimson
            else -> WarningAmber
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Total Plans", "${plans.size}", RoyalBluePrimary, Modifier.weight(1f))
            MetricTile("Active Plans", "$activeCount", SuccessGreen, Modifier.weight(1f))
            MetricTile("Offers", offerStatusLabel, offerStatusColor, Modifier.weight(1f))
        }
    }

    // Global Offer + Promotional Banner Management Card
    item(key = "vip_offer_banner_card") {
        AdminOfferAndBannerCard(
            config = config,
            busy = busy,
            onSave = onSaveOfferConfig
        )
    }

    // Plans Management Section
    item(key = "vip_plans_section_header") {
        SectionHeader(
            title = "VIP Plans (${plans.size})",
            subtitle = "कीमत बदलने पर पुराने VIP सदस्यों की वैधता या भुगतान इतिहास कभी नहीं बदलेगा",
            trailing = {
                Button(
                    onClick = onAddNewPlan,
                    enabled = !busy,
                    modifier = Modifier.testTag("add_new_vip_plan_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Plan")
                }
            }
        )
    }

    when (plansState) {
        is UiState.Loading -> {
            item(key = "vip_plans_loading") { LoadingCard() }
        }

        is UiState.Error -> {
            item(key = "vip_plans_error") { ErrorStateCard(plansState.message) }
        }

        is UiState.Success -> {
            if (plans.isEmpty()) {
                item(key = "vip_plans_seed_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CrownGoldContainer.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, CrownGold),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "डिफ़ॉल्ट VIP प्लान Firebase में लोड करें",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnCrownGoldContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "अभी Firebase के /vip_plans कलेक्शन में कोई प्लान सेव नहीं है। एक क्लिक में 4 डिफ़ॉल्ट प्लान (₹99, ₹249, ₹449, ₹999) लोड करें या 'New Plan' से नया प्लान जोड़ें।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = onSeedDefaultPlans,
                                enabled = !busy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("seed_default_vip_plans_button")
                            ) {
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Load Default 4 VIP Plans to Firebase")
                            }
                        }
                    }
                }
            } else {
                items(plans, key = { "admin_vip_plan_${it.id}" }) { plan ->
                    AdminVipPlanRowCard(
                        plan = plan,
                        offerActiveNow = offerActiveNow,
                        busy = busy,
                        onToggleActive = { onTogglePlanActive(plan) },
                        onEdit = { onEditPlan(plan) },
                        onDelete = { onDeletePlan(plan) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminOfferAndBannerCard(
    config: VipOfferConfig,
    busy: Boolean,
    onSave: (
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
    ) -> Unit
) {
    var offersEnabled by remember(config.offersEnabled, config.updatedAt) { mutableStateOf(config.offersEnabled) }
    var offerName by remember(config.offerName, config.updatedAt) { mutableStateOf(config.offerName) }
    var offerStartDate by remember(config.offerStartDate, config.updatedAt) { mutableStateOf(config.offerStartDate) }
    var offerEndDate by remember(config.offerEndDate, config.updatedAt) { mutableStateOf(config.offerEndDate) }

    var bannerEnabled by remember(config.bannerEnabled, config.updatedAt) { mutableStateOf(config.bannerEnabled) }
    var bannerHeading by remember(config.bannerHeading, config.updatedAt) { mutableStateOf(config.bannerHeading) }
    var bannerDescription by remember(config.bannerDescription, config.updatedAt) { mutableStateOf(config.bannerDescription) }
    var bannerOfferText by remember(config.bannerOfferText, config.updatedAt) { mutableStateOf(config.bannerOfferText) }
    var bannerDiscountStr by remember(config.bannerDiscountPercent, config.updatedAt) {
        mutableStateOf(if (config.bannerDiscountPercent > 0L) config.bannerDiscountPercent.toString() else "")
    }
    var bannerColorKey by remember(config.bannerColorKey, config.updatedAt) { mutableStateOf(config.bannerColorKey) }

    val previewConfig = VipOfferConfig(
        offersEnabled = offersEnabled,
        offerName = offerName,
        offerStartDate = offerStartDate,
        offerEndDate = offerEndDate,
        offerStartMillis = VipPricingUtils.isoDateToStartOfDayMillis(offerStartDate),
        offerEndMillis = VipPricingUtils.isoDateToEndOfDayMillis(offerEndDate),
        bannerEnabled = bannerEnabled,
        bannerHeading = bannerHeading.ifBlank { "Special Board Exam Offer!" },
        bannerDescription = bannerDescription.ifBlank { "Limited time discount on all Class 10 Bihar Board VIP Plans." },
        bannerOfferText = bannerOfferText.ifBlank { offerName.ifBlank { "FESTIVE SALE" } },
        bannerDiscountPercent = bannerDiscountStr.toLongOrNull()?.coerceIn(0L, 99L) ?: 0L,
        bannerColorKey = bannerColorKey
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_offer_banner_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section A: Global Offers Toggle & Schedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. Global Offer Control (सभी ऑफर ON/OFF)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (offersEnabled) {
                            "ON — Offer Price & Discount Badge active within dates"
                        } else {
                            "OFF — Students see regular Old Price (MRP) only"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = offersEnabled,
                    onCheckedChange = { offersEnabled = it },
                    modifier = Modifier.testTag("admin_toggle_all_offers_switch")
                )
            }

            OutlinedTextField(
                value = offerName,
                onValueChange = { offerName = it },
                label = { Text("Offer Name (e.g. Board Exam Special / Diwali Sale)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_offer_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = offerStartDate,
                    onValueChange = { offerStartDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-05-01") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_offer_start_date_input")
                )
                OutlinedTextField(
                    value = offerEndDate,
                    onValueChange = { offerEndDate = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-05-31") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_offer_end_date_input")
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        offerStartDate = DateUtils.todayIso()
                        offerEndDate = DateUtils.todayPlusDaysIso(7)
                    },
                    label = { Text("7 Days from Today") }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        offerStartDate = DateUtils.todayIso()
                        offerEndDate = DateUtils.todayPlusDaysIso(30)
                    },
                    label = { Text("30 Days from Today") }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        offerStartDate = ""
                        offerEndDate = ""
                    },
                    label = { Text("No Date Limit") }
                )
            }

            HorizontalDivider()

            // Section B: Promotional Banner Management
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. Promotional Banner (VIP Plans के ऊपर)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (bannerEnabled) {
                            "ON — Shown above VIP Plans (auto-hides after End Date)"
                        } else {
                            "OFF — Hidden from Student App"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = bannerEnabled,
                    onCheckedChange = { bannerEnabled = it },
                    modifier = Modifier.testTag("admin_toggle_banner_switch")
                )
            }

            // Live Banner Preview
            Text(
                text = "Live Banner Preview:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            VipPromotionalBannerCard(config = previewConfig)

            OutlinedTextField(
                value = bannerHeading,
                onValueChange = { bannerHeading = it },
                label = { Text("Banner Heading (e.g. बिहार बोर्ड 2026 स्पेशल ऑफर!)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_banner_heading_input")
            )

            OutlinedTextField(
                value = bannerDescription,
                onValueChange = { bannerDescription = it },
                label = { Text("Banner Description (e.g. सभी VIP प्लान पर सीमित समय की भारी छूट)") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_banner_desc_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = bannerOfferText,
                    onValueChange = { bannerOfferText = it },
                    label = { Text("Offer Tag Text") },
                    placeholder = { Text("FLASH SALE") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("admin_banner_offer_text_input")
                )
                OutlinedTextField(
                    value = bannerDiscountStr,
                    onValueChange = { input -> bannerDiscountStr = input.filter { it.isDigit() }.take(2) },
                    label = { Text("Discount %") },
                    placeholder = { Text("40") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("admin_banner_discount_input")
                )
            }

            Text(
                text = "Background Color Theme:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BannerColorTheme.entries.forEach { theme ->
                    val selected = theme.key == bannerColorKey
                    val swatchBrush = Brush.linearGradient(
                        listOf(Color(theme.startArgb.toInt()), Color(theme.endArgb.toInt()))
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { bannerColorKey = theme.key }
                            .testTag("banner_color_${theme.key}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(swatchBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected ${theme.label}",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        offersEnabled,
                        offerName,
                        offerStartDate,
                        offerEndDate,
                        bannerEnabled,
                        bannerHeading,
                        bannerDescription,
                        bannerOfferText,
                        bannerDiscountStr.toLongOrNull()?.coerceIn(0L, 99L) ?: 0L,
                        bannerColorKey
                    )
                },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_vip_offer_banner_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "Saving..." else "Save Offer & Banner to Firebase")
            }
        }
    }
}

@Composable
private fun AdminVipPlanRowCard(
    plan: DynamicVipPlan,
    offerActiveNow: Boolean,
    busy: Boolean,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val savedDiscount = VipPricingUtils.calculateDiscountPercent(plan.originalPriceInr, plan.offerPriceInr)
    val liveStudentPrice = plan.effectivePrice(offerActiveNow)
    val liveStudentDiscount = plan.effectiveDiscountPercent(offerActiveNow)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_vip_plan_card_${plan.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.active) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = if (plan.hasSpecialGift && plan.active) 1.5.dp else 1.dp,
            color = if (plan.hasSpecialGift && plan.active) CrownGold else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatusPill(
                            text = if (plan.active) "ACTIVE" else "INACTIVE",
                            containerColor = if (plan.active) SuccessGreenContainer else ErrorCrimsonContainer,
                            contentColor = if (plan.active) SuccessGreen else ErrorCrimson
                        )
                    }
                    if (plan.titleHindi.isNotBlank()) {
                        Text(
                            text = plan.titleHindi,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "ID: ${plan.id} • ${plan.durationDays} Days (${plan.durationLabel}) • Order #${plan.displayOrder}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = plan.active,
                    onCheckedChange = { onToggleActive() },
                    enabled = !busy,
                    modifier = Modifier.testTag("toggle_vip_plan_active_${plan.id}")
                )
            }

            // Pricing Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (savedDiscount > 0L) {
                    Text(
                        text = "₹${plan.originalPriceInr}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = "₹${plan.offerPriceInr}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = RoyalBluePrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    StatusPill(
                        text = "$savedDiscount% OFF",
                        containerColor = SuccessGreenContainer,
                        contentColor = SuccessGreen,
                        icon = Icons.Filled.LocalOffer
                    )
                } else {
                    Text(
                        text = "₹${plan.originalPriceInr}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = RoyalBluePrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    StatusPill(
                        text = "Regular Price",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = if (!plan.active) {
                    "छात्रों से छिपा हुआ (Inactive) — कोई नया भुगतान नहीं हो सकता"
                } else if (liveStudentDiscount > 0L) {
                    "छात्रों को अभी दिख रहा मूल्य: ₹$liveStudentPrice ($liveStudentDiscount% OFF सक्रिय)"
                } else if (savedDiscount > 0L && !offerActiveNow) {
                    "ऑफर बंद/समाप्त है — छात्रों को अभी नियमित मूल्य ₹$liveStudentPrice दिख रहा है"
                } else {
                    "छात्रों को अभी दिख रहा मूल्य: ₹$liveStudentPrice"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (liveStudentDiscount > 0L) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            if (plan.hasSpecialGift || plan.badge.isNotBlank() || plan.offerName.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (plan.hasSpecialGift) {
                        StatusPill(
                            text = "Special Gift",
                            containerColor = CrownGold,
                            contentColor = OnCrownGoldContainer,
                            icon = Icons.Filled.CardGiftcard
                        )
                    }
                    if (plan.badge.isNotBlank()) {
                        StatusPill(
                            text = plan.badge,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (plan.offerName.isNotBlank()) {
                        StatusPill(
                            text = plan.offerName,
                            containerColor = WarningAmberContainer,
                            contentColor = WarningAmber
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !busy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_vip_plan_${plan.id}")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Price / Details")
                }
                IconButton(
                    onClick = onDelete,
                    enabled = !busy,
                    modifier = Modifier.testTag("delete_vip_plan_${plan.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete ${plan.title}",
                        tint = ErrorCrimson
                    )
                }
            }
        }
    }
}

@Composable
fun AdminVipPlanEditorDialog(
    initial: DynamicVipPlan?,
    nextDisplayOrder: Long,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val isEditing = initial != null
    var planIdInput by remember { mutableStateOf(initial?.id ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var titleHindi by remember { mutableStateOf(initial?.titleHindi ?: "") }
    var originalPriceStr by remember { mutableStateOf(initial?.originalPriceInr?.toString() ?: "") }
    var offerPriceStr by remember { mutableStateOf(initial?.offerPriceInr?.toString() ?: "") }
    var durationDaysStr by remember { mutableStateOf(initial?.durationDays?.toString() ?: "30") }
    var durationLabel by remember { mutableStateOf(initial?.durationLabel ?: "") }
    var badge by remember { mutableStateOf(initial?.badge ?: "") }
    var offerName by remember { mutableStateOf(initial?.offerName ?: "") }
    var hasSpecialGift by remember { mutableStateOf(initial?.hasSpecialGift ?: false) }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var displayOrderStr by remember { mutableStateOf((initial?.displayOrder ?: nextDisplayOrder).toString()) }

    val parsedOld = originalPriceStr.toLongOrNull() ?: 0L
    val parsedNew = offerPriceStr.toLongOrNull() ?: parsedOld
    val autoDiscount = VipPricingUtils.calculateDiscountPercent(parsedOld, parsedNew)
    val savings = (parsedOld - parsedNew).coerceAtLeast(0L)
    val priceValid = parsedOld in 1L..100000L && parsedNew in 1L..parsedOld
    val parsedDays = durationDaysStr.toLongOrNull() ?: 0L
    val canSubmit = !busy && title.trim().length >= 2 && priceValid && parsedDays in 1L..1825L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit VIP Plan (${initial?.id})" else "Add New VIP Plan",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isEditing) {
                    OutlinedTextField(
                        value = planIdInput,
                        onValueChange = { planIdInput = it },
                        label = { Text("Plan ID (optional, e.g. PLAN_2M)") },
                        placeholder = { Text("Auto-generated if left blank") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_vip_plan_id")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Plan Title * (e.g. 1 Month VIP)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_vip_plan_title")
                )

                OutlinedTextField(
                    value = titleHindi,
                    onValueChange = { titleHindi = it },
                    label = { Text("Hindi Title (e.g. 1 महीना VIP)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = originalPriceStr,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }.take(6)
                            val wasSynced = offerPriceStr == originalPriceStr || offerPriceStr.isBlank()
                            originalPriceStr = clean
                            if (wasSynced && !isEditing) {
                                offerPriceStr = clean
                            }
                        },
                        label = { Text("Old Price / MRP (₹) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editor_vip_old_price")
                    )
                    OutlinedTextField(
                        value = offerPriceStr,
                        onValueChange = { input ->
                            offerPriceStr = input.filter { it.isDigit() }.take(6)
                        },
                        label = { Text("New Offer Price (₹) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editor_vip_new_price")
                    )
                }

                // Auto-calculated discount preview banner
                Surface(
                    color = when {
                        parsedOld > 0L && parsedNew > parsedOld -> ErrorCrimsonContainer
                        autoDiscount > 0L -> SuccessGreenContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_vip_discount_preview")
                ) {
                    Text(
                        text = when {
                            parsedOld > 0L && parsedNew > parsedOld ->
                                "New Offer Price (₹$parsedNew) cannot exceed Old Price (₹$parsedOld)."
                            autoDiscount > 0L ->
                                "Auto-Calculated Discount: $autoDiscount% OFF (छात्र ₹$savings बचाएंगे)"
                            parsedOld > 0L ->
                                "No discount (0% OFF) — Regular price ₹$parsedOld"
                            else ->
                                "Enter Old Price and New Price to auto-calculate discount %"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            parsedOld > 0L && parsedNew > parsedOld -> ErrorCrimson
                            autoDiscount > 0L -> SuccessGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationDaysStr,
                        onValueChange = { durationDaysStr = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Duration (Days) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editor_vip_duration_days")
                    )
                    OutlinedTextField(
                        value = durationLabel,
                        onValueChange = { durationLabel = it },
                        label = { Text("Duration Label") },
                        placeholder = { Text("1 Month") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = badge,
                        onValueChange = { badge = it },
                        label = { Text("Badge (optional)") },
                        placeholder = { Text("Most Popular") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = displayOrderStr,
                        onValueChange = { displayOrderStr = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Order") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f)
                    )
                }

                OutlinedTextField(
                    value = offerName,
                    onValueChange = { offerName = it },
                    label = { Text("Plan Offer Name (optional)") },
                    placeholder = { Text("e.g. Exam Booster Deal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Includes Special Gift?", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hasSpecialGift, onCheckedChange = { hasSpecialGift = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active (Visible to Students)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val requestedId = planIdInput.trim().ifBlank { "PLAN_${title.trim()}" }
                    onSave(
                        initial?.id,
                        requestedId,
                        title.trim(),
                        titleHindi.trim(),
                        parsedOld,
                        parsedNew,
                        parsedDays,
                        durationLabel.trim().ifBlank { "$parsedDays Days" },
                        hasSpecialGift,
                        badge.trim(),
                        offerName.trim(),
                        active,
                        displayOrderStr.toLongOrNull() ?: nextDisplayOrder
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.testTag("editor_vip_plan_save_button")
            ) {
                Text(if (busy) "Saving..." else "Save Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
