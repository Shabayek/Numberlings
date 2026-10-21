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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * The words of the goodnight, in one place so the screen and its test cannot
 * drift apart.
 */
object Goodnight {
    const val HEADLINE = "Rest is part of learning."

    const val MESSAGE =
        "While you rest, your brain sorts and keeps everything you practised " +
            "today - that is what makes tomorrow's puzzles feel easier.\n\n" +
            "And this little phone is a classroom, not a toy box: a few happy " +
            "minutes of learning, and then it goes quiet so the real day can begin."

    const val EXIT = "Exit"
    const val LEARN = "Learn something new"

    /** The bedtime-voice version, slower and warmer than the usual narration. */
    const val SPOKEN =
        "Sleep is part of learning, little friend. Your brain sorts and keeps " +
            "what you practised today while you rest. Sweet dreams."
}

/**
 * The end of a session: a quiet word about sleeping and about what a phone is
 * for, then two honest choices - close the app, or come back and learn
 * something new. Never a dead end.
 */
@Composable
fun GoodnightScreen(onExit: () -> Unit, onLearnMore: () -> Unit) {
    val settings by GameHub.repository.settings.collectAsState(initial = GrownUps())

    LaunchedEffect(settings.narration) {
        if (settings.narration) GameHub.speaker.say(Goodnight.SPOKEN, slow = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Numberling(
            modifier = Modifier
                .size(140.dp)
                .clickable {
                    if (settings.narration) GameHub.speaker.say(Goodnight.SPOKEN, slow = true)
                },
            mood = Mood.SLEEPY,
            body = Leaf
        )
        Spacer(Modifier.height(16.dp))
        Text(
            Goodnight.HEADLINE,
            style = MaterialTheme.typography.headlineMedium,
            color = Cocoa,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            Goodnight.MESSAGE,
            style = MaterialTheme.typography.bodyLarge,
            color = CocoaSoft,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onExit) { Text(Goodnight.EXIT) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onLearnMore) { Text(Goodnight.LEARN) }
    }
}

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
