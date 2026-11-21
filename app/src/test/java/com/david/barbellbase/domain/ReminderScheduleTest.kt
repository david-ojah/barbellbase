package com.david.barbellbase.domain

import com.david.barbellbase.data.db.AlertSound
import com.david.barbellbase.data.db.ReminderKind
import com.david.barbellbase.data.db.ReminderRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderScheduleTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun rule(
        interval: Int = 60,
        start: Int = 8 * 60,
        end: Int = 22 * 60,
        days: Int = 0,
        enabled: Boolean = true,
    ) = ReminderRuleEntity(
        kind = ReminderKind.WATER,
        label = "Water",
        enabled = enabled,
        intervalMinutes = interval,
        windowStartMin = start,
        windowEndMin = end,
        daysOfWeek = days,
        sound = AlertSound.ALARM,
    )

    private fun millis(date: LocalDateTime): Long =
        date.atZone(zone).toInstant().toEpochMilli()

    private fun at(millis: Long): LocalDateTime =
        java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    @Test
    fun `fires at the next interval inside the window`() {
        val next = ReminderSchedule.nextFire(rule(), millis(LocalDateTime.of(2026, 3, 2, 8, 7)), zone)
        assertEquals(LocalDateTime.of(2026, 3, 2, 9, 0), at(next!!))
    }

    @Test
    fun `fires immediately at window start when asked earlier`() {
        val next = ReminderSchedule.nextFire(rule(), millis(LocalDateTime.of(2026, 3, 2, 6, 0)), zone)
        assertEquals(LocalDateTime.of(2026, 3, 2, 8, 0), at(next!!))
    }

    @Test
    fun `rolls to the next morning after the window closes`() {
        val next = ReminderSchedule.nextFire(rule(), millis(LocalDateTime.of(2026, 3, 2, 22, 30)), zone)
        assertEquals(LocalDateTime.of(2026, 3, 3, 8, 0), at(next!!))
    }

    @Test
    fun `respects a single-day mask`() {
        // 2026-03-02 is a Monday.
        val mondayOnly = rule(days = ReminderSchedule.dayMask(setOf(DayOfWeek.MONDAY)))
        val next = ReminderSchedule.nextFire(mondayOnly, millis(LocalDateTime.of(2026, 3, 3, 12, 0)), zone)
        assertEquals(LocalDateTime.of(2026, 3, 9, 8, 0), at(next!!))
        assertTrue(ReminderSchedule.isDayAllowed(mondayOnly, LocalDate.of(2026, 3, 2)))
        assertFalse(ReminderSchedule.isDayAllowed(mondayOnly, LocalDate.of(2026, 3, 3)))
    }

    @Test
    fun `disabled rules never fire`() {
        assertNull(ReminderSchedule.nextFire(rule(enabled = false), millis(LocalDateTime.of(2026, 3, 2, 8, 0)), zone))
    }

    @Test
    fun `an empty window never fires`() {
        assertNull(ReminderSchedule.nextFire(rule(start = 600, end = 600), millis(LocalDateTime.of(2026, 3, 2, 8, 0)), zone))
    }

    @Test
    fun `overnight window spans midnight`() {
        val overnight = rule(interval = 120, start = 22 * 60, end = 2 * 60)
        val next = ReminderSchedule.nextFire(overnight, millis(LocalDateTime.of(2026, 3, 2, 23, 10)), zone)
        assertEquals(LocalDateTime.of(2026, 3, 3, 0, 0), at(next!!))
    }

    @Test
    fun `occurrences fill a full day`() {
        val from = millis(LocalDateTime.of(2026, 3, 2, 0, 0))
        val to = millis(LocalDateTime.of(2026, 3, 3, 0, 0))
        val slots = ReminderSchedule.occurrences(rule(interval = 60), from, to, zone)
        // 08:00 through 21:00 inclusive.
        assertEquals(14, slots.size)
        assertEquals(LocalDateTime.of(2026, 3, 2, 8, 0), at(slots.first()))
        assertEquals(LocalDateTime.of(2026, 3, 2, 21, 0), at(slots.last()))
    }

    @Test
    fun `day mask round trips`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
        val mask = ReminderSchedule.dayMask(days)
        assertEquals(days, ReminderSchedule.daysFromMask(mask))
        assertEquals(DayOfWeek.entries.toSet(), ReminderSchedule.daysFromMask(0))
    }
}
