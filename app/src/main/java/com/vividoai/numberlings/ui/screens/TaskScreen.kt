package com.vividoai.numberlings.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.core.data.BlockFinish
import com.vividoai.numberlings.core.data.DeviceNotes
import com.vividoai.numberlings.core.engine.Curriculum
import com.vividoai.numberlings.core.engine.TaskFactory
import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.core.model.SkillKind
import com.vividoai.numberlings.core.model.Task
import com.vividoai.numberlings.ui.puzzle.Mood
import com.vividoai.numberlings.ui.puzzle.Numberling
import com.vividoai.numberlings.ui.puzzle.NumberlingBodyColor
import com.vividoai.numberlings.ui.puzzle.PuzzleArt
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.CocoaSoft
import com.vividoai.numberlings.ui.theme.MeadowColors
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class PuzzleFrame { SHOWING, AFTER_WORK }

private fun nextSkillFor(meadow: Meadow, seed: Long): SkillKind {
    val skills = Curriculum.meadowSkills[meadow] ?: return SkillKind.COUNT
    return GameHub.repository.adaptive.value.nextSkill(skills, seed)
}

@Composable
fun TaskScreen(meadow: Meadow, onBack: () -> Unit, celebrateBlock: (BlockFinish) -> Unit) {
    val context = LocalContext.current
    val notes = remember { DeviceNotes(context) }
    // A block in progress survives a restart: the child never has to start the
    // six dots over again because the app was closed.
    val restored = remember(meadow) { notes.blockProgress(meadow) }

    var seed by remember { mutableStateOf(restored?.second ?: Random.nextLong()) }
    var skill by remember { mutableStateOf(nextSkillFor(meadow, seed)) }
    var solvedInBlock by remember { mutableIntStateOf(restored?.first ?: 0) }
    var frame by remember { mutableStateOf(PuzzleFrame.SHOWING) }
    var biasAndTier by remember { mutableIntStateOf(0) }
    var encouraged by remember { mutableStateOf(false) }
    var friendMood by remember { mutableStateOf(Mood.HAPPY) }
    var moodTick by remember { mutableIntStateOf(0) }
    var wrongTick by remember { mutableIntStateOf(0) }
    var wrongRecorded by remember { mutableStateOf(false) }
    /** One tap is one attempt: a burst of taps can never score the next puzzle. */
    var accepting by remember { mutableStateOf(false) }

    fun tierForNow(): Int {
        val bias = GameHub.repository.settingsNow().bias
        return (GameHub.repository.adaptive.value.tierFor(skill) + bias).coerceIn(0, 2)
    }

    fun preparePuzzle() {
        seed = seed + 7
        skill = nextSkillFor(meadow, seed)
        biasAndTier = tierForNow()
        encouraged = false
        friendMood = Mood.HAPPY
        wrongRecorded = false
        notes.saveBlockProgress(meadow, solvedInBlock, seed)
    }

    // The friend warms back to rest after cheering or wobbling.
    LaunchedEffect(moodTick) {
        if (friendMood != Mood.HAPPY) {
            delay(1200)
            friendMood = Mood.HAPPY
        }
    }

    LaunchedEffect(Unit) {
        if (restored != null) {
            // Pick up exactly where the child left off, same puzzle and all.
            biasAndTier = tierForNow()
        } else {
            preparePuzzle()
        }
    }

    val task: Task = remember(skill, biasAndTier, seed) {
        TaskFactory.make(skill, biasAndTier, (seed * 31) + 7)
    }

    val accent = MeadowColors[meadow.ordinal % MeadowColors.size]

    LaunchedEffect(task) {
        if (GameHub.repository.settingsNow().narration) {
            GameHub.speaker.say(task.spoken)
        }
        // A short pause before the first tap is answerable, so a hurried
        // double tap cannot land on the puzzle that follows.
        delay(350)
        accepting = true
    }

    // Every wrong attempt gets its own kind word, and the puzzle opens again
    // for another try.
    LaunchedEffect(wrongTick) {
        if (wrongTick > 0) {
            if (GameHub.repository.settingsNow().narration) {
                GameHub.speaker.say("Let's count together.")
            }
            delay(500)
            accepting = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Numberling(
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        if (frame == PuzzleFrame.SHOWING &&
                            GameHub.repository.settingsNow().narration
                        ) {
                            GameHub.speaker.say(task.spoken)
                        }
                    },
                mood = if (encouraged) Mood.OOPS else friendMood,
                body = NumberlingBodyColor(meadow.ordinal, MeadowColors)
            )
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.width(10.dp))
            Text(meadow.title, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.weight(1f))
            Text("solved $solvedInBlock", color = Cocoa)
        }

        // Six dots: how far this block has wandered. A pre-reader can watch
        // progress without reading a single word.
        if (frame == PuzzleFrame.SHOWING) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                repeat(6) { dot ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (dot < solvedInBlock) accent
                                else CocoaSoft.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }

        if (frame == PuzzleFrame.SHOWING) {
            fun answer(index: Int) {
                if (!accepting) return
                accepting = false
                val right = index == task.correctIndex
                if (right) {
                    GameHub.repository.record(task.skill, true)
                    GameHub.sound.poke()
                    encouraged = false
                    friendMood = Mood.CHEER
                    moodTick++
                    solvedInBlock++
                    if (solvedInBlock >= 6) {
                        notes.clearBlockProgress(meadow)
                        val finish = GameHub.repository.finishBlock()
                        if (finish.marketDay) {
                            celebrateBlock(finish)
                        } else {
                            frame = PuzzleFrame.AFTER_WORK
                        }
                    } else {
                        preparePuzzle()
                    }
                } else {
                    // One wobble per puzzle is enough to note: retrying the same
                    // puzzle must not keep costing the child their progress.
                    if (!wrongRecorded) {
                        wrongRecorded = true
                        GameHub.repository.record(task.skill, false)
                    }
                    GameHub.sound.maybe()
                    encouraged = true
                    wrongTick++
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                PuzzleArt(task.visual, Modifier.fillMaxSize())
                if (task.tapTheGroups) {
                    // No buttons: the two drawn groups ARE the answers, wrapped
                    // in soft meadow-colored outlines. A child points before
                    // they can read.
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(3.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                                .clickable { answer(0) }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(3.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                                .clickable { answer(1) }
                        )
                    }
                }
            }
            Text(task.prompt, style = MaterialTheme.typography.titleLarge, color = Cocoa)
            OutlinedButton(onClick = {
                if (GameHub.repository.settingsNow().narration) GameHub.speaker.say(task.spoken)
            }) { Text("Again, please") }

            if (!task.tapTheGroups) {
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
                                    onClick = { answer(task.choices.indexOf(choice)) }
                                ) {
                                    Text(choice.label, style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (encouraged) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Numberling(
                        modifier = Modifier.size(72.dp),
                        mood = Mood.OOPS,
                        body = NumberlingBodyColor(meadow.ordinal, MeadowColors)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Not quite yet — the Numberlings will count it together with you.",
                        color = Cocoa
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Numberling(
                        modifier = Modifier.size(120.dp),
                        mood = Mood.CHEER,
                        body = NumberlingBodyColor(meadow.ordinal, MeadowColors)
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Beautiful work — one more heart inside the village.",
                        style = MaterialTheme.typography.titleLarge,
                        color = Cocoa
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
