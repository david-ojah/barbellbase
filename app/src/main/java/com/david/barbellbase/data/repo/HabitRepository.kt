package com.david.barbellbase.data.repo

import com.david.barbellbase.data.db.HabitDao
import com.david.barbellbase.data.db.HabitKind
import com.david.barbellbase.data.db.HabitLogEntity
import com.david.barbellbase.data.db.LogSource
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.domain.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class DayTotal(val epochDay: Long, val amount: Int)

@Singleton
class HabitRepository @Inject constructor(
    private val dao: HabitDao,
    private val settings: SettingsRepository,
) {
    private val dayTicker = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }

    val waterToday: Flow<Int> = dayTicker.flatMapLatest {
        val now = Clock.now()
        dao.observeWaterTotal(Clock.startOfDay(now), Clock.endOfDay(now))
    }

    val walkToday: Flow<Int> = dayTicker.flatMapLatest {
        val now = Clock.now()
        dao.observeWalkTotal(Clock.startOfDay(now), Clock.endOfDay(now))
    }

    val waterTarget: Flow<Int> = settings.settings.map { it.waterTargetMl }
    val walkTarget: Flow<Int> = settings.settings.map { it.walkTargetMin }

    fun observeRecent(kind: HabitKind, limit: Int = 50): Flow<List<HabitLogEntity>> =
        dao.observeRecent(kind, limit)

    suspend fun logWater(amountMl: Int, source: LogSource = LogSource.APP) {
        dao.insert(
            HabitLogEntity(
                kind = HabitKind.WATER,
                amountMl = amountMl,
                source = source,
            ),
        )
    }

    suspend fun logWalk(minutes: Int, source: LogSource = LogSource.APP) {
        dao.insert(
            HabitLogEntity(
                kind = HabitKind.WALK,
                durationMin = minutes,
                source = source,
            ),
        )
    }

    suspend fun undoLast(kind: HabitKind) {
        val last = dao.last(kind) ?: return
        dao.delete(last.id)
    }

    fun waterSeries(days: Int): Flow<List<DayTotal>> = series(HabitKind.WATER, days)

    fun walkSeries(days: Int): Flow<List<DayTotal>> = series(HabitKind.WALK, days)

    /**
     * Emits exactly [days] entries, oldest first and ending today, so charts keep a stable
     * column per day instead of collapsing to a single bar on a day with no logs.
     */
    private fun series(kind: HabitKind, days: Int): Flow<List<DayTotal>> = dayTicker.flatMapLatest {
        val now = Clock.now()
        val today = Clock.epochDay(now)
        val from = Clock.startOfDay(now) - (days - 1).toLong() * 86_400_000L
        val to = Clock.endOfDay(now)
        dao.observeAllBetween(from, to).map { logs ->
            val totals = logs.filter { it.kind == kind }
                .groupBy { Clock.epochDay(it.loggedAt) }
                .mapValues { (_, entries) ->
                    if (kind == HabitKind.WATER) {
                        entries.sumOf { it.amountMl ?: 0 }
                    } else {
                        entries.sumOf { it.durationMin ?: 0 }
                    }
                }
            (days - 1 downTo 0).map { back ->
                val day = today - back
                DayTotal(day, totals[day] ?: 0)
            }
        }
    }

    fun todayCombined(): Flow<Triple<Int, Int, Int>> = combine(waterToday, walkToday, waterTarget) { w, walk, target ->
        Triple(w, walk, target)
    }

    suspend fun waterTodayValue(): Int {
        val now = Clock.now()
        return dao.observeWaterTotal(Clock.startOfDay(now), Clock.endOfDay(now)).first()
    }

    suspend fun walkTodayValue(): Int {
        val now = Clock.now()
        return dao.observeWalkTotal(Clock.startOfDay(now), Clock.endOfDay(now)).first()
    }
}
