package com.example.data.model

import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

enum class FoodEntryType(val label: String, val badgeColorHex: Long) {
    BRAND("Varumärke", 0xFF2563EB),
    MEAL("Måltid", 0xFF059669),
    BASIC("Baslivsmedel", 0xFFD97706),
    FASTFOOD("Restaurang", 0xFFDC2626),
    SUPERMARKET("Mataffär", 0xFF0284C7),
    DRINK("Dryck", 0xFF06B6D4),
    GLOBAL_AI("Global databas", 0xFF8B5CF6)
}

enum class FoodUnit(val label: String, val shortLabel: String, val category: String) {
    PORTION("Normal portion", "port", "portion"),
    GRAM("Gram", "g", "vikt"),
    HEKTO("Hekto (100g)", "hg", "vikt"),
    KILO("Kilogram (1000g)", "kg", "vikt"),
    DECILITER("Deciliter (100ml)", "dl", "volym"),
    MILLILITER("Milliliter", "ml", "volym"),
    LITER("Liter (1000ml)", "l", "volym"),
    MATSKED("Matsked (15ml)", "msk", "volym"),
    TESKED("Tesked (5ml)", "tsk", "volym"),
    PIECE("Styck", "st", "styck")
}

@JsonClass(generateAdapter = true)
data class SwedishFoodEntry(
    val id: String,
    val name: String,
    val brand: String? = null,
    val category: String,
    val itemType: FoodEntryType = FoodEntryType.BASIC,
    val caloriesPer100g: Int,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double = 0.0,
    val sugarPer100g: Double = 0.0,
    val saltPer100g: Double = 0.0,
    val standardPortionGrams: Int = 100, // Medelportion baserat på vad en normal människa äter
    val standardPortionDescription: String = "1 normal portion",
    val pieceWeightGrams: Int? = null,
    val densityGramsPerMl: Double = 1.0,
    val defaultUnit: FoodUnit = FoodUnit.PORTION,
    val barcode: String? = null
) {
    val defaultServingGrams: Int
        get() = standardPortionGrams

    val displayName: String
        get() = if (!brand.isNullOrBlank()) "$brand $name" else name

    /**
     * Converts a given amount and unit into grams.
     */
    fun calculateGrams(unit: FoodUnit, amount: Double): Int {
        if (amount <= 0.0) return 0
        return when (unit) {
            FoodUnit.PORTION -> (standardPortionGrams * amount).roundToInt()
            FoodUnit.GRAM -> amount.roundToInt()
            FoodUnit.HEKTO -> (amount * 100.0).roundToInt()
            FoodUnit.KILO -> (amount * 1000.0).roundToInt()
            FoodUnit.MILLILITER -> (amount * densityGramsPerMl).roundToInt()
            FoodUnit.DECILITER -> (amount * 100.0 * densityGramsPerMl).roundToInt()
            FoodUnit.LITER -> (amount * 1000.0 * densityGramsPerMl).roundToInt()
            FoodUnit.MATSKED -> (amount * 15.0 * densityGramsPerMl).roundToInt()
            FoodUnit.TESKED -> (amount * 5.0 * densityGramsPerMl).roundToInt()
            FoodUnit.PIECE -> {
                val weight = pieceWeightGrams ?: standardPortionGrams
                (weight * amount).roundToInt()
            }
        }.coerceAtLeast(1)
    }

    /**
     * Calculates nutritional values for a specific gram weight.
     */
    fun toIngredient(grams: Int): FoodIngredient {
        val factor = grams / 100.0
        return FoodIngredient(
            name = displayName,
            amountGrams = grams,
            calories = (caloriesPer100g * factor).roundToInt(),
            protein = ((proteinPer100g * factor * 10.0).roundToInt()) / 10.0,
            carbs = ((carbsPer100g * factor * 10.0).roundToInt()) / 10.0,
            fat = ((fatPer100g * factor * 10.0).roundToInt()) / 10.0,
            fiber = ((fiberPer100g * factor * 10.0).roundToInt()) / 10.0,
            sugar = ((sugarPer100g * factor * 10.0).roundToInt()) / 10.0,
            salt = ((saltPer100g * factor * 100.0).roundToInt()) / 100.0
        )
    }

    /**
     * Convenience method to get nutritional values for a specific unit and amount.
     */
    fun toIngredient(unit: FoodUnit, amount: Double): FoodIngredient {
        val grams = calculateGrams(unit, amount)
        return toIngredient(grams)
    }
}
