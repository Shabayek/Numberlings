package com.vividoai.numberlings.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.ui.puzzle.Mood
import com.vividoai.numberlings.ui.puzzle.Numberling
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.MeadowColors
import com.vividoai.numberlings.ui.theme.Paper

private data class WelcomePage(
    val story: String,
    val spoken: String,
    val friend: Int,       // meadow colour for the friend's body
    val mood: Mood,
    val masthead: String   // big, warm page title
)

private val pages = listOf(
    WelcomePage(
        masthead = "Hello!",
        story = "These are the Numberlings. Small, round, and always curious. They live in a quiet village of six meadows.",
        spoken = "Hello! These are the Numberlings. They live in a quiet village of six meadows.",
        friend = 0,
        mood = Mood.HAPPY
    ),
    WelcomePage(
        masthead = "Puzzles grow the village",
        story = "Every little puzzle a Numberling solves makes the meadows warmer: flowers, orchards, brooks and shapes.",
        spoken = "Every puzzle makes the meadows grow warmer. Let's count together!",
        friend = 3,
        mood = Mood.CHEER
    ),
    WelcomePage(
        masthead = "Off we wander",
        story = "No timers, no hurry. A Numberling nods when you answer, and counts with you when you're unsure. Ready?",
        spoken = "No timers, no hurry. Off we wander!",
        friend = 1,
        mood = Mood.THINK
    )
)

/**
 * The first-run welcome: a short, warm story a pre-reader can follow by
 * listening and tapping one big button. Shown once; never again unless the
 * child wants to hear it (the village is always the next screen).
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]

    LaunchedEffect(current) {
        if (GameHub.repository.settingsNow().narration) {
            GameHub.speaker.say(current.spoken)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.6f))
        Text(
            current.masthead,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Cocoa,
            textAlign = TextAlign.Center
        )
        Numberling(
            modifier = Modifier.size(160.dp),
            mood = current.mood,
            body = MeadowColors[current.friend % MeadowColors.size]
        )
        Text(
            current.story,
            style = MaterialTheme.typography.bodyLarge,
            color = Cocoa,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (index == page) Cocoa else Cocoa.copy(alpha = 0.2f))
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (page < pages.lastIndex) {
            Button(
                onClick = {
                    GameHub.sound.tap()
                    page++
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text("Next", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "A grown-up can change the voice and pace in the village, under Grown-ups.",
                style = MaterialTheme.typography.bodySmall,
                color = Cocoa.copy(alpha = 0.6f)
            )
        } else {
            Button(
                onClick = {
                    GameHub.sound.yes()
                    GameHub.repository.completeOnboarding()
                    onDone()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text("Let's play", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
