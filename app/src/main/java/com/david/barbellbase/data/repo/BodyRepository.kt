package com.david.barbellbase.data.repo

import com.david.barbellbase.data.db.BodyDao
import com.david.barbellbase.data.db.BodyMetricEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyRepository @Inject constructor(private val dao: BodyDao) {

    fun observeAll(): Flow<List<BodyMetricEntity>> = dao.observeAll()

    fun observeLatest(): Flow<BodyMetricEntity?> = dao.observeLatest()

    suspend fun save(
        weightKg: Double,
        date: LocalDate = LocalDate.now(),
        bodyFatPct: Double? = null,
        waistCm: Double? = null,
        notes: String = "",
    ) {
        val day = date.toEpochDay()
        dao.deleteByDay(day)
        dao.insert(
            BodyMetricEntity(
                epochDay = day,
                weightKg = weightKg,
                bodyFatPct = bodyFatPct,
                waistCm = waistCm,
                notes = notes,
            ),
        )
    }

    suspend fun delete(epochDay: Long) = dao.deleteByDay(epochDay)
}
