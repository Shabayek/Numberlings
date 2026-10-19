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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.R
import com.vividoai.numberlings.core.data.GrownUps
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Grown-ups zone", style = MaterialTheme.typography.headlineMedium)

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
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Unlock all six meadows", style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.unlock_blurb),
                    color = Cocoa
                )
                UnlockButton(snackbar)
                Text(stringResource(R.string.unlock_community), color = Cocoa)
                TextButton(onClick = {
                    scope.launch { snackbar.showSnackbar("Community unlock applied — welcome to the whole village.") }
                }) {
                    Text("Apply Community unlock")
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
                        val csv = GameHub.repository.exportCsv()
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Numberlings progress\n" + csv)
                        }
                        context.startActivity(Intent.createChooser(share, "Share progress"))
                    }
                }) {
                    Text("Share progress with a grown-up")
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
