package com.example

import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.health.HealthConnectDailyData
import com.example.health.HealthConnectManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityCalorieCalculationTest {

    @Test
    fun workoutType_estimateCalories_scalesWithWeightAndDuration() {
        // Running MET = 9.5; 70 kg * 1 hour (60 min) = 665 kcal
        val kcal70 = WorkoutType.RUNNING.estimateCalories(weightKg = 70.0, durationMinutes = 60)
        assertEquals(665, kcal70)

        // Strength MET = 5.5; 80 kg * 30 min = 220 kcal
        val kcalStrength = WorkoutType.STRENGTH.estimateCalories(weightKg = 80.0, durationMinutes = 30)
        assertEquals(220, kcalStrength)
    }

    @Test
    fun calculateDailyActivityEnergy_usesStepsEstimateWhenHcActiveCaloriesZero() {
        val hcData = HealthConnectDailyData(
            date = "2026-11-13",
            steps = 5000L,
            activeCaloriesBurned = 0,
            isSynced = true
        )
        val manualWorkouts = listOf(
            WorkoutLog(
                logId = "w1",
                date = "2026-11-13",
                workoutType = WorkoutType.STRENGTH.id,
                title = "Styrketräning",
                durationMinutes = 45,
                caloriesBurned = 300,
                syncedToHealthConnect = false
            )
        )

        val summary = HealthConnectManager.calculateDailyActivityEnergy(
            baseCalorieGoal = 2200,
            weightKg = 70.0,
            healthConnectData = hcData,
            manualWorkouts = manualWorkouts
        )

        // 5000 steps * 0.04 = 200 kcal; + 300 manual = 500 kcal total burned
        assertEquals(200, summary.stepEstimatedCalories)
        assertEquals(500, summary.totalBurnedCalories)
        assertEquals(2700, summary.adjustedCalorieGoal)
    }

    @Test
    fun calculateDailyActivityEnergy_avoidsDoubleCountingSyncedManualWorkouts() {
        val hcData = HealthConnectDailyData(
            date = "2026-11-13",
            steps = 8000L,
            activeCaloriesBurned = 450,
            isSynced = true
        )
        val manualWorkouts = listOf(
            WorkoutLog(
                logId = "w1",
                date = "2026-11-13",
                workoutType = WorkoutType.RUNNING.id,
                title = "Löpning",
                durationMinutes = 30,
                caloriesBurned = 330,
                syncedToHealthConnect = true
            ),
            WorkoutLog(
                logId = "w2",
                date = "2026-11-13",
                workoutType = WorkoutType.WALKING.id,
                title = "Kvällspromenad",
                durationMinutes = 30,
                caloriesBurned = 130,
                syncedToHealthConnect = false
            )
        )

        val summary = HealthConnectManager.calculateDailyActivityEnergy(
            baseCalorieGoal = 2000,
            weightKg = 70.0,
            healthConnectData = hcData,
            manualWorkouts = manualWorkouts
        )

        // HC active calories = 450 (covers w1 + steps); unsynced manual = 130 => 580 burned
        assertEquals(130, summary.manualWorkoutCalories)
        assertEquals(580, summary.totalBurnedCalories)
        assertEquals(2580, summary.adjustedCalorieGoal)
    }
}
