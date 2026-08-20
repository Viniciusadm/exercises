package com.archieapps.exercises.core.net

import kotlinx.serialization.json.Json

val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}
