package com.vividoai.numberlings.core.data

import android.content.Context
import com.vividoai.numberlings.core.engine.AdaptiveState
import com.vividoai.numberlings.core.data.GrownUps
import com.vividoai.numberlings.core.model.StickerAlbum
import com.vividoai.numberlings.core.model.SkillKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

    /** One puzzle block done: heart up, load the day's determinstic sticker. */
    fun finishBlockForSticker(): Int {
        _hearts.value += 1
        _blocksCompleted.value += 1
        val stickerIndex = StickerAlbum.indexFor(_blocksCompleted.value)
        persistSnapshot()
        return stickerIndex
    }

    /** Market Day hands over the earned sticker — the only way one is granted. */
    fun grantSticker(index: Int) {
        _stickers.value = _stickers.value + index
        persistSnapshot()
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

    private fun persistSnapshot() {
        // Capture the moment plainly: the values are read here, on the caller's
        // thread, so an old coroutine can never overwrite a newer answer with
        // yesterday's numbers. Writes are serialized so their order follows
        // the calls.
        val mastery = adaptive.value.toCsv()
        val stickers = _stickers.value.sorted().joinToString(",")
        val hearts = _hearts.value
        val blocks = _blocksCompleted.value
        scope.launch {
            saveMutex.withLock {
                val latest = database.progressDao().current() ?: ProgressEntity(masteryCsv = "")
                database.progressDao().upsert(
                    latest.copy(
                        masteryCsv = mastery,
                        stickersCsv = stickers,
                        hearts = hearts,
                        blocksCompleted = blocks
                    )
                )
            }
        }
    }

    private val saveMutex = Mutex()

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
