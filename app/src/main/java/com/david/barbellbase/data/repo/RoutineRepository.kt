package com.david.barbellbase.data.repo

import androidx.room.withTransaction
import com.david.barbellbase.data.db.BarbellBaseDatabase
import com.david.barbellbase.data.db.RoutineDao
import com.david.barbellbase.data.db.RoutineDayEntity
import com.david.barbellbase.data.db.RoutineEntity
import com.david.barbellbase.data.db.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class RoutineSummary(
    val routine: RoutineEntity,
    val days: List<RoutineDayEntity>,
    val setTargets: Map<String, Int>,
)

data class DayDetail(
    val day: RoutineDayEntity,
    val routine: RoutineEntity?,
    val items: List<RoutineExerciseEntity>,
)

@Singleton
class RoutineRepository @Inject constructor(
    private val db: BarbellBaseDatabase,
    private val dao: RoutineDao,
) {

    fun observeSummaries(): Flow<List<RoutineSummary>> = combine(        dao.observeRoutines(),
        dao.observeDaysForAll(),
        dao.observeAllDayExercises(),
    ) { routines, days, exercises ->
        routines.map { routine ->
            RoutineSummary(
                routine = routine,
                days = days.filter { it.routineId == routine.id }.sortedBy { it.position },
                setTargets = exercises.groupBy { it.dayId }.mapValues { (_, list) -> list.size },
            )
        }
    }

    fun observeDayDetail(dayId: String): Flow<DayDetail?> = combine(
        dao.observeDayExercises(dayId),
        dao.observeDay(dayId),
    ) { items, day ->
        if (day == null) null else DayDetail(day, dao.getRoutine(day.routineId), items)
    }

    fun observeDays(routineId: String): Flow<List<RoutineDayEntity>> = dao.observeDays(routineId)

    fun observeRoutine(routineId: String): Flow<RoutineEntity?> = dao.observeRoutine(routineId)

    suspend fun getDay(dayId: String) = dao.getDay(dayId)

    suspend fun createRoutine(name: String): String {
        return db.withTransaction {
            val routine = RoutineEntity(name = name.ifBlank { "Routine" })
            dao.upsertRoutine(routine)
            dao.upsertDay(RoutineDayEntity(routineId = routine.id, name = "Day 1", position = 0))
            routine.id
        }
    }

    suspend fun renameRoutine(id: String, name: String) {
        val routine = dao.getRoutine(id) ?: return
        dao.upsertRoutine(routine.copy(name = name))
    }

    suspend fun deleteRoutine(id: String) = dao.deleteRoutine(id)

    suspend fun addDay(routineId: String): String {
        return db.withTransaction {
            val next = (dao.maxDayPosition(routineId) ?: -1) + 1
            val day = RoutineDayEntity(routineId = routineId, name = "Day ${next + 1}", position = next)
            dao.upsertDay(day)
            day.id
        }
    }

    suspend fun renameDay(dayId: String, name: String) {
        val day = dao.getDay(dayId) ?: return
        dao.upsertDay(day.copy(name = name))
    }

    suspend fun deleteDay(dayId: String) = dao.deleteDay(dayId)

    suspend fun addExerciseToDay(
        dayId: String,
        exerciseId: String,
        targetSets: Int = 3,
        repsMin: Int = 8,
        repsMax: Int = 12,
        restSec: Int = 120,
    ) {
        db.withTransaction {
            val next = (dao.maxExercisePosition(dayId) ?: -1) + 1
            dao.upsertDayExercise(
                RoutineExerciseEntity(
                    dayId = dayId,
                    exerciseId = exerciseId,
                    position = next,
                    targetSets = targetSets,
                    repsMin = repsMin,
                    repsMax = repsMax,
                    restSec = restSec,
                ),
            )
        }
    }

    suspend fun updateDayExercise(entry: RoutineExerciseEntity) = dao.upsertDayExercise(entry)

    suspend fun removeDayExercise(id: String) = dao.deleteDayExercise(id)

    suspend fun moveDay(routineId: String, dayId: String, delta: Int) {
        db.withTransaction {
            val days = dao.getDays(routineId).toMutableList()
            val index = days.indexOfFirst { it.id == dayId }
            val target = index + delta
            if (index < 0 || target !in days.indices) return@withTransaction
            val item = days.removeAt(index)
            days.add(target, item)
            days.forEachIndexed { i, day -> dao.upsertDay(day.copy(position = i)) }
        }
    }

    suspend fun moveDayExercise(dayId: String, entryId: String, delta: Int) {
        db.withTransaction {
            val items = dao.getDayExercises(dayId).toMutableList()
            val index = items.indexOfFirst { it.id == entryId }
            val target = index + delta
            if (index < 0 || target !in items.indices) return@withTransaction
            val item = items.removeAt(index)
            items.add(target, item)
            items.forEachIndexed { i, entry -> dao.upsertDayExercise(entry.copy(position = i)) }
        }
    }
}
