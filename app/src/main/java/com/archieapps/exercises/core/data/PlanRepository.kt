package com.archieapps.exercises.core.data

import com.archieapps.exercises.core.db.JsonCacheDao
import com.archieapps.exercises.core.db.JsonCacheEntity
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.dto.PlanConfirmMiscDto
import com.archieapps.exercises.core.net.dto.PlanConfirmRequest
import com.archieapps.exercises.core.net.dto.PlanMiscDto
import com.archieapps.exercises.core.net.dto.PlanOptionsDto
import com.archieapps.exercises.core.net.dto.PlanProposalDto
import com.archieapps.exercises.core.net.dto.PlanRequest
import com.archieapps.exercises.core.net.dto.TemplateDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class PlanDraft(
    val proposal: PlanProposalDto,
    val misc: PlanMiscDto = PlanMiscDto(),
)

@Serializable
data class PlanConfirmation(
    val templates: List<TemplateDto>,
    val misc: PlanConfirmMiscDto,
)

class PlanRepository(
    private val api: ExercisesApi,
    private val cache: JsonCacheDao,
    private val caller: ApiCaller,
    private val json: Json,
) {
    suspend fun options(): ApiResult<PlanOptionsDto> {
        val result = caller.call { api.planOptions() }

        if (result is ApiResult.Success) {
            put(OPTIONS_KEY, json.encodeToString(PlanOptionsDto.serializer(), result.value))
        }

        return result
    }

    suspend fun cachedOptions(): PlanOptionsDto? = read(OPTIONS_KEY, PlanOptionsDto.serializer())

    suspend fun create(request: PlanRequest): ApiResult<PlanDraft> =
        when (val result = caller.envelope { api.createPlan(request) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val draft = PlanDraft(
                    proposal = result.value.body ?: PlanProposalDto(),
                    misc = decode(result.value.misc, PlanMiscDto.serializer()) ?: PlanMiscDto(),
                )

                saveDraft(draft)
                ApiResult.Success(draft)
            }
        }

    suspend fun confirm(request: PlanConfirmRequest): ApiResult<PlanConfirmation> =
        when (val result = caller.envelope { api.confirmPlan(request) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(
                PlanConfirmation(
                    templates = result.value.body ?: emptyList(),
                    misc = decode(result.value.misc, PlanConfirmMiscDto.serializer()) ?: PlanConfirmMiscDto(),
                )
            )
        }

    suspend fun saveDraft(draft: PlanDraft) =
        put(DRAFT_KEY, json.encodeToString(PlanDraft.serializer(), draft))

    suspend fun draft(): PlanDraft? = read(DRAFT_KEY, PlanDraft.serializer())

    suspend fun clearDraft() = cache.remove(DRAFT_KEY)

    private suspend fun put(key: String, payload: String) = cache.put(
        JsonCacheEntity(key = key, payload = payload, updatedAt = System.currentTimeMillis())
    )

    private suspend fun <T> read(
        key: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T? = cache.find(key)?.let { entity ->
        runCatching { json.decodeFromString(serializer, entity.payload) }.getOrNull()
    }

    private fun <T> decode(
        element: kotlinx.serialization.json.JsonElement?,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T? = element?.let { runCatching { json.decodeFromJsonElement(serializer, it) }.getOrNull() }

    private companion object {
        const val OPTIONS_KEY = "plan_options"
        const val DRAFT_KEY = "plan_proposal"
    }
}
