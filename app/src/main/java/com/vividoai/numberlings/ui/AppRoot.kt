package com.vividoai.numberlings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.core.data.GrownUps
import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.ui.grownups.GrownUpsGate
import com.vividoai.numberlings.ui.grownups.GrownUpsHome
import com.vividoai.numberlings.ui.screens.SleepyInvitation
import com.vividoai.numberlings.ui.screens.TaskScreen
import com.vividoai.numberlings.ui.screens.VillageScreen
import kotlinx.coroutines.delay

/** Where we are. A small, closed world of four places: village, puzzle, sleep, grown-ups. */
sealed interface Destination {
    data object Village : Destination
    data class MeadowPlay(val meadow: Meadow) : Destination
    data object GrownUps : Destination
    data object GrownUpsHome : Destination
    data object Sleeping : Destination
}

/**
 * The game loop: play, celebrate, rest.
 * Nothing here is reachable from outside the app, nothing loops forever,
 * and play always leads honestly back to rest.
 */
@Composable
fun AppRoot() {
    var destination by remember { mutableStateOf<Destination>(Destination.Village) }
    var sleepy by remember { mutableStateOf(false) }
    val settings by GameHub.repository.settings.collectAsState(initial = GrownUps())

    // Wellbeing: after the grown-up-set number of minutes the village invites rest.
    LaunchedEffect(settings.sessionCapMinutes) {
        if (settings.sessionCapMinutes <= 0) return@LaunchedEffect
        delay(settings.sessionCapMinutes * 60_000L)
        sleepy = true
    }

    when (val where = destination) {
        Destination.Village -> {
            if (sleepy) {
                SleepyInvitation(
                    onRest = { sleepy = false },
                    onPlayMore = { sleepy = false }
                )
            } else {
                VillageScreen(
                    openMeadow = { meadow -> destination = Destination.MeadowPlay(meadow) },
                    openGrownUps = { destination = Destination.GrownUps }
                )
            }
        }

        is Destination.MeadowPlay -> TaskScreen(
            meadow = where.meadow,
            onBack = { destination = Destination.Village },
            celebrateBlock = { }
        )

        Destination.GrownUps -> GrownUpsGate(
            unlocked = { destination = Destination.GrownUpsHome },
            back = { destination = Destination.Village },
            sound = GameHub.sound
        )

        Destination.GrownUpsHome -> GrownUpsHome(
            backToPlay = { destination = Destination.Village }
        )

        Destination.Sleeping -> SleepyInvitation(
            onRest = { destination = Destination.Village },
            onPlayMore = { destination = Destination.Village }
        )
    }
}
