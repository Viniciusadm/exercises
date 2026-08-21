package com.archieapps.exercises.core.net

import java.io.IOException
import com.archieapps.exercises.core.access.AccessCode
import kotlinx.serialization.json.Json
import retrofit2.HttpException

enum class FailureKind { OFFLINE, UNAUTHORIZED, SERVER, EMPTY, GATED }

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class Failure(val message: String, val kind: FailureKind) : ApiResult<Nothing>
}

class ApiCaller(private val json: Json) {
    suspend fun <T> call(block: suspend () -> Envelope<T>): ApiResult<T> =
        when (val result = envelope(block)) {
            is ApiResult.Success -> ApiResult.Success(result.value.body!!)
            is ApiResult.Failure -> result
        }

    suspend fun <T> envelope(block: suspend () -> Envelope<T>): ApiResult<Envelope<T>> = try {
        val envelope = block()

        when {
            !envelope.success -> ApiResult.Failure(envelope.errorText(), kindOf(envelope.errorText()))
            envelope.body == null -> ApiResult.Failure(envelope.errorText(), FailureKind.EMPTY)
            else -> ApiResult.Success(envelope)
        }
    } catch (error: HttpException) {
        val message = messageOf(error)

        ApiResult.Failure(
            message,
            when {
                error.code() == 401 || error.code() == 403 -> FailureKind.UNAUTHORIZED
                error.code() == 400 -> kindOf(message)
                else -> FailureKind.SERVER
            },
        )
    } catch (error: IOException) {
        ApiResult.Failure("Sem conexão com o servidor.", FailureKind.OFFLINE)
    }

    private fun kindOf(message: String): FailureKind {
        if (! message.contains(GATE_MESSAGE)) return FailureKind.SERVER

        AccessCode.request()

        return FailureKind.GATED
    }

    private fun messageOf(error: HttpException): String {
        val raw = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
        val parsed = raw?.let {
            runCatching { json.decodeFromString<Envelope<kotlinx.serialization.json.JsonElement>>(it) }.getOrNull()
        }

        return parsed?.errorText() ?: when (error.code()) {
            401, 403 -> "Sessão expirada. Entre novamente."
            404 -> "Recurso não encontrado."
            429 -> "Muitas requisições. Tente em instantes."
            else -> "Erro ${error.code()} no servidor."
        }
    }
}

private const val GATE_MESSAGE = "digo inv"

fun Envelope<*>.errorText(): String =
    error?.takeIf { it.isNotBlank() } ?: message?.takeIf { it.isNotBlank() } ?: "Não foi possível carregar os dados."
