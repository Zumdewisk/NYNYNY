package com.example.ui.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodIngredient
import com.example.data.model.FoodUnit
import com.example.data.model.Recipe
import com.example.data.model.SwedishFoodDatabase
import com.example.data.model.SwedishFoodEntry
import com.example.data.repository.AiNutritionService
import com.example.ui.theme.CalorieOver
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroCarbs
import com.example.ui.theme.MacroFat
import com.example.ui.theme.MacroFiber
import com.example.ui.theme.MacroProtein
import com.example.ui.util.formatMacroDecimal
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBuilderSheet(
    existingRecipe: Recipe? = null,
    aiNutritionService: AiNutritionService?,
    onDismiss: () -> Unit,
    onSaveRecipe: (Recipe) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var recipeName by remember(existingRecipe) { mutableStateOf(existingRecipe?.name ?: "") }
    var recipeDescription by remember(existingRecipe) { mutableStateOf(existingRecipe?.description ?: "") }
    var servings by remember(existingRecipe) { mutableIntStateOf((existingRecipe?.servings ?: 2).coerceIn(1, 50)) }
    var ingredients by remember(existingRecipe) { mutableStateOf(existingRecipe?.ingredients() ?: emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var searchVersion by remember { mutableIntStateOf(0) }
    var isAiSearching by remember { mutableStateOf(false) }
    var aiStatusText by remember { mutableStateOf<String?>(null) }

    var foodForAmountDialog by remember { mutableStateOf<SwedishFoodEntry?>(null) }

    val matchingFoods = remember(searchQuery, searchVersion) {
        if (searchQuery.isBlank()) {
            SwedishFoodDatabase.search("").take(8)
        } else {
            SwedishFoodDatabase.search(searchQuery).take(15)
        }
    }

    val totalCalories = remember(ingredients) { ingredients.sumOf { it.calories } }
    val totalProtein = remember(ingredients) { ingredients.sumOf { it.protein } }
    val totalCarbs = remember(ingredients) { ingredients.sumOf { it.carbs } }
    val totalFat = remember(ingredients) { ingredients.sumOf { it.fat } }
    val totalFiber = remember(ingredients) { ingredients.sumOf { it.fiber } }

    val perServingKcal = remember(totalCalories, servings) {
        (totalCalories.toDouble() / servings.coerceAtLeast(1)).roundToInt()
    }
    val perServingProtein = remember(totalProtein, servings) { totalProtein / servings.coerceAtLeast(1) }
    val perServingCarbs = remember(totalCarbs, servings) { totalCarbs / servings.coerceAtLeast(1) }
    val perServingFat = remember(totalFat, servings) { totalFat / servings.coerceAtLeast(1) }
    val perServingFiber = remember(totalFiber, servings) { totalFiber / servings.coerceAtLeast(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingRecipe == null) "Skapa nytt recept" else "Redigera recept",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Stäng receptskapare")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = recipeName,
                        onValueChange = { recipeName = it },
                        label = { Text("Receptnamn (t.ex. Proteinpannkakor)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recipe_name_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = recipeDescription,
                        onValueChange = { recipeDescription = it },
                        label = { Text("Beskrivning / anteckning (valfritt)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Servings Stepper
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Antal portioner",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Näringsvärden räknas om per portion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (servings > 1) servings-- },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("recipe_servings_minus")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Minska portioner")
                                    }
                                }
                                Text(
                                    text = "$servings st",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (servings < 50) servings++ },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("recipe_servings_plus")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Öka portioner",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Nutrition Per Serving Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Näring per portion",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Totalt: $totalCalories kcal ($servings port)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MacroMiniStat("Kcal", "$perServingKcal", MaterialTheme.colorScheme.primary)
                                MacroMiniStat("Protein", "${formatMacroDecimal(perServingProtein)}g", MacroProtein)
                                MacroMiniStat("Kolh.", "${formatMacroDecimal(perServingCarbs)}g", MacroCarbs)
                                MacroMiniStat("Fett", "${formatMacroDecimal(perServingFat)}g", MacroFat)
                                MacroMiniStat("Fiber", "${formatMacroDecimal(perServingFiber)}g", MacroFiber)
                            }
                        }
                    }
                }

                // Added Ingredients Section
                item {
                    Text(
                        text = "Ingredienser i receptet (${ingredients.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (ingredients.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Inga ingredienser tillagda ännu. Sök i den svenska livsmedelsdatabasen nedan för att lägga till.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    itemsIndexed(ingredients) { idx, ing ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ing.name} (${ing.amountGrams} g)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${ing.calories} kcal • P ${formatMacroDecimal(ing.protein)}g • K ${formatMacroDecimal(ing.carbs)}g • F ${formatMacroDecimal(ing.fat)}g",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        ingredients = ingredients.toMutableList().apply { removeAt(idx) }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Ta bort ${ing.name}",
                                        tint = CalorieOver
                                    )
                                }
                            }
                        }
                    }
                }

                // Swedish Food Database Search
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lägg till livsmedel från svensk databas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Sök t.ex. havregryn, kvarg, kyckling, ägg...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Rensa sökning")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_ingredient_search_input")
                    )

                    if (searchQuery.trim().length >= 2 && aiNutritionService != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val q = searchQuery.trim()
                                if (q.isNotEmpty() && !isAiSearching) {
                                    isAiSearching = true
                                    aiStatusText = "Söker efter \"$q\" med AI..."
                                    coroutineScope.launch {
                                        val res = aiNutritionService.searchGlobalFoodDatabase(q)
                                        res.onSuccess { found ->
                                            if (found.isNotEmpty()) {
                                                SwedishFoodDatabase.addDynamicFoods(found)
                                                searchVersion++
                                                aiStatusText = "Lade till ${found.size} träffar i databasen."
                                            } else {
                                                aiStatusText = "Inga extra träffar hittades."
                                            }
                                        }.onFailure { err ->
                                            aiStatusText = "Kunde inte söka: ${err.localizedMessage ?: "Okänt fel"}"
                                        }
                                        isAiSearching = false
                                    }
                                }
                            },
                            enabled = !isAiSearching,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isAiSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Sök \"$searchQuery\" med AI-databas")
                        }
                    }

                    if (!aiStatusText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiStatusText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Search result rows
                items(matchingFoods.size) { index ->
                    val entry = matchingFoods[index]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { foodForAmountDialog = entry }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${entry.caloriesPer100g} kcal/100g • P ${formatMacroDecimal(entry.proteinPer100g)}g • K ${formatMacroDecimal(entry.carbsPer100g)}g • F ${formatMacroDecimal(entry.fatPer100g)}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.16f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Lägg till ${entry.displayName}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp).size(18.dp)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val canSave = recipeName.isNotBlank() && ingredients.isNotEmpty()
            Button(
                onClick = {
                    val saved = Recipe.fromIngredients(
                        recipeId = existingRecipe?.recipeId ?: "",
                        userId = existingRecipe?.userId ?: "",
                        name = recipeName,
                        description = recipeDescription,
                        servings = servings,
                        ingredients = ingredients
                    )
                    onSaveRecipe(saved)
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 6.dp)
                    .testTag("save_recipe_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingRecipe == null) "Spara recept ($perServingKcal kcal/port)" else "Uppdatera recept",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    val selectedFood = foodForAmountDialog
    if (selectedFood != null) {
        IngredientAmountDialog(
            entry = selectedFood,
            onDismiss = { foodForAmountDialog = null },
            onConfirm = { ingredient ->
                ingredients = ingredients + ingredient
                foodForAmountDialog = null
            }
        )
    }
}

@Composable
private fun IngredientAmountDialog(
    entry: SwedishFoodEntry,
    onDismiss: () -> Unit,
    onConfirm: (FoodIngredient) -> Unit
) {
    val units = listOf(
        FoodUnit.GRAM,
        FoodUnit.PORTION,
        FoodUnit.PIECE,
        FoodUnit.DECILITER,
        FoodUnit.MATSKED,
        FoodUnit.TESKED,
        FoodUnit.MILLILITER
    )
    var selectedUnit by remember { mutableStateOf(FoodUnit.GRAM) }
    var amountText by remember { mutableStateOf("100") }

    val parsedAmount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val previewIngredient = remember(entry, selectedUnit, parsedAmount) {
        if (parsedAmount > 0.0) entry.toIngredient(selectedUnit, parsedAmount) else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = entry.displayName,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Välj enhet och mängd",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    units.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = {
                                selectedUnit = unit
                                amountText = when (unit) {
                                    FoodUnit.GRAM -> "100"
                                    FoodUnit.MILLILITER -> "100"
                                    else -> "1"
                                }
                            },
                            label = { Text(unit.shortLabel, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Mängd (${selectedUnit.label})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (previewIngredient != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${previewIngredient.amountGrams} g • ${previewIngredient.calories} kcal",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "P: ${formatMacroDecimal(previewIngredient.protein)}g • K: ${formatMacroDecimal(previewIngredient.carbs)}g • F: ${formatMacroDecimal(previewIngredient.fat)}g",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (previewIngredient != null) {
                        onConfirm(previewIngredient)
                    }
                },
                enabled = previewIngredient != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                )
            ) {
                Text("Lägg till", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
internal fun MacroMiniStat(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
