package com.archieapps.exercises.core.di

import android.content.Context
import com.archieapps.exercises.BuildConfig
import androidx.room.Room
import com.archieapps.exercises.core.anatomy.BodyGeometryLoader
import com.archieapps.exercises.core.data.AnatomyRepository
import com.archieapps.exercises.core.data.AuthRepository
import com.archieapps.exercises.core.data.ExerciseRepository
import com.archieapps.exercises.core.data.HomeRepository
import com.archieapps.exercises.core.data.PlanRepository
import com.archieapps.exercises.core.data.SessionRepository
import com.archieapps.exercises.core.data.TemplateRepository
import com.archieapps.exercises.core.data.WorkoutRepository
import com.archieapps.exercises.core.db.ExercisesDatabase
import com.archieapps.exercises.core.db.MIGRATION_2_3
import com.archieapps.exercises.core.db.MIGRATION_3_4
import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiJson
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.SessionInterceptor
import com.archieapps.exercises.core.net.SlowCallInterceptor
import com.archieapps.exercises.core.rest.RestNotifications
import com.archieapps.exercises.core.rest.RestTimerController
import com.archieapps.exercises.core.settings.SettingsStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context) {
    private companion object {
        val SLOW_PATHS = listOf("workout-plans")
        const val SLOW_READ_TIMEOUT_SECONDS = 90
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsStore(context, scope)

    val restTimer = RestTimerController(context)

    private val client = OkHttpClient.Builder()
        .addInterceptor(SessionInterceptor({ settings.current.token }))
        .addInterceptor(SlowCallInterceptor(SLOW_PATHS, SLOW_READ_TIMEOUT_SECONDS))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: ExercisesApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ExercisesApi::class.java)

    private val database = Room
        .databaseBuilder(context, ExercisesDatabase::class.java, "exercises.db")
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    private val caller = ApiCaller(ApiJson)

    val authRepository = AuthRepository(api, caller, settings) {
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }

    val homeRepository = HomeRepository(api, database.jsonCache(), caller, ApiJson)

    val anatomyRepository = AnatomyRepository(api, database.jsonCache(), caller, ApiJson)

    val bodyGeometry = BodyGeometryLoader(context, ApiJson)

    val exerciseRepository = ExerciseRepository(api, database.exercises(), caller, anatomyRepository)

    val workoutRepository = WorkoutRepository(api, database.jsonCache(), caller, ApiJson)

    val templateRepository = TemplateRepository(api, database.jsonCache(), caller, ApiJson)

    val planRepository = PlanRepository(api, database.jsonCache(), caller, ApiJson)

    val sessionRepository = SessionRepository(
        api,
        database.jsonCache(),
        database.pendingWorkouts(),
        caller,
        ApiJson,
    )

    init {
        RestNotifications(context).ensureChannels()

        scope.launch {
            settings.state
                .map { it.isLoggedIn }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    if (loggedIn) {
                        sessionRepository.flushPending()
                        exerciseRepository.sync()
                        anatomyRepository.refresh()
                    }
                }
        }
    }
}
