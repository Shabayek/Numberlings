package com.vividoai.numberlings.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.core.engine.Curriculum
import com.vividoai.numberlings.core.engine.TaskFactory
import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.core.model.SkillKind
import com.vividoai.numberlings.core.model.Task
import com.vividoai.numberlings.ui.puzzle.PuzzleArt
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.MeadowColors
import kotlin.random.Random

private enum class PuzzleFrame { SHOWING, AFTER_WORK }

private fun nextSkillFor(meadow: Meadow, seed: Long): SkillKind {
    val skills = Curriculum.meadowSkills[meadow] ?: return SkillKind.COUNT
    return GameHub.repository.adaptive.value.nextSkill(skills, seed)
}

@Composable
fun TaskScreen(meadow: Meadow, onBack: () -> Unit, celebrateBlock: (Meadow) -> Unit) {
    var seed by remember { mutableStateOf(Random.nextLong()) }
    var skill by remember { mutableStateOf(nextSkillFor(meadow, seed)) }
    var solvedInBlock by remember { mutableIntStateOf(0) }
    var frame by remember { mutableStateOf(PuzzleFrame.SHOWING) }
    var biasAndTier by remember { mutableIntStateOf(0) }

    fun preparePuzzle() {
        seed = seed + 7
        skill = nextSkillFor(meadow, seed)
        val bias = GameHub.repository.settingsNow().bias
        biasAndTier = (GameHub.repository.adaptive.value.tierFor(skill) + bias).coerceIn(0, 2)
    }

    LaunchedEffect(Unit) { preparePuzzle() }

    val task: Task = remember(skill, biasAndTier, seed) {
        TaskFactory.make(skill, biasAndTier, (seed * 31) + 7)
    }

    val accent = MeadowColors[meadow.ordinal % MeadowColors.size]

    LaunchedEffect(task) {
        if (GameHub.repository.settingsNow().narration) {
            GameHub.speaker.say(task.spoken)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.width(10.dp))
            Text(meadow.title, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.weight(1f))
            Text("solved $solvedInBlock", color = Cocoa)
        }

        if (frame == PuzzleFrame.SHOWING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                PuzzleArt(task.visual, Modifier.fillMaxSize())
            }
            Text(task.prompt, style = MaterialTheme.typography.titleLarge, color = Cocoa)
            OutlinedButton(onClick = {
                if (GameHub.repository.settingsNow().narration) GameHub.speaker.say(task.spoken)
            }) { Text("Again, please") }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                task.choices.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { choice ->
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val index = task.choices.indexOf(choice)
                                    val right = index == task.correctIndex
                                    GameHub.repository.record(task.skill, right)
                                    if (right) {
                                        GameHub.sound.poke()
                                        solvedInBlock++
                                        if (solvedInBlock >= 6) {
                                            GameHub.repository.celebrateAndRest()
                                            frame = PuzzleFrame.AFTER_WORK
                                            celebrateBlock(meadow)
                                        } else {
                                            preparePuzzle()
                                        }
                                    } else {
                                        GameHub.sound.maybe()
                                    }
                                }
                            ) {
                                Text(choice.label, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Beautiful work — one more heart inside the village.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = {
                        solvedInBlock = 0
                        preparePuzzle()
                        frame = PuzzleFrame.SHOWING
                    }) { Text("Keep exploring") }
                }
            }
        }
    }
}
