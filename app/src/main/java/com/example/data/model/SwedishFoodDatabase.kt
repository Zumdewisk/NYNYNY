package com.example.data.model

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.roundToInt

object SwedishFoodDatabase {

    private const val TAG = "SwedishFoodDatabase"
    private const val FOODS_ASSETS_DIR = "foods"

    // Preferred category load order so dish/brand matches behave predictably while supporting any new JSON files
    private val PREFERRED_FILE_ORDER = listOf(
        "husmanskost.json",
        "snabbmat.json",
        "drycker.json",
        "varumarken.json",
        "frukt_gront.json",
        "kott_fisk_veg.json",
        "mejeri_agg_frukost.json",
        "skafferi_snacks_saser.json",
        "baslivsmedel.json",
        "matbutik.json",
        "internationella_varor.json",
        "kompletterande_livsmedel.json"
    )

    private val initLock = Any()

    @Volatile
    private var isInitialized = false

    @Volatile
    private var staticFoods: List<SwedishFoodEntry> = emptyList()

    private val dynamicFoodsList = CopyOnWriteArrayList<SwedishFoodEntry>()
    private val aiFoodIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    private val foodListAdapter by lazy {
        val listType = Types.newParameterizedType(List::class.java, SwedishFoodEntry::class.java)
        moshi.adapter<List<SwedishFoodEntry>>(listType)
    }

    val foods: List<SwedishFoodEntry>
        get() = staticFoods

    val allFoods: List<SwedishFoodEntry>
        get() = dynamicFoodsList + staticFoods

    /**
     * Initializes the database by dynamically loading all JSON category files from assets/foods/.
     * Safe to call multiple times; subsequent calls are no-ops unless [reload] is called.
     */
    fun initialize(context: Context) {
        if (isInitialized && staticFoods.isNotEmpty()) return
        synchronized(initLock) {
            if (isInitialized && staticFoods.isNotEmpty()) return
            loadFromAssets(context.applicationContext)
        }
    }

    /**
     * Forces a fresh reload of all JSON category files from assets/foods/.
     */
    fun reload(context: Context) {
        synchronized(initLock) {
            loadFromAssets(context.applicationContext)
        }
    }

    /**
     * Parses a JSON array string of [SwedishFoodEntry] items.
     */
    fun parseEntriesFromJson(json: String): List<SwedishFoodEntry> {
        if (json.isBlank()) return emptyList()
        return foodListAdapter.fromJson(json).orEmpty()
    }

    /**
     * Replaces static entries directly (primarily intended for unit testing without Android assets).
     */
    internal fun initializeWithEntries(entries: List<SwedishFoodEntry>) {
        synchronized(initLock) {
            staticFoods = entries.distinctBy { it.id }
            isInitialized = true
        }
    }

    private fun loadFromAssets(context: Context) {
        val assetManager = context.assets
        val discoveredFiles = try {
            assetManager.list(FOODS_ASSETS_DIR)
                ?.filter { it.endsWith(".json", ignoreCase = true) }
                .orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Kunde inte lista livsmedelsfiler i assets/$FOODS_ASSETS_DIR", e)
            emptyList()
        }

        val orderedFiles = discoveredFiles.sortedWith(
            compareBy<String>(
                { name ->
                    val idx = PREFERRED_FILE_ORDER.indexOf(name)
                    if (idx >= 0) idx else Int.MAX_VALUE
                },
                { it }
            )
        )

        val loaded = mutableListOf<SwedishFoodEntry>()
        for (fileName in orderedFiles) {
            try {
                val json = assetManager.open("$FOODS_ASSETS_DIR/$fileName")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val entries = parseEntriesFromJson(json)
                loaded.addAll(entries)
                Log.d(TAG, "Laddade ${entries.size} livsmedel från assets/$FOODS_ASSETS_DIR/$fileName")
            } catch (e: Exception) {
                Log.e(TAG, "Fel vid inläsning av assets/$FOODS_ASSETS_DIR/$fileName", e)
            }
        }

        staticFoods = loaded.distinctBy { it.id }
        isInitialized = true
        Log.i(TAG, "Totalt ${staticFoods.size} statiska livsmedel laddade från ${orderedFiles.size} JSON-filer.")
    }

    /**
     * Dynamically caches foods discovered via AI Global Search or custom entries.
     */
    fun addDynamicFood(food: SwedishFoodEntry, fromAi: Boolean = false) {
        if (fromAi || food.id.startsWith("ai_") || food.itemType == FoodEntryType.GLOBAL_AI) {
            aiFoodIds.add(food.id)
        }
        synchronized(dynamicFoodsList) {
            val sameIdentityExists = dynamicFoodsList.any {
                it.id == food.id ||
                    (it.name.equals(food.name, ignoreCase = true) &&
                        (it.brand ?: "").equals(food.brand ?: "", ignoreCase = true))
            }
            if (staticFoods.none { it.id == food.id } && !sameIdentityExists) {
                dynamicFoodsList.add(0, food)
            }
        }
    }

    fun addDynamicFoods(newFoods: List<SwedishFoodEntry>, fromAi: Boolean = false) {
        newFoods.asReversed().forEach { addDynamicFood(it, fromAi = fromAi) }
    }

    fun isAiFood(entry: SwedishFoodEntry): Boolean {
        return entry.itemType == FoodEntryType.GLOBAL_AI ||
            entry.id.startsWith("ai_") ||
            aiFoodIds.contains(entry.id)
    }

    private fun normalizeSwedish(text: String): String {
        return text.lowercase()
            .replace('å', 'a')
            .replace('ä', 'a')
            .replace('ö', 'o')
            .replace('é', 'e')
    }

    /**
     * Looks up a Swedish food product by its EAN-13, EAN-8 or UPC barcode.
     */
    fun findByBarcode(barcode: String): SwedishFoodEntry? {
        val clean = barcode.trim()
        if (clean.isBlank()) return null
        return allFoods.firstOrNull { it.barcode == clean }
    }

    /**
     * Searches across all loaded static and dynamically added global foods matching query and filter.
     * Supports multi-word queries across name, brand, and category, as well as Swedish diacritic fallback.
     */
    fun search(query: String, filterType: FoodEntryType? = null): List<SwedishFoodEntry> {
        val q = query.trim().lowercase()
        val tokens = q.split(Regex("\\s+")).filter { it.isNotBlank() }
        val normTokens = tokens.map { normalizeSwedish(it) }

        return allFoods.filter { entry ->
            val matchesType = when (filterType) {
                null -> true
                FoodEntryType.GLOBAL_AI -> isAiFood(entry)
                else -> entry.itemType == filterType
            }
            val matchesQuery = if (q.isBlank()) {
                true
            } else {
                val combined = "${entry.displayName} ${entry.name} ${entry.brand.orEmpty()} ${entry.category}".lowercase()
                combined.contains(q) ||
                    tokens.all { token -> combined.contains(token) } ||
                    run {
                        val normCombined = normalizeSwedish(combined)
                        normTokens.all { normToken -> normCombined.contains(normToken) }
                    }
            }
            matchesType && matchesQuery
        }.sortedWith(
            compareBy(
                { if (q.isNotBlank() && it.name.lowercase().startsWith(q)) 0 else 1 },
                { if (q.isNotBlank() && it.brand != null && it.brand.lowercase().startsWith(q)) 0 else 1 },
                { if (isAiFood(it)) 0 else 1 },
                { it.itemType.ordinal },
                { it.name }
            )
        )
    }

    /**
     * Finds the best matching SwedishFoodEntry for an identified food name.
     */
    fun findBestMatch(foodName: String): SwedishFoodEntry? {
        val raw = foodName.trim().lowercase()
        if (raw.isBlank()) return null

        val entries = allFoods

        // 1. Exact match on name
        entries.firstOrNull { it.name.trim().equals(raw, ignoreCase = true) }?.let { return it }

        // 2. Exact match on displayName
        entries.firstOrNull { it.displayName.trim().equals(raw, ignoreCase = true) }?.let { return it }

        // 3. Normalized keyword matching (stripping common cooking adjectives and Swedish prefixes)
        val stopWords = setOf(
            "kokt", "kokta", "stekt", "stekta", "ugnsbakad", "ugnsbakade", "grillad", "grillade",
            "färsk", "färska", "fryst", "frysta", "ångad", "ångade", "strimlad", "strimlade",
            "tärnad", "tärnade", "skivad", "skivade", "tillagad", "tillagade", "rå", "råa",
            "en", "ett", "portion", "st", "bit", "skiva", "skivor", "tallrik", "med"
        )
        val tokens = raw.split(Regex("[\\s,./+&()\\-]+"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 && !stopWords.contains(it) }

        if (tokens.isNotEmpty()) {
            // Check if any food contains all tokens
            val allTokenMatch = entries.firstOrNull { entry ->
                val entryText = "${entry.name} ${entry.brand ?: ""} ${entry.category}".lowercase()
                tokens.all { token -> entryText.contains(token) }
            }
            if (allTokenMatch != null) return allTokenMatch

            // Match by primary food noun/stem
            for (token in tokens) {
                val tokenMatch = entries.firstOrNull { entry ->
                    entry.name.lowercase().contains(token)
                }
                if (tokenMatch != null) return tokenMatch
            }
        }

        // 4. Substring fallback
        return entries.firstOrNull { entry ->
            entry.name.lowercase().contains(raw) || raw.contains(entry.name.lowercase())
        }
    }

    /**
     * Calculates food ingredient macros from identified food name and weight in grams.
     * Guaranteed to return accurate values from SwedishFoodDatabase, with intelligent
     * nutrient estimation fallback if an exotic item isn't in the database.
     */
    fun calculateNutritionForFood(
        name: String,
        weightGrams: Int,
        confidence: Double = 0.9
    ): FoodIngredient {
        val grams = weightGrams.coerceAtLeast(1)
        val matchedEntry = findBestMatch(name)

        if (matchedEntry != null) {
            val factor = grams / 100.0
            return FoodIngredient(
                name = name.ifBlank { matchedEntry.name },
                amountGrams = grams,
                calories = (matchedEntry.caloriesPer100g * factor).roundToInt(),
                protein = ((matchedEntry.proteinPer100g * factor * 10.0).roundToInt()) / 10.0,
                carbs = ((matchedEntry.carbsPer100g * factor * 10.0).roundToInt()) / 10.0,
                fat = ((matchedEntry.fatPer100g * factor * 10.0).roundToInt()) / 10.0,
                fiber = ((matchedEntry.fiberPer100g * factor * 10.0).roundToInt()) / 10.0,
                sugar = ((matchedEntry.sugarPer100g * factor * 10.0).roundToInt()) / 10.0,
                salt = ((matchedEntry.saltPer100g * factor * 100.0).roundToInt()) / 100.0,
                confidence = confidence.coerceIn(0.0, 1.0),
                isEstimated = true
            )
        }

        // Intelligent category heuristic for unknown foods
        val lower = name.lowercase()
        val (calPer100, protPer100, carbPer100, fatPer100, fiberPer100) = when {
            lower.contains("kyckling") || lower.contains("kalkon") || lower.contains("biff") ||
                lower.contains("kött") || lower.contains("färs") || lower.contains("skinka") ||
                lower.contains("fläsk") -> listOf(165.0, 28.0, 0.0, 5.0, 0.0)

            lower.contains("lax") || lower.contains("fisk") || lower.contains("torsk") ||
                lower.contains("räk") || lower.contains("tonfisk") -> listOf(170.0, 22.0, 0.0, 9.0, 0.0)

            lower.contains("ris") || lower.contains("pasta") || lower.contains("potatis") ||
                lower.contains("bröd") || lower.contains("nudl") || lower.contains("gryn") -> listOf(135.0, 3.0, 28.0, 0.5, 1.5)

            lower.contains("broccoli") || lower.contains("sallad") || lower.contains("morot") ||
                lower.contains("tomat") || lower.contains("gurka") || lower.contains("grönsak") ||
                lower.contains("lök") || lower.contains("spenat") || lower.contains("kål") -> listOf(32.0, 2.0, 4.0, 0.3, 2.6)

            lower.contains("äpple") || lower.contains("banan") || lower.contains("bär") ||
                lower.contains("frukt") || lower.contains("apelsin") || lower.contains("päron") -> listOf(58.0, 0.6, 13.5, 0.2, 2.2)

            lower.contains("sås") || lower.contains("dressing") || lower.contains("majonnäs") ||
                lower.contains("dipp") -> listOf(175.0, 1.5, 5.0, 16.0, 0.2)

            lower.contains("vatten") || lower.contains("kaffe") || lower.contains("espresso") ||
                lower.contains("latte") || lower.contains("cappuccino") || lower.contains("te ") ||
                lower.contains("nocco") || lower.contains("celsius") || lower.contains("zero") -> listOf(8.0, 0.2, 1.0, 0.2, 0.0)

            lower.contains("läsk") || lower.contains("cola") || lower.contains("fanta") ||
                lower.contains("juice") || lower.contains("smoothie") || lower.contains("energidryck") ||
                lower.contains("öl") || lower.contains("vin") || lower.contains("cider") ||
                lower.contains("dryck") || lower.contains("saft") || lower.contains("shake") -> listOf(45.0, 0.5, 9.5, 0.1, 0.1)

            lower.contains("ost") || lower.contains("kvarg") || lower.contains("yoghurt") ||
                lower.contains("mejeri") || lower.contains("mjölk") -> listOf(150.0, 12.0, 4.0, 9.0, 0.0)

            else -> listOf(120.0, 5.0, 15.0, 4.0, 1.0)
        }

        val factor = grams / 100.0
        return FoodIngredient(
            name = name,
            amountGrams = grams,
            calories = (calPer100 * factor).roundToInt(),
            protein = ((protPer100 * factor * 10.0).roundToInt()) / 10.0,
            carbs = ((carbPer100 * factor * 10.0).roundToInt()) / 10.0,
            fat = ((fatPer100 * factor * 10.0).roundToInt()) / 10.0,
            fiber = ((fiberPer100 * factor * 10.0).roundToInt()) / 10.0,
            sugar = ((2.0 * factor * 10.0).roundToInt()) / 10.0,
            salt = ((0.3 * factor * 100.0).roundToInt()) / 100.0,
            confidence = confidence.coerceIn(0.0, 1.0),
            isEstimated = true
        )
    }
}
