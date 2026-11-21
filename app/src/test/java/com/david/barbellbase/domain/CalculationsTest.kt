package com.david.barbellbase.domain

import com.david.barbellbase.data.prefs.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.abs

class CalculationsTest {

    private fun close(expected: Double, actual: Double, tolerance: Double = 0.0001) {
        assertEquals(expected, actual, tolerance)
    }

    @Test
    fun `kilogram values pass through unchanged`() {
        close(100.0, Units.fromKg(100.0, UnitSystem.KG))
        close(100.0, Units.toKg(100.0, UnitSystem.KG))
        assertEquals("kg", Units.label(UnitSystem.KG))
    }

    @Test
    fun `pounds round trip within floating point tolerance`() {
        val lb = Units.fromKg(100.0, UnitSystem.LB)
        close(220.462, lb, 0.01)
        close(100.0, Units.toKg(lb, UnitSystem.LB))
        assertEquals("lb", Units.label(UnitSystem.LB))
    }

    @Test
    fun `formatting trims trailing zeros and rounds to halves`() {
        assertEquals("100", Units.format(100.0, UnitSystem.KG))
        assertEquals("102.5", Units.format(102.5, UnitSystem.KG))
        assertEquals("102.5", Units.format(102.55, UnitSystem.KG))
        assertEquals("225", Units.format(102.0583, UnitSystem.LB))
        assertEquals("—", Units.format(null, UnitSystem.KG))
        assertEquals("60 kg", Units.format(60.0, UnitSystem.KG, withUnit = true))
    }

    @Test
    fun `step rounds to the nearest increment`() {
        close(100.0, Units.roundToStep(101.2, 2.5))
        close(102.5, Units.roundToStep(103.0, 2.5))
        close(103.0, Units.roundToStep(103.0, 0.0))
    }

    @Test
    fun `epley one rep max uses the single rep directly`() {
        close(100.0, Metrics.e1rm(100.0, 1)!!)
        close(120.0, Metrics.e1rm(100.0, 6)!!)
        close(133.3333, Metrics.e1rm(100.0, 10)!!, 0.01)
    }

    @Test
    fun `epley ignores invalid input`() {
        assertNull(Metrics.e1rm(null, 5))
        assertNull(Metrics.e1rm(100.0, null))
        assertNull(Metrics.e1rm(100.0, 0))
        assertNull(Metrics.e1rm(0.0, 5))
    }

    @Test
    fun `volume multiplies weight by reps`() {
        close(800.0, Metrics.volume(80.0, 10))
        close(0.0, Metrics.volume(null, 10))
        close(0.0, Metrics.volume(80.0, 0))
    }

    @Test
    fun `duration formatting handles minutes and hours`() {
        assertEquals("0:45", TimeFormat.duration(45))
        assertEquals("2:05", TimeFormat.duration(125))
        assertEquals("1:02:05", TimeFormat.duration(3725))
        assertEquals("0:00", TimeFormat.duration(-10))
    }
}
