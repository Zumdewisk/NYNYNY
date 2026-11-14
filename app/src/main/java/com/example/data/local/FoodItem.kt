package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.FoodIngredient
import com.example.data.model.MealItem

/**
 * Room Database entity representing a logged food item or ingredient.
 * Stores granular, offline nutrition data locally.
 */
@Entity(
    tableName = "food_items",
    indices = [
        Index(value = ["date"]),
        Index(value = ["meal_id"]),
        Index(value = ["date", "meal_type"])
    ]
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "meal_id")
    val mealId: String = "",

    @ColumnInfo(name = "user_id")
    val userId: String = "",

    @ColumnInfo(name = "date")
    val date: String, // YYYY-MM-DD

    @ColumnInfo(name = "meal_type")
    val mealType: String = "lunch", // frukost, lunch, middag, mellanmal, ovrigt

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "amount_grams")
    val amountGrams: Int = 100,

    @ColumnInfo(name = "calories")
    val calories: Int = 0,

    @ColumnInfo(name = "protein")
    val protein: Double = 0.0,

    @ColumnInfo(name = "carbs")
    val carbs: Double = 0.0,

    @ColumnInfo(name = "fat")
    val fat: Double = 0.0,

    @ColumnInfo(name = "fiber")
    val fiber: Double = 0.0,

    @ColumnInfo(name = "sugar")
    val sugar: Double = 0.0,

    @ColumnInfo(name = "salt")
    val salt: Double = 0.0,

    @ColumnInfo(name = "ai_analyzed")
    val aiAnalyzed: Boolean = false,

    @ColumnInfo(name = "image_url")
    val imageUrl: String = "",

    @ColumnInfo(name = "logged_at")
    val loggedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts this local FoodItem to a FoodIngredient model.
     */
    fun toFoodIngredient(): FoodIngredient {
        return FoodIngredient(
            name = name,
            amountGrams = amountGrams,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            salt = salt
        )
    }

    companion object {
        /**
         * Creates a FoodItem entity from a FoodIngredient and meal context.
         */
        fun fromIngredient(
            ingredient: FoodIngredient,
            mealId: String,
            userId: String,
            date: String,
            mealType: String,
            aiAnalyzed: Boolean = false,
            imageUrl: String = ""
        ): FoodItem {
            return FoodItem(
                mealId = mealId,
                userId = userId,
                date = date,
                mealType = mealType,
                name = ingredient.name,
                amountGrams = ingredient.amountGrams,
                calories = ingredient.calories,
                protein = ingredient.protein,
                carbs = ingredient.carbs,
                fat = ingredient.fat,
                fiber = ingredient.fiber,
                sugar = ingredient.sugar,
                salt = ingredient.salt,
                aiAnalyzed = aiAnalyzed,
                imageUrl = imageUrl
            )
        }

        /**
         * Creates a top-level FoodItem entity representing a complete meal.
         */
        fun fromMealItem(meal: MealItem): FoodItem {
            return FoodItem(
                mealId = meal.mealId.ifEmpty { meal.id },
                userId = meal.userId,
                date = meal.date,
                mealType = meal.mealType,
                name = meal.name,
                amountGrams = meal.getParsedIngredients().sumOf { it.amountGrams }.takeIf { it > 0 } ?: 100,
                calories = meal.calories,
                protein = meal.protein,
                carbs = meal.carbs,
                fat = meal.fat,
                fiber = meal.fiber,
                sugar = meal.sugar,
                salt = meal.salt,
                aiAnalyzed = meal.aiAnalyzed,
                imageUrl = meal.imageUrl
            )
        }
    }
}
