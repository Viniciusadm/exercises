package com.archieapps.exercises.core.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "json_cache")
data class JsonCacheEntity(
    @PrimaryKey val key: String,
    val payload: String,
    val updatedAt: Long,
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val executionType: String,
    val usesWeight: Boolean,
    val muscleGroup: String?,
    val difficulty: String?,
    val equipment: String?,
    val restSeconds: Int?,
    val weightOptions: List<Double> = emptyList(),
    val notes: String?,
    val lastPerformedAt: String?,
    val sessionsCount: Int,
    val bestWeightKg: Double?,
    val totalVolumeKg: Double,
)

@Entity(tableName = "pending_workouts")
data class PendingWorkoutEntity(
    @PrimaryKey val clientUuid: String,
    val payload: String,
    val createdAt: Long,
    val lastError: String? = null,
)

@Dao
interface PendingWorkoutDao {
    @Query("SELECT * FROM pending_workouts ORDER BY createdAt")
    fun observeAll(): Flow<List<PendingWorkoutEntity>>

    @Query("SELECT * FROM pending_workouts ORDER BY createdAt")
    suspend fun all(): List<PendingWorkoutEntity>

    @Query("SELECT COUNT(*) FROM pending_workouts")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: PendingWorkoutEntity)

    @Query("DELETE FROM pending_workouts WHERE clientUuid = :clientUuid")
    suspend fun delete(clientUuid: String)

    @Query("UPDATE pending_workouts SET lastError = :error WHERE clientUuid = :clientUuid")
    suspend fun markError(clientUuid: String, error: String?)
}

@Dao
interface JsonCacheDao {
    @Query("SELECT * FROM json_cache WHERE `key` = :key")
    fun observe(key: String): Flow<JsonCacheEntity?>

    @Query("SELECT * FROM json_cache WHERE `key` = :key")
    suspend fun find(key: String): JsonCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: JsonCacheEntity)

    @Query("DELETE FROM json_cache WHERE `key` = :key")
    suspend fun remove(key: String)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun find(id: Int): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM exercises WHERE id NOT IN (:keepIds)")
    suspend fun deleteMissing(keepIds: List<Int>)
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN weightOptions TEXT NOT NULL DEFAULT ''")
    }
}

class Converters {
    @TypeConverter
    fun weightOptionsToString(options: List<Double>?): String =
        options.orEmpty().joinToString(separator = ",")

    @TypeConverter
    fun weightOptionsFromString(stored: String?): List<Double> =
        stored.orEmpty().split(",").mapNotNull { it.toDoubleOrNull() }
}

@Database(
    entities = [JsonCacheEntity::class, ExerciseEntity::class, PendingWorkoutEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ExercisesDatabase : RoomDatabase() {
    abstract fun jsonCache(): JsonCacheDao

    abstract fun exercises(): ExerciseDao

    abstract fun pendingWorkouts(): PendingWorkoutDao
}
