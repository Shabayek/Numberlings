package com.vividoai.numberlings.core.data

import android.content.Context
import com.vividoai.numberlings.core.engine.AdaptiveState
import com.vividoai.numberlings.core.engine.Curriculum
import com.vividoai.numberlings.core.engine.ProgressReport
import com.vividoai.numberlings.core.data.GrownUps
import com.vividoai.numberlings.core.model.Meadow
import com.vividoai.numberlings.core.model.StickerAlbum
import com.vividoai.numberlings.core.model.SkillKind
import kotlinx.coroutines.CoroutineExceptionHandler
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

    /**
     * Background work must never take the app down: a write that cannot land
     * (a full disk, a database file replaced underneath us) is a bookkeeping
     * problem, not a reason to close the child's session.
     */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, failure ->
            android.util.Log.w("Numberlings", "background work failed", failure)
        }
    )
    private val database = VillageDatabase.build(context)
    private val grownups = GrownUpsStore(context)

    /**
     * The write queue. Created lazily so it can never be null: the writer
     * coroutine starts inside `init`, and property initialisers run in
     * declaration order — a plain `val` declared further down the class would
     * still be null when the writer first reaches for it.
     */
    private val saveQueue: Channel<Snapshot> by lazy { Channel(Channel.UNLIMITED) }

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
            try {
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
            } catch (failure: Exception) {
                // A village we cannot read yet is still a village we can play in:
                // the memory starts empty and the first write will create the row.
                android.util.Log.w("Numberlings", "could not read the village yet", failure)
            }
        }

        grownups.flow.onEach { loaded ->
            currentSettings = loaded
        }.launchIn(scope)

        // One writer, one queue: snapshots are saved in the order they
        // happened, so the notebook never ends up holding an older moment.
        startWriter()
    }

    /**
     * Starts the one writer that owns the queue. If it ever stops, it starts
     * again: a silent writer would mean a child quietly loses their progress.
     */
    private fun startWriter() {
        scope.launch {
            try {
                for (snapshot in saveQueue) {
                    try {
                        val latest = database.progressDao().current() ?: ProgressEntity(masteryCsv = "")
                        database.progressDao().upsert(
                            latest.copy(
                                masteryCsv = snapshot.mastery,
                                stickersCsv = snapshot.stickers,
                                hearts = snapshot.hearts,
                                blocksCompleted = snapshot.blocks
                            )
                        )
                    } catch (failure: Exception) {
                        // A write that cannot land is not worth a crash: the
                        // memory stays the truth and the next snapshot tries again.
                        android.util.Log.w("Numberlings", "progress write failed", failure)
                    }
                }
            } catch (failure: Exception) {
                android.util.Log.w("Numberlings", "progress writer stopped; starting it again", failure)
                startWriter()
            }
        }
    }

    fun setGrownUps(next: GrownUps) {
        scope.launch {
            try {
                grownups.save(next)
            } catch (failure: Exception) {
                android.util.Log.w("Numberlings", "settings save failed", failure)
            }
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

    /**
     * Forget one meadow: the skills that live there start again, so the
     * practice can be met fresh. Hearts, blocks, stickers and the other
     * meadows are left exactly as they were.
     */
    fun forgetMeadow(meadow: Meadow) {
        val skills = Curriculum.meadowSkills[meadow].orEmpty()
        adaptive.value = adaptive.value.forget(skills)
        persistSnapshot()
    }

    /**
     * Erase the progress on this device: hearts, blocks, skills and stickers.
     * The grown-up settings and the ownership of the meadows are not progress,
     * so they stay.
     */
    fun clearAllProgress() {
        adaptive.value = AdaptiveState()
        _hearts.value = 0
        _blocksCompleted.value = 0
        _stickers.value = emptySet()
        persistSnapshot()
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

    /**
     * The report a grown-up shares: plain words, named skills, and a short
     * machine-readable section at the end.
     */
    suspend fun exportReport(): String = withContext(Dispatchers.IO) {
        val percent = adaptive.value.scores.mapValues { (_, score) ->
            (score.mastery * 100).toInt().coerceIn(0, 100)
        }
        ProgressReport.render(
            masteryPercent = percent,
            hearts = _hearts.value,
            blocks = _blocksCompleted.value,
            stickers = _stickers.value
        )
    }
}
