package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlin.math.roundToInt

@IgnoreExtraProperties
data class Recipe(
    @DocumentId val id: String = "",
    val recipeId: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val servings: Int = 2,
    val totalCalories: Int = 0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalFiber: Double = 0.0,
    val itemsJson: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun ingredients(): List<FoodIngredient> = FoodIngredient.parseList(itemsJson)

    fun perServingCalories(): Int {
        val safeServings = servings.coerceAtLeast(1)
        return (totalCalories.toDouble() / safeServings).roundToInt()
    }

    fun perServingProtein(): Double = roundOneDecimal(totalProtein / servings.coerceAtLeast(1))
    fun perServingCarbs(): Double = roundOneDecimal(totalCarbs / servings.coerceAtLeast(1))
    fun perServingFat(): Double = roundOneDecimal(totalFat / servings.coerceAtLeast(1))
    fun perServingFiber(): Double = roundOneDecimal(totalFiber / servings.coerceAtLeast(1))

    fun toMealItem(date: String, mealType: String, portionsEaten: Double = 1.0): MealItem {
        val safeServings = servings.coerceAtLeast(1)
        val scale = portionsEaten.coerceAtLeast(0.1) / safeServings.toDouble()
        val scaledIngredients = ingredients().map { ing ->
            ing.copy(
                amountGrams = (ing.amountGrams * scale).roundToInt().coerceAtLeast(1),
                calories = (ing.calories * scale).roundToInt(),
                protein = roundOneDecimal(ing.protein * scale),
                carbs = roundOneDecimal(ing.carbs * scale),
                fat = roundOneDecimal(ing.fat * scale),
                fiber = roundOneDecimal(ing.fiber * scale),
                sugar = roundOneDecimal(ing.sugar * scale),
                salt = roundTwoDecimals(ing.salt * scale)
            )
        }
        val portionLabel = if (portionsEaten == 1.0) "1 port" else "${portionsEaten.toString().replace('.', ',')} port"
        return MealItem(
            date = date,
            mealType = mealType,
            name = "$name ($portionLabel)",
            calories = (totalCalories * scale).roundToInt(),
            protein = roundOneDecimal(totalProtein * scale),
            carbs = roundOneDecimal(totalCarbs * scale),
            fat = roundOneDecimal(totalFat * scale),
            fiber = roundOneDecimal(totalFiber * scale),
            itemsJson = FoodIngredient.serializeList(scaledIngredients),
            aiAnalyzed = false
        )
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "recipeId" to recipeId,
            "userId" to userId,
            "name" to name,
            "description" to description,
            "servings" to servings,
            "totalCalories" to totalCalories,
            "totalProtein" to totalProtein,
            "totalCarbs" to totalCarbs,
            "totalFat" to totalFat,
            "totalFiber" to totalFiber,
            "itemsJson" to itemsJson,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        ).filterValues { it != null }
    }

    companion object {
        fun fromIngredients(
            recipeId: String = "",
            userId: String = "",
            name: String,
            description: String,
            servings: Int,
            ingredients: List<FoodIngredient>
        ): Recipe {
            val safeServings = servings.coerceIn(1, 100)
            return Recipe(
                id = recipeId,
                recipeId = recipeId,
                userId = userId,
                name = name.trim(),
                description = description.trim(),
                servings = safeServings,
                totalCalories = ingredients.sumOf { it.calories },
                totalProtein = roundOneDecimal(ingredients.sumOf { it.protein }),
                totalCarbs = roundOneDecimal(ingredients.sumOf { it.carbs }),
                totalFat = roundOneDecimal(ingredients.sumOf { it.fat }),
                totalFiber = roundOneDecimal(ingredients.sumOf { it.fiber }),
                itemsJson = FoodIngredient.serializeList(ingredients)
            )
        }

        private fun roundOneDecimal(v: Double): Double = (v * 10.0).roundToInt() / 10.0
        private fun roundTwoDecimals(v: Double): Double = (v * 100.0).roundToInt() / 100.0
    }
}

data class PlannedMeal(
    val mealType: String,
    val mealLabel: String,
    val dishName: String,
    val description: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val ingredients: List<FoodIngredient>
) {
    fun toMealItem(date: String): MealItem {
        val validType = when (mealType.lowercase()) {
            "frukost", "lunch", "middag", "mellanmal", "ovrigt" -> mealType.lowercase()
            else -> "mellanmal"
        }
        return MealItem(
            date = date,
            mealType = validType,
            name = dishName,
            calories = calories,
            protein = proteinG,
            carbs = carbsG,
            fat = fatG,
            fiber = fiberG,
            itemsJson = FoodIngredient.serializeList(ingredients),
            aiAnalyzed = true
        )
    }
}

@JsonClass(generateAdapter = true)
internal data class MealPlanIngredientDto(
    val name: String = "Livsmedel",
    val weight_g: Int = 100,
    val calories: Int = 0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0
)

@JsonClass(generateAdapter = true)
internal data class PlannedMealDto(
    val meal_type: String = "mellanmal",
    val meal_label: String = "Måltid",
    val dish_name: String = "Måltid",
    val description: String = "",
    val calories: Int? = null,
    val protein_g: Double? = null,
    val carbs_g: Double? = null,
    val fat_g: Double? = null,
    val fiber_g: Double = 0.0,
    val ingredients: List<MealPlanIngredientDto> = emptyList()
)

@JsonClass(generateAdapter = true)
internal data class AiDailyMealPlanDto(
    val plan_title: String = "Personligt dagsschema",
    val summary: String = "",
    val total_calories: Int? = null,
    val total_protein_g: Double? = null,
    val total_carbs_g: Double? = null,
    val total_fat_g: Double? = null,
    val meals: List<PlannedMealDto> = emptyList()
)

data class AiDailyMealPlan(
    val planTitle: String,
    val summary: String,
    val targetCalories: Int,
    val totalCalories: Int,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val meals: List<PlannedMeal>
) {
    fun toMealItems(date: String): List<MealItem> = meals.map { it.toMealItem(date) }

    companion object {
        private val moshi: Moshi by lazy { Moshi.Builder().build() }
        private val planAdapter by lazy { moshi.adapter(AiDailyMealPlanDto::class.java) }

        fun parseFromJson(jsonString: String, requestedCalories: Int): AiDailyMealPlan {
            val dto = planAdapter.fromJson(jsonString.trim())
                ?: throw IllegalStateException("AI-svaret kunde inte tolkas.")

            if (dto.meals.isEmpty()) {
                throw IllegalStateException("AI returnerade inga måltider i schemat.")
            }

            val meals = dto.meals.map { mealDto ->
                val ingredients = mealDto.ingredients.map { ing ->
                    FoodIngredient(
                        name = ing.name.ifBlank { "Livsmedel" },
                        amountGrams = ing.weight_g.coerceAtLeast(1),
                        calories = ing.calories.coerceAtLeast(0),
                        protein = ing.protein_g.coerceAtLeast(0.0),
                        carbs = ing.carbs_g.coerceAtLeast(0.0),
                        fat = ing.fat_g.coerceAtLeast(0.0),
                        confidence = 0.9,
                        isEstimated = true
                    )
                }

                PlannedMeal(
                    mealType = mealDto.meal_type.lowercase(),
                    mealLabel = mealDto.meal_label.ifBlank { "Måltid" },
                    dishName = mealDto.dish_name.ifBlank { "Måltid" },
                    description = mealDto.description,
                    calories = (mealDto.calories ?: ingredients.sumOf { it.calories }).coerceAtLeast(0),
                    proteinG = roundOne((mealDto.protein_g ?: ingredients.sumOf { it.protein }).coerceAtLeast(0.0)),
                    carbsG = roundOne((mealDto.carbs_g ?: ingredients.sumOf { it.carbs }).coerceAtLeast(0.0)),
                    fatG = roundOne((mealDto.fat_g ?: ingredients.sumOf { it.fat }).coerceAtLeast(0.0)),
                    fiberG = roundOne(mealDto.fiber_g.coerceAtLeast(0.0)),
                    ingredients = ingredients
                )
            }

            val computedCalories = meals.sumOf { it.calories }
            val computedProtein = roundOne(meals.sumOf { it.proteinG })
            val computedCarbs = roundOne(meals.sumOf { it.carbsG })
            val computedFat = roundOne(meals.sumOf { it.fatG })

            return AiDailyMealPlan(
                planTitle = dto.plan_title.ifBlank { "Personligt dagsschema" },
                summary = dto.summary,
                targetCalories = requestedCalories,
                totalCalories = dto.total_calories?.takeIf { it > 0 } ?: computedCalories,
                totalProteinG = roundOne(dto.total_protein_g?.takeIf { it > 0.0 } ?: computedProtein),
                totalCarbsG = roundOne(dto.total_carbs_g?.takeIf { it > 0.0 } ?: computedCarbs),
                totalFatG = roundOne(dto.total_fat_g?.takeIf { it > 0.0 } ?: computedFat),
                meals = meals
            )
        }

        private fun roundOne(v: Double): Double = (v * 10.0).roundToInt() / 10.0
    }
}
