package com.vividoai.numberlings.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.core.model.StickerAlbum
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.CocoaSoft

private fun stickerColor(index: Int): Color = when (StickerAlbum.meadow(index)) {
    0 -> Color(0xFF6FA07A)
    1 -> Color(0xFFE58F8F)
    2 -> Color(0xFF7FA9C9)
    3 -> Color(0xFFF2B84B)
    4 -> Color(0xFF7C6BA0)
    else -> Color(0xFFC97F5E)
}

/** The album: every place earned by practice, stamped forever. */
@Composable
fun StickerAlbumScreen(onBack: () -> Unit) {
    val earned by GameHub.repository.stickers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Sticker album", style = MaterialTheme.typography.headlineMedium, color = Cocoa)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        Text(
            "Earned so far: ${earned.size} of ${StickerAlbum.TOTAL}. Market Day gives the worn ones a shine.",
            color = CocoaSoft
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(StickerAlbum.TOTAL) { index ->
                val inAlbum = index in earned
                val tint = if (inAlbum) stickerColor(index) else CocoaSoft.copy(alpha = 0.16f)
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tint)
                        .let { it }
                        .let { base ->
                            if (inAlbum) base else base.border(2.dp, CocoaSoft.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        color = if (inAlbum) Color.White else Cocoa,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
