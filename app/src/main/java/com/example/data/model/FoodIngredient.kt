package com.example.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class FoodIngredient(
    val name: String = "",
    val amountGrams: Int = 100,
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val salt: Double = 0.0,
    val confidence: Double = 0.9,
    val isEstimated: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("amountGrams", amountGrams)
            put("calories", calories)
            put("protein", protein)
            put("carbs", carbs)
            put("fat", fat)
            put("fiber", fiber)
            put("sugar", sugar)
            put("salt", salt)
            put("confidence", confidence)
            put("isEstimated", isEstimated)
        }
    }

    companion object {
        private val moshi: Moshi by lazy { Moshi.Builder().build() }
        private val listAdapter by lazy {
            val type = Types.newParameterizedType(List::class.java, FoodIngredient::class.java)
            moshi.adapter<List<FoodIngredient>>(type)
        }

        fun fromJson(json: JSONObject): FoodIngredient {
            return FoodIngredient(
                name = json.optString("name", ""),
                amountGrams = json.optInt("amountGrams", 100),
                calories = json.optInt("calories", 0),
                protein = json.optDouble("protein", 0.0),
                carbs = json.optDouble("carbs", 0.0),
                fat = json.optDouble("fat", 0.0),
                fiber = json.optDouble("fiber", 0.0),
                sugar = json.optDouble("sugar", 0.0),
                salt = json.optDouble("salt", 0.0),
                confidence = json.optDouble("confidence", 0.9),
                isEstimated = json.optBoolean("isEstimated", true)
            )
        }

        fun parseList(jsonString: String?): List<FoodIngredient> {
            if (jsonString.isNullOrBlank()) return emptyList()
            return try {
                listAdapter.fromJson(jsonString).orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun serializeList(list: List<FoodIngredient>): String {
            return try {
                listAdapter.toJson(list) ?: "[]"
            } catch (_: Exception) {
                "[]"
            }
        }
    }
}
