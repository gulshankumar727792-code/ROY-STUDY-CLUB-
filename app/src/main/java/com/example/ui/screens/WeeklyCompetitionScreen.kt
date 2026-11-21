package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.UiState
import com.example.data.WeeklyChampion
import com.example.data.WeeklyLeaderboardEntry
import com.example.ui.components.BoardHeaderBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.VipCrownBadge
import com.example.ui.theme.CrownGold
import com.example.ui.theme.CrownGoldContainer
import com.example.ui.theme.OnCrownGoldContainer
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer

@Composable
fun WeeklyCompetitionScreen(
    selectedWeekKey: String,
    currentUserId: String,
    leaderboardState: UiState<List<WeeklyLeaderboardEntry>>,
    championsState: UiState<List<WeeklyChampion>>,
    onWeekKeyChange: (String) -> Unit
) {
    var weekInput by remember(selectedWeekKey) { mutableStateOf(selectedWeekKey) }
    val champions = (championsState as? UiState.Success)?.data.orEmpty()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("weekly_competition_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BoardHeaderBanner(
                    title = "साप्ताहिक चैंपियन / Weekly Champion",
                    subtitle = "दैनिक टेस्ट के अंकों और कम समय (टाई-ब्रेकर) के आधार पर हर सप्ताह विजेता चुना जाता है।",
                    badge = "Week $selectedWeekKey"
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weekInput,
                        onValueChange = { weekInput = it },
                        label = { Text("Week Key (e.g. ${DateUtils.currentWeekKey()})") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onWeekKeyChange(weekInput) },
                        modifier = Modifier.testTag("apply_week_key_button")
                    ) {
                        Text("Load", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                SectionHeader(
                    title = "साप्ताहिक लीडरबोर्ड / Leaderboard",
                    subtitle = "Sorted by Total Marks → Correct Answers → Lowest Time"
                )
            }

            when (leaderboardState) {
                is UiState.Loading -> item { LoadingCard() }
                is UiState.Error -> item { ErrorStateCard(leaderboardState.message) }
                is UiState.Success -> {
                    val entries = leaderboardState.data
                    if (entries.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Leaderboard,
                                title = "इस सप्ताह अभी कोई स्कोर नहीं है",
                                subtitle = "Attempt published Daily Tests to enter the $selectedWeekKey Weekly Champion leaderboard."
                            )
                        }
                    } else {
                        itemsIndexed(entries, key = { _, e -> e.id }) { idx, entry ->
                            LeaderboardRowCard(
                                rank = idx + 1,
                                entry = entry,
                                isMe = entry.userId == currentUserId
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "विजेता इतिहास / Verified Champions",
                    subtitle = "Owner Admin verified Weekly Champions, gifts & special offers (private contact kept confidential)"
                )
            }

            when (championsState) {
                is UiState.Loading -> item { LoadingCard() }
                is UiState.Error -> item { ErrorStateCard(championsState.message) }
                is UiState.Success -> {
                    if (champions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.EmojiEvents,
                                title = "अभी कोई साप्ताहिक चैंपियन घोषित नहीं हुआ है",
                                subtitle = "Weekly Champions verified by Owner Admin will be celebrated here."
                            )
                        }
                    } else {
                        items(champions, key = { it.id }) { champ ->
                            ChampionHistoryCard(champ)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    rank: Int,
    entry: WeeklyLeaderboardEntry,
    isMe: Boolean
) {
    val rankBg = when (rank) {
        1 -> CrownGold
        2 -> Color(0xFFD0D9E8)
        3 -> Color(0xFFE6B89C)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val rankFg = when (rank) {
        1 -> OnCrownGoldContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isMe || rank == 1) 1.5.dp else 1.dp,
            color = if (rank == 1) CrownGold else if (isMe) RoyalBluePrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(rankBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = rankFg,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isMe) "${entry.displayName} (You)" else entry.displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (entry.vipBadge) {
                        VipCrownBadge("VIP")
                    }
                }
                Text(
                    text = buildString {
                        if (entry.district.isNotBlank()) append("${entry.district} • ")
                        append("${entry.testsCount} Daily Tests • ${entry.totalCorrect} Correct")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Tie-breaker Time: ${DateUtils.formatDurationSeconds(entry.totalTimeSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.totalMarks}",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalBluePrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Marks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChampionHistoryCard(champ: WeeklyChampion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CrownGold),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    text = champ.weekLabel,
                    containerColor = CrownGoldContainer,
                    contentColor = OnCrownGoldContainer,
                    icon = Icons.Filled.EmojiEvents
                )
                StatusPill(
                    text = "${champ.totalMarks} Marks",
                    containerColor = SuccessGreenContainer,
                    contentColor = SuccessGreen
                )
            }

            Text(
                text = champ.winnerName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = buildString {
                    if (champ.winnerDistrict.isNotBlank()) append("${champ.winnerDistrict} • ")
                    append("${champ.totalCorrect} Correct • ${champ.testsCount} Tests • ${DateUtils.formatDurationSeconds(champ.totalTimeSeconds)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (champ.giftDetails.isNotBlank() || champ.specialOffer.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = OnCrownGoldContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = listOf(champ.giftDetails, champ.specialOffer)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnCrownGoldContainer
                    )
                }
            }

            if (champ.announcementNote.isNotBlank()) {
                Text(
                    text = champ.announcementNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
