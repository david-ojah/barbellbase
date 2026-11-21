package com.david.barbellbase.data.repo

import com.david.barbellbase.data.db.Equipment
import com.david.barbellbase.data.db.ExerciseDao
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.Measurement
import com.david.barbellbase.data.db.MuscleGroup
import com.david.barbellbase.data.db.seedExercises
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(private val dao: ExerciseDao) {

    fun observeAll(): Flow<List<ExerciseEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<ExerciseEntity?> = dao.observeById(id)

    suspend fun get(id: String): ExerciseEntity? = dao.getById(id)

    suspend fun ensureSeeded() {
        if (dao.count() == 0) dao.insertAll(seedExercises())
    }

    suspend fun save(exercise: ExerciseEntity) = dao.upsert(exercise)

    suspend fun archive(exercise: ExerciseEntity) = dao.upsert(exercise.copy(isArchived = true))

    suspend fun createCustom(
        name: String,
        primary: List<MuscleGroup>,
        secondary: List<MuscleGroup>,
        equipment: Equipment,
        measurement: Measurement,
        defaultRestSec: Int,
        instructions: String = "",
    ): ExerciseEntity {
        val exercise = ExerciseEntity(
            name = name.trim(),
            primaryMuscles = primary,
            secondaryMuscles = secondary,
            equipment = equipment,
            measurement = measurement,
            defaultRestSec = defaultRestSec,
            instructions = instructions,
            isCustom = true,
        )
        dao.upsert(exercise)
        return exercise
    }
}
