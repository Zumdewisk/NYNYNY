package com.example.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealItem
import com.example.data.model.UserProfile
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroCarbs
import com.example.ui.theme.MacroFat
import com.example.ui.theme.MacroProtein
import com.example.ui.util.formatSwedishDateLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class DaySummary(
    val date: String,
    val displayLabel: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val mealsCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    userProfile: UserProfile?,
    allMeals: List<MealItem>,
    onSelectDay: (String) -> Unit,
    onNavigateToWeight: () -> Unit = {}
) {
    var selectedTimeframe by remember { mutableIntStateOf(0) } // 0 = 7 dagar, 1 = 30 dagar
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val daysCount = if (selectedTimeframe == 0) 7 else 30
    val targetCalories = userProfile?.targetCalories ?: 2200

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val dayLabelFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("sv-SE")) }

    val daySummaries = remember(allMeals, selectedTimeframe) {
        (daysCount - 1 downTo 0).map { daysAgo ->
            val dateObj = today.minusDays(daysAgo.toLong())
            val dateStr = dateObj.format(formatter)
            val label = if (daysCount == 7) {
                dateObj.format(dayLabelFormatter).replace(".", "").replaceFirstChar { it.uppercase() }
            } else {
                "${dateObj.dayOfMonth}/${dateObj.monthValue}"
            }

            val daysMeals = allMeals.filter { it.date == dateStr }
            DaySummary(
                date = dateStr,
                displayLabel = label,
                calories = daysMeals.sumOf { it.calories },
                protein = daysMeals.sumOf { it.protein },
                carbs = daysMeals.sumOf { it.carbs },
                fat = daysMeals.sumOf { it.fat },
                mealsCount = daysMeals.size
            )
        }
    }

    val daysWithMeals = daySummaries.filter { it.calories > 0 }
    val avgCalories = if (daysWithMeals.isNotEmpty()) daysWithMeals.map { it.calories }.average().roundToInt() else 0
    val avgProtein = if (daysWithMeals.isNotEmpty()) daysWithMeals.map { it.protein }.average().roundToInt() else 0
    val avgCarbs = if (daysWithMeals.isNotEmpty()) daysWithMeals.map { it.carbs }.average().roundToInt() else 0
    val avgFat = if (daysWithMeals.isNotEmpty()) daysWithMeals.map { it.fat }.average().roundToInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistik och historik", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weight Tracking Quick Navigation Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNavigateToWeight() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldPrimary.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonitorWeight,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Viktspårning och viktkurva",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Logga din vikt dagligen och följ utvecklingen",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Öppna ›",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Timeframe Tabs (Vecka / Månad)
            item {
                TabRow(
                    selectedTabIndex = selectedTimeframe,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Tab(
                        selected = selectedTimeframe == 0,
                        onClick = {
                            selectedTimeframe = 0
                            selectedBarIndex = null
                        },
                        text = { Text("Senaste 7 dagarna", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTimeframe == 1,
                        onClick = {
                            selectedTimeframe = 1
                            selectedBarIndex = null
                        },
                        text = { Text("Senaste 30 dagarna", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Interactive Calorie Chart Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Kaloriintag",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Snitt: $avgCalories kcal / dag",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Mål: $targetCalories",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Interactive Bar Chart with Canvas Tap Support
                        val maxCal = (daySummaries.maxOfOrNull { it.calories } ?: targetCalories)
                            .coerceAtLeast(targetCalories)
                            .coerceAtLeast(1000)

                        val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant
                        val targetLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .pointerInput(daySummaries) {
                                    detectTapGestures { offset ->
                                        if (daySummaries.isNotEmpty() && size.width > 0) {
                                            val space = size.width.toFloat() / daySummaries.size
                                            val index = (offset.x / space).toInt().coerceIn(0, daySummaries.lastIndex)
                                            selectedBarIndex = if (selectedBarIndex == index) null else index
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barCount = daySummaries.size.coerceAtLeast(1)
                                val space = size.width / barCount
                                val barWidth = (space * 0.62f).coerceAtMost(28.dp.toPx())

                                // Dashed Target line
                                val targetY = size.height - ((targetCalories.toFloat() / maxCal.toFloat()) * (size.height - 20.dp.toPx()))
                                drawLine(
                                    color = targetLineColor,
                                    start = Offset(0f, targetY),
                                    end = Offset(size.width, targetY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )

                                daySummaries.forEachIndexed { i, day ->
                                    val barHeight = if (day.calories == 0) {
                                        6.dp.toPx()
                                    } else {
                                        ((day.calories.toFloat() / maxCal.toFloat()) * (size.height - 24.dp.toPx()))
                                            .coerceAtLeast(8.dp.toPx())
                                    }
                                    val left = (i * space) + (space - barWidth) / 2
                                    val top = size.height - barHeight

                                    val isSelected = selectedBarIndex == i
                                    val barColor = when {
                                        isSelected -> EmeraldPrimary
                                        day.calories == 0 -> emptyBarColor
                                        day.calories > targetCalories + 200 -> MacroCarbs
                                        else -> EmeraldPrimary.copy(alpha = 0.72f)
                                    }

                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }
                        }

                        // Day Labels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (daySummaries.size <= 7) {
                                daySummaries.forEachIndexed { idx, day ->
                                    Text(
                                        text = day.displayLabel,
                                        fontSize = 11.sp,
                                        color = if (selectedBarIndex == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selectedBarIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { selectedBarIndex = idx }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(daySummaries.first().displayLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(daySummaries[daySummaries.size / 2].displayLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(daySummaries.last().displayLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Selected Bar Details Box
                        selectedBarIndex?.let { idx ->
                            val day = daySummaries[idx]
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectDay(day.date) },
                                color = EmeraldPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = formatSwedishDateLabel(day.date),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${day.calories} kcal • P: ${day.protein.roundToInt()}g • K: ${day.carbs.roundToInt()}g • F: ${day.fat.roundToInt()}g",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Öppna dagbok ›",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Average Nutrition Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Genomsnittligt näringsintag",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatNutrientPill(
                                title = "Protein",
                                value = "${avgProtein} g",
                                color = MacroProtein,
                                modifier = Modifier.weight(1f)
                            )
                            StatNutrientPill(
                                title = "Kolhydrater",
                                value = "${avgCarbs} g",
                                color = MacroCarbs,
                                modifier = Modifier.weight(1f)
                            )
                            StatNutrientPill(
                                title = "Fett",
                                value = "${avgFat} g",
                                color = MacroFat,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val hasMeals = daysWithMeals.isNotEmpty()
                        val totalMacroGrams = if (hasMeals) (avgProtein + avgCarbs + avgFat).coerceAtLeast(1) else 1
                        val pPercent = if (hasMeals) ((avgProtein.toFloat() / totalMacroGrams) * 100f).roundToInt() else 0
                        val cPercent = if (hasMeals) ((avgCarbs.toFloat() / totalMacroGrams) * 100f).roundToInt() else 0
                        val fPercent = if (hasMeals) (100 - pPercent - cPercent).coerceAtLeast(0) else 0

                        if (hasMeals) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            ) {
                                Box(modifier = Modifier.weight(pPercent.coerceAtLeast(1).toFloat()).fillMaxSize().background(MacroProtein))
                                Box(modifier = Modifier.weight(cPercent.coerceAtLeast(1).toFloat()).fillMaxSize().background(MacroCarbs))
                                Box(modifier = Modifier.weight(fPercent.coerceAtLeast(1).toFloat()).fillMaxSize().background(MacroFat))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Protein: $pPercent%", fontSize = 11.sp, color = MacroProtein, fontWeight = FontWeight.SemiBold)
                                Text("Kolhydrater: $cPercent%", fontSize = 11.sp, color = MacroCarbs, fontWeight = FontWeight.SemiBold)
                                Text("Fett: $fPercent%", fontSize = 11.sp, color = MacroFat, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Inga måltider registrerade för denna period ännu.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Måltidshistorik header
            item {
                Text(
                    text = "Tidigare dagar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // List of recorded days
            val recordedDays = daySummaries.filter { it.calories > 0 }.reversed()
            if (recordedDays.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Logga måltider för att se din dagliga historik här.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(recordedDays) { day ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectDay(day.date) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formatSwedishDateLabel(day.date),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${day.mealsCount} ${if (day.mealsCount == 1) "måltid" else "måltider"} • P: ${day.protein.roundToInt()}g • K: ${day.carbs.roundToInt()}g • F: ${day.fat.roundToInt()}g",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${day.calories} kcal",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatNutrientPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum")
            )
        }
    }
}
