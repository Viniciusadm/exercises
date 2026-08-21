package com.archieapps.exercises

import com.archieapps.exercises.core.net.ApiCaller
import com.archieapps.exercises.core.net.ApiJson
import com.archieapps.exercises.core.net.ApiResult
import com.archieapps.exercises.core.net.ExercisesApi
import com.archieapps.exercises.core.net.FailureKind
import com.archieapps.exercises.core.net.LoginRequest
import com.archieapps.exercises.core.net.SessionInterceptor
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiCallerTest {
    private lateinit var server: MockWebServer
    private var token: String? = "abc123"

    private val caller = ApiCaller(ApiJson)

    private val api: ExercisesApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(SessionInterceptor { token })
            .build()

        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ExercisesApi::class.java)
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful envelope is unwrapped and the auth header is sent`() = runTest {
        server.enqueue(json(200, """{"message":"ok","body":[],"success":true}"""))

        val result = caller.call { api.calendar("2026-08-01", "2026-08-19") }

        assertTrue(result is ApiResult.Success)
        val request = server.takeRequest()
        assertEquals("Bearer abc123", request.getHeader("Authorization"))
        assertEquals("application/json", request.getHeader("Accept"))
        assertEquals("/api/workouts/calendar?from=2026-08-01&to=2026-08-19", request.path)
    }


    @Test
    fun `envelope with success false becomes a server failure carrying the message`() = runTest {
        server.enqueue(json(400, """{"message":"Email ou senha inválidos.","error":null,"success":false}"""))

        val result = caller.call { api.login(LoginRequest("a@b.c", "nope")) }

        assertTrue(result is ApiResult.Failure)
        result as ApiResult.Failure
        assertEquals(FailureKind.SERVER, result.kind)
        assertEquals("Email ou senha inválidos.", result.message)
    }

    @Test
    fun `unauthorized responses are reported as expired session`() = runTest {
        server.enqueue(json(401, """{"message":"Unauthenticated."}"""))

        val result = caller.call { api.stats() }

        result as ApiResult.Failure
        assertEquals(FailureKind.UNAUTHORIZED, result.kind)
        assertEquals("Unauthenticated.", result.message)
    }

    @Test
    fun `network errors are reported as offline`() = runTest {
        server.shutdown()

        val result = caller.call { api.stats() }

        result as ApiResult.Failure
        assertEquals(FailureKind.OFFLINE, result.kind)
        assertEquals("Sem conexão com o servidor.", result.message)
    }

    @Test
    fun `an envelope without body is not treated as success`() = runTest {
        server.enqueue(json(200, """{"message":"Dados retornados com sucesso.","body":null,"success":true}"""))

        val result = caller.call { api.stats() }

        result as ApiResult.Failure
        assertEquals(FailureKind.EMPTY, result.kind)
    }

    private fun json(code: Int, body: String) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
