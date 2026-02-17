package com.voicedeutsch.master.data.local.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun fromLongNullable(value: Long?): Long? = value

    @TypeConverter
    fun toLongNullable(value: Long?): Long? = value

    @TypeConverter
    fun fromFloatNullable(value: Float?): Float? = value

    @TypeConverter
    fun toFloatNullable(value: Float?): Float? = value
}