package com.voicedeutsch.master.data.local.database.converter

import androidx.room.TypeConverter
import com.voicedeutsch.master.data.local.JsonFactory
import kotlinx.serialization.encodeToString

/**
 * Room type converters for complex types stored as JSON strings.
 *
 * M3 FIX: Uses [JsonFactory.instance] instead of creating a local [Json] object.
 * This ensures the same serialization config (ignoreUnknownKeys, isLenient,
 * coerceInputValues, encodeDefaults) is used here and in the Koin-provided
 * Json singleton. Previously a divergence in config (e.g. missing
 * coerceInputValues) could cause data deserialized by Room to behave
 * differently from data deserialized in domain mappers.
 */
class Converters {

    private val json get() = JsonFactory.instance

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }
}
