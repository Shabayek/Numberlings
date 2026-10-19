package com.vividoai.numberlings.ui.grownups

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.core.audio.SoundCue
import com.vividoai.numberlings.ui.theme.BerryDeep
import com.vividoai.numberlings.ui.theme.Cocoa
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Two-step grown-up gate: a still hold, then a mental-arithmetic question.
 * Sturdy for small hands, a short warm-up for grown-ups.
 */
@Composable
fun GrownUpsGate(unlocked: () -> Unit, back: () -> Unit, sound: SoundCue?) {
    val passed = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (passed.value) {
            ChallengeStep(unlocked, back, sound)
        } else {
            HoldStep(lockDone = { passed.value = true }, back, sound)
        }
    }
}

@Composable
private fun HoldStep(lockDone: () -> Unit, back: () -> Unit, sound: SoundCue?) {
    var holding by remember { mutableStateOf(false) }
    var fill by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(holding) {
        if (holding) {
            fill = 0f
            repeat(30) {
                delay(100)
                fill += 1f / 30f
            }
            fill = 1f
            sound?.poke()
            lockDone()
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text(
            text = androidx.compose.ui.res.stringResource(com.vividoai.numberlings.R.string.gate_hold),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(24.dp))
        RoundHoldButton(holding, fill, onHoldStart = { holding = true }, onHoldStop = { holding = false })
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = back) {
            androidx.compose.material3.Text("Back")
        }
    }
}

@Composable
fun RoundHoldButton(holding: Boolean, fill: Float, onHoldStart: () -> Unit, onHoldStop: () -> Unit) {
    val radius = 64.dp
    androidx.compose.material3.Surface(
        tonalElevation = 4.dp,
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onHoldStart()
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.all { !it.pressed }) break
                    }
                    onHoldStop()
                }
            }
            .padding(24.dp)
    ) {
        CircularProgressIndicator(
            progress = { fill },
            modifier = Modifier.height(radius * 2).padding(8.dp)
        )
    }
}

@Composable
private fun ChallengeStep(unlocked: () -> Unit, back: () -> Unit, sound: SoundCue?) {
    val seed = remember { System.currentTimeMillis() }
    val question = remember(seed) { GateQuestion.generate(seed) }
    var typed by remember { mutableStateOf("") }
    var shaky by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text(
            text = androidx.compose.ui.res.stringResource(com.vividoai.numberlings.R.string.gate_question_title),
            style = MaterialTheme.typography.titleLarge
        )
        androidx.compose.material3.Text(question.text, style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            label = { androidx.compose.material3.Text("Answer") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                if (question.isRight(typed)) {
                    sound?.yes()
                    unlocked()
                } else {
                    sound?.maybe()
                }
            }) {
                androidx.compose.material3.Text(
                    androidx.compose.ui.res.stringResource(com.vividoai.numberlings.R.string.gate_check)
                )
            }
            TextButton(onClick = back) {
                androidx.compose.material3.Text("Back")
            }
        }
    }
}

/** One mental-arithmetic check from a stable seed; regenerates per visit, never scores kids. */
data class GateQuestion(val a: Int, val b: Int) {
    val text = "$a × $b"
    private val answer = a * b
    fun isRight(input: String) = input.trim().toIntOrNull() == answer

    companion object {
        fun generate(seed: Long): GateQuestion {
            val rnd = Random(seed)
            val a = rnd.nextInt(5, 13)
            val b = rnd.nextInt(5, 13)
            return GateQuestion(a, b)
        }
    }
}
