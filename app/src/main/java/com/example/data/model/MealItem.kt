package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MealItem(
    @DocumentId val id: String = "",
    val mealId: String = "",
    val userId: String = "",
    val date: String = "", // YYYY-MM-DD
    val mealType: String = "lunch", // frukost, lunch, middag, mellanmal, ovrigt
    val name: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val salt: Double = 0.0,
    val itemsJson: String = "[]",
    val aiAnalyzed: Boolean = false,
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun getParsedIngredients(): List<FoodIngredient> {
        return FoodIngredient.parseList(itemsJson)
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "mealId" to mealId,
            "userId" to userId,
            "date" to date,
            "mealType" to mealType,
            "name" to name,
            "calories" to calories,
            "protein" to protein,
            "carbs" to carbs,
            "fat" to fat,
            "fiber" to fiber,
            "sugar" to sugar,
            "salt" to salt,
            "itemsJson" to itemsJson,
            "aiAnalyzed" to aiAnalyzed,
            "imageUrl" to imageUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        ).filterValues { it != null }
    }
}
