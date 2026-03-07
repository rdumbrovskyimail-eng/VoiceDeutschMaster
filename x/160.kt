// src/test/java/com/voicedeutsch/master/util/DateUtilsTest.kt
package com.voicedeutsch.master.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DateUtilsTest {

    // ── nowTimestamp ──────────────────────────────────────────────────────

    @Test fun nowTimestamp_returnsCurrentTimeApproximately() {
        val before = System.currentTimeMillis()
        val result = DateUtils.nowTimestamp()
        val after = System.currentTimeMillis()
        assertTrue(result in before..after)
    }

    @Test fun nowTimestamp_isPositive() { assertTrue(DateUtils.nowTimestamp() > 0) }

    // ── todayDateString ───────────────────────────────────────────────────

    @Test fun todayDateString_matchesFormat_yyyyMMdd() {
        assertTrue(DateUtils.todayDateString().matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test fun todayDateString_containsCurrentYear() {
        assertTrue(DateUtils.todayDateString().startsWith(java.time.LocalDate.now().year.toString()))
    }

    // ── timestampToLocalDateTime ──────────────────────────────────────────

    @Test fun timestampToLocalDateTime_knownEpoch_returnsNonNull() {
        assertNotNull(DateUtils.timestampToLocalDateTime(0L))
    }

    @Test fun timestampToLocalDateTime_andBackRoundtrip_preservesMillisApproximately() {
        val ts = 1_741_000_000_000L
        val ldt = DateUtils.timestampToLocalDateTime(ts)
        val restored = DateUtils.localDateTimeToTimestamp(ldt)
        assertTrue(kotlin.math.abs(restored - ts) < 1_000)
    }

    // ── localDateTimeToTimestamp ──────────────────────────────────────────

    @Test fun localDateTimeToTimestamp_returnsPositiveValue() {
        assertTrue(DateUtils.localDateTimeToTimestamp(java.time.LocalDateTime.now()) > 0)
    }

    // ── daysBetween ───────────────────────────────────────────────────────

    @Test fun daysBetween_sameTimestamp_returnsZero() {
        val ts = System.currentTimeMillis()
        assertEquals(0L, DateUtils.daysBetween(ts, ts))
    }

    @Test fun daysBetween_exactlyOneDay_returnsOne() {
        val start = System.currentTimeMillis()
        assertEquals(1L, DateUtils.daysBetween(start, start + 24 * 60 * 60 * 1000L))
    }

    @Test fun daysBetween_sevenDays_returnsSeven() {
        val start = System.currentTimeMillis()
        assertEquals(7L, DateUtils.daysBetween(start, start + 7 * 24 * 60 * 60 * 1000L))
    }

    @Test fun daysBetween_lessThanOneDay_returnsZero() {
        val start = System.currentTimeMillis()
        assertEquals(0L, DateUtils.daysBetween(start, start + 12 * 60 * 60 * 1000L))
    }

    @Test fun daysBetween_endBeforeStart_returnsNegative() {
        val now = System.currentTimeMillis()
        assertTrue(DateUtils.daysBetween(now, now - 24 * 60 * 60 * 1000L) < 0)
    }

    // ── addDaysToTimestamp ────────────────────────────────────────────────

    @Test fun addDaysToTimestamp_oneDay_adds86400000ms() {
        val base = 1_000_000_000L
        assertEquals(base + 86_400_000L, DateUtils.addDaysToTimestamp(base, 1f))
    }

    @Test fun addDaysToTimestamp_halfDay_adds43200000ms() {
        val base = 1_000_000_000L
        assertEquals(base + 43_200_000L, DateUtils.addDaysToTimestamp(base, 0.5f))
    }

    @Test fun addDaysToTimestamp_zeroDays_returnsOriginal() {
        val base = 1_741_000_000_000L
        assertEquals(base, DateUtils.addDaysToTimestamp(base, 0f))
    }

    @Test fun addDaysToTimestamp_sevenDays_correctlyAddsWeek() {
        val base = 1_000_000_000L
        assertEquals(base + 7 * 86_400_000L, DateUtils.addDaysToTimestamp(base, 7f))
    }

    // ── isOverdue ─────────────────────────────────────────────────────────

    @Test fun isOverdue_pastTimestamp_returnsTrue() {
        assertTrue(DateUtils.isOverdue(System.currentTimeMillis() - 60_000L))
    }

    @Test fun isOverdue_futureTimestamp_returnsFalse() {
        assertFalse(DateUtils.isOverdue(System.currentTimeMillis() + 60_000L))
    }

    @Test fun isOverdue_currentTimestamp_returnsTrue() {
        assertTrue(DateUtils.isOverdue(System.currentTimeMillis()))
    }

    // ── overdueDays ───────────────────────────────────────────────────────

    @Test fun overdueDays_futureTimestamp_returnsZero() {
        assertEquals(0L, DateUtils.overdueDays(System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000L))
    }

    @Test fun overdueDays_exactlyOneDayAgo_returnsOne() {
        assertEquals(1L, DateUtils.overdueDays(System.currentTimeMillis() - 25 * 60 * 60 * 1000L))
    }

    @Test fun overdueDays_threeDaysAgo_returnsThree() {
        assertEquals(3L, DateUtils.overdueDays(System.currentTimeMillis() - 3 * 25 * 60 * 60 * 1000L))
    }

    // ── formatDuration ────────────────────────────────────────────────────

    @Test fun formatDuration_lessThan60Minutes_returnsMinutes() { assertEquals("15 мин", DateUtils.formatDuration(15)) }
    @Test fun formatDuration_exactly60Minutes_returnsOneHour() { assertEquals("1 ч", DateUtils.formatDuration(60)) }
    @Test fun formatDuration_exactlyTwoHours_returnsTwoHours() { assertEquals("2 ч", DateUtils.formatDuration(120)) }
    @Test fun formatDuration_90Minutes_returnsHourAndMinutes() { assertEquals("1 ч 30 мин", DateUtils.formatDuration(90)) }
    @Test fun formatDuration_zeroMinutes_returnsZeroMin() { assertEquals("0 мин", DateUtils.formatDuration(0)) }
    @Test fun formatDuration_61Minutes_returnsCorrect() { assertEquals("1 ч 1 мин", DateUtils.formatDuration(61)) }
    @Test fun formatDuration_59Minutes_returnsMinutes() { assertEquals("59 мин", DateUtils.formatDuration(59)) }
    @Test fun formatDuration_180Minutes_returnsThreeHours() { assertEquals("3 ч", DateUtils.formatDuration(180)) }

    // ── formatRelativeTime ────────────────────────────────────────────────

    @Test fun formatRelativeTime_today_returnsSodnya() {
        assertEquals("сегодня", DateUtils.formatRelativeTime(System.currentTimeMillis()))
    }

    @Test fun formatRelativeTime_yesterday_returnsVchera() {
        assertEquals("вчера", DateUtils.formatRelativeTime(System.currentTimeMillis() - 25 * 60 * 60 * 1000L))
    }

    @Test fun formatRelativeTime_3DaysAgo_returnsDaysFormat() {
        val result = DateUtils.formatRelativeTime(System.currentTimeMillis() - 3 * 25 * 60 * 60 * 1000L)
        assertTrue(result.contains("дн. назад"), "Expected 'дн. назад' in '$result'")
    }

    @Test fun formatRelativeTime_2WeeksAgo_returnsWeeksFormat() {
        val result = DateUtils.formatRelativeTime(System.currentTimeMillis() - 14 * 25 * 60 * 60 * 1000L)
        assertTrue(result.contains("нед. назад"), "Expected 'нед. назад' in '$result'")
    }

    @Test fun formatRelativeTime_2MonthsAgo_returnsMonthsFormat() {
        val result = DateUtils.formatRelativeTime(System.currentTimeMillis() - 62 * 24 * 60 * 60 * 1000L)
        assertTrue(result.contains("мес. назад"), "Expected 'мес. назад' in '$result'")
    }

    // ── formatTime ────────────────────────────────────────────────────────

    @Test fun formatTime_midnight_returns0000() { assertEquals("00:00", DateUtils.formatTime(0, 0)) }
    @Test fun formatTime_noon_returns1200() { assertEquals("12:00", DateUtils.formatTime(12, 0)) }
    @Test fun formatTime_19_30_returns1930() { assertEquals("19:30", DateUtils.formatTime(19, 30)) }
    @Test fun formatTime_singleDigitHourAndMinute_hasPadding() { assertEquals("09:05", DateUtils.formatTime(9, 5)) }
    @Test fun formatTime_endOfDay_returns2359() { assertEquals("23:59", DateUtils.formatTime(23, 59)) }

    // ── isSameDay ─────────────────────────────────────────────────────────

    @Test fun isSameDay_sameTimestamp_returnsTrue() {
        val ts = System.currentTimeMillis()
        assertTrue(DateUtils.isSameDay(ts, ts))
    }

    @Test fun isSameDay_sameDay1SecondDiff_returnsTrue() {
        val ts = System.currentTimeMillis()
        assertTrue(DateUtils.isSameDay(ts, ts + 1000L))
    }

    @Test fun isSameDay_differentDays_returnsFalse() {
        val today = System.currentTimeMillis()
        assertFalse(DateUtils.isSameDay(today, today + 25 * 60 * 60 * 1000L))
    }

    @Test fun isSameDay_exactlyMidnightBoundary_returnsFalse() {
        val start = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertFalse(DateUtils.isSameDay(start - 1, start))
    }
}
