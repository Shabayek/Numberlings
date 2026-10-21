package com.vividoai.numberlings.ui.grownups

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.R
import com.vividoai.numberlings.core.data.DeviceNotes
import com.vividoai.numberlings.core.data.GrownUps
import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.ui.theme.BerryDeep
import com.vividoai.numberlings.ui.theme.Cocoa
import kotlinx.coroutines.launch

/**
 * The Grown-ups zone. Calm, honest, plain-language, and free of nonsense.
 * All knobs live here — inside the play area there are none, which is the point.
 */
@Composable
fun GrownUpsHome(backToPlay: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val grownUps by GameHub.repository.settings.collectAsState(initial = GrownUps())
    var choosingMeadow by remember { mutableStateOf(false) }
    var confirmingMeadow by remember { mutableStateOf<Meadow?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    // A finished purchase opens the meadows for good, on this device.
    DisposableEffect(Unit) {
        UnlockStore.onPurchased = { GameHub.repository.unlockAllMeadows() }
        onDispose { UnlockStore.onPurchased = null }
    }

    /** Say it now: the newest message replaces the last one instead of queueing. */
    fun say(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Grown-ups zone",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = backToPlay) {
                Text("Back to play")
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("In the play area", style = MaterialTheme.typography.titleLarge)
                ToggleRow(
                    "Read the puzzles aloud",
                    grownUps.narration,
                    onChange = { next -> GameHub.repository.setGrownUps(grownUps.copy(narration = next)) }
                )
                ToggleRow(
                    "Soft sound effects",
                    grownUps.sound,
                    onChange = { next -> GameHub.repository.setGrownUps(grownUps.copy(sound = next)) }
                )
                SliderRow("Sound level", grownUps.soundVolume, 0f..100f, steps = 0) { next ->
                    GameHub.repository.setGrownUps(grownUps.copy(soundVolume = next))
                }
                Text("${grownUps.soundVolume} / 100", color = Cocoa)
                SliderRow(
                    "Voice pace",
                    grownUps.voiceSpeed,
                    5f..15f,
                    steps = 9
                ) { next -> GameHub.repository.setGrownUps(grownUps.copy(voiceSpeed = next)) }
                Text("%.1fx voice".format(grownUps.rateFloat), color = Cocoa)
                SliderRow(
                    "Sleep invitation after",
                    grownUps.sessionCapMinutes,
                    5f..60f,
                    steps = 0
                ) { next -> GameHub.repository.setGrownUps(grownUps.copy(sessionCapMinutes = next)) }
                Text("Village rests after ${grownUps.sessionCapMinutes} minutes", color = Cocoa)
                SliderRow("Gentle difficulty nudge", grownUps.bias, -1f..1f, steps = 1) { next ->
                    GameHub.repository.setGrownUps(grownUps.copy(bias = next))
                }
                ToggleRow(
                    "Replay the welcome story",
                    !grownUps.onboarded,
                    onChange = { replay -> GameHub.repository.setGrownUps(grownUps.copy(onboarded = !replay)) }
                )
                Text("Start hearing the story again when you press 'back to play'.", color = Cocoa)
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Unlock all six meadows", style = MaterialTheme.typography.titleLarge)
                if (grownUps.meadowsUnlocked) {
                    // Honest state: the meadows are open, and there is nothing
                    // left to pay for.
                    Text(
                        "Every meadow is already open — bought, or given by the Community Unlock. " +
                            "It is remembered on this device, for good.",
                        color = Cocoa
                    )
                } else {
                    Text(
                        stringResource(R.string.unlock_blurb),
                        color = Cocoa
                    )
                    UnlockButton(snackbar)
                    Text(
                        "The price is shown by Google Play itself when the purchase is available on this device.",
                        color = Cocoa
                    )
                    Text(stringResource(R.string.unlock_community), color = Cocoa)
                    TextButton(onClick = {
                        GameHub.repository.unlockAllMeadows()
                        say("Community unlock applied — every meadow in the village is open.")
                    }) {
                        Text("Apply Community unlock")
                    }
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Progress at a glance", style = MaterialTheme.typography.titleLarge)
                val hearts by GameHub.repository.hearts.collectAsState()
                val blocks by GameHub.repository.blocksCompleted.collectAsState()
                Text("Hearts earned: $hearts — puzzle blocks: $blocks", color = Cocoa)
                Button(onClick = {
                    scope.launch {
                        val report = GameHub.repository.exportReport()
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Numberlings progress report")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(share, "Share progress"))
                    }
                }) {
                    Text("Share progress with a grown-up")
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Start again", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Erase what has been practised. Only a grown-up can do this, and it " +
                        "cannot be undone.",
                    color = Cocoa
                )
                OutlinedButton(onClick = { choosingMeadow = true }) {
                    Text("Forget one meadow…")
                }
                Text(
                    "The skills in that meadow start again from the beginning. Hearts, " +
                        "blocks, stickers and the other meadows stay as they are.",
                    color = Cocoa
                )
                Button(
                    onClick = { confirmClearAll = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BerryDeep)
                ) {
                    Text("Clear all progress…")
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Privacy, plainly", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.privacy_summary), color = Cocoa)
            }
        }
    }
    SnackbarHost(snackbar)

    // Which meadow should start again?
    if (choosingMeadow) {
        AlertDialog(
            onDismissRequest = { choosingMeadow = false },
            title = { Text("Forget a meadow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Which meadow should start again? The skills in it will be met " +
                            "from the beginning. Everything else stays as it is.",
                        color = Cocoa
                    )
                    Spacer(Modifier.height(8.dp))
                    Meadow.entries.forEach { meadow ->
                        TextButton(
                            onClick = {
                                choosingMeadow = false
                                confirmingMeadow = meadow
                            }
                        ) {
                            Text(meadow.title, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { choosingMeadow = false }) { Text("Cancel") }
            }
        )
    }

    // Last chance before one meadow is forgotten.
    val meadowToForget = confirmingMeadow
    if (meadowToForget != null) {
        AlertDialog(
            onDismissRequest = { confirmingMeadow = null },
            title = { Text("Forget ${meadowToForget.title}?") },
            text = {
                Text(
                    "The skills in this meadow will start again from the beginning. " +
                        "Hearts, blocks, stickers and the other meadows stay exactly as " +
                        "they are. This cannot be undone.",
                    color = Cocoa
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingMeadow = null
                    GameHub.repository.forgetMeadow(meadowToForget)
                    say("${meadowToForget.title} will start again from the beginning.")
                }) {
                    Text("Forget it", color = BerryDeep)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingMeadow = null }) { Text("Keep it") }
            }
        )
    }

    // Last chance before everything is erased.
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear all progress?") },
            text = {
                Text(
                    "This erases every heart, every finished block, every skill and " +
                        "every sticker on this device. The grown-up settings and the " +
                        "meadow unlock stay. This cannot be undone.",
                    color = Cocoa
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearAll = false
                    GameHub.repository.clearAllProgress()
                    DeviceNotes(context).clearBlockProgress()
                    say("All progress has been cleared.")
                }) {
                    Text("Clear everything", color = BerryDeep)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Int) -> Unit
) {
    Column {
        Text(title, color = Cocoa)
        Slider(
            value = value.toFloat(),
            onValueChange = { next -> onChange(next.toInt()) },
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Cocoa)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
