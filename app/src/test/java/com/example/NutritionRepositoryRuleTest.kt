package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AiFoodCorrection
import com.example.data.model.FoodIngredient
import com.example.data.model.MealItem
import com.example.data.model.Recipe
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.data.repository.NutritionRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_authenticatedUser_savesAndRetrievesProfile() = runBlocking {
        signInTestUser("alice@test.com")
        val repo = NutritionRepository(firestore)

        val profile = UserProfile(
            userId = auth.currentUser!!.uid,
            email = "alice@test.com",
            displayName = "Alice",
            age = 29,
            heightCm = 172,
            weightKg = 65.0,
            targetCalories = 2100,
            targetProteinG = 130,
            targetCarbsG = 220,
            targetFatG = 65,
            targetWaterMl = 2300
        )

        val saveResult = repo.saveUserProfile(profile)
        val failure = saveResult.exceptionOrNull()
        assertTrue("Failed with: ${failure?.message} (cause: ${failure?.cause?.message})", saveResult.isSuccess)

        val observed = repo.getUserProfile().getOrThrow()
        assertEquals(2100, observed?.targetCalories)
    }

    @Test
    fun addMeal_authenticatedUser_success() = runBlocking {
        signInTestUser("alice@test.com")
        val repo = NutritionRepository(firestore)

        val ingredient = FoodIngredient(
            name = "Kycklingfilé",
            amountGrams = 150,
            calories = 218,
            protein = 46.5,
            carbs = 0.0,
            fat = 3.8
        )

        val meal = MealItem(
            userId = auth.currentUser!!.uid,
            date = "2026-11-03",
            mealType = "lunch",
            name = "Kycklinglunch",
            calories = 218,
            protein = 46.5,
            carbs = 0.0,
            fat = 3.8,
            itemsJson = FoodIngredient.serializeList(listOf(ingredient)),
            aiAnalyzed = true
        )

        val result = repo.addMeal(meal)
        assertTrue(result.isSuccess)

        val meals = withTimeout(5000L) {
            repo.observeMealsForDate("2026-11-03").first { it.isNotEmpty() }
        }
        assertTrue(meals.any { it.name == "Kycklinglunch" })
    }

    @Test
    fun logWater_authenticatedUser_success() = runBlocking {
        signInTestUser("alice@test.com")
        val repo = NutritionRepository(firestore)

        val result = repo.logWater(250, "2026-11-03")
        assertTrue(result.isSuccess)

        val logs = withTimeout(5000L) {
            repo.observeWaterLogsForDate("2026-11-03").first { it.isNotEmpty() }
        }
        assertEquals(250, logs.first().amountMl)
    }

    @Test
    fun observeMeals_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = NutritionRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(3000L) {
                repo.observeMealsForDate("2026-11-03").first()
            }
        } catch (e: Exception) {
            caughtException = e
        }

        assertNotNull("Expected permission denied exception", caughtException)
        val unwrapped = caughtException?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unwrapped?.code)
    }

    @Test
    fun addWorkoutLog_authenticatedUser_success() = runBlocking {
        signInTestUser("alice@test.com")
        val repo = NutritionRepository(firestore)

        val workout = WorkoutLog(
            userId = auth.currentUser!!.uid,
            date = "2026-11-13",
            workoutType = WorkoutType.RUNNING.id,
            title = "Morgonlöpning",
            durationMinutes = 35,
            caloriesBurned = 390,
            notes = "Bra tempo"
        )

        val result = repo.addWorkoutLog(workout)
        assertTrue(result.isSuccess)

        val logs = withTimeout(5000L) {
            repo.observeWorkoutLogsForDate("2026-11-13").first { it.isNotEmpty() }
        }
        assertEquals(390, logs.first().caloriesBurned)
    }

    @Test
    fun saveRecipe_authenticatedUser_success() = runBlocking {
        signInTestUser("alice@test.com")
        val repo = NutritionRepository(firestore)

        val recipe = Recipe.fromIngredients(
            userId = auth.currentUser!!.uid,
            name = "Proteingröt",
            description = "Frukostfavorit",
            servings = 2,
            ingredients = listOf(
                FoodIngredient(
                    name = "Havregryn",
                    amountGrams = 100,
                    calories = 370,
                    protein = 13.0,
                    carbs = 58.0,
                    fat = 7.0
                )
            )
        )

        val result = repo.saveRecipe(recipe)
        assertTrue(result.isSuccess)

        val recipes = withTimeout(5000L) {
            repo.observeRecipes().first { it.isNotEmpty() }
        }
        assertTrue(recipes.any { it.name == "Proteingröt" && it.servings == 2 })
    }

    @Test
    fun saveAiCorrection_authenticatedUser_success() = runBlocking {
        signInTestUser("alice@test.com")
        val repo = NutritionRepository(firestore)

        val correction = AiFoodCorrection(
            originalDish = "Nötfärstacos",
            userFeedback = "Det är kycklingfärs istället för nötfärs",
            correctedDish = "Kycklingtacos",
            learningRule = "Användarens tacos använder kycklingfärs istället för nötfärs.",
            keywords = "tacos, kycklingfärs"
        )

        val result = repo.saveAiCorrection(correction)
        assertTrue(result.isSuccess)

        val observed = withTimeout(5000L) {
            repo.observeAiCorrections().first { it.isNotEmpty() }
        }
        assertTrue(observed.any { it.correctedDish == "Kycklingtacos" && it.learningRule.contains("kycklingfärs") })
    }
}
