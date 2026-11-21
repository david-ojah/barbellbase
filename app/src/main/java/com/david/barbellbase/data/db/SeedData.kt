package com.david.barbellbase.data.db

import com.david.barbellbase.data.db.Equipment.BAND
import com.david.barbellbase.data.db.Equipment.BARBELL
import com.david.barbellbase.data.db.Equipment.BODYWEIGHT
import com.david.barbellbase.data.db.Equipment.CABLE
import com.david.barbellbase.data.db.Equipment.DUMBBELL
import com.david.barbellbase.data.db.Equipment.KETTLEBELL
import com.david.barbellbase.data.db.Equipment.MACHINE
import com.david.barbellbase.data.db.MuscleGroup.BACK
import com.david.barbellbase.data.db.MuscleGroup.BICEPS
import com.david.barbellbase.data.db.MuscleGroup.CALVES
import com.david.barbellbase.data.db.MuscleGroup.CHEST
import com.david.barbellbase.data.db.MuscleGroup.CORE
import com.david.barbellbase.data.db.MuscleGroup.FOREARMS
import com.david.barbellbase.data.db.MuscleGroup.FULL_BODY
import com.david.barbellbase.data.db.MuscleGroup.GLUTES
import com.david.barbellbase.data.db.MuscleGroup.HAMSTRINGS
import com.david.barbellbase.data.db.MuscleGroup.QUADS
import com.david.barbellbase.data.db.MuscleGroup.SHOULDERS
import com.david.barbellbase.data.db.MuscleGroup.TRICEPS
import java.util.UUID

private data class Seed(
    val name: String,
    val primary: List<MuscleGroup>,
    val secondary: List<MuscleGroup>,
    val equipment: Equipment,
    val measurement: Measurement = Measurement.WEIGHT_REPS,
    val rest: Int = 120,
)

private val seeds = listOf(
    // Chest
    Seed("Barbell Bench Press", listOf(CHEST), listOf(TRICEPS, SHOULDERS), BARBELL),
    Seed("Incline Barbell Bench Press", listOf(CHEST), listOf(SHOULDERS, TRICEPS), BARBELL),
    Seed("Dumbbell Bench Press", listOf(CHEST), listOf(TRICEPS, SHOULDERS), DUMBBELL),
    Seed("Incline Dumbbell Press", listOf(CHEST), listOf(SHOULDERS, TRICEPS), DUMBBELL),
    Seed("Machine Chest Press", listOf(CHEST), listOf(TRICEPS), MACHINE),
    Seed("Cable Fly", listOf(CHEST), listOf(SHOULDERS), CABLE, rest = 90),
    Seed("Pec Deck", listOf(CHEST), emptyList(), MACHINE, rest = 90),
    Seed("Push-Up", listOf(CHEST), listOf(TRICEPS, CORE), BODYWEIGHT, rest = 60),
    Seed("Dip", listOf(CHEST), listOf(TRICEPS, SHOULDERS), BODYWEIGHT),
    Seed("Diamond Push-Up", listOf(TRICEPS), listOf(CHEST), BODYWEIGHT, Measurement.REPS_ONLY, 60),
    // Back
    Seed("Deadlift", listOf(BACK), listOf(HAMSTRINGS, GLUTES, CORE), BARBELL, rest = 180),
    Seed("Sumo Deadlift", listOf(BACK), listOf(GLUTES, QUADS), BARBELL, rest = 180),
    Seed("Barbell Row", listOf(BACK), listOf(BICEPS, CORE), BARBELL),
    Seed("Pendlay Row", listOf(BACK), listOf(BICEPS), BARBELL),
    Seed("Dumbbell Row", listOf(BACK), listOf(BICEPS), DUMBBELL, rest = 90),
    Seed("Chest-Supported Row", listOf(BACK), listOf(BICEPS), MACHINE, rest = 90),
    Seed("Seated Cable Row", listOf(BACK), listOf(BICEPS), CABLE, rest = 90),
    Seed("Lat Pulldown", listOf(BACK), listOf(BICEPS), CABLE, rest = 90),
    Seed("Pull-Up", listOf(BACK), listOf(BICEPS, CORE), BODYWEIGHT, Measurement.REPS_ONLY),
    Seed("Chin-Up", listOf(BACK), listOf(BICEPS), BODYWEIGHT, Measurement.REPS_ONLY),
    Seed("T-Bar Row", listOf(BACK), listOf(BICEPS), MACHINE, rest = 90),
    Seed("Straight-Arm Pulldown", listOf(BACK), listOf(CORE), CABLE, rest = 60),
    Seed("Rack Pull", listOf(BACK), listOf(GLUTES, HAMSTRINGS), BARBELL, rest = 180),
    Seed("Barbell Shrug", listOf(BACK), listOf(SHOULDERS), BARBELL, rest = 90),
    // Shoulders
    Seed("Overhead Press", listOf(SHOULDERS), listOf(TRICEPS, CORE), BARBELL, rest = 150),
    Seed("Seated Dumbbell Press", listOf(SHOULDERS), listOf(TRICEPS), DUMBBELL, rest = 120),
    Seed("Arnold Press", listOf(SHOULDERS), listOf(TRICEPS), DUMBBELL, rest = 120),
    Seed("Machine Shoulder Press", listOf(SHOULDERS), listOf(TRICEPS), MACHINE, rest = 90),
    Seed("Lateral Raise", listOf(SHOULDERS), emptyList(), DUMBBELL, rest = 60),
    Seed("Cable Lateral Raise", listOf(SHOULDERS), emptyList(), CABLE, rest = 60),
    Seed("Rear Delt Fly", listOf(SHOULDERS), listOf(BACK), DUMBBELL, rest = 60),
    Seed("Face Pull", listOf(SHOULDERS), listOf(BACK), CABLE, rest = 60),
    Seed("Upright Row", listOf(SHOULDERS), listOf(BICEPS), BARBELL, rest = 90),
    Seed("Landmine Press", listOf(SHOULDERS), listOf(CHEST), BARBELL, rest = 90),
    // Biceps
    Seed("Barbell Curl", listOf(BICEPS), listOf(FOREARMS), BARBELL, rest = 90),
    Seed("Dumbbell Curl", listOf(BICEPS), listOf(FOREARMS), DUMBBELL, rest = 75),
    Seed("Hammer Curl", listOf(BICEPS), listOf(FOREARMS), DUMBBELL, rest = 75),
    Seed("Incline Dumbbell Curl", listOf(BICEPS), emptyList(), DUMBBELL, rest = 75),
    Seed("Preacher Curl", listOf(BICEPS), emptyList(), MACHINE, rest = 75),
    Seed("Cable Curl", listOf(BICEPS), emptyList(), CABLE, rest = 60),
    Seed("Concentration Curl", listOf(BICEPS), emptyList(), DUMBBELL, rest = 60),
    // Triceps
    Seed("Close-Grip Bench Press", listOf(TRICEPS), listOf(CHEST), BARBELL, rest = 120),
    Seed("Skull Crusher", listOf(TRICEPS), emptyList(), BARBELL, rest = 90),
    Seed("Cable Pushdown", listOf(TRICEPS), emptyList(), CABLE, rest = 60),
    Seed("Overhead Cable Extension", listOf(TRICEPS), emptyList(), CABLE, rest = 60),
    Seed("Dumbbell Kickback", listOf(TRICEPS), emptyList(), DUMBBELL, rest = 60),
    // Legs
    Seed("Back Squat", listOf(QUADS), listOf(GLUTES, CORE), BARBELL, rest = 180),
    Seed("Front Squat", listOf(QUADS), listOf(CORE), BARBELL, rest = 180),
    Seed("Leg Press", listOf(QUADS), listOf(GLUTES), MACHINE, rest = 120),
    Seed("Hack Squat", listOf(QUADS), listOf(GLUTES), MACHINE, rest = 150),
    Seed("Goblet Squat", listOf(QUADS), listOf(GLUTES), DUMBBELL, rest = 90),
    Seed("Smith Machine Squat", listOf(QUADS), listOf(GLUTES), MACHINE, rest = 150),
    Seed("Romanian Deadlift", listOf(HAMSTRINGS), listOf(GLUTES, BACK), BARBELL, rest = 150),
    Seed("Stiff-Leg Deadlift", listOf(HAMSTRINGS), listOf(GLUTES), BARBELL, rest = 150),
    Seed("Leg Curl", listOf(HAMSTRINGS), emptyList(), MACHINE, rest = 90),
    Seed("Nordic Curl", listOf(HAMSTRINGS), emptyList(), BODYWEIGHT, Measurement.REPS_ONLY, 90),
    Seed("Leg Extension", listOf(QUADS), emptyList(), MACHINE, rest = 90),
    Seed("Bulgarian Split Squat", listOf(QUADS), listOf(GLUTES), DUMBBELL, rest = 120),
    Seed("Walking Lunge", listOf(QUADS), listOf(GLUTES), DUMBBELL, rest = 120),
    Seed("Hip Thrust", listOf(GLUTES), listOf(HAMSTRINGS), BARBELL, rest = 120),
    Seed("Glute Bridge", listOf(GLUTES), listOf(HAMSTRINGS), BODYWEIGHT, rest = 90),
    Seed("Cable Pull-Through", listOf(GLUTES), listOf(HAMSTRINGS), CABLE, rest = 90),
    Seed("Good Morning", listOf(HAMSTRINGS), listOf(BACK), BARBELL, rest = 120),
    Seed("Standing Calf Raise", listOf(CALVES), emptyList(), MACHINE, rest = 60),
    Seed("Seated Calf Raise", listOf(CALVES), emptyList(), MACHINE, rest = 60),
    // Core
    Seed("Plank", listOf(CORE), emptyList(), BODYWEIGHT, Measurement.TIME, 45),
    Seed("Side Plank", listOf(CORE), emptyList(), BODYWEIGHT, Measurement.TIME, 45),
    Seed("Hanging Leg Raise", listOf(CORE), listOf(FOREARMS), BODYWEIGHT, Measurement.REPS_ONLY, 75),
    Seed("Cable Crunch", listOf(CORE), emptyList(), CABLE, rest = 60),
    Seed("Ab Wheel Rollout", listOf(CORE), listOf(SHOULDERS), Equipment.OTHER, Measurement.REPS_ONLY, 75),
    Seed("Russian Twist", listOf(CORE), emptyList(), Equipment.OTHER, Measurement.REPS_ONLY, 45),
    Seed("Dead Bug", listOf(CORE), emptyList(), BODYWEIGHT, Measurement.REPS_ONLY, 45),
    // Forearms
    Seed("Wrist Curl", listOf(FOREARMS), emptyList(), DUMBBELL, rest = 45),
    Seed("Reverse Wrist Curl", listOf(FOREARMS), emptyList(), DUMBBELL, rest = 45),
    Seed("Farmer's Carry", listOf(FOREARMS), listOf(CORE, FULL_BODY), DUMBBELL, Measurement.TIME, 90),
    Seed("Dead Hang", listOf(FOREARMS), listOf(BACK), BODYWEIGHT, Measurement.TIME, 60),
    // Full body / power
    Seed("Power Clean", listOf(FULL_BODY), listOf(BACK, QUADS), BARBELL, rest = 180),
    Seed("Clean and Press", listOf(FULL_BODY), listOf(SHOULDERS), BARBELL, rest = 180),
    Seed("Kettlebell Swing", listOf(GLUTES), listOf(HAMSTRINGS, CORE), KETTLEBELL, rest = 90),
    Seed("Thruster", listOf(FULL_BODY), listOf(QUADS, SHOULDERS), BARBELL, rest = 120),
    Seed("Turkish Get-Up", listOf(FULL_BODY), listOf(CORE, SHOULDERS), KETTLEBELL, rest = 120),
    Seed("Burpee", listOf(FULL_BODY), emptyList(), BODYWEIGHT, Measurement.REPS_ONLY, 60),
    Seed("Band Pull-Apart", listOf(SHOULDERS), listOf(BACK), BAND, Measurement.REPS_ONLY, 45),
)

fun seedExercises(): List<ExerciseEntity> = seeds.map { seed ->
    ExerciseEntity(
        id = UUID.nameUUIDFromBytes(seed.name.toByteArray()).toString(),
        name = seed.name,
        primaryMuscles = seed.primary,
        secondaryMuscles = seed.secondary,
        equipment = seed.equipment,
        measurement = seed.measurement,
        instructions = "",
        isCustom = false,
        defaultRestSec = seed.rest,
    )
}

fun defaultReminderRules(): List<ReminderRuleEntity> = listOf(
    ReminderRuleEntity(
        kind = ReminderKind.WATER,
        label = "Water",
        intervalMinutes = 60,
        windowStartMin = 8 * 60,
        windowEndMin = 22 * 60,
        daysOfWeek = 0,
        sound = AlertSound.ALARM,
        vibration = true,
        ringSeconds = 60,
        waterAmountMl = 250,
        waterTargetMl = 2500,
        walkSuggestMin = null,
        walkTargetMin = null,
    ),
    ReminderRuleEntity(
        kind = ReminderKind.WALK,
        label = "Walk",
        intervalMinutes = 90,
        windowStartMin = 9 * 60,
        windowEndMin = 19 * 60,
        daysOfWeek = 0,
        sound = AlertSound.CHIME,
        vibration = true,
        ringSeconds = 0,
        waterAmountMl = null,
        waterTargetMl = null,
        walkSuggestMin = 10,
        walkTargetMin = 30,
    ),
)
