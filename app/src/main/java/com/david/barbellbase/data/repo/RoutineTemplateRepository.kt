package com.david.barbellbase.data.repo

import androidx.room.withTransaction
import com.david.barbellbase.data.db.BarbellBaseDatabase
import com.david.barbellbase.data.db.ExerciseDao
import com.david.barbellbase.data.db.RoutineDao
import com.david.barbellbase.data.db.RoutineDayEntity
import com.david.barbellbase.data.db.RoutineEntity
import com.david.barbellbase.data.db.RoutineExerciseEntity
import javax.inject.Inject
import javax.inject.Singleton

object RoutineTemplateValidator {

    /** Returns template exercise names that are not present in [availableNames]. */
    fun missingExerciseNames(
        template: RoutineTemplate,
        availableNames: Collection<String>,
    ): List<String> {
        val available = availableNames.map { it.trim().lowercase() }.toSet()
        return template.days
            .flatMap { it.exercises }
            .map { it.exerciseName }
            .filter { it.trim().lowercase() !in available }
            .distinct()
    }
}

/**
 * Imports a [RoutineTemplate] into the user's routines as an ordinary, fully
 * editable routine. Exercise references are resolved by name against the local
 * exercise library so custom and archived exercises do not break imports.
 */
@Singleton
class RoutineTemplateRepository @Inject constructor(
    private val db: BarbellBaseDatabase,
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao,
) {

    val templates: List<RoutineTemplate> = RoutineTemplates

    suspend fun import(templateId: String): Result<String> = runCatching {
        val template = templates.firstOrNull { it.id == templateId }
            ?: error("Unknown routine template")

        val exercisesByName = exerciseDao.getAll().associateBy { it.name.trim().lowercase() }
        val missing = RoutineTemplateValidator.missingExerciseNames(template, exercisesByName.keys)
        require(missing.isEmpty()) {
            "Missing exercises: ${missing.joinToString()}"
        }

        db.withTransaction {
            val routine = RoutineEntity(name = template.name, notes = template.description)
            routineDao.upsertRoutine(routine)

            template.days.forEachIndexed { dayIndex, templateDay ->
                val day = RoutineDayEntity(
                    routineId = routine.id,
                    name = templateDay.name,
                    position = dayIndex,
                )
                routineDao.upsertDay(day)

                templateDay.exercises.forEachIndexed { exerciseIndex, item ->
                    val exercise = exercisesByName.getValue(item.exerciseName.trim().lowercase())
                    routineDao.upsertDayExercise(
                        RoutineExerciseEntity(
                            dayId = day.id,
                            exerciseId = exercise.id,
                            position = exerciseIndex,
                            targetSets = item.targetSets,
                            repsMin = item.repsMin,
                            repsMax = item.repsMax,
                            restSec = item.restSec,
                        ),
                    )
                }
            }

            routine.id
        }
    }
}
