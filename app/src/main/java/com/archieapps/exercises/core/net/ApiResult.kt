package com.archieapps.exercises.core.net

import java.io.IOException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

enum class FailureKind { OFFLINE, UNAUTHORIZED, SERVER, EMPTY }

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class Failure(val message: String, val kind: FailureKind) : ApiResult<Nothing>
}

class ApiCaller(private val json: Json) {
    suspend fun <T> call(block: suspend () -> Envelope<T>): ApiResult<T> = try {
        val envelope = block()
        val body = envelope.body

        when {
            !envelope.success -> ApiResult.Failure(envelope.errorText(), FailureKind.SERVER)
            body == null -> ApiResult.Failure(envelope.errorText(), FailureKind.EMPTY)
            else -> ApiResult.Success(body)
        }
    } catch (error: HttpException) {
        ApiResult.Failure(
            messageOf(error),
            if (error.code() == 401 || error.code() == 403) FailureKind.UNAUTHORIZED else FailureKind.SERVER,
        )
    } catch (error: IOException) {
        ApiResult.Failure("Sem conexão com o servidor.", FailureKind.OFFLINE)
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

fun Envelope<*>.errorText(): String =
    error?.takeIf { it.isNotBlank() } ?: message?.takeIf { it.isNotBlank() } ?: "Não foi possível carregar os dados."
