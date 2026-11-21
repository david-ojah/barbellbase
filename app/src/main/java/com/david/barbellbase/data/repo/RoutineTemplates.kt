package com.david.barbellbase.data.repo

/**
 * A ready-made routine users can import. Exercises are referenced by name so the
 * template survives exported/imported backups and is resolved against the local
 * exercise library at import time.
 */
data class RoutineTemplate(
    val id: String,
    val name: String,
    val level: String,
    val description: String,
    val days: List<TemplateDay>,
) {
    val exerciseCount: Int get() = days.sumOf { it.exercises.size }
}

data class TemplateDay(
    val name: String,
    val exercises: List<TemplateExercise>,
)

data class TemplateExercise(
    val exerciseName: String,
    val targetSets: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 12,
    val restSec: Int = 120,
)

val RoutineTemplates: List<RoutineTemplate> = listOf(
    RoutineTemplate(
        id = "push-pull-legs",
        name = "Push / Pull / Legs",
        level = "Intermediate",
        description = "Three training days rotating push, pull and legs. A proven balance of volume and recovery.",
        days = listOf(
            TemplateDay(
                name = "Push",
                exercises = listOf(
                    TemplateExercise("Barbell Bench Press", 4, 6, 10, 150),
                    TemplateExercise("Overhead Press", 3, 6, 10, 150),
                    TemplateExercise("Incline Dumbbell Press", 3, 8, 12, 120),
                    TemplateExercise("Lateral Raise", 3, 12, 15, 60),
                    TemplateExercise("Cable Pushdown", 3, 10, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Pull",
                exercises = listOf(
                    TemplateExercise("Barbell Row", 4, 6, 10, 150),
                    TemplateExercise("Lat Pulldown", 3, 8, 12, 90),
                    TemplateExercise("Seated Cable Row", 3, 8, 12, 90),
                    TemplateExercise("Face Pull", 3, 12, 15, 60),
                    TemplateExercise("Barbell Curl", 3, 8, 12, 75),
                    TemplateExercise("Hammer Curl", 3, 10, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Legs",
                exercises = listOf(
                    TemplateExercise("Back Squat", 4, 6, 10, 180),
                    TemplateExercise("Romanian Deadlift", 3, 8, 12, 150),
                    TemplateExercise("Leg Press", 3, 10, 15, 120),
                    TemplateExercise("Leg Curl", 3, 10, 15, 90),
                    TemplateExercise("Standing Calf Raise", 4, 12, 20, 60),
                ),
            ),
        ),
    ),
    RoutineTemplate(
        id = "upper-lower",
        name = "Upper / Lower",
        level = "Intermediate",
        description = "Four days alternating upper and lower body. Two different sessions per half keeps progress steady.",
        days = listOf(
            TemplateDay(
                name = "Upper A",
                exercises = listOf(
                    TemplateExercise("Barbell Bench Press", 4, 5, 8, 150),
                    TemplateExercise("Barbell Row", 4, 6, 10, 150),
                    TemplateExercise("Overhead Press", 3, 6, 10, 150),
                    TemplateExercise("Lat Pulldown", 3, 8, 12, 90),
                    TemplateExercise("Lateral Raise", 3, 12, 15, 60),
                    TemplateExercise("Barbell Curl", 3, 8, 12, 75),
                ),
            ),
            TemplateDay(
                name = "Lower A",
                exercises = listOf(
                    TemplateExercise("Back Squat", 4, 5, 8, 180),
                    TemplateExercise("Romanian Deadlift", 3, 8, 12, 150),
                    TemplateExercise("Leg Press", 3, 10, 15, 120),
                    TemplateExercise("Leg Curl", 3, 10, 15, 90),
                    TemplateExercise("Standing Calf Raise", 4, 12, 20, 60),
                ),
            ),
            TemplateDay(
                name = "Upper B",
                exercises = listOf(
                    TemplateExercise("Incline Dumbbell Press", 4, 8, 12, 120),
                    TemplateExercise("Seated Cable Row", 4, 8, 12, 90),
                    TemplateExercise("Seated Dumbbell Press", 3, 8, 12, 120),
                    TemplateExercise("Pull-Up", 3, 6, 12, 90),
                    TemplateExercise("Cable Fly", 3, 12, 15, 60),
                    TemplateExercise("Hammer Curl", 3, 10, 15, 60),
                    TemplateExercise("Cable Pushdown", 3, 10, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Lower B",
                exercises = listOf(
                    TemplateExercise("Deadlift", 3, 5, 8, 180),
                    TemplateExercise("Front Squat", 3, 6, 10, 180),
                    TemplateExercise("Bulgarian Split Squat", 3, 8, 12, 120),
                    TemplateExercise("Leg Extension", 3, 12, 15, 90),
                    TemplateExercise("Seated Calf Raise", 4, 15, 20, 60),
                ),
            ),
        ),
    ),
    RoutineTemplate(
        id = "full-body-3x",
        name = "Full Body 3x",
        level = "Beginner",
        description = "Three full-body sessions per week. The simplest way to build a base with basic compound lifts.",
        days = listOf(
            TemplateDay(
                name = "Day A",
                exercises = listOf(
                    TemplateExercise("Back Squat", 3, 5, 8, 180),
                    TemplateExercise("Barbell Bench Press", 3, 5, 8, 150),
                    TemplateExercise("Barbell Row", 3, 8, 12, 120),
                    TemplateExercise("Lateral Raise", 3, 12, 15, 60),
                    TemplateExercise("Cable Crunch", 3, 12, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Day B",
                exercises = listOf(
                    TemplateExercise("Deadlift", 3, 5, 8, 180),
                    TemplateExercise("Overhead Press", 3, 5, 8, 150),
                    TemplateExercise("Lat Pulldown", 3, 8, 12, 90),
                    TemplateExercise("Dumbbell Curl", 3, 10, 15, 60),
                    TemplateExercise("Standing Calf Raise", 3, 12, 20, 60),
                ),
            ),
            TemplateDay(
                name = "Day C",
                exercises = listOf(
                    TemplateExercise("Leg Press", 3, 10, 15, 120),
                    TemplateExercise("Incline Dumbbell Press", 3, 8, 12, 120),
                    TemplateExercise("Seated Cable Row", 3, 8, 12, 90),
                    TemplateExercise("Face Pull", 3, 12, 15, 60),
                    TemplateExercise("Cable Pushdown", 3, 10, 15, 60),
                ),
            ),
        ),
    ),
    RoutineTemplate(
        id = "bro-split",
        name = "5-Day Bro Split",
        level = "Intermediate",
        description = "One muscle group per day across five sessions. High volume if you like focused training days.",
        days = listOf(
            TemplateDay(
                name = "Chest",
                exercises = listOf(
                    TemplateExercise("Barbell Bench Press", 4, 6, 10, 150),
                    TemplateExercise("Incline Dumbbell Press", 4, 8, 12, 120),
                    TemplateExercise("Cable Fly", 3, 12, 15, 60),
                    TemplateExercise("Dip", 3, 8, 12, 90),
                ),
            ),
            TemplateDay(
                name = "Back",
                exercises = listOf(
                    TemplateExercise("Barbell Row", 4, 6, 10, 150),
                    TemplateExercise("Lat Pulldown", 4, 8, 12, 90),
                    TemplateExercise("Seated Cable Row", 3, 8, 12, 90),
                    TemplateExercise("Straight-Arm Pulldown", 3, 12, 15, 60),
                    TemplateExercise("Barbell Shrug", 3, 12, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Shoulders",
                exercises = listOf(
                    TemplateExercise("Overhead Press", 4, 6, 10, 150),
                    TemplateExercise("Seated Dumbbell Press", 3, 8, 12, 120),
                    TemplateExercise("Lateral Raise", 4, 12, 15, 60),
                    TemplateExercise("Rear Delt Fly", 3, 12, 15, 60),
                    TemplateExercise("Face Pull", 3, 12, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Arms",
                exercises = listOf(
                    TemplateExercise("Barbell Curl", 4, 8, 12, 75),
                    TemplateExercise("Close-Grip Bench Press", 4, 8, 12, 120),
                    TemplateExercise("Hammer Curl", 3, 10, 15, 60),
                    TemplateExercise("Skull Crusher", 3, 10, 15, 90),
                    TemplateExercise("Cable Curl", 3, 12, 15, 60),
                    TemplateExercise("Cable Pushdown", 3, 12, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Legs",
                exercises = listOf(
                    TemplateExercise("Back Squat", 4, 6, 10, 180),
                    TemplateExercise("Romanian Deadlift", 3, 8, 12, 150),
                    TemplateExercise("Leg Press", 3, 10, 15, 120),
                    TemplateExercise("Leg Extension", 3, 12, 15, 90),
                    TemplateExercise("Leg Curl", 3, 10, 15, 90),
                    TemplateExercise("Standing Calf Raise", 4, 12, 20, 60),
                ),
            ),
        ),
    ),
    RoutineTemplate(
        id = "starting-strength",
        name = "Starting Strength",
        level = "Beginner",
        description = "Alternate Workout A and Workout B three times per week. Add a small amount of weight when all sets are completed.",
        days = listOf(
            TemplateDay(
                name = "Workout A",
                exercises = listOf(
                    TemplateExercise("Back Squat", 3, 5, 5, 180),
                    TemplateExercise("Barbell Bench Press", 3, 5, 5, 150),
                    TemplateExercise("Deadlift", 1, 5, 5, 180),
                ),
            ),
            TemplateDay(
                name = "Workout B",
                exercises = listOf(
                    TemplateExercise("Back Squat", 3, 5, 5, 180),
                    TemplateExercise("Overhead Press", 3, 5, 5, 150),
                    TemplateExercise("Power Clean", 5, 3, 3, 180),
                ),
            ),
        ),
    ),
    RoutineTemplate(
        id = "five-three-one",
        name = "5/3/1",
        level = "Advanced",
        description = "Four days built around the main lifts. Use your own percentages for the main sets and keep accessories moderate.",
        days = listOf(
            TemplateDay(
                name = "Press",
                exercises = listOf(
                    TemplateExercise("Overhead Press", 3, 5, 8, 180),
                    TemplateExercise("Seated Dumbbell Press", 3, 8, 12, 120),
                    TemplateExercise("Lateral Raise", 3, 12, 15, 60),
                    TemplateExercise("Cable Pushdown", 3, 10, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Deadlift",
                exercises = listOf(
                    TemplateExercise("Deadlift", 3, 5, 8, 180),
                    TemplateExercise("Romanian Deadlift", 3, 8, 12, 150),
                    TemplateExercise("Leg Curl", 3, 10, 15, 90),
                    TemplateExercise("Hanging Leg Raise", 3, 10, 15, 75),
                ),
            ),
            TemplateDay(
                name = "Bench",
                exercises = listOf(
                    TemplateExercise("Barbell Bench Press", 3, 5, 8, 150),
                    TemplateExercise("Incline Dumbbell Press", 3, 8, 12, 120),
                    TemplateExercise("Dumbbell Row", 3, 8, 12, 90),
                    TemplateExercise("Cable Fly", 3, 12, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Squat",
                exercises = listOf(
                    TemplateExercise("Back Squat", 3, 5, 8, 180),
                    TemplateExercise("Leg Press", 3, 10, 15, 120),
                    TemplateExercise("Leg Curl", 3, 10, 15, 90),
                    TemplateExercise("Standing Calf Raise", 3, 12, 20, 60),
                ),
            ),
        ),
    ),
    RoutineTemplate(
        id = "dumbbell-only",
        name = "Dumbbell Only",
        level = "Beginner",
        description = "A dumbbell-focused plan for a home setup with minimal equipment.",
        days = listOf(
            TemplateDay(
                name = "Day A",
                exercises = listOf(
                    TemplateExercise("Dumbbell Bench Press", 4, 8, 12, 120),
                    TemplateExercise("Dumbbell Row", 4, 8, 12, 90),
                    TemplateExercise("Seated Dumbbell Press", 3, 8, 12, 120),
                    TemplateExercise("Dumbbell Curl", 3, 10, 15, 60),
                    TemplateExercise("Lateral Raise", 3, 12, 15, 60),
                ),
            ),
            TemplateDay(
                name = "Day B",
                exercises = listOf(
                    TemplateExercise("Goblet Squat", 4, 10, 15, 90),
                    TemplateExercise("Walking Lunge", 3, 10, 12, 120),
                    TemplateExercise("Glute Bridge", 3, 12, 15, 90),
                    TemplateExercise("Standing Calf Raise", 3, 12, 20, 60),
                    TemplateExercise("Dead Bug", 3, 10, 12, 45),
                ),
            ),
            TemplateDay(
                name = "Day C",
                exercises = listOf(
                    TemplateExercise("Incline Dumbbell Press", 3, 8, 12, 120),
                    TemplateExercise("Arnold Press", 3, 8, 12, 120),
                    TemplateExercise("Dumbbell Row", 3, 8, 12, 90),
                    TemplateExercise("Hammer Curl", 3, 10, 15, 60),
                    TemplateExercise("Dumbbell Kickback", 3, 10, 15, 60),
                ),
            ),
        ),
    ),
    RoutineTemplate(
        id = "bodyweight-home",
        name = "Bodyweight / Home",
        level = "Beginner",
        description = "No barbell required. Three bodyweight sessions that progress through reps and harder variations.",
        days = listOf(
            TemplateDay(
                name = "Day A",
                exercises = listOf(
                    TemplateExercise("Push-Up", 4, 10, 20, 60),
                    TemplateExercise("Pull-Up", 4, 5, 12, 90),
                    TemplateExercise("Dip", 3, 8, 15, 90),
                    TemplateExercise("Dead Bug", 3, 10, 15, 45),
                    TemplateExercise("Glute Bridge", 3, 12, 20, 90),
                ),
            ),
            TemplateDay(
                name = "Day B",
                exercises = listOf(
                    TemplateExercise("Chin-Up", 4, 5, 12, 90),
                    TemplateExercise("Diamond Push-Up", 3, 8, 15, 60),
                    TemplateExercise("Nordic Curl", 3, 5, 10, 90),
                    TemplateExercise("Burpee", 3, 10, 15, 60),
                    TemplateExercise("Hanging Leg Raise", 3, 10, 15, 75),
                ),
            ),
            TemplateDay(
                name = "Day C",
                exercises = listOf(
                    TemplateExercise("Push-Up", 3, 10, 20, 60),
                    TemplateExercise("Pull-Up", 3, 5, 12, 90),
                    TemplateExercise("Glute Bridge", 3, 12, 20, 90),
                    TemplateExercise("Hanging Leg Raise", 3, 10, 15, 75),
                    TemplateExercise("Burpee", 3, 10, 15, 60),
                ),
            ),
        ),
    ),
)
