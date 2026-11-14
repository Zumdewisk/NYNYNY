package com.example

import com.example.data.model.FoodEntryType
import com.example.data.model.FoodUnit
import com.example.data.model.SwedishFoodDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SwedishFoodDatabaseTest {

    @Before
    fun setUp() {
        val candidateDirs = listOf(
            File("src/main/assets/foods"),
            File("app/src/main/assets/foods")
        )
        val foodsDir = candidateDirs.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Kunde inte hitta mappen src/main/assets/foods")

        val jsonFiles = foodsDir.listFiles { file -> file.extension.equals("json", ignoreCase = true) }
            ?.sortedBy { it.name }
            .orEmpty()

        val allEntries = jsonFiles.flatMap { file ->
            SwedishFoodDatabase.parseEntriesFromJson(file.readText(Charsets.UTF_8))
        }
        SwedishFoodDatabase.initializeWithEntries(allEntries)
    }

    @Test
    fun `loads all expanded food and drink entries with unique ids`() {
        val candidateDirs = listOf(
            File("src/main/assets/foods"),
            File("app/src/main/assets/foods")
        )
        val foodsDir = candidateDirs.first { it.exists() && it.isDirectory }
        val rawEntries = foodsDir.listFiles { file -> file.extension.equals("json", ignoreCase = true) }
            .orEmpty()
            .flatMap { SwedishFoodDatabase.parseEntriesFromJson(it.readText(Charsets.UTF_8)) }

        val allStatic = SwedishFoodDatabase.foods
        assertEquals("Inga dubbla ID:n får förekomma i matdatabasen", rawEntries.size, allStatic.size)
        assertTrue("Databasen ska ha minst 370 livsmedel och drycker, hade ${allStatic.size}", allStatic.size >= 370)

        val nonNullBarcodes = allStatic.mapNotNull { it.barcode }
        assertEquals("Inga dubbla streckkoder får förekomma", nonNullBarcodes.size, nonNullBarcodes.distinct().size)

        assertTrue(allStatic.any { it.itemType == FoodEntryType.MEAL })
        assertTrue(allStatic.any { it.itemType == FoodEntryType.FASTFOOD })
        assertTrue(allStatic.any { it.itemType == FoodEntryType.BRAND })
        assertTrue(allStatic.any { it.itemType == FoodEntryType.BASIC })
        assertTrue(allStatic.any { it.itemType == FoodEntryType.SUPERMARKET })
        assertTrue(allStatic.any { it.itemType == FoodEntryType.DRINK })
    }

    @Test
    fun `preserves swedish characters and barcode lookup`() {
        val mjolk = SwedishFoodDatabase.findByBarcode("7310865000378")
        assertNotNull(mjolk)
        assertEquals("Standardmjölk 3.0%", mjolk?.name)
        assertEquals(FoodUnit.DECILITER, mjolk?.defaultUnit)

        val peanut = SwedishFoodDatabase.findByBarcode("7311041042301")
        assertNotNull(peanut)
        assertEquals("Jordnötssmör Creamy (100% jordnötter)", peanut?.name)
    }

    @Test
    fun `search and best match work across expanded foods and drinks`() {
        val mealMatch = SwedishFoodDatabase.findBestMatch("Köttbullar")
        assertNotNull(mealMatch)
        assertEquals("meal_kottbullar", mealMatch?.id)

        val kvargHits = SwedishFoodDatabase.search("kvarg")
        assertTrue(kvargHits.size >= 2)

        val ingredient = SwedishFoodDatabase.calculateNutritionForFood("Äpple", 200)
        assertEquals(104, ingredient.calories)
        assertEquals(200, ingredient.amountGrams)

        // Broadened foods coverage
        assertTrue(SwedishFoodDatabase.search("wallenbergare").isNotEmpty())
        assertTrue(SwedishFoodDatabase.search("halloumi").isNotEmpty())
        assertTrue(SwedishFoodDatabase.search("kikärtor").isNotEmpty())
        assertTrue(SwedishFoodDatabase.search("kanelbulle").isNotEmpty())
        assertTrue(SwedishFoodDatabase.search("mandlar").isNotEmpty())
        assertTrue(SwedishFoodDatabase.search("whopper").isNotEmpty())

        // Drinks coverage & DRINK type filtering
        val allDrinks = SwedishFoodDatabase.search("", FoodEntryType.DRINK)
        assertTrue("Dryckeskategorin ska ha minst 50 drycker, hade ${allDrinks.size}", allDrinks.size >= 50)
        assertTrue(SwedishFoodDatabase.search("kaffe", FoodEntryType.DRINK).isNotEmpty())
        assertTrue(SwedishFoodDatabase.search("pepsi", FoodEntryType.DRINK).isNotEmpty())
        assertTrue(SwedishFoodDatabase.search("vin", FoodEntryType.DRINK).isNotEmpty())
        assertTrue(SwedishFoodDatabase.search("smoothie", FoodEntryType.DRINK).isNotEmpty())

        // Beverage heuristic fallback test for unlisted drink
        val unknownDrink = SwedishFoodDatabase.calculateNutritionForFood("Ismatcha specialdryck", 300)
        assertEquals(300, unknownDrink.amountGrams)
        assertTrue(unknownDrink.calories > 0)
    }
}
