package com.vividoai.numberlings.ui.grownups

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.core.audio.SoundCue
import com.vividoai.numberlings.core.data.DeviceNotes
import com.vividoai.numberlings.ui.theme.BerryDeep
import com.vividoai.numberlings.ui.theme.Cocoa
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Two-step grown-up gate: a still hold, then a mental-arithmetic question.
 * Sturdy for small hands, a short warm-up for grown-ups.
 *
 * A grown-up who has just passed the gate is not asked to do it again for a
 * while — the ritual belongs to the grown-up, not to the clock.
 */
@Composable
fun GrownUpsGate(unlocked: () -> Unit, back: () -> Unit, sound: SoundCue?) {
    val context = LocalContext.current
    val notes = remember { DeviceNotes(context) }
    val passed = remember { mutableStateOf(false) }

    // A recent pass still counts: no ritual every time the app is opened.
    LaunchedEffect(Unit) {
        if (notes.gateGraceActive()) {
            passed.value = true
            unlocked()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (passed.value) {
            ChallengeStep(
                unlocked = {
                    notes.markGatePassed()
                    unlocked()
                },
                back = back,
                sound = sound
            )
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
        } else {
            // Released early: the ring empties, so it never shows progress
            // that is not happening.
            fill = 0f
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
    val diameter = 176.dp
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(androidx.compose.foundation.shape.CircleShape)
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
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { fill },
            modifier = Modifier.size((diameter - 32.dp))
        )
    }
}

@Composable
private fun ChallengeStep(unlocked: () -> Unit, back: () -> Unit, sound: SoundCue?) {
    val seed = remember { System.currentTimeMillis() }
    val question = remember(seed) { GateQuestion.generate(seed) }
    var typed by remember { mutableStateOf(TextFieldValue()) }
    var wobble by remember { mutableFloatStateOf(0f) }
    var wrong by remember { mutableStateOf(false) }
    /** After a rejected answer, the next tap offers the old text for replacing. */
    var replaceOnNextPress by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // A tap that lands on a rejected answer must not quietly collapse the
    // selection and start appending behind it.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release && replaceOnNextPress) {
                replaceOnNextPress = false
                typed = typed.copy(selection = TextRange(0, typed.text.length))
            }
        }
    }

    // A gentle side-to-side nudge when the answer is not right, so a grown-up
    // can see that nothing happened instead of guessing.
    LaunchedEffect(wobble) {
        if (wobble > 0f) {
            delay(320)
            wobble = 0f
        }
    }
    val nudge = if (wobble > 0f) sin(wobble * 18f) * 12f * wobble else 0f

    Column(
        modifier = Modifier.offset { IntOffset(nudge.roundToInt(), 0) },
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
            onValueChange = {
                typed = it
                if (wrong) wrong = false
            },
            label = { androidx.compose.material3.Text("Answer") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            interactionSource = interactionSource,
            modifier = Modifier.onFocusChanged { state ->
                // Tapping into the field offers the whole old answer for
                // replacing, so a grown-up never has to delete it by hand.
                if (state.isFocused && typed.text.isNotEmpty()) {
                    typed = typed.copy(selection = TextRange(0, typed.text.length))
                }
            }
        )
        if (wrong) {
            androidx.compose.material3.Text(
                "Not quite — try again.",
                style = MaterialTheme.typography.bodyLarge,
                color = BerryDeep
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                if (question.isRight(typed.text)) {
                    sound?.yes()
                    unlocked()
                } else {
                    sound?.maybe()
                    wrong = true
                    wobble = 1f
                    // The rejected answer is offered for replacing, so the
                    // next thing typed swaps it out instead of following it.
                    replaceOnNextPress = true
                    typed = typed.copy(selection = TextRange(0, typed.text.length))
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
