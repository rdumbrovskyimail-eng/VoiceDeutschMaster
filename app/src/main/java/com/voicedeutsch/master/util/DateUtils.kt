package com.voicedeutsch.master.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Date/time utilities — timestamp conversion, formatting, interval calculations.
 *
 * Used in SRS calculations, Room converters, UI date display,
 * session duration tracking, and streak computation.
 */
object DateUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Returns the current system time as epoch milliseconds.
     */
    fun nowTimestamp(): Long = System.currentTimeMillis()

    /**
     * Returns today's date as a formatted string (yyyy-MM-dd).
     */
    fun todayDateString(): String = LocalDate.now().format(dateFormatter)

    /**
     * Converts an epoch millisecond timestamp to [LocalDateTime] using the system default timezone.
     */
    fun timestampToLocalDateTime(timestamp: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())

    /**
     * Converts a [LocalDateTime] to epoch milliseconds using the system default timezone.
     */
    fun localDateTimeToTimestamp(dateTime: LocalDateTime): Long =
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * Calculates the number of whole days between two timestamps.
     */
    fun daysBetween(startTimestamp: Long, endTimestamp: Long): Long {
        val start = Instant.ofEpochMilli(startTimestamp)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val end = Instant.ofEpochMilli(endTimestamp)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(start, end)
    }

    /**
     * Adds a fractional number of days to a timestamp.
     * Used by the SRS algorithm to compute the next review date.
     *
     * @param timestamp base epoch millis
     * @param days fractional days to add (e.g., 0.5 for 12 hours)
     * @return new epoch millis
     */
    fun addDaysToTimestamp(timestamp: Long, days: Float): Long {
        val millis = (days * 24 * 60 * 60 * 1000).toLong()
        return timestamp + millis
    }

    /**
     * Returns `true` when the given review timestamp is in the past (i.e., the item is due).
     */
    fun isOverdue(nextReviewTimestamp: Long): Boolean =
        nextReviewTimestamp <= nowTimestamp()

    /**
     * Returns the number of days an item is overdue.
     * Returns 0 when the item is not yet due.
     */
    fun overdueDays(nextReviewTimestamp: Long): Long {
        if (!isOverdue(nextReviewTimestamp)) return 0
        return daysBetween(nextReviewTimestamp, nowTimestamp())
    }

    /**
     * Formats a duration given in minutes into a human-readable Russian string.
     *
     * Examples: "15 мин", "2 ч", "1 ч 30 мин"
     */
    fun formatDuration(minutes: Int): String = when {
        minutes < 60 -> "$minutes мин"
        minutes % 60 == 0 -> "${minutes / 60} ч"
        else -> "${minutes / 60} ч ${minutes % 60} мин"
    }

    /**
     * Formats a timestamp as a human-readable relative time string in Russian.
     *
     * Examples: "сегодня", "вчера", "3 дн. назад", "2 нед. назад", "1 мес. назад"
     */
    fun formatRelativeTime(timestamp: Long): String {
        val days = daysBetween(timestamp, nowTimestamp())
        return when {
            days == 0L -> "сегодня"
            days == 1L -> "вчера"
            days < 7 -> "$days дн. назад"
            days < 30 -> "${days / 7} нед. назад"
            else -> "${days / 30} мес. назад"
        }
    }

    /**
     * Formats hour and minute into "HH:mm" string.
     */
    fun formatTime(hour: Int, minute: Int): String =
        LocalTime.of(hour, minute).format(timeFormatter)

    /**
     * Returns `true` when two timestamps fall on the same calendar day in the system timezone.
     */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val date1 = timestampToLocalDateTime(timestamp1).toLocalDate()
        val date2 = timestampToLocalDateTime(timestamp2).toLocalDate()
        return date1 == date2
    }
}