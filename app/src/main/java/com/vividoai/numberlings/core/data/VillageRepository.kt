package com.vividoai.numberlings.core.data

import android.content.Context
import com.vividoai.numberlings.core.engine.AdaptiveState
import com.vividoai.numberlings.core.data.GrownUps
import com.vividoai.numberlings.core.model.StickerAlbum
import com.vividoai.numberlings.core.model.SkillKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What one finished puzzle block brought: a heart, and sometimes Market Day. */
data class BlockFinish(
    /** Every fifth block the stall keeper opens the market. */
    val marketDay: Boolean,
    /** The sticker to hand over, or -1 when the album is complete. */
    val stickerIndex: Int,
    /** True when every sticker is already earned. */
    val albumFull: Boolean
)

/** Village notebook: one repository class owning Room + GrownUpsStore + our adaptive state. */
class VillageRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = VillageDatabase.build(context)
    private val grownups = GrownUpsStore(context)

    val adaptive = MutableStateFlow(AdaptiveState())

    private val _hearts = MutableStateFlow(0)
    val hearts: StateFlow<Int> = _hearts.asStateFlow()

    private val _blocksCompleted = MutableStateFlow(0)
    val blocksCompleted: StateFlow<Int> = _blocksCompleted.asStateFlow()

    private val _stickers = MutableStateFlow<Set<Int>>(emptySet())
    val stickers: StateFlow<Set<Int>> = _stickers.asStateFlow()

    val settings = grownups.flow

    /** Snap-shot of the latest settings, for gentle reads during play. */
    var currentSettings = GrownUps()
        private set

    fun settingsNow(): GrownUps = currentSettings

    fun record(skill: SkillKind, correct: Boolean) {
        adaptive.value = adaptive.value.after(skill, correct)
        persistSnapshot()
    }

    /**
     * One puzzle block done: heart up, and — every fifth block — a Market Day
     * with the next sticker the album is still missing. A sticker the child
     * already owns is never handed over twice.
     */
    fun finishBlock(): BlockFinish {
        _hearts.value += 1
        _blocksCompleted.value += 1
        val blocks = _blocksCompleted.value
        val marketDay = StickerAlbum.isMarketDay(blocks)
        val stickerIndex = if (marketDay) StickerAlbum.nextUnowned(_stickers.value) else -1
        persistSnapshot()
        return BlockFinish(
            marketDay = marketDay,
            stickerIndex = stickerIndex,
            albumFull = marketDay && stickerIndex < 0
        )
    }

    /**
     * Market Day hands over the earned sticker — the only way one is granted.
     * Returns false when the album already held that sticker, so the app never
     * says "is yours" about something the child had long ago.
     */
    fun grantSticker(index: Int): Boolean {
        if (index < 0 || index >= StickerAlbum.TOTAL) return false
        if (index in _stickers.value) return false
        _stickers.value = _stickers.value + index
        persistSnapshot()
        return true
    }

    init {
        // Fresh start: read the village in once. During play the memory above
        // is the truth — the database writes are its snapshots. Listening to
        // every row change here would let a stale write win and quietly
        // un-earn a heart or a finished block.
        scope.launch {
            val entity = database.progressDao().current()
            if (entity == null) {
                // First run: a blank, kind village.
                database.progressDao().upsert(ProgressEntity(masteryCsv = ""))
            } else {
                _hearts.value = entity.hearts
                _blocksCompleted.value = entity.blocksCompleted
                _stickers.value = entity.stickersCsv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()
                adaptive.value = adaptive.value.fromCsv(entity.masteryCsv)
            }
        }

        grownups.flow.onEach { loaded ->
            currentSettings = loaded
        }.launchIn(scope)

        // One writer, one queue: snapshots are saved in the order they
        // happened, so the notebook never ends up holding an older moment.
        scope.launch {
            for (snapshot in saveQueue) {
                val latest = database.progressDao().current() ?: ProgressEntity(masteryCsv = "")
                database.progressDao().upsert(
                    latest.copy(
                        masteryCsv = snapshot.mastery,
                        stickersCsv = snapshot.stickers,
                        hearts = snapshot.hearts,
                        blocksCompleted = snapshot.blocks
                    )
                )
            }
        }
    }

    fun setGrownUps(next: GrownUps) {
        scope.launch {
            grownups.save(next)
        }
    }

    /** Called when the child (or their grown-up) finishes the first welcome. */
    fun completeOnboarding() {
        setGrownUps(currentSettings.copy(onboarded = true))
    }

    /**
     * The family owns every meadow — bought once, or given by the Community
     * Unlock. It is remembered on this device and opens every meadow for good.
     */
    fun unlockAllMeadows() {
        setGrownUps(currentSettings.copy(meadowsUnlocked = true))
    }

    private fun persistSnapshot() {
        // Capture the moment plainly: the values are read here, on the caller's
        // thread, so an old write can never overwrite a newer answer with
        // yesterday's numbers. The snapshots then travel through one queue to
        // one writer, so they are also saved in the order they happened.
        saveQueue.trySend(
            Snapshot(
                mastery = adaptive.value.toCsv(),
                stickers = _stickers.value.sorted().joinToString(","),
                hearts = _hearts.value,
                blocks = _blocksCompleted.value
            )
        )
    }

    private data class Snapshot(
        val mastery: String,
        val stickers: String,
        val hearts: Int,
        val blocks: Int
    )

    private val saveQueue = Channel<Snapshot>(Channel.UNLIMITED)

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val latest = database.progressDao().current() ?: return@withContext ""
        buildString {
            appendLine("skill,mastery_percent")
            latest.masteryCsv.split(";").forEach { pair ->
                if (pair.isNotBlank()) {
                    val parts = pair.split(":")
                    appendLine("${parts[0]},${parts.getOrNull(1) ?: "0"}")
                }
            }
            appendLine("hearts,${latest.hearts}")
            appendLine("blocks_completed,${latest.blocksCompleted}")
        }
    }
}
