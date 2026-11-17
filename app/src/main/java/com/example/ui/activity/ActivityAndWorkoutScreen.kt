package com.example.ui.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutType
import com.example.health.DailyActivityEnergySummary
import com.example.health.HealthConnectAvailability
import com.example.health.HealthConnectDailyData
import com.example.health.HealthConnectWorkoutSummary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroCarbs
import com.example.ui.theme.MacroProtein
import com.example.ui.theme.MacroWater
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityAndWorkoutScreen(
    currentDate: String,
    formattedDateDisplay: String,
    userWeightKg: Double,
    healthConnectAvailability: HealthConnectAvailability,
    hasHealthConnectReadPermission: Boolean,
    hasHealthConnectWritePermission: Boolean,
    isSyncingHealthConnect: Boolean,
    healthConnectData: HealthConnectDailyData?,
    activityEnergySummary: DailyActivityEnergySummary,
    manualWorkouts: List<WorkoutLog>,
    onRequestHealthConnectPermissions: () -> Unit,
    onOpenHealthConnectSettings: () -> Unit,
    onRefreshHealthConnect: () -> Unit,
    onLogWorkout: (
        workoutType: WorkoutType,
        title: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        notes: String,
        syncToHealthConnect: Boolean
    ) -> Unit,
    onDeleteWorkout: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf(WorkoutType.STRENGTH) }
    var customTitle by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("45") }
    var caloriesText by remember { mutableStateOf("") }
    var caloriesManuallyEdited by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }
    var syncToHealthConnect by remember(hasHealthConnectWritePermission) {
        mutableStateOf(hasHealthConnectWritePermission)
    }

    val durationMinutes = durationText.toIntOrNull()?.coerceIn(1, 600) ?: 0
    val autoEstimatedCalories = remember(selectedType, userWeightKg, durationMinutes) {
        if (durationMinutes > 0) selectedType.estimateCalories(userWeightKg, durationMinutes) else 0
    }

    LaunchedEffect(selectedType, durationMinutes, userWeightKg, caloriesManuallyEdited) {
        if (!caloriesManuallyEdited) {
            caloriesText = if (autoEstimatedCalories > 0) autoEstimatedCalories.toString() else ""
        }
    }

    val effectiveCalories = caloriesText.toIntOrNull()?.coerceIn(0, 10000) ?: autoEstimatedCalories
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("sv-SE")) }

    val externalHcWorkouts = remember(healthConnectData) {
        healthConnectData?.workouts?.filter { !it.isFromNaringAi }.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Träning & Aktivitet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = formattedDateDisplay,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tillbaka"
                        )
                    }
                },
                actions = {
                    if (healthConnectAvailability == HealthConnectAvailability.AVAILABLE && hasHealthConnectReadPermission) {
                        IconButton(
                            onClick = onRefreshHealthConnect,
                            enabled = !isSyncingHealthConnect,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (isSyncingHealthConnect) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Synka Health Connect",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            // 1. Dynamic Calorie Goal Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.38f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EmeraldPrimary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Dynamiskt kalorimål",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Uppdateras automatiskt av steg & träning",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EnergyBreakdownPill(
                                label = "Basmål",
                                value = "${activityEnergySummary.baseCalorieGoal}",
                                suffix = "kcal",
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            EnergyBreakdownPill(
                                label = "Förbränt",
                                value = "+${activityEnergySummary.totalBurnedCalories}",
                                suffix = "kcal",
                                color = MacroCarbs,
                                modifier = Modifier.weight(1f)
                            )
                            Text("=", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            EnergyBreakdownPill(
                                label = "Nytt mål",
                                value = "${activityEnergySummary.adjustedCalorieGoal}",
                                suffix = "kcal",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Google Health Connect Status & Live Metrics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MacroWater.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HealthAndSafety,
                                        contentDescription = null,
                                        tint = MacroWater,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Google Health Connect",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            healthConnectAvailability == HealthConnectAvailability.NOT_SUPPORTED ->
                                                "Ej tillgänglig på enheten"
                                            healthConnectAvailability == HealthConnectAvailability.UPDATE_REQUIRED ->
                                                "Uppdatering / installation krävs"
                                            hasHealthConnectReadPermission ->
                                                "Ansluten • Steg, kalorier & pass"
                                            else -> "Tillstånd krävs för automatisk synk"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (healthConnectAvailability == HealthConnectAvailability.AVAILABLE && hasHealthConnectReadPermission) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.16f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Aktiv",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        when {
                            healthConnectAvailability == HealthConnectAvailability.AVAILABLE && hasHealthConnectReadPermission -> {
                                val steps = healthConnectData?.steps ?: 0L
                                val stepGoal = 10_000L
                                val stepProgress = (steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
                                val hcActive = healthConnectData?.activeCaloriesBurned ?: 0

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(1.dp, MacroWater.copy(alpha = 0.25f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                                    contentDescription = null,
                                                    tint = MacroWater,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Steg idag",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = numberFormat.format(steps),
                                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            LinearProgressIndicator(
                                                progress = { stepProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp),
                                                color = MacroWater,
                                                trackColor = MacroWater.copy(alpha = 0.16f),
                                                strokeCap = StrokeCap.Round
                                            )
                                            Text(
                                                text = if (hcActive == 0 && activityEnergySummary.stepEstimatedCalories > 0) {
                                                    "≈ ${activityEnergySummary.stepEstimatedCalories} kcal uppskattat"
                                                } else {
                                                    "Mål: 10 000 steg"
                                                },
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(1.dp, MacroCarbs.copy(alpha = 0.28f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = MacroCarbs,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Aktiva kcal (HC)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "$hcActive kcal",
                                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "${externalHcWorkouts.size} importerade pass",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (healthConnectData?.errorMessage != null) {
                                    Text(
                                        text = healthConnectData.errorMessage,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                OutlinedButton(
                                    onClick = onOpenHealthConnectSettings,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Hantera Health Connect-behörigheter", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            healthConnectAvailability == HealthConnectAvailability.AVAILABLE -> {
                                Text(
                                    text = "Ge Zevra tillstånd att läsa dina steg, aktiva kalorier och träningspass så justeras ditt dagliga kalorimål automatiskt.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onRequestHealthConnectPermissions,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("grant_health_connect_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Anslut Health Connect", fontWeight = FontWeight.Bold)
                                }
                            }

                            healthConnectAvailability == HealthConnectAvailability.UPDATE_REQUIRED -> {
                                Text(
                                    text = "Health Connect behöver installeras eller uppdateras via Google Play för att synka steg och träningspass.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = onOpenHealthConnectSettings,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Öppna Health Connect i Play Butik", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            else -> {
                                Text(
                                    text = "Google Health Connect är inte tillgängligt på den här enheten. Du kan fortfarande logga egna träningspass nedan så uppdateras kalorimålet direkt.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Log Custom Workout Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Logga eget träningspass",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Uppskattad förbränning beräknas från din vikt (${userWeightKg.toInt()} kg)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Workout type selection grid
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WorkoutType.entries.forEach { type ->
                                val isSelected = type == selectedType
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedType = type
                                        caloriesManuallyEdited = false
                                    },
                                    label = {
                                        Text(
                                            text = type.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = workoutIcon(type),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("workout_type_${type.id}")
                                )
                            }
                        }

                        // Quick Duration Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Tid (snabbval)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(15, 30, 45, 60, 90).forEach { mins ->
                                    val selected = durationMinutes == mins
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                durationText = mins.toString()
                                                caloriesManuallyEdited = false
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$mins min",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Duration and Calories side by side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = durationText,
                                onValueChange = { input ->
                                    durationText = input.filter { it.isDigit() }.take(3)
                                    caloriesManuallyEdited = false
                                },
                                label = { Text("Tid (min)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("workout_duration_input")
                            )

                            OutlinedTextField(
                                value = caloriesText,
                                onValueChange = { input ->
                                    caloriesText = input.filter { it.isDigit() }.take(4)
                                    caloriesManuallyEdited = true
                                },
                                label = { Text("Kalorier (kcal)") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("workout_calories_input")
                            )
                        }

                        if (caloriesManuallyEdited && autoEstimatedCalories > 0) {
                            Text(
                                text = "Uppskattad förbränning: $autoEstimatedCalories kcal • Tryck för att återställa",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        caloriesManuallyEdited = false
                                        caloriesText = autoEstimatedCalories.toString()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Namn på pass (valfritt)") },
                            placeholder = { Text("T.ex. ${selectedType.displayName} på gymmet") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Anteckning (valfritt)") },
                            placeholder = { Text("T.ex. 5 km i lugnt tempo") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (healthConnectAvailability == HealthConnectAvailability.AVAILABLE && hasHealthConnectWritePermission) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Spara även till Health Connect",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Dela passet och kalorierna med andra hälsoappar",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = syncToHealthConnect,
                                    onCheckedChange = { syncToHealthConnect = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (durationMinutes > 0) {
                                    val titleToSave = customTitle.trim().ifBlank { selectedType.displayName }
                                    onLogWorkout(
                                        selectedType,
                                        titleToSave,
                                        durationMinutes,
                                        effectiveCalories,
                                        notesText.trim(),
                                        syncToHealthConnect && hasHealthConnectWritePermission
                                    )
                                    customTitle = ""
                                    notesText = ""
                                    caloriesManuallyEdited = false
                                }
                            },
                            enabled = durationMinutes > 0 && effectiveCalories > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_workout_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "Logga träningspass (+$effectiveCalories kcal)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // 4. Logged Workouts & Health Connect Sessions for Selected Date
            item {
                Text(
                    text = "Registrerade träningspass ($currentDate)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (manualWorkouts.isEmpty() && externalHcWorkouts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Inga träningspass loggade denna dag",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Logga ett pass ovan eller synka från Health Connect.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(manualWorkouts, key = { "manual_${it.logId.ifBlank { it.id }}" }) { workout ->
                    ManualWorkoutCard(
                        workout = workout,
                        onDelete = { onDeleteWorkout(workout.logId.ifBlank { workout.id }) }
                    )
                }

                items(externalHcWorkouts, key = { "hc_${it.id}_${it.startTimeMillis}" }) { hcWorkout ->
                    HealthConnectWorkoutCard(workout = hcWorkout)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun EnergyBreakdownPill(
    label: String,
    value: String,
    suffix: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum")
        )
        Text(
            text = suffix,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ManualWorkoutCard(
    workout: WorkoutLog,
    onDelete: () -> Unit
) {
    val type = remember(workout.workoutType) { WorkoutType.fromId(workout.workoutType) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = workoutIcon(type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = workout.title.ifBlank { type.displayName },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${workout.durationMinutes} min • ${workout.caloriesBurned} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MacroCarbs
                    )
                    if (workout.notes.isNotBlank()) {
                        Text(
                            text = workout.notes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (workout.syncedToHealthConnect) {
                        Text(
                            text = "Synkad till Health Connect",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Radera träningspass",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HealthConnectWorkoutCard(workout: HealthConnectWorkoutSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MacroWater.copy(alpha = 0.38f))
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MacroWater.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MacroWater,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = workout.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (workout.caloriesBurned > 0) {
                            "${workout.durationMinutes} min • ${workout.caloriesBurned} kcal"
                        } else {
                            "${workout.durationMinutes} min • Health Connect"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MacroProtein,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MacroWater.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "Health Connect",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MacroWater,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun workoutIcon(type: WorkoutType): ImageVector {
    return when (type) {
        WorkoutType.STRENGTH -> Icons.Default.FitnessCenter
        WorkoutType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
        WorkoutType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
        WorkoutType.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
        WorkoutType.HIIT -> Icons.Default.Bolt
        WorkoutType.SWIMMING -> Icons.Default.Pool
        WorkoutType.YOGA -> Icons.Default.SelfImprovement
        WorkoutType.OTHER -> Icons.Default.SportsGymnastics
    }
}
