package com.vividoai.numberlings.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vividoai.numberlings.GameHub
import com.vividoai.numberlings.core.data.BlockFinish
import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.ui.grownups.GrownUpsGate
import com.vividoai.numberlings.ui.grownups.GrownUpsHome
import com.vividoai.numberlings.ui.puzzle.Mood
import com.vividoai.numberlings.ui.puzzle.Numberling
import com.vividoai.numberlings.ui.screens.MarketDayScreen
import com.vividoai.numberlings.ui.screens.OnboardingScreen
import com.vividoai.numberlings.ui.screens.SleepyInvitation
import com.vividoai.numberlings.ui.screens.GoodnightScreen
import com.vividoai.numberlings.ui.screens.StickerAlbumScreen
import com.vividoai.numberlings.ui.screens.TaskScreen
import com.vividoai.numberlings.ui.screens.VillageScreen
import com.vividoai.numberlings.ui.theme.Cocoa
import com.vividoai.numberlings.ui.theme.Paper
import kotlinx.coroutines.delay

/** Where we are. A small, closed world of places: village, puzzle, market, album, grown-ups. */
sealed interface Destination {
    data object Village : Destination
    data class MeadowPlay(val meadow: Meadow) : Destination
    data class MarketDay(val finish: BlockFinish) : Destination
    data object Album : Destination
    data object GrownUps : Destination
    data object GrownUpsHome : Destination
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
    /** The end of a session: the goodnight word, then exit or learn more. */
    var goodnight by remember { mutableStateOf(false) }
    /** Bumped whenever the child asks for "a few more", so the rest timer
     *  starts a fresh interval instead of never firing again. */
    var restRound by remember { mutableIntStateOf(0) }
    val activity = LocalActivity.current
    val settingsOrNull by GameHub.repository.settings.collectAsState(initial = null)

    // A soft, branded beat while the settings load from the device — a child
    // never faces a blank screen.
    val settings = settingsOrNull ?: run {
        Box(
            Modifier
                .fillMaxSize()
                .background(Paper),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Numberling(
                    modifier = Modifier.size(112.dp),
                    mood = Mood.HAPPY,
                    body = com.vividoai.numberlings.ui.theme.Leaf
                )
                Text(
                    "Numberlings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Cocoa
                )
            }
        }
        return
    }

    // Wellbeing: after the grown-up-set number of minutes the village invites
    // rest. Asking for "a few more" re-arms the clock for another interval.
    LaunchedEffect(settings.sessionCapMinutes, restRound) {
        if (settings.sessionCapMinutes <= 0) return@LaunchedEffect
        delay(settings.sessionCapMinutes * 60_000L)
        sleepy = true
    }

    // The hardware back button belongs to the same closed world: from any
    // place other than the village it leads home instead of leaving the app.
    BackHandler(enabled = destination != Destination.Village) {
        destination = Destination.Village
    }

    when (val where = destination) {
        Destination.Village -> {
            when {
                goodnight -> GoodnightScreen(
                    onExit = { activity?.finish() },
                    onLearnMore = {
                        goodnight = false
                        restRound++
                    }
                )
                sleepy -> SleepyInvitation(
                    onRest = {
                        sleepy = false
                        goodnight = true
                    },
                    onPlayMore = {
                        sleepy = false
                        restRound++
                    }
                )
                !settings.onboarded -> OnboardingScreen(
                    onDone = { destination = Destination.Village }
                )
                else -> VillageScreen(
                    openMeadow = { meadow -> destination = Destination.MeadowPlay(meadow) },
                    openStickers = { destination = Destination.Album },
                    openGrownUps = { destination = Destination.GrownUps }
                )
            }
        }

        is Destination.MeadowPlay -> TaskScreen(
            meadow = where.meadow,
            onBack = { destination = Destination.Village },
            celebrateBlock = { finish ->
                destination = Destination.MarketDay(finish)
            }
        )

        is Destination.MarketDay -> MarketDayScreen(
            finish = where.finish,
            onDone = { destination = Destination.Village }
        )

        Destination.Album -> StickerAlbumScreen(
            onBack = { destination = Destination.Village }
        )

        Destination.GrownUps -> GrownUpsGate(
            unlocked = { destination = Destination.GrownUpsHome },
            back = { destination = Destination.Village },
            sound = GameHub.sound
        )

        Destination.GrownUpsHome -> GrownUpsHome(
            backToPlay = { destination = Destination.Village }
        )
    }
}
