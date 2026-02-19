package com.voicedeutsch.master.data.local

import kotlinx.serialization.json.Json

/**
 * Single source of truth for the [Json] serialization configuration.
 *
 * Used by:
 *   - [Converters] (Room @TypeConverter — cannot use Koin DI)
 *   - Koin `appModule` (provides the same instance via `single { JsonFactory.instance }`)
 *
 * All serialization/deserialization in the app MUST use [instance] to avoid
 * config divergence (e.g. one place enables `coerceInputValues` and another
 * doesn't, causing different parsing behavior for the same JSON).
 */
object JsonFactory {

    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
        prettyPrint = false
    }
}
