package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.db.JsonCacheDao
import com.archieapps.exercises.core.db.JsonCacheEntity
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.SafeCode
import com.archieapps.exercises.core.net.dto.CalendarDayDto
import com.archieapps.exercises.core.net.dto.StatsDto
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HomeSnapshot(
    val stats: StatsDto,
    val calendar: List<CalendarDayDto> = emptyList(),
)

data class CachedHome(val snapshot: HomeSnapshot, val syncedAt: Long)

class HomeRepository(
    private val api: ExercisesApi,
    private val cache: JsonCacheDao,
    private val caller: ApiCaller,
    private val json: Json,
    private val heatmapWeeks: Int = 26,
) {
    fun observe(): Flow<CachedHome?> = cache.observe(CACHE_KEY).map { entity ->
        entity?.let {
            runCatching { CachedHome(json.decodeFromString<HomeSnapshot>(it.payload), it.updatedAt) }.getOrNull()
        }
    }

    suspend fun refresh(today: LocalDate = SafeCode.today()): ApiResult<Unit> {
        val stats = caller.call { api.stats() }
        if (stats is ApiResult.Failure) return stats

        val from = today.with(DayOfWeek.MONDAY).minusWeeks((heatmapWeeks - 1).toLong())
        val calendar = caller.call { api.calendar(from.toString(), today.toString()) }
        if (calendar is ApiResult.Failure) return calendar

        val snapshot = HomeSnapshot(
            stats = (stats as ApiResult.Success).value,
            calendar = (calendar as ApiResult.Success).value,
        )

        cache.put(
            JsonCacheEntity(
                key = CACHE_KEY,
                payload = json.encodeToString(snapshot),
                updatedAt = System.currentTimeMillis(),
            )
        )

        return ApiResult.Success(Unit)
    }

    private companion object {
        const val CACHE_KEY = "home"
    }
}
