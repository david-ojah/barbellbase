package com.david.barbellbase.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun musclesToString(value: List<MuscleGroup>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun stringToMuscles(value: String): List<MuscleGroup> =
        if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { runCatching { MuscleGroup.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun measurementToString(value: Measurement): String = value.name

    @TypeConverter
    fun stringToMeasurement(value: String): Measurement =
        runCatching { Measurement.valueOf(value) }.getOrDefault(Measurement.WEIGHT_REPS)

    @TypeConverter
    fun equipmentToString(value: Equipment): String = value.name

    @TypeConverter
    fun stringToEquipment(value: String): Equipment =
        runCatching { Equipment.valueOf(value) }.getOrDefault(Equipment.OTHER)

    @TypeConverter
    fun setTypeToString(value: SetType): String = value.name

    @TypeConverter
    fun stringToSetType(value: String): SetType =
        runCatching { SetType.valueOf(value) }.getOrDefault(SetType.NORMAL)

    @TypeConverter
    fun reminderKindToString(value: ReminderKind): String = value.name

    @TypeConverter
    fun stringToReminderKind(value: String): ReminderKind =
        runCatching { ReminderKind.valueOf(value) }.getOrDefault(ReminderKind.CUSTOM)

    @TypeConverter
    fun soundToString(value: AlertSound): String = value.name

    @TypeConverter
    fun stringToSound(value: String): AlertSound =
        runCatching { AlertSound.valueOf(value) }.getOrDefault(AlertSound.CHIME)

    @TypeConverter
    fun habitKindToString(value: HabitKind): String = value.name

    @TypeConverter
    fun stringToHabitKind(value: String): HabitKind =
        runCatching { HabitKind.valueOf(value) }.getOrDefault(HabitKind.WATER)

    @TypeConverter
    fun sourceToString(value: LogSource): String = value.name

    @TypeConverter
    fun stringToSource(value: String): LogSource =
        runCatching { LogSource.valueOf(value) }.getOrDefault(LogSource.APP)

    @TypeConverter
    fun actionToString(value: ReminderAction): String = value.name

    @TypeConverter
    fun stringToAction(value: String): ReminderAction =
        runCatching { ReminderAction.valueOf(value) }.getOrDefault(ReminderAction.FIRED)
}
