package com.example

import com.example.data.model.AiFoodCorrection
import com.example.data.model.AiMealCorrectionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCorrectionModelsTest {

    @Test
    fun parse_updatesDishMacrosAndExtractsLearningRule() {
        val rawJson = """
            {
              "dish_name": "Kycklingtacopaj med ris",
              "total_calories": 640,
              "total_protein_g": 46.5,
              "total_carbs_g": 58.0,
              "total_fat_g": 22.0,
              "items": [
                {
                  "name": "Kycklingfärs stekt",
                  "weight_g": 180,
                  "calories": 290,
                  "protein_g": 36.0,
                  "carbs_g": 1.0,
                  "fat_g": 14.0
                },
                {
                  "name": "Ris kokt vitt",
                  "weight_g": 150,
                  "calories": 195,
                  "protein_g": 4.0,
                  "carbs_g": 42.0,
                  "fat_g": 0.5
                }
              ],
              "preparation_notes": "Stekt kycklingfärs i tacokrydda",
              "learned_rule": "Användarens tacopaj med ljust kött ska identifieras som kycklingfärs istället för nötfärs.",
              "ai_reply": "Tack! Jag har ändrat köttet till kycklingfärs och uppdaterat riskvantiteten.",
              "keywords": "tacopaj, kycklingfärs, ris"
            }
        """.trimIndent()

        val result = AiMealCorrectionParser.parse(rawJson, "Nötfärstacos")

        assertEquals("Kycklingtacopaj med ris", result.correctedAnalysis.mealName)
        assertEquals(2, result.correctedAnalysis.items.size)
        assertEquals(485, result.correctedAnalysis.totalCalories)
        assertEquals(40.0, result.correctedAnalysis.totalProteinG, 0.1)
        assertEquals(
            "Användarens tacopaj med ljust kött ska identifieras som kycklingfärs istället för nötfärs.",
            result.learningRule
        )
        assertTrue(result.aiReply.contains("kycklingfärs"))

        val entity = result.toAiFoodCorrection("Nötfärstacos", "Det är kycklingfärs istället för nötfärs")
        assertEquals("Nötfärstacos", entity.originalDish)
        assertEquals("Kycklingtacopaj med ris", entity.correctedDish)
        assertEquals("tacopaj, kycklingfärs, ris", entity.keywords)
    }

    @Test
    fun formatRulesForPrompt_respectsMaxRulesAndFormatsSwedishSection() {
        val rules = (1..20).map { idx ->
            AiFoodCorrection(
                originalDish = "Rätt $idx",
                correctedDish = "Korrigerad $idx",
                learningRule = "Regel nummer $idx"
            )
        }

        val prompt = AiFoodCorrection.formatRulesForPrompt(rules, maxRules = 15)

        assertTrue(prompt.contains("PERSONLIGT MINNE & LÄRDA REGLER"))
        assertTrue(prompt.contains("1. Regel nummer 1"))
        assertTrue(prompt.contains("15. Regel nummer 15"))
        assertTrue(!prompt.contains("16. Regel nummer 16"))
        assertEquals("", AiFoodCorrection.formatRulesForPrompt(emptyList()))
    }
}
