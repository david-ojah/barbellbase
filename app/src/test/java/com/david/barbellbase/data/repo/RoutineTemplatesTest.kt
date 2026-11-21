package com.david.barbellbase.data.repo

import com.david.barbellbase.data.db.seedExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTemplatesTest {

    private val seedNames = seedExercises().map { it.name }

    @Test
    fun `template ids are unique`() {
        val ids = RoutineTemplates.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `every template exercise exists in the seed catalog`() {
        RoutineTemplates.forEach { template ->
            val missing = RoutineTemplateValidator.missingExerciseNames(template, seedNames)
            assertTrue(
                "${template.name} references unknown exercises: ${missing.joinToString()}",
                missing.isEmpty(),
            )
        }
    }

    @Test
    fun `templates have days and exercises`() {
        RoutineTemplates.forEach { template ->
            assertTrue("${template.name} has no days", template.days.isNotEmpty())
            template.days.forEach { day ->
                assertTrue("${template.name} / ${day.name} has no exercises", day.exercises.isNotEmpty())
            }
        }
    }

    @Test
    fun `template targets are sane`() {
        RoutineTemplates.forEach { template ->
            template.days.forEach { day ->
                day.exercises.forEach { exercise ->
                    assertTrue(exercise.targetSets in 1..20)
                    assertTrue(exercise.repsMin in 1..100)
                    assertTrue(exercise.repsMax in 1..100)
                    assertTrue(exercise.repsMin <= exercise.repsMax)
                    assertTrue(exercise.restSec in 0..900)
                }
            }
        }
    }

    @Test
    fun `validator reports unknown exercise names case insensitively`() {
        val template = RoutineTemplate(
            id = "test",
            name = "Test",
            level = "Beginner",
            description = "",
            days = listOf(
                TemplateDay(
                    name = "Day",
                    exercises = listOf(
                        TemplateExercise("barbell bench press"),
                        TemplateExercise("Not A Real Exercise"),
                    ),
                ),
            ),
        )

        assertEquals(listOf("Not A Real Exercise"), RoutineTemplateValidator.missingExerciseNames(template, seedNames))
    }
}
