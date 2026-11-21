package com.david.barbellbase.domain

import com.david.barbellbase.data.prefs.UnitSystem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object Units {
    const val LB_PER_KG = 2.2046226218487757

    fun fromKg(kg: Double, units: UnitSystem): Double =
        if (units == UnitSystem.LB) kg * LB_PER_KG else kg

    fun toKg(value: Double, units: UnitSystem): Double =
        if (units == UnitSystem.LB) value / LB_PER_KG else value

    fun label(units: UnitSystem): String = if (units == UnitSystem.LB) "lb" else "kg"

    fun stepFor(units: UnitSystem, kgStep: Double): Double =
        if (units == UnitSystem.LB) kgStep * LB_PER_KG else kgStep

    fun roundToStep(value: Double, step: Double): Double {
        if (step <= 0.0) return value
        return (value / step).roundToInt() * step
    }

    /** Formats a canonical kg value for display in the user's unit system, trimming trailing zeros. */
    fun format(kg: Double?, units: UnitSystem, withUnit: Boolean = false): String {
        if (kg == null) return "—"
        val value = fromKg(kg, units)
        val rounded = (value * 2.0).roundToInt() / 2.0
        val text = if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
        return if (withUnit) "$text ${label(units)}" else text
    }
}

object Metrics {
    /** Epley estimate, guarding the single-rep case. */
    fun e1rm(weightKg: Double?, reps: Int?): Double? {
        if (weightKg == null || reps == null || reps <= 0 || weightKg <= 0) return null
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    fun volume(weightKg: Double?, reps: Int?): Double {
        if (weightKg == null || reps == null || reps <= 0) return 0.0
        return weightKg * reps
    }
}

object Clock {
    fun zone(): ZoneId = ZoneId.systemDefault()

    fun now(): Long = System.currentTimeMillis()

    fun startOfDay(epochMillis: Long, zone: ZoneId = zone()): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDay(epochMillis: Long, zone: ZoneId = zone()): Long =
        startOfDay(epochMillis, zone) + 24 * 60 * 60 * 1000L

    fun epochDay(epochMillis: Long, zone: ZoneId = zone()): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toEpochDay()

    fun dateOf(epochMillis: Long, zone: ZoneId = zone()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun startOfWeek(epochMillis: Long, mondayFirst: Boolean, zone: ZoneId = zone()): Long {
        val date = dateOf(epochMillis, zone)
        val target = if (mondayFirst) java.time.DayOfWeek.MONDAY else java.time.DayOfWeek.SUNDAY
        var d = date
        while (d.dayOfWeek != target) d = d.minusDays(1)
        return d.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

object TimeFormat {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
    private val fullFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

    fun clock(epochMillis: Long, zone: ZoneId = Clock.zone()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFormatter)

    fun day(epochMillis: Long, zone: ZoneId = Clock.zone()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dayFormatter)

    fun full(epochMillis: Long, zone: ZoneId = Clock.zone()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(fullFormatter)

    fun duration(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0)
        val m = safe / 60
        val s = safe % 60
        return if (m >= 60) {
            val h = m / 60
            String.format(Locale.US, "%d:%02d:%02d", h, m % 60, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun minutesLabel(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}
