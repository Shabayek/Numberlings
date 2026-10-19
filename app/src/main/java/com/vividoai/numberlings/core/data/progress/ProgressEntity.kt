package com.vividoai.numberlings.core.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** On-device progress notebook (Room, this device only). */
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val id: String = "village",
    /** skillId:mastery(0-100) pairs, semicolon separated. */
    val masteryCsv: String,
    /** Earned album stickers, comma separated indices. */
    val stickersCsv: String = "",
    val hearts: Int = 0,
    val blocksCompleted: Int = 0
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE id = 'village' LIMIT 1")
    fun observe(): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE id = 'village' LIMIT 1")
    suspend fun current(): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ProgressEntity)
}

@Database(entities = [ProgressEntity::class], version = 2, exportSchema = false)
abstract class VillageDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        fun build(context: Context): VillageDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                VillageDatabase::class.java,
                "numberlings.db"
            ).fallbackToDestructiveMigration().build()
    }
}
