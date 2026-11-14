package com.example

import com.example.data.model.AiDailyMealPlan
import com.example.data.model.FoodIngredient
import com.example.data.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeAndMealPlanTest {

    @Test
    fun recipeFromIngredients_calculatesTotalsAndPerServingCorrectly() {
        val ingredients = listOf(
            FoodIngredient(
                name = "Havregryn",
                amountGrams = 100,
                calories = 370,
                protein = 13.0,
                carbs = 58.0,
                fat = 7.0,
                fiber = 10.0
            ),
            FoodIngredient(
                name = "Ägg",
                amountGrams = 120,
                calories = 172,
                protein = 15.0,
                carbs = 1.2,
                fat = 12.0,
                fiber = 0.0
            )
        )

        val recipe = Recipe.fromIngredients(
            recipeId = "r1",
            userId = "u1",
            name = "Grötplättar",
            description = "Mättande frukost",
            servings = 2,
            ingredients = ingredients
        )

        assertEquals(542, recipe.totalCalories)
        assertEquals(28.0, recipe.totalProtein, 0.01)
        assertEquals(59.2, recipe.totalCarbs, 0.01)
        assertEquals(19.0, recipe.totalFat, 0.01)
        assertEquals(10.0, recipe.totalFiber, 0.01)

        assertEquals(271, recipe.perServingCalories())
        assertEquals(14.0, recipe.perServingProtein(), 0.01)
        assertEquals(29.6, recipe.perServingCarbs(), 0.01)
        assertEquals(9.5, recipe.perServingFat(), 0.01)
        assertEquals(5.0, recipe.perServingFiber(), 0.01)

        val meal = recipe.toMealItem(date = "2026-11-13", mealType = "frukost", portionsEaten = 1.5)
        assertEquals("frukost", meal.mealType)
        assertEquals("Grötplättar (1,5 port)", meal.name)
        assertEquals(407, meal.calories)
        assertEquals(21.0, meal.protein, 0.01)
        assertEquals(2, FoodIngredient.parseList(meal.itemsJson).size)
    }

    @Test
    fun aiDailyMealPlan_parseFromJson_parsesMealsAndConvertsToMealItems() {
        val rawJson = """
            {
              "plan_title": "Högprotein svensk dag",
              "summary": "Balanserad dag med fokus på protein och fullkorn.",
              "total_calories": 2150,
              "total_protein_g": 165.0,
              "total_carbs_g": 210.0,
              "total_fat_g": 65.0,
              "meals": [
                {
                  "meal_type": "frukost",
                  "meal_label": "Frukost",
                  "dish_name": "Havregrynsgröt med kvarg och bär",
                  "description": "Koka havregryn med vatten och toppa med kvarg.",
                  "calories": 480,
                  "protein_g": 34.0,
                  "carbs_g": 55.0,
                  "fat_g": 10.0,
                  "fiber_g": 8.0,
                  "ingredients": [
                    {"name": "Havregryn", "weight_g": 60, "calories": 222, "protein_g": 7.8, "carbs_g": 34.8, "fat_g": 4.2},
                    {"name": "Lättkvarg naturell", "weight_g": 200, "calories": 132, "protein_g": 24.0, "carbs_g": 7.0, "fat_g": 0.4}
                  ]
                },
                {
                  "meal_type": "lunch",
                  "meal_label": "Lunch",
                  "dish_name": "Kycklingfilé med ris och broccoli",
                  "description": "Stekt kyckling med kokt jasminris.",
                  "calories": 690,
                  "protein_g": 56.0,
                  "carbs_g": 75.0,
                  "fat_g": 15.0,
                  "fiber_g": 5.5,
                  "ingredients": [
                    {"name": "Kycklingfilé", "weight_g": 180, "calories": 261, "protein_g": 45.0, "carbs_g": 0.0, "fat_g": 4.5}
                  ]
                },
                {
                  "meal_type": "middag",
                  "meal_label": "Middag",
                  "dish_name": "Ugnsbakad lax med potatis",
                  "description": "Krydda med dill och citron.",
                  "calories": 720,
                  "protein_g": 48.0,
                  "carbs_g": 52.0,
                  "fat_g": 32.0,
                  "fiber_g": 4.5,
                  "ingredients": []
                },
                {
                  "meal_type": "mellanmal",
                  "meal_label": "Mellanmål",
                  "dish_name": "Keso och äpple",
                  "description": "Snabbt mellanmål.",
                  "calories": 260,
                  "protein_g": 27.0,
                  "carbs_g": 28.0,
                  "fat_g": 8.0,
                  "fiber_g": 3.0,
                  "ingredients": []
                }
              ]
            }
        """.trimIndent()

        val plan = AiDailyMealPlan.parseFromJson(rawJson, requestedCalories = 2150)
        assertEquals("Högprotein svensk dag", plan.planTitle)
        assertEquals(2150, plan.totalCalories)
        assertEquals(4, plan.meals.size)
        assertEquals(2, plan.meals.first().ingredients.size)

        val mealItems = plan.toMealItems("2026-11-13")
        assertEquals(4, mealItems.size)
        assertEquals("frukost", mealItems[0].mealType)
        assertEquals("lunch", mealItems[1].mealType)
        assertEquals("middag", mealItems[2].mealType)
        assertEquals("mellanmal", mealItems[3].mealType)
        assertTrue(mealItems.all { it.aiAnalyzed && it.date == "2026-11-13" })
    }
}
