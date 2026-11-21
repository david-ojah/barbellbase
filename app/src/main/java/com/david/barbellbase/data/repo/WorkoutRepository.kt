package com.david.barbellbase.data.repo

import androidx.room.withTransaction
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.BarbellBaseDatabase
import com.david.barbellbase.data.db.SessionAggregate
import com.david.barbellbase.data.db.SessionDao
import com.david.barbellbase.data.db.SessionEntity
import com.david.barbellbase.data.db.SetEntryEntity
import com.david.barbellbase.data.db.SetType
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.data.db.ExerciseDao
import com.david.barbellbase.data.db.RoutineDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ExerciseBlock(
    val exercise: ExerciseEntity,
    val sets: List<SetEntryEntity>,
)

data class WorkoutData(
    val session: SessionEntity,
    val blocks: List<ExerciseBlock>,
)

@Singleton
class WorkoutRepository @Inject constructor(
    private val db: BarbellBaseDatabase,
    private val sessionDao: SessionDao,
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val settings: SettingsRepository,
) {

    fun observeActiveSession(): Flow<SessionEntity?> =
        settings.settings
            .map { it.activeSessionId }
            .distinctUntilChanged()
            .flatMapLatest { id -> if (id == null) flowOf(null) else sessionDao.observeSession(id) }

    fun observeWorkout(sessionId: String): Flow<WorkoutData?> = combine(
        sessionDao.observeSession(sessionId),
        sessionDao.observeSets(sessionId),
    ) { session, sets -> session to sets }.map { (session, sets) ->
        if (session == null) return@map null
        val orderedIds = sets.sortedBy { it.exerciseOrder }.map { it.exerciseId }.distinct()
        val exercises = exerciseDao.getByIds(orderedIds).associateBy { it.id }
        val blocks = orderedIds.mapNotNull { id ->
            exercises[id]?.let { exercise ->
                ExerciseBlock(exercise, sets.filter { it.exerciseId == id }.sortedBy { it.position })
            }
        }
        WorkoutData(session, blocks)
    }

    fun observeHistory(limit: Int = 200): Flow<List<SessionEntity>> =
        sessionDao.observeHistory(limit)

    fun observeRecent(limit: Int = 5): Flow<List<SessionEntity>> =
        sessionDao.observeRecent(limit)

    fun observeSession(id: String): Flow<SessionEntity?> = sessionDao.observeSession(id)

    fun observeSessionSets(id: String): Flow<List<SetEntryEntity>> = sessionDao.observeSets(id)

    fun observeAllCompletedSets(): Flow<List<SetEntryEntity>> = sessionDao.observeAllCompletedSets()

    suspend fun startEmptySession(name: String): String = db.withTransaction {
        val existing = sessionDao.unfinishedSessions().firstOrNull()
        if (existing != null) {
            settings.setActiveSession(existing.id)
            return@withTransaction existing.id
        }
        val session = SessionEntity(name = name.ifBlank { "Workout" })
        sessionDao.upsertSession(session)
        settings.setActiveSession(session.id)
        session.id
    }

    suspend fun startFromDay(dayId: String): String = db.withTransaction {
        val existing = sessionDao.unfinishedSessions().firstOrNull()
        if (existing != null) {
            settings.setActiveSession(existing.id)
            return@withTransaction existing.id
        }
        val day = routineDao.getDay(dayId) ?: return@withTransaction startEmptySession("Workout")
        val routine = routineDao.getRoutine(day.routineId)
        val planned = routineDao.getDayExercises(dayId)
        val session = SessionEntity(
            routineDayId = dayId,
            name = if (routine != null) "${routine.name} · ${day.name}" else day.name,
        )
        sessionDao.upsertSession(session)

        val newSets = mutableListOf<SetEntryEntity>()
        planned.forEachIndexed { blockIndex, plan ->
            repeat(plan.targetSets.coerceAtLeast(1)) { setIndex ->
                newSets += SetEntryEntity(
                    sessionId = session.id,
                    exerciseId = plan.exerciseId,
                    exerciseOrder = blockIndex,
                    position = setIndex,
                    weightKg = plan.targetWeightKg,
                    reps = plan.repsMin,
                    restSec = plan.restSec,
                )
            }
        }
        sessionDao.insertSets(newSets)
        settings.setActiveSession(session.id)
        session.id
    }

    suspend fun addExercise(sessionId: String, exerciseId: String, setCount: Int = 3) {
        db.withTransaction {
            val sets = sessionDao.getSets(sessionId)
            val nextOrder = (sets.maxOfOrNull { it.exerciseOrder } ?: -1) + 1
            val previous = sessionDao.lastCompletedSetsForExercise(exerciseId)
            val created = (0 until setCount.coerceIn(1, 12)).map { index ->
                val prior = previous.getOrNull(index)
                SetEntryEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    exerciseOrder = nextOrder,
                    position = index,
                    weightKg = prior?.weightKg,
                    reps = prior?.reps,
                    setType = prior?.setType ?: SetType.NORMAL,
                )
            }
            sessionDao.insertSets(created)
        }
    }

    suspend fun addSet(sessionId: String, exerciseId: String) {
        db.withTransaction {
            val sets = sessionDao.getSets(sessionId).filter { it.exerciseId == exerciseId }
            val order = sets.firstOrNull()?.exerciseOrder ?: 0
            val last = sets.lastOrNull()
            val next = SetEntryEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                exerciseOrder = order,
                position = (sets.maxOfOrNull { it.position } ?: -1) + 1,
                weightKg = last?.weightKg,
                reps = last?.reps,
                setType = last?.setType ?: SetType.NORMAL,
                restSec = last?.restSec,
            )
            sessionDao.upsertSet(next)
        }
    }

    suspend fun removeExercise(sessionId: String, exerciseId: String) {
        db.withTransaction {
            sessionDao.getSets(sessionId)
                .filter { it.exerciseId == exerciseId }
                .forEach { sessionDao.deleteSet(it.id) }
        }
    }

    suspend fun completeSet(
        setId: String,
        weightKg: Double?,
        reps: Int?,
        rpe: Double?,
        setType: SetType,
        durationSec: Int? = null,
    ): Int {
        val set = sessionDao.getSet(setId) ?: return 0
        val exercise = exerciseDao.getById(set.exerciseId)
        val rest = exercise?.defaultRestSec ?: 120
        sessionDao.upsertSet(
            set.copy(
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                setType = setType,
                durationSec = durationSec,
                completedAt = System.currentTimeMillis(),
                restSec = rest,
            ),
        )
        return rest
    }

    suspend fun updateSet(
        setId: String,
        weightKg: Double?,
        reps: Int?,
        rpe: Double?,
        setType: SetType,
        completed: Boolean,
        durationSec: Int? = null,
    ) {
        val set = sessionDao.getSet(setId) ?: return
        sessionDao.upsertSet(
            set.copy(
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                setType = setType,
                durationSec = durationSec,
                completedAt = if (completed) (set.completedAt ?: System.currentTimeMillis()) else null,
            ),
        )
    }

    suspend fun setType(setId: String, type: SetType) {
        val set = sessionDao.getSet(setId) ?: return
        sessionDao.upsertSet(set.copy(setType = type))
    }

    suspend fun deleteSet(setId: String) = sessionDao.deleteSet(setId)

    suspend fun finishSession(sessionId: String) {
        db.withTransaction {
            val session = sessionDao.getSession(sessionId) ?: return@withTransaction
            sessionDao.upsertSession(session.copy(finishedAt = System.currentTimeMillis()))
            settings.setActiveSession(null)
            settings.setRest(null, 0)
        }
    }

    suspend fun discardSession(sessionId: String) {
        db.withTransaction {
            sessionDao.deleteSession(sessionId)
            settings.setActiveSession(null)
            settings.setRest(null, 0)
        }
    }

    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSession(sessionId)
    }

    suspend fun renameSession(sessionId: String, name: String) {
        val session = sessionDao.getSession(sessionId) ?: return
        sessionDao.upsertSession(session.copy(name = name))
    }

    suspend fun updateSessionNotes(sessionId: String, notes: String) {
        val session = sessionDao.getSession(sessionId) ?: return
        sessionDao.upsertSession(session.copy(notes = notes))
    }

    suspend fun previousSets(exerciseId: String): List<SetEntryEntity> =
        sessionDao.lastCompletedSetsForExercise(exerciseId)

    suspend fun statsFor(sessionId: String) = sessionDao.statsForSession(sessionId)

    suspend fun aggregates(sessionIds: List<String>): List<SessionAggregate> =
        if (sessionIds.isEmpty()) emptyList() else sessionDao.aggregatesFor(sessionIds)

    suspend fun lastCompletedSets(exerciseId: String): List<SetEntryEntity> =
        sessionDao.lastCompletedSetsForExercise(exerciseId)

    fun observeCompletedForExercise(exerciseId: String): Flow<List<SetEntryEntity>> =
        sessionDao.observeCompletedForExercise(exerciseId)

    fun observeVolumeSince(from: Long): Flow<Double> = sessionDao.observeVolumeSince(from)

    fun observeSessionCountSince(from: Long): Flow<Int> = sessionDao.observeSessionCountSince(from)

    fun observeSetCountSince(from: Long): Flow<Int> = sessionDao.observeSetCountSince(from)

    /**
     * Recomputes the suggested day whenever routines, days or finished sessions change, so the Home
     * card stays correct after the user builds a routine or completes a workout.
     */
    fun observeSuggestedNextDay(): Flow<Pair<com.david.barbellbase.data.db.RoutineEntity, com.david.barbellbase.data.db.RoutineDayEntity>?> =
        combine(
            routineDao.observeRoutines(),
            routineDao.observeDaysForAll(),
            sessionDao.observeAllFinished(),
        ) { routines, days, finished ->
            if (routines.isEmpty()) return@combine null
            val lastDay = finished
                .maxByOrNull { it.finishedAt ?: it.startedAt }
                ?.routineDayId
                ?.let { id -> days.firstOrNull { it.id == id } }
            if (lastDay != null) {
                val routine = routines.firstOrNull { it.id == lastDay.routineId }
                val routineDays = days.filter { it.routineId == lastDay.routineId }.sortedBy { it.position }
                val next = routineDays.firstOrNull { it.position > lastDay.position } ?: routineDays.firstOrNull()
                if (routine != null && next != null) return@combine routine to next
            }
            val routine = routines.first()
            val routineDays = days.filter { it.routineId == routine.id }.sortedBy { it.position }
            routineDays.firstOrNull()?.let { routine to it }
        }

    suspend fun suggestNextDay(): Pair<com.david.barbellbase.data.db.RoutineEntity, com.david.barbellbase.data.db.RoutineDayEntity>? {
        val routines = routineDao.observeRoutinesOnce()
        if (routines.isEmpty()) return null

        val last = sessionDao.getLastFinished()
        if (last?.routineDayId != null) {
            val day = routineDao.getDay(last.routineDayId)
            if (day != null) {
                val days = routineDao.getDays(day.routineId)
                val next = days.firstOrNull { it.position > day.position } ?: days.firstOrNull()
                val routine = routineDao.getRoutine(day.routineId)
                if (next != null && routine != null) return routine to next
            }
        }

        val routine = routines.first()
        val days = routineDao.getDays(routine.id)
        return days.firstOrNull()?.let { routine to it }
    }
}
