package com.archieapps.exercises.core.net

import com.archieapps.exercises.core.net.dto.CalendarDayDto
import com.archieapps.exercises.core.net.dto.CatalogMetaDto
import com.archieapps.exercises.core.net.dto.ExerciseDto
import com.archieapps.exercises.core.net.dto.ExerciseHistoryDto
import com.archieapps.exercises.core.net.dto.ExerciseRequest
import com.archieapps.exercises.core.net.dto.LatestExerciseDto
import com.archieapps.exercises.core.net.dto.NameRequest
import com.archieapps.exercises.core.net.dto.StatsDto
import com.archieapps.exercises.core.net.dto.TemplateDto
import com.archieapps.exercises.core.net.dto.TemplateRequest
import com.archieapps.exercises.core.net.dto.WorkoutDto
import com.archieapps.exercises.core.net.dto.WorkoutRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ExercisesApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Envelope<Session>

    @GET("api/workouts/stats")
    suspend fun stats(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Envelope<StatsDto>

    @GET("api/workouts/calendar")
    suspend fun calendar(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Envelope<List<CalendarDayDto>>

    @GET("api/workouts")
    suspend fun workouts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("exercise_id") exerciseId: Int? = null,
        @Query("include_exercises") includeExercises: Int? = null,
    ): Envelope<List<WorkoutDto>>

    @GET("api/workouts/{id}")
    suspend fun workout(@Path("id") id: Int): Envelope<WorkoutDto>

    @POST("api/workouts")
    suspend fun createWorkout(@Body request: WorkoutRequest): Envelope<WorkoutDto>

    @PUT("api/workouts/{id}")
    suspend fun updateWorkout(@Path("id") id: Int, @Body request: WorkoutRequest): Envelope<WorkoutDto>

    @DELETE("api/workouts/{id}")
    suspend fun deleteWorkout(@Path("id") id: Int): Envelope<Map<String, Int>>

    @GET("api/exercises")
    suspend fun exercises(
        @Query("per_page") perPage: Int = 500,
        @Query("sort") sort: String = "name",
    ): Envelope<List<ExerciseDto>>

    @GET("api/exercises/meta")
    suspend fun catalogMeta(): Envelope<CatalogMetaDto>

    @GET("api/exercises/{id}/history")
    suspend fun exerciseHistory(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 60,
    ): Envelope<ExerciseHistoryDto>

    @POST("api/exercises")
    suspend fun createExercise(@Body request: ExerciseRequest): Envelope<ExerciseDto>

    @PUT("api/exercises/{id}")
    suspend fun updateExercise(@Path("id") id: Int, @Body request: ExerciseRequest): Envelope<ExerciseDto>

    @GET("api/workout-exercises/latest")
    suspend fun latestExercise(
        @Query("exercise_id") exerciseId: Int? = null,
        @Query("name") name: String? = null,
        @Query("sessions") sessions: Int = 3,
    ): Envelope<LatestExerciseDto>

    @GET("api/workout-templates")
    suspend fun templates(): Envelope<List<TemplateDto>>

    @POST("api/workout-templates")
    suspend fun createTemplate(@Body request: TemplateRequest): Envelope<TemplateDto>

    @PUT("api/workout-templates/{id}")
    suspend fun updateTemplate(@Path("id") id: Int, @Body request: TemplateRequest): Envelope<TemplateDto>

    @DELETE("api/workout-templates/{id}")
    suspend fun deleteTemplate(@Path("id") id: Int): Envelope<Map<String, Int>>

    @POST("api/workout-templates/from-workout/{id}")
    suspend fun templateFromWorkout(@Path("id") id: Int, @Body request: NameRequest): Envelope<TemplateDto>
}
