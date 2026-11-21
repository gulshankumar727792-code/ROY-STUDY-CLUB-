package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.ReferralConfig
import com.example.data.ReferralRecord
import com.example.data.ReferralStatus
import com.example.data.ReferralTransaction
import com.example.ui.components.EmptyStateCard
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

enum class AdminReferralFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    ELIGIBLE("1st Test Done"),
    SUCCESSFUL("Successful"),
    REJECTED("Rejected")
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.adminReferralTabContent(
    config: ReferralConfig,
    referrals: List<ReferralRecord>,
    transactions: List<ReferralTransaction>,
    activeFilter: AdminReferralFilter,
    busy: Boolean,
    onFilterChange: (AdminReferralFilter) -> Unit,
    onSaveConfig: (enabled: Boolean, rewardDays: Long, maxLimit: Long) -> Unit,
    onExportCsvSave: () -> Unit,
    onExportCsvShare: () -> Unit,
    onRequestApprove: (ReferralRecord) -> Unit,
    onRequestReject: (ReferralRecord) -> Unit
) {
    val pendingCount = referrals.count { it.statusEnum() == ReferralStatus.PENDING }
    val eligibleCount = referrals.count { it.statusEnum() == ReferralStatus.PENDING && it.firstDailyTestCompleted }
    val successfulCount = referrals.count { it.statusEnum() == ReferralStatus.SUCCESSFUL }
    val rejectedCount = referrals.count { it.statusEnum() == ReferralStatus.REJECTED }

    val filteredReferrals = when (activeFilter) {
        AdminReferralFilter.ALL -> referrals
        AdminReferralFilter.PENDING -> referrals.filter { it.statusEnum() == ReferralStatus.PENDING }
        AdminReferralFilter.ELIGIBLE -> referrals.filter { it.statusEnum() == ReferralStatus.PENDING && it.firstDailyTestCompleted }
        AdminReferralFilter.SUCCESSFUL -> referrals.filter { it.statusEnum() == ReferralStatus.SUCCESSFUL }
        AdminReferralFilter.REJECTED -> referrals.filter { it.statusEnum() == ReferralStatus.REJECTED }
    }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Total", "${referrals.size}", RoyalBluePrimary, Modifier.weight(1f))
            MetricTile("Pending", "$pendingCount", WarningAmber, Modifier.weight(1f))
            MetricTile("Test Done", "$eligibleCount", Color(0xFFB37200), Modifier.weight(1f))
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Successful", "$successfulCount", SuccessGreen, Modifier.weight(1f))
            MetricTile("Rejected", "$rejectedCount", ErrorCrimson, Modifier.weight(1f))
            MetricTile("Reward Txs", "${transactions.size}", RoyalBluePrimary, Modifier.weight(1f))
        }
    }

    // Global Referral Settings Card
    item {
        AdminReferralSettingsCard(
            config = config,
            busy = busy,
            onSaveConfig = onSaveConfig
        )
    }

    // CSV Report Export Card
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Export Referral Reports (CSV)", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Exports all ${referrals.size} referral records and ${transactions.size} auditable reward transactions to a standard CSV file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportCsvSave,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_export_referral_csv_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save CSV")
                    }
                    OutlinedButton(
                        onClick = onExportCsvShare,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_share_referral_csv_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share CSV")
                    }
                }
            }
        }
    }

    // Filter Chips + Referrals List
    item {
        SectionHeader(
            title = "Referral Records (${filteredReferrals.size})",
            subtitle = "Verify referrer & referred details, approve eligible referrals or reject suspicious activity"
        )
    }
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AdminReferralFilter.entries.forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.testTag("admin_ref_filter_${filter.name.lowercase()}")
                )
            }
        }
    }

    if (filteredReferrals.isEmpty()) {
        item {
            EmptyStateCard(
                icon = Icons.Filled.CardGiftcard,
                title = "No referrals in this filter",
                subtitle = "Student referrals will appear here automatically once new students register with a referral code."
            )
        }
    } else {
        items(filteredReferrals, key = { it.id }) { ref ->
            AdminReferralItemCard(
                ref = ref,
                busy = busy,
                onApprove = { onRequestApprove(ref) },
                onReject = { onRequestReject(ref) }
            )
        }
    }

    // Complete Auditable Reward Transaction History
    item {
        SectionHeader(
            title = "Auditable Reward Transaction History (${transactions.size})",
            subtitle = "Immutable ledger of every VIP referral reward credited"
        )
    }
    if (transactions.isEmpty()) {
        item {
            EmptyStateCard(
                icon = Icons.Filled.History,
                title = "No reward transactions yet",
                subtitle = "Approved referral rewards create immutable dual-entry audit logs here."
            )
        }
    } else {
        items(transactions, key = { it.id }) { tx ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${tx.beneficiaryName} (${tx.role})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(
                            text = "+${tx.rewardDays}d VIP",
                            containerColor = SuccessGreenContainer,
                            contentColor = SuccessGreen
                        )
                    }
                    Text(
                        text = "Pair: ${tx.counterpartyName} • Code: ${tx.referralCode} • TxID: ${tx.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "VIP Until: ${DateUtils.formatDateTime(tx.vipExpiresAfter)} • By: ${tx.processedBy} • ${DateUtils.formatDateTime(tx.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminReferralSettingsCard(
    config: ReferralConfig,
    busy: Boolean,
    onSaveConfig: (enabled: Boolean, rewardDays: Long, maxLimit: Long) -> Unit
) {
    var enabled by remember(config.enabled) { mutableStateOf(config.enabled) }
    var rewardDaysText by remember(config.rewardDays) { mutableStateOf(config.rewardDays.toString()) }
    var maxLimitText by remember(config.maxSuccessfulReferrals) { mutableStateOf(config.maxSuccessfulReferrals.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CrownGold),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Referral System Controls", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (enabled) "Program Active — new referrals allowed" else "Program Disabled — new referrals blocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) SuccessGreen else ErrorCrimson
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    modifier = Modifier.testTag("admin_referral_enable_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rewardDaysText,
                    onValueChange = { rewardDaysText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("VIP Reward Days") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_referral_reward_days_input")
                )
                OutlinedTextField(
                    value = maxLimitText,
                    onValueChange = { maxLimitText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Max Referrals / Student") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_referral_max_limit_input")
                )
            }

            Button(
                onClick = {
                    val days = (rewardDaysText.toLongOrNull() ?: 7L).coerceIn(1L, 365L)
                    val limit = (maxLimitText.toLongOrNull() ?: 20L).coerceIn(1L, 500L)
                    onSaveConfig(enabled, days, limit)
                },
                enabled = !busy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_save_referral_settings_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Referral Settings")
            }
        }
    }
}

@Composable
private fun AdminReferralItemCard(
    ref: ReferralRecord,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val st = ref.statusEnum()
    val (bg, fg) = when (st) {
        ReferralStatus.SUCCESSFUL -> SuccessGreenContainer to SuccessGreen
        ReferralStatus.REJECTED -> ErrorCrimsonContainer to ErrorCrimson
        ReferralStatus.PENDING -> WarningAmberContainer to WarningAmber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (st == ReferralStatus.PENDING && ref.firstDailyTestCompleted) 1.5.dp else 1.dp,
            color = if (st == ReferralStatus.PENDING && ref.firstDailyTestCompleted) CrownGold else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Code: ${ref.referralCode}",
                    style = MaterialTheme.typography.titleSmall,
                    color = RoyalBluePrimary
                )
                StatusPill(text = st.code, containerColor = bg, contentColor = fg)
            }

            Text(
                text = "Referrer: ${ref.referrerName} (${ref.referrerUid.take(10)}...)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Referred: ${ref.referredName} (${ref.referredUid.take(10)}...)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Registered: ${DateUtils.formatDateTime(ref.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = if (ref.firstDailyTestCompleted) {
                        "1st Daily Test: DONE"
                    } else {
                        "1st Daily Test: NOT DONE"
                    },
                    containerColor = if (ref.firstDailyTestCompleted) SuccessGreenContainer else CrownGoldContainer,
                    contentColor = if (ref.firstDailyTestCompleted) SuccessGreen else OnCrownGoldContainer
                )
                if (ref.rewardGranted) {
                    StatusPill(
                        text = "+${ref.rewardDays}d Credited",
                        containerColor = CrownGold,
                        contentColor = OnCrownGoldContainer
                    )
                }
            }

            if (ref.firstDailyTestCompleted && ref.firstDailyTestTitle.isNotBlank()) {
                Text(
                    text = "Completed Test: ${ref.firstDailyTestTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuccessGreen
                )
            }
            if (ref.rejectionReason.isNotBlank()) {
                Text(
                    text = "Rejection Reason: ${ref.rejectionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorCrimson
                )
            }

            if (st == ReferralStatus.PENDING && !ref.rewardGranted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        enabled = !busy && ref.firstDailyTestCompleted,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_approve_referral_${ref.id}")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Approve & Credit")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !busy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCrimson),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_reject_referral_${ref.id}")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminApproveReferralDialog(
    referral: ReferralRecord,
    rewardDays: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve Referral & Credit VIP?") },
        text = {
            Text(
                "This transaction-safe action will credit +$rewardDays VIP days to BOTH:\n" +
                    "• Referrer: ${referral.referrerName}\n" +
                    "• Referred Student: ${referral.referredName}\n\n" +
                    "Both rewards will be permanently recorded in the immutable audit ledger."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Text("Confirm & Credit +${rewardDays}d")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminRejectReferralDialog(
    referral: ReferralRecord,
    onDismiss: () -> Unit,
    onConfirmReject: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Suspicious Referral") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Referrer: ${referral.referrerName}\nReferred: ${referral.referredName}\nNo VIP reward will be granted."
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Rejection Reason (Required) *") },
                    placeholder = { Text("e.g. Duplicate account / suspicious activity") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_reject_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReject(reason) },
                enabled = reason.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson)
            ) {
                Text("Reject Referral")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
