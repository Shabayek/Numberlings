package com.vividoai.numberlings.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.core.model.StickerAlbum
import com.vividoai.numberlings.ui.puzzle.Mood
import com.vividoai.numberlings.ui.puzzle.Numberling
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.CocoaSoft
import com.vividoai.numberlings.ui.theme.Sunflower

private fun stickerColor(index: Int): Color = when (StickerAlbum.meadow(index)) {
    0 -> Color(0xFF6FA07A)
    1 -> Color(0xFFE58F8F)
    2 -> Color(0xFF7FA9C9)
    3 -> Color(0xFFF2B84B)
    4 -> Color(0xFF7C6BA0)
    else -> Color(0xFFC97F5E)
}

/**
 * Market Day. The stall keeper asks for exactly N apples in the basket.
 * Counting them right earns the honest sticker. No timer, no failure: if the
 * handful is off, we simply re-count together.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketDayScreen(stickerIndex: Int, onDone: () -> Unit) {
    val target = 3 + (stickerIndex % 5)
    val itemsShown = target + 2
    val tapped = remember { mutableStateListOf<Int>() }
    var granted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Market Day", style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Numberling(
                modifier = Modifier.size(80.dp),
                mood = if (granted) Mood.CHEER else Mood.THINK,
                body = Sunflower
            )
            Text(
                "The stall keeper would love exactly $target apples in the basket. Tap them, then tap Ready!",
                style = MaterialTheme.typography.bodyLarge,
                color = Cocoa
            )
        }

        // Wrap, never overflow: Market Day up to 9 plates must fit on the
        // narrowest screen, so they flow onto extra rows instead of leaving
        // unreachable apples outside the viewport.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(itemsShown) { item ->
                val inTapped = item in tapped
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (inTapped) stickerColor(stickerIndex) else CocoaSoft.copy(alpha = 0.28f))
                        .clickable {
                            GameHub.sound.poke()
                            if (inTapped) tapped.remove(item) else tapped.add(item)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍎".takeIf { inTapped } ?: "", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        Text("In the basket: ${tapped.size}", color = Cocoa)

        if (!granted) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    GameHub.sound.tap()
                    tapped.clear()
                }) { Text("Wait — start again") }
                Button(onClick = {
                    if (tapped.size == target) {
                        GameHub.sound.yes()
                        GameHub.repository.grantSticker(stickerIndex)
                        granted = true
                    } else {
                        GameHub.sound.maybe()
                    }
                }) { Text("Ready!") }
            }
        } else {
            Spacer(Modifier.height(6.dp))
            Card {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Thank you! Sticker ${stickerIndex + 1} of ${StickerAlbum.TOTAL} is yours.",
                        color = Cocoa
                    )
                }
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Back to the village")
            }
        }
    }
}
