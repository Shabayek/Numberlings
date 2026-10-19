package com.vividoai.numberlings.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.core.data.GrownUps
import com.vividoai.numberlings.ui.puzzle.Mood
import com.vividoai.numberlings.ui.puzzle.Numberling
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.CocoaSoft
import com.vividoai.numberlings.ui.theme.Leaf
import com.vividoai.numberlings.ui.theme.Paper

private val goodnight = "The meadows are resting now. Sweet dreams, little friend. We will be here tomorrow, rain or shine."

/**
 * Wellbeing card the village shows at the end of a session — a soft invitation
 * to rest, never a lockout, and no shaming for wanting a few more puzzles.
 * A sleepy Numberling says goodnight in the slowest, warmest voice it has,
 * and tapping the friend says it again.
 */
@Composable
fun SleepyInvitation(onRest: () -> Unit, onPlayMore: () -> Unit) {
    val settings by GameHub.repository.settings.collectAsState(initial = GrownUps())

    // One goodnight, in a bedtime-slow voice — repeated only if the child taps
    // the friend to hear it again. narration off = silence, still cozy.
    LaunchedEffect(settings.narration) {
        if (settings.narration) GameHub.speaker.say(goodnight, slow = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Numberling(
            modifier = Modifier
                .size(150.dp)
                .clickable {
                    if (settings.narration) GameHub.speaker.say(goodnight, slow = true)
                },
            mood = Mood.SLEEPY,
            body = Leaf
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "The village is getting sleepy.",
            style = MaterialTheme.typography.headlineMedium,
            color = Cocoa,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Your meadows will still be here later — no rush, ever.",
            color = CocoaSoft,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPlayMore) { Text("A few more puzzles, please") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRest) { Text("Sweet dreams — see you soon") }
    }
}
