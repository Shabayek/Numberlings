package com.vividoai.numberlings.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.core.engine.Curriculum
import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.ui.puzzle.MeadowArt
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.CocoaSoft
import com.vividoai.numberlings.ui.theme.MeadowColors

/**
 * Morning in the village: one card per meadow. A meadow wakes itself up at its
 * gentle moment: the previous one's puzzles stay warm, so review is always alive.
 */
@Composable
fun VillageScreen(openMeadow: (Meadow) -> Unit, openStickers: () -> Unit, openGrownUps: () -> Unit) {
    val blocks by GameHub.repository.blocksCompleted.collectAsState()
    val hearts by GameHub.repository.hearts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Two slim rows so nothing is ever clipped on small screens: the
        // village identity with the grown-ups door on top, the child's own
        // scoreboard and sticker album beneath.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Numberlings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = openGrownUps) {
                Text("Grown-ups")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("hearts $hearts · puzzles $blocks", style = MaterialTheme.typography.bodyLarge, color = CocoaSoft)
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = openStickers) {
                Text("Stickers")
            }
        }
        Spacer(Modifier.height(2.dp))

        Meadow.entries.forEachIndexed { index, meadow ->
            val unlocked = blocks >= unlockBarrier(index)
            val accent = MeadowColors[index % MeadowColors.size]
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (unlocked) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 2.dp else 0.dp),
                modifier = Modifier.clickable(enabled = unlocked) { openMeadow(meadow) }
            ) {
                Column(Modifier.padding(18.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        MeadowArt(
                            meadow = meadow,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(meadow.title, fontWeight = FontWeight.Bold, color = if (unlocked) accent else CocoaSoft)
                    Text(meadow.subtitle, color = Cocoa)
                }
            }
            if (!unlocked) {
                Text("Sleeping quietly until ${unlockBarrier(index)} puzzles wake the village.", color = CocoaSoft)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun unlockBarrier(meadowIndex: Int): Int = 12 * meadowIndex
