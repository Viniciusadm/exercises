package com.archieapps.exercises.core.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Envelope<T>(
    val message: String? = null,
    val body: T? = null,
    val error: String? = null,
    val success: Boolean = false,
    val pagination: Pagination? = null,
    val misc: JsonElement? = null,
)

@Serializable
data class Pagination(
    val limit: Int = 0,
    val current: Int = 1,
    val last: Int = 1,
    val count: Int = 0,
    val total: Int = 0,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class Session(
    val user: SessionUser? = null,
    val token: String,
    val type: String = "Bearer",
)

@Serializable
data class SessionUser(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    @SerialName("image") val image: String? = null,
)
