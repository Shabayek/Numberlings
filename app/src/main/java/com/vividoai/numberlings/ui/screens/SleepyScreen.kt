package com.vividoai.numberlings.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Wellbeing card the village shows at the end of a session — a soft invitation
 * to rest, never a lockout, and no shaming for wanting a few more puzzles.
 */
@Composable
fun SleepyInvitation(onRest: () -> Unit, onPlayMore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("The village is getting sleepy.", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        Text("Your meadows will still be here later — no rush, ever.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPlayMore) { Text("A few more puzzles, please") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRest) { Text("Sweet dreams — see you soon") }
    }
}
