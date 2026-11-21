package com.david.barbellbase.domain

import com.david.barbellbase.data.db.ReminderRuleEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ceil

/**
 * Pure scheduling maths for reminders.
 *
 * A rule fires every [ReminderRuleEntity.intervalMinutes] inside a local-time window,
 * on the days selected by [ReminderRuleEntity.daysOfWeek] (bit 0 = Monday, 0 = every day).
 */
object ReminderSchedule {

    fun isDayAllowed(rule: ReminderRuleEntity, date: LocalDate): Boolean {
        if (rule.daysOfWeek == 0) return true
        val bit = 1 shl (date.dayOfWeek.value - 1)
        return rule.daysOfWeek and bit != 0
    }

    /**
     * First firing strictly after [afterMillis], or null when the rule can never fire again
     * (e.g. disabled or a zero-length window).
     */
    fun nextFire(
        rule: ReminderRuleEntity,
        afterMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (!rule.enabled) return null
        if (rule.intervalMinutes <= 0) return null

        val after = Instant.ofEpochMilli(afterMillis).atZone(zone)
        var date = after.toLocalDate()

        repeat(9) {
            val window = windowFor(rule, date, zone)
            if (window != null && isDayAllowed(rule, date)) {
                val (start, end) = window
                val anchorMillis = start.toInstant().toEpochMilli()
                val endMillis = end.toInstant().toEpochMilli()
                if (endMillis > anchorMillis) {
                    val intervalMillis = rule.intervalMinutes * 60_000L
                    val earliest = maxOf(afterMillis, anchorMillis)
                    val steps = if (earliest <= anchorMillis) 0L
                    else ceil((earliest - anchorMillis).toDouble() / intervalMillis).toLong()
                    var candidate = anchorMillis + steps * intervalMillis
                    if (candidate <= afterMillis) candidate += intervalMillis
                    if (candidate < endMillis) return candidate
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun windowFor(
        rule: ReminderRuleEntity,
        date: LocalDate,
        zone: ZoneId,
    ): Pair<ZonedDateTime, ZonedDateTime>? {
        val startMin = rule.windowStartMin.coerceIn(0, 24 * 60)
        val endMin = rule.windowEndMin.coerceIn(0, 24 * 60)
        if (startMin == endMin) return null // empty window

        val start = date.atStartOfDay(zone).plusMinutes(startMin.toLong())
        val endDate = if (endMin < startMin) date.plusDays(1) else date
        val end = endDate.atStartOfDay(zone).plusMinutes(endMin.toLong())
        return start to end
    }

    /** All firing times between [fromMillis] (inclusive) and [toMillis] (exclusive). */
    fun occurrences(
        rule: ReminderRuleEntity,
        fromMillis: Long,
        toMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Long> {
        val result = mutableListOf<Long>()
        var cursor = fromMillis - 1
        var guard = 0
        while (guard++ < 1000) {
            val next = nextFire(rule, cursor, zone) ?: break
            if (next >= toMillis) break
            result += next
            cursor = next
        }
        return result
    }

    fun dayMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }

    fun daysFromMask(mask: Int): Set<DayOfWeek> =
        if (mask == 0) DayOfWeek.entries.toSet()
        else DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

    fun minutesToLocalTime(minutesOfDay: Int): LocalTime =
        LocalTime.of((minutesOfDay / 60).coerceIn(0, 23), (minutesOfDay % 60).coerceIn(0, 59))
}
