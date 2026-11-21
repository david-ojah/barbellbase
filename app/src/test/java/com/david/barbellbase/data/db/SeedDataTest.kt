package com.david.barbellbase.data.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {

    @Test
    fun `untouched built-in exercises are redundant`() {
        val seeds = seedExercises()
        assertTrue(seeds.isNotEmpty())
        seeds.forEach { assertTrue(isUnmodifiedBuiltInExercise(it)) }
    }

    @Test
    fun `archived built-in exercise still syncs`() {
        val seed = seedExercises().first()
        assertFalse(isUnmodifiedBuiltInExercise(seed.copy(isArchived = true)))
    }

    @Test
    fun `edited built-in exercise still syncs`() {
        val seed = seedExercises().first()
        assertFalse(isUnmodifiedBuiltInExercise(seed.copy(defaultRestSec = seed.defaultRestSec + 5)))
        assertFalse(isUnmodifiedBuiltInExercise(seed.copy(isCustom = true)))
    }

    @Test
    fun `custom exercise is never treated as redundant`() {
        val seed = seedExercises().first()
        val custom = ExerciseEntity(name = "My lift", isCustom = true)
        assertFalse(isUnmodifiedBuiltInExercise(custom))
        assertFalse(isUnmodifiedBuiltInExercise(seed.copy(id = "custom-id", isCustom = true)))
    }
}
