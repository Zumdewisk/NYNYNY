package com.example.ui.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.FoodItem
import com.example.data.model.FoodEntryType
import com.example.data.model.FoodIngredient
import com.example.data.model.FoodUnit
import com.example.data.model.MealItem
import com.example.data.model.SwedishFoodDatabase
import com.example.data.model.SwedishFoodEntry
import com.example.data.repository.AiNutritionService
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroCarbs
import com.example.ui.theme.MacroFat
import com.example.ui.theme.MacroProtein
import com.example.ui.util.formatMacroDecimal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualFoodEntryScreen(
    currentDate: String,
    initialMealType: String = "lunch",
    aiNutritionService: AiNutritionService? = null,
    onSaveMeal: (MealItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sök, 1 = Eget livsmedel
    var selectedMealType by remember { mutableStateOf(initialMealType) }

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<FoodEntryType?>(null) }
    var foodForPortionConfig by remember { mutableStateOf<SwedishFoodEntry?>(null) }
    var searchVersion by remember { mutableIntStateOf(0) }
    var isAiSearching by remember { mutableStateOf(false) }
    var aiSearchMessage by remember { mutableStateOf<String?>(null) }

    // Custom food state
    var customName by remember { mutableStateOf("") }
    var customBrand by remember { mutableStateOf("") }
    var customKcal by remember { mutableStateOf("") }
    var customProtein by remember { mutableStateOf("") }
    var customCarbs by remember { mutableStateOf("") }
    var customFat by remember { mutableStateOf("") }

    // Filtered food items (reacts to searchQuery, selectedTypeFilter, and dynamic updates)
    val searchResults = remember(searchQuery, selectedTypeFilter, searchVersion) {
        SwedishFoodDatabase.search(searchQuery, selectedTypeFilter)
    }

    val performGlobalAiSearch: (String) -> Unit = { query ->
        val cleanQuery = query.trim()
        if (cleanQuery.isNotEmpty() && !isAiSearching) {
            isAiSearching = true
            aiSearchMessage = "Söker i livsmedelsdatabasen efter \"$cleanQuery\"..."
            coroutineScope.launch {
                try {
                    val service = aiNutritionService
                    if (service != null) {
                        val result = service.searchGlobalFoodDatabase(cleanQuery)
                        result.onSuccess { items ->
                            if (items.isNotEmpty()) {
                                SwedishFoodDatabase.addDynamicFoods(items)
                                searchVersion++
                                aiSearchMessage = "Hittade och lade till ${items.size} varor i databasen!"
                            } else {
                                aiSearchMessage = "Inga resultat hittades för \"$cleanQuery\"."
                            }
                        }.onFailure { _ ->
                            val fallback = AiNutritionService.getSmartSearchFallback(cleanQuery)
                            SwedishFoodDatabase.addDynamicFoods(fallback)
                            searchVersion++
                            aiSearchMessage = "Lade till ${fallback.size} förslag i databasen."
                        }
                    } else {
                        val fallback = AiNutritionService.getSmartSearchFallback(cleanQuery)
                        SwedishFoodDatabase.addDynamicFoods(fallback)
                        searchVersion++
                        aiSearchMessage = "Lade till ${fallback.size} förslag i databasen."
                    }
                } catch (e: Exception) {
                    aiSearchMessage = "Kunde inte genomföra sökningen: ${e.message}"
                } finally {
                    isAiSearching = false
                }
            }
        }
    }

    // Auto-search: when user types (e.g. 'jordnötssmör' or any product) and pauses, auto-query global database if local hits are few
    androidx.compose.runtime.LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 3) {
            kotlinx.coroutines.delay(650)
            val currentLocalHits = SwedishFoodDatabase.search(q, selectedTypeFilter)
            if (currentLocalHits.size <= 2 && !isAiSearching) {
                performGlobalAiSearch(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logga mat manuellt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Meal Category FilterChips (Frukost, Lunch, Middag, Mellanmål)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "frukost" to "Frukost",
                    "lunch" to "Lunch",
                    "middag" to "Middag",
                    "mellanmal" to "Mellanmål"
                ).forEach { (catKey, label) ->
                    FilterChip(
                        selected = selectedMealType == catKey,
                        onClick = { selectedMealType = catKey },
                        label = { Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Tabs: Sök i databas vs Eget livsmedel
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = EmeraldPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sök mat och varumärken", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Eget livsmedel", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
                )
            }

            if (selectedTab == 0) {
                // Search Input Field with IME Search action
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        aiSearchMessage = null
                    },
                    placeholder = { Text("Sök vara, maträtt eller varumärke...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAiSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF8B5CF6)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    aiSearchMessage = null
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Rensa")
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { performGlobalAiSearch(searchQuery) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("food_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Sub-filter chips (FlowRow / wrap)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("Alla", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldDark
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == FoodEntryType.SUPERMARKET,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == FoodEntryType.SUPERMARKET) null else FoodEntryType.SUPERMARKET },
                        label = { Text("🛒 Matbutik", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF0284C7)
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == FoodEntryType.MEAL,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == FoodEntryType.MEAL) null else FoodEntryType.MEAL },
                        label = { Text("🍲 Måltider", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF059669).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF059669)
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == FoodEntryType.BRAND,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == FoodEntryType.BRAND) null else FoodEntryType.BRAND },
                        label = { Text("🏷️ Varumärken", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF2563EB)
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == FoodEntryType.FASTFOOD,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == FoodEntryType.FASTFOOD) null else FoodEntryType.FASTFOOD },
                        label = { Text("🍔 Snabbmat", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFDC2626)
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == FoodEntryType.BASIC,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == FoodEntryType.BASIC) null else FoodEntryType.BASIC },
                        label = { Text("🥦 Basvaror", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFD97706)
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == FoodEntryType.DRINK,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == FoodEntryType.DRINK) null else FoodEntryType.DRINK },
                        label = { Text("🥤 Drycker", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF06B6D4).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF0891B2)
                        )
                    )
                    FilterChip(
                        selected = selectedTypeFilter == FoodEntryType.GLOBAL_AI,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == FoodEntryType.GLOBAL_AI) null else FoodEntryType.GLOBAL_AI },
                        label = { Text("✨ AI-sök", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF8B5CF6)
                        )
                    )
                }

                // AI Global Search Banner (Lifesum & Store Mode)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable(enabled = !isAiSearching && searchQuery.isNotBlank()) {
                            performGlobalAiSearch(searchQuery)
                        }
                        .testTag("global_ai_search_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAiSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF8B5CF6)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAiSearching) "Söker i livsmedelsdatabasen..." else "AI-livsmedelsdatabas (butik och globalt)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "Hämta träffar för \"$searchQuery\" från svenska butiker och världen"
                                } else {
                                    "Sök bland varor, rätter och varumärken från svenska butiker och världen"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isAiSearching && searchQuery.isNotBlank()) {
                            Button(
                                onClick = { performGlobalAiSearch(searchQuery) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Sök AI", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }

                if (aiSearchMessage != null) {
                    Text(
                        text = aiSearchMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Results counter
                Text(
                    text = "${searchResults.size} träffar (tryck för att välja portion eller mängd):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Results list or Empty State
                if (searchResults.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Hittade inte '$searchQuery' lokalt" else "Inga livsmedel hittades",
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Vår globala AI-databas täcker all mat i hela världen – inklusive svenska och internationella restauranger, märken och rätter.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotBlank()) {
                                Button(
                                    onClick = { performGlobalAiSearch(searchQuery) },
                                    enabled = !isAiSearching,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.testTag("ai_search_empty_state_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sök '$searchQuery' i globala databasen")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults, key = { it.id }) { food ->
                            FoodResultCard(
                                food = food,
                                onClick = { foodForPortionConfig = food }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            } else {
                // Custom food tab
                CustomFoodEntryForm(
                    currentDate = currentDate,
                    selectedMealType = selectedMealType,
                    customName = customName,
                    onCustomNameChange = { customName = it },
                    customBrand = customBrand,
                    onCustomBrandChange = { customBrand = it },
                    customKcal = customKcal,
                    onCustomKcalChange = { customKcal = it },
                    customProtein = customProtein,
                    onCustomProteinChange = { customProtein = it },
                    customCarbs = customCarbs,
                    onCustomCarbsChange = { customCarbs = it },
                    customFat = customFat,
                    onCustomFatChange = { customFat = it },
                    onSaveMeal = { meal ->
                        // Save to Firestore and Room
                        coroutineScope.launch {
                            try {
                                AppDatabase.getDatabase(context).foodItemDao().insert(FoodItem.fromMealItem(meal))
                            } catch (_: Exception) {}
                        }
                        onSaveMeal(meal)
                    }
                )
            }
        }
    }

    // Modal Bottom Sheet / Inspector for Portion and Quantity Selection
    if (foodForPortionConfig != null) {
        PortionQuantityConfigSheet(
            food = foodForPortionConfig!!,
            selectedMealType = selectedMealType,
            currentDate = currentDate,
            onDismiss = { foodForPortionConfig = null },
            onConfirmSave = { meal ->
                coroutineScope.launch {
                    try {
                        AppDatabase.getDatabase(context).foodItemDao().insert(FoodItem.fromMealItem(meal))
                    } catch (_: Exception) {}
                }
                foodForPortionConfig = null
                onSaveMeal(meal)
            }
        )
    }
}

/**
 * High-quality food card showing brand tag, meal type, portion guide, and per-100g macros.
 */
@Composable
private fun FoodResultCard(
    food: SwedishFoodEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("food_item_${food.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type & Brand Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(food.itemType.badgeColorHex).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            if (food.itemType == FoodEntryType.GLOBAL_AI) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(food.itemType.badgeColorHex),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = food.itemType.label,
                                color = Color(food.itemType.badgeColorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Brand Pill if available
                    if (!food.brand.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = food.brand,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Calories per 100g or 100ml
                val unitLabel = if (food.itemType == FoodEntryType.DRINK) "100ml" else "100g"
                Text(
                    text = "${food.caloriesPer100g} kcal/$unitLabel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Food Name
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Portion description
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${food.standardPortionDescription} (${food.standardPortionGrams} g)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Macro summary per 100g
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "P: ${com.example.ui.util.formatMacroDecimal(food.proteinPer100g)}g",
                    fontSize = 11.sp,
                    color = MacroProtein,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "K: ${com.example.ui.util.formatMacroDecimal(food.carbsPer100g)}g",
                    fontSize = 11.sp,
                    color = MacroCarbs,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "F: ${com.example.ui.util.formatMacroDecimal(food.fatPer100g)}g",
                    fontSize = 11.sp,
                    color = MacroFat,
                    fontWeight = FontWeight.Bold
                )
                if (food.fiberPer100g > 0) {
                    Text(
                        text = "Fiber: ${com.example.ui.util.formatMacroDecimal(food.fiberPer100g)}g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Comprehensive Portion and Quantity Selection Modal Bottom Sheet.
 * Allows choosing:
 * 1. "Normal portion" (medelportion baserat på vad en normal människa äter, t.ex. 350g)
 * 2. "Vikt" (g, hg, kg)
 * 3. "Volym" (dl, ml, l, msk, tsk)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortionQuantityConfigSheet(
    food: SwedishFoodEntry,
    selectedMealType: String,
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirmSave: (MealItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Unit category: 0 = Portion, 1 = Vikt (Vikt), 2 = Volym (Volym)
    var unitMode by remember { mutableIntStateOf(0) }

    // Portion multiplier (0.5, 1.0, 1.5, 2.0 or custom)
    var portionMultiplierStr by remember { mutableStateOf("1") }

    // Weight inputs
    var weightUnit by remember { mutableStateOf(FoodUnit.GRAM) }
    var weightAmountStr by remember { mutableStateOf(food.standardPortionGrams.toString()) }

    // Volume inputs
    var volumeUnit by remember { mutableStateOf(food.defaultUnit.takeIf { it.category == "volym" } ?: FoodUnit.DECILITER) }
    var volumeAmountStr by remember {
        val safeDensity = food.densityGramsPerMl.coerceAtLeast(0.1)
        val initialDl = (food.standardPortionGrams / (100.0 * safeDensity))
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", initialDl))
    }

    // Active meal type override
    var activeMealType by remember { mutableStateOf(selectedMealType) }

    // Calculate effective grams based on selected unit mode
    val effectiveGrams = remember(unitMode, portionMultiplierStr, weightUnit, weightAmountStr, volumeUnit, volumeAmountStr) {
        when (unitMode) {
            0 -> {
                val multiplier = portionMultiplierStr.replace(',', '.').toDoubleOrNull() ?: 1.0
                food.calculateGrams(FoodUnit.PORTION, multiplier)
            }
            1 -> {
                val amount = weightAmountStr.replace(',', '.').toDoubleOrNull() ?: 0.0
                food.calculateGrams(weightUnit, amount)
            }
            2 -> {
                val amount = volumeAmountStr.replace(',', '.').toDoubleOrNull() ?: 0.0
                food.calculateGrams(volumeUnit, amount)
            }
            else -> food.standardPortionGrams
        }
    }

    // Calculated ingredient nutrition
    val currentIngredient = remember(effectiveGrams) {
        food.toIngredient(effectiveGrams)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header: Title, Brand, Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!food.brand.isNullOrBlank()) {
                        Text(
                            text = food.brand.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${food.category} • ${food.caloriesPer100g} kcal per 100g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Stäng")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Meal Type Selector
            Text(
                text = "Måltidskategori:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "frukost" to "Frukost",
                    "lunch" to "Lunch",
                    "middag" to "Middag",
                    "mellanmal" to "Mellanmål"
                ).forEach { (catKey, label) ->
                    FilterChip(
                        selected = activeMealType == catKey,
                        onClick = { activeMealType = catKey },
                        label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Unit Mode Selector (Portion vs Vikt vs Volym)
            Text(
                text = "Välj hur du vill ange mängden:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option 1: Portion
                UnitModeCard(
                    icon = Icons.Default.Restaurant,
                    title = "Normal portion",
                    subtitle = "Medelstorlek",
                    isSelected = unitMode == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { unitMode = 0 }
                )

                // Option 2: Vikt
                UnitModeCard(
                    icon = Icons.Default.Scale,
                    title = "Vikt",
                    subtitle = "Gram / hg / kg",
                    isSelected = unitMode == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { unitMode = 1 }
                )

                // Option 3: Volym
                UnitModeCard(
                    icon = Icons.Default.LocalDrink,
                    title = "Volym",
                    subtitle = "dl / ml / msk",
                    isSelected = unitMode == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { unitMode = 2 }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Inputs based on Selected Unit Mode
            when (unitMode) {
                0 -> {
                    // --- PORTION MODE ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1 normal portion är ${food.standardPortionGrams} g (${food.standardPortionDescription})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Baserad på vad en vuxen människa normalt äter som en medelportion.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Quick Multiplier Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("0.5", "1", "1.5", "2").forEach { mult ->
                                    val isCurrent = portionMultiplierStr == mult
                                    OutlinedButton(
                                        onClick = { portionMultiplierStr = mult },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isCurrent) EmeraldPrimary else Color.Transparent,
                                            contentColor = if (isCurrent) Color.Black else MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(1.dp, if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("${mult}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Stepper & Text Input for Portion Count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val cur = portionMultiplierStr.replace(',', '.').toDoubleOrNull() ?: 1.0
                                        val next = (cur - 0.25).coerceAtLeast(0.25)
                                        portionMultiplierStr = String.format(java.util.Locale.US, "%.2f", next).trimEnd('0').trimEnd('.')
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minska portion")
                                }

                                OutlinedTextField(
                                    value = portionMultiplierStr,
                                    onValueChange = { portionMultiplierStr = it },
                                    label = { Text("Antal portioner") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                IconButton(
                                    onClick = {
                                        val cur = portionMultiplierStr.replace(',', '.').toDoubleOrNull() ?: 1.0
                                        val next = cur + 0.25
                                        portionMultiplierStr = String.format(java.util.Locale.US, "%.2f", next).trimEnd('0').trimEnd('.')
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Öka portion")
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // --- WEIGHT MODE (g, hg, kg) ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Weight Unit Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(FoodUnit.GRAM, FoodUnit.HEKTO, FoodUnit.KILO).forEach { u ->
                                    FilterChip(
                                        selected = weightUnit == u,
                                        onClick = { weightUnit = u },
                                        label = { Text(u.label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = weightAmountStr,
                                    onValueChange = { weightAmountStr = it },
                                    label = { Text("Vikt i ${weightUnit.shortLabel}") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }

                            // Quick adjustment buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(50, 100, 150, 200, 300).forEach { gramsPreset ->
                                    OutlinedButton(
                                        onClick = {
                                            weightUnit = FoodUnit.GRAM
                                            weightAmountStr = gramsPreset.toString()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("${gramsPreset} g", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // --- VOLUME MODE (dl, ml, l, msk, tsk) ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Volume Unit Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(FoodUnit.DECILITER, FoodUnit.MILLILITER, FoodUnit.MATSKED, FoodUnit.TESKED, FoodUnit.LITER).forEach { u ->
                                    FilterChip(
                                        selected = volumeUnit == u,
                                        onClick = { volumeUnit = u },
                                        label = { Text(u.shortLabel, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = volumeAmountStr,
                                onValueChange = { volumeAmountStr = it },
                                label = { Text("Volym i ${volumeUnit.shortLabel}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Text(
                                text = "Motsvarar ca $effectiveGrams gram (baserat på livsmedlets densitet).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Live Nutrition Calculation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Beräknat näringsinnehåll",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "För $effectiveGrams gram",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${currentIngredient.calories} kcal",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroStatItem(label = "Protein", value = "${formatMacroDecimal(currentIngredient.protein)}g", color = MacroProtein)
                        MacroStatItem(label = "Kolhydrater", value = "${formatMacroDecimal(currentIngredient.carbs)}g", color = MacroCarbs)
                        MacroStatItem(label = "Fett", value = "${formatMacroDecimal(currentIngredient.fat)}g", color = MacroFat)
                        MacroStatItem(label = "Fiber", value = "${formatMacroDecimal(currentIngredient.fiber)}g", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm & Save Button
            Button(
                onClick = {
                    val unitLabel = when (unitMode) {
                        0 -> "$portionMultiplierStr portion ($effectiveGrams g)"
                        1 -> "$weightAmountStr ${weightUnit.shortLabel}"
                        2 -> "$volumeAmountStr ${volumeUnit.shortLabel} ($effectiveGrams g)"
                        else -> "$effectiveGrams g"
                    }

                    val meal = MealItem(
                        date = currentDate,
                        mealType = activeMealType,
                        name = "${food.displayName} ($unitLabel)",
                        calories = currentIngredient.calories,
                        protein = currentIngredient.protein,
                        carbs = currentIngredient.carbs,
                        fat = currentIngredient.fat,
                        fiber = currentIngredient.fiber,
                        sugar = currentIngredient.sugar,
                        salt = currentIngredient.salt,
                        itemsJson = FoodIngredient.serializeList(listOf(currentIngredient)),
                        aiAnalyzed = false
                    )
                    onConfirmSave(meal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_log_food_button"),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Logga i dagboken (${currentIngredient.calories} kcal)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Clickable card for selecting unit mode (Portion, Vikt, Volym)
 */
@Composable
private fun UnitModeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.5.dp, if (isSelected) EmeraldPrimary else Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Single item in macro breakdown
 */
@Composable
private fun MacroStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Form for manually entering custom foods
 */
@Composable
private fun CustomFoodEntryForm(
    currentDate: String,
    selectedMealType: String,
    customName: String,
    onCustomNameChange: (String) -> Unit,
    customBrand: String,
    onCustomBrandChange: (String) -> Unit,
    customKcal: String,
    onCustomKcalChange: (String) -> Unit,
    customProtein: String,
    onCustomProteinChange: (String) -> Unit,
    customCarbs: String,
    onCustomCarbsChange: (String) -> Unit,
    customFat: String,
    onCustomFatChange: (String) -> Unit,
    onSaveMeal: (MealItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = customName,
                onValueChange = onCustomNameChange,
                label = { Text("Maträtt / Livsmedelsnamn") },
                placeholder = { Text("T.ex. Hemlagad fiskgratäng") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = customBrand,
                onValueChange = onCustomBrandChange,
                label = { Text("Varumärke / Tillverkare (valfritt)") },
                placeholder = { Text("T.ex. Hemlagat, Felix, ICA") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = customKcal,
                onValueChange = onCustomKcalChange,
                label = { Text("Kalorier (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customProtein,
                    onValueChange = onCustomProteinChange,
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = customCarbs,
                    onValueChange = onCustomCarbsChange,
                    label = { Text("Kolhydrater (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = customFat,
                    onValueChange = onCustomFatChange,
                    label = { Text("Fett (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (customName.isNotBlank()) {
                        val kcal = customKcal.toIntOrNull() ?: 0
                        val p = customProtein.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val c = customCarbs.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val f = customFat.replace(',', '.').toDoubleOrNull() ?: 0.0

                        val fullName = if (customBrand.isNotBlank()) "$customBrand $customName" else customName

                        val ingredient = FoodIngredient(
                            name = fullName,
                            amountGrams = 100,
                            calories = kcal,
                            protein = p,
                            carbs = c,
                            fat = f
                        )

                        val meal = MealItem(
                            date = currentDate,
                            mealType = selectedMealType,
                            name = fullName,
                            calories = kcal,
                            protein = p,
                            carbs = c,
                            fat = f,
                            itemsJson = FoodIngredient.serializeList(listOf(ingredient)),
                            aiAnalyzed = false
                        )
                        onSaveMeal(meal)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                ),
                enabled = customName.isNotBlank() && customKcal.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Spara i matdagboken", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
