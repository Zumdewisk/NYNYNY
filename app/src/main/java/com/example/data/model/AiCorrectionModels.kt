package com.example.data.model

import com.example.data.repository.AiMealAnalysisResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlin.math.roundToInt

@IgnoreExtraProperties
data class AiFoodCorrection(
    @DocumentId val id: String = "",
    val correctionId: String = "",
    val userId: String = "",
    val originalDish: String = "",
    val userFeedback: String = "",
    val correctedDish: String = "",
    val learningRule: String = "",
    val keywords: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "correctionId" to correctionId,
            "userId" to userId,
            "originalDish" to originalDish,
            "userFeedback" to userFeedback,
            "correctedDish" to correctedDish,
            "learningRule" to learningRule,
            "keywords" to keywords,
            "createdAt" to createdAt
        ).filterValues { it != null }
    }

    companion object {
        /**
         * Builds a compact Swedish prompt section with the user's previously taught AI rules
         * so Gemini avoids repeating past mistakes on future scans and corrections.
         */
        fun formatRulesForPrompt(
            corrections: List<AiFoodCorrection>,
            maxRules: Int = 15
        ): String {
            val active = corrections
                .filter { it.learningRule.isNotBlank() || it.correctedDish.isNotBlank() }
                .take(maxRules)
            if (active.isEmpty()) return ""

            return buildString {
                appendLine("PERSONLIGT MINNE & LÄRDA REGLER FRÅN ANVÄNDAREN (följ dessa om de passar bilden/måltiden):")
                active.forEachIndexed { idx, corr ->
                    val ruleText = corr.learningRule.ifBlank {
                        "När AI gissade '${corr.originalDish}' menade användaren '${corr.correctedDish}' (${corr.userFeedback})."
                    }
                    appendLine("${idx + 1}. $ruleText")
                }
            }.trimEnd()
        }
    }
}

data class AiMealCorrectionResult(
    val correctedAnalysis: AiMealAnalysisResult,
    val learningRule: String,
    val aiReply: String,
    val keywords: String
) {
    fun toAiFoodCorrection(originalDish: String, userFeedback: String): AiFoodCorrection {
        return AiFoodCorrection(
            originalDish = originalDish.trim().take(200),
            userFeedback = userFeedback.trim().take(500),
            correctedDish = correctedAnalysis.mealName.trim().take(200),
            learningRule = learningRule.trim().take(400),
            keywords = keywords.trim().lowercase().take(300)
        )
    }
}

@JsonClass(generateAdapter = true)
internal data class AiCorrectionItemDto(
    val name: String = "",
    val weight_g: Int = 100,
    val calories: Int = 0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0
)

@JsonClass(generateAdapter = true)
internal data class AiCorrectionResponseDto(
    val dish_name: String = "",
    val total_calories: Int? = null,
    val total_protein_g: Double? = null,
    val total_carbs_g: Double? = null,
    val total_fat_g: Double? = null,
    val items: List<AiCorrectionItemDto> = emptyList(),
    val preparation_notes: String = "",
    val learned_rule: String = "",
    val ai_reply: String = "",
    val keywords: String = ""
)

internal object AiMealCorrectionParser {
    private val moshi: Moshi by lazy { Moshi.Builder().build() }
    private val adapter by lazy { moshi.adapter(AiCorrectionResponseDto::class.java) }

    fun parse(rawJson: String, originalDishName: String): AiMealCorrectionResult {
        val dto = adapter.fromJson(rawJson.trim())
            ?: throw IllegalStateException("AI-svaret för rättning kunde inte tolkas.")

        val items = dto.items.mapNotNull { itemDto ->
            val foodName = itemDto.name.trim()
            if (foodName.isBlank()) return@mapNotNull null

            val weightGrams = itemDto.weight_g.coerceAtLeast(1)
            val dbEstimated = SwedishFoodDatabase.calculateNutritionForFood(
                name = foodName,
                weightGrams = weightGrams,
                confidence = 0.96
            )

            val hasAiMacros = itemDto.calories > 0 ||
                itemDto.protein_g > 0.0 ||
                itemDto.carbs_g > 0.0 ||
                itemDto.fat_g > 0.0

            if (hasAiMacros) {
                dbEstimated.copy(
                    calories = itemDto.calories.coerceAtLeast(0),
                    protein = roundOne(itemDto.protein_g.coerceAtLeast(0.0)),
                    carbs = roundOne(itemDto.carbs_g.coerceAtLeast(0.0)),
                    fat = roundOne(itemDto.fat_g.coerceAtLeast(0.0)),
                    confidence = 0.98,
                    isEstimated = false
                )
            } else {
                dbEstimated.copy(confidence = 0.95, isEstimated = false)
            }
        }

        val finalDishName = dto.dish_name.trim().ifBlank { originalDishName.ifBlank { "Rättad måltid" } }
        val sumCalories = items.sumOf { it.calories }
        val sumProtein = roundOne(items.sumOf { it.protein })
        val sumCarbs = roundOne(items.sumOf { it.carbs })
        val sumFat = roundOne(items.sumOf { it.fat })

        val totalCalories = if (sumCalories > 0) sumCalories else (dto.total_calories ?: 0).coerceAtLeast(0)
        val totalProtein = if (sumProtein > 0.0) sumProtein else roundOne((dto.total_protein_g ?: 0.0).coerceAtLeast(0.0))
        val totalCarbs = if (sumCarbs > 0.0) sumCarbs else roundOne((dto.total_carbs_g ?: 0.0).coerceAtLeast(0.0))
        val totalFat = if (sumFat > 0.0) sumFat else roundOne((dto.total_fat_g ?: 0.0).coerceAtLeast(0.0))

        val totalFiber = roundOne(items.sumOf { it.fiber })
        val totalSugar = roundOne(items.sumOf { it.sugar })
        val totalSalt = roundTwo(items.sumOf { it.salt })

        val reasoning = buildString {
            if (dto.ai_reply.isNotBlank()) {
                append(dto.ai_reply.trim())
            }
            if (dto.preparation_notes.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append("Tillagning: ${dto.preparation_notes.trim()}")
            }
        }.ifBlank { "Uppdaterad utifrån din feedback." }

        val analysisResult = AiMealAnalysisResult(
            mealName = finalDishName,
            confidence = "high",
            reasoning = reasoning,
            totalCalories = totalCalories,
            totalProteinG = totalProtein,
            totalCarbsG = totalCarbs,
            totalFatG = totalFat,
            totalFiberG = totalFiber,
            totalSugarG = totalSugar,
            totalSaltG = totalSalt,
            items = items
        )

        val fallbackRule = dto.learned_rule.trim().ifBlank {
            "Kom ihåg att '$finalDishName' ska användas när användaren korrigerar '$originalDishName'."
        }
        val fallbackReply = dto.ai_reply.trim().ifBlank {
            "Uppdaterade måltiden till $finalDishName ($totalCalories kcal) och sparade detta i mitt minne."
        }

        return AiMealCorrectionResult(
            correctedAnalysis = analysisResult,
            learningRule = fallbackRule,
            aiReply = fallbackReply,
            keywords = dto.keywords.trim()
        )
    }

    private fun roundOne(v: Double): Double = (v * 10.0).roundToInt() / 10.0
    private fun roundTwo(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}
