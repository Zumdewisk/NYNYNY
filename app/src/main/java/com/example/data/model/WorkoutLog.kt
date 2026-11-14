package com.example.data.model

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

enum class WorkoutType(
    val id: String,
    val displayName: String,
    val metValue: Double,
    val healthConnectExerciseType: Int
) {
    STRENGTH(
        id = "strength",
        displayName = "Styrketräning",
        metValue = 5.5,
        healthConnectExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
    ),
    RUNNING(
        id = "running",
        displayName = "Löpning",
        metValue = 9.5,
        healthConnectExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
    ),
    WALKING(
        id = "walking",
        displayName = "Promenad",
        metValue = 3.8,
        healthConnectExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING
    ),
    CYCLING(
        id = "cycling",
        displayName = "Cykling",
        metValue = 7.5,
        healthConnectExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING
    ),
    HIIT(
        id = "hiit",
        displayName = "HIIT / Kondition",
        metValue = 8.5,
        healthConnectExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
    ),
    SWIMMING(
        id = "swimming",
        displayName = "Simning",
        metValue = 7.0,
        healthConnectExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
    ),
    YOGA(
        id = "yoga",
        displayName = "Yoga / Rörlighet",
        metValue = 3.0,
        healthConnectExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_YOGA
    ),
    OTHER(
        id = "other",
        displayName = "Annan träning",
        metValue = 5.0,
        healthConnectExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    );

    fun estimateCalories(weightKg: Double, durationMinutes: Int): Int {
        val safeWeight = weightKg.coerceIn(35.0, 250.0)
        val safeDuration = durationMinutes.coerceIn(1, 600)
        return (metValue * safeWeight * (safeDuration / 60.0)).roundToInt().coerceAtLeast(1)
    }

    companion object {
        fun fromId(id: String): WorkoutType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }

        fun swedishNameForHealthConnectType(exerciseType: Int): String {
            return when (exerciseType) {
                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Styrketräning"
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Löpning"
                ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Promenad"
                ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Vandring"
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Cykling"
                ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT / Kondition"
                ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
                ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Simning"
                ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
                ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "Pilates"
                ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Crosstrainer"
                ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Roddmaskin"
                ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "Fotboll"
                ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "Tennis"
                ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL -> "Racketsport"
                ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "Dans"
                ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> "Skidåkning"
                else -> "Träningspass"
            }
        }
    }
}

@IgnoreExtraProperties
data class WorkoutLog(
    @DocumentId val id: String = "",
    val logId: String = "",
    val userId: String = "",
    val date: String = "", // "YYYY-MM-DD"
    val workoutType: String = WorkoutType.STRENGTH.id,
    val title: String = "",
    val durationMinutes: Int = 30,
    val caloriesBurned: Int = 0,
    val notes: String = "",
    val syncedToHealthConnect: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "logId" to logId,
            "userId" to userId,
            "date" to date,
            "workoutType" to workoutType,
            "title" to title,
            "durationMinutes" to durationMinutes,
            "caloriesBurned" to caloriesBurned,
            "notes" to notes,
            "syncedToHealthConnect" to syncedToHealthConnect,
            "createdAt" to createdAt
        ).filterValues { it != null }
    }
}
