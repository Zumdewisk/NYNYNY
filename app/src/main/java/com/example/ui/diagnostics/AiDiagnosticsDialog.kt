package com.example.ui.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.diagnostics.AiDiagnosticsService
import com.example.data.diagnostics.AppCheckDiagnostics
import com.example.data.diagnostics.VisionTestResult
import com.example.data.repository.AiNutritionService
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.launch

@Composable
fun AiDiagnosticsDialog(
    aiNutritionService: AiNutritionService,
    lastCameraBitmap: Bitmap? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val diagnosticsService = remember { AiDiagnosticsService(aiNutritionService) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: App Check, 1: Resursbild, 2: Kamerabild

    var appCheckDiag by remember { mutableStateOf<AppCheckDiagnostics?>(null) }
    var isCheckingAppCheck by remember { mutableStateOf(false) }

    var resourceTestResult by remember { mutableStateOf<VisionTestResult?>(null) }
    var isRunningResourceTest by remember { mutableStateOf(false) }

    var cameraTestResult by remember { mutableStateOf<VisionTestResult?>(null) }
    var isRunningCameraTest by remember { mutableStateOf(false) }

    var nonFoodTestResult by remember { mutableStateOf<VisionTestResult?>(null) }
    var isRunningNonFoodTest by remember { mutableStateOf(false) }
    var jsonTestOutput by remember { mutableStateOf<String?>(null) }

    var customDebugTokenInput by remember { mutableStateOf("") }

    // Auto-run App Check test on initial open
    LaunchedEffect(Unit) {
        isCheckingAppCheck = true
        try {
            appCheckDiag = diagnosticsService.verifyAppCheck(context)
        } finally {
            isCheckingAppCheck = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI- och App Check-diagnostik",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Felsök Firebase, App Check, AI och bildflöde",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Stäng")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = EmeraldDark
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("App Check", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Testbild", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Kamera", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("JSON-test", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> AppCheckTabContent(
                            appCheckDiag = appCheckDiag,
                            isLoading = isCheckingAppCheck,
                            customDebugToken = customDebugTokenInput,
                            onCustomDebugTokenChange = { customDebugTokenInput = it },
                            onSaveCustomToken = {
                                val saved = diagnosticsService.saveCustomDebugToken(context, customDebugTokenInput)
                                Toast.makeText(
                                    context,
                                    if (saved) "Debug-token sparad & App Check ominstallerat!" else "Kunde inte spara token",
                                    Toast.LENGTH_SHORT
                                ).show()
                                coroutineScope.launch {
                                    isCheckingAppCheck = true
                                    try {
                                        appCheckDiag = diagnosticsService.verifyAppCheck(context)
                                    } finally {
                                        isCheckingAppCheck = false
                                    }
                                }
                            },
                            onRefresh = {
                                coroutineScope.launch {
                                    isCheckingAppCheck = true
                                    try {
                                        appCheckDiag = diagnosticsService.verifyAppCheck(context)
                                    } finally {
                                        isCheckingAppCheck = false
                                    }
                                }
                            }
                        )

                        1 -> ResourceVisionTabContent(
                            testResult = resourceTestResult,
                            isLoading = isRunningResourceTest,
                            onRunTest = {
                                coroutineScope.launch {
                                    isRunningResourceTest = true
                                    try {
                                        resourceTestResult = diagnosticsService.runStandaloneResourceTest(
                                            context = context,
                                            resId = R.drawable.img_salmon_plate,
                                            resourceName = "Lax & potatis (R.drawable.img_salmon_plate)"
                                        )
                                    } finally {
                                        isRunningResourceTest = false
                                    }
                                }
                            }
                        )

                        2 -> CameraVisionTabContent(
                            testResult = cameraTestResult,
                            lastCameraBitmap = lastCameraBitmap,
                            isLoading = isRunningCameraTest,
                            onRunTest = {
                                if (lastCameraBitmap != null) {
                                    coroutineScope.launch {
                                        isRunningCameraTest = true
                                        try {
                                            cameraTestResult = diagnosticsService.runCameraBitmapTest(
                                                bitmap = lastCameraBitmap,
                                                label = "Faktisk Kamerabild (${lastCameraBitmap.width}x${lastCameraBitmap.height})"
                                            )
                                        } finally {
                                            isRunningCameraTest = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Ingen kamerabild tillgänglig ännu. Ta en bild med kameran först!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        3 -> SpecialTestsTabContent(
                            nonFoodResult = nonFoodTestResult,
                            isLoadingNonFood = isRunningNonFoodTest,
                            onRunNonFoodTest = {
                                coroutineScope.launch {
                                    isRunningNonFoodTest = true
                                    try {
                                        nonFoodTestResult = diagnosticsService.runNonFoodTest()
                                    } finally {
                                        isRunningNonFoodTest = false
                                    }
                                }
                            },
                            jsonTestReport = jsonTestOutput,
                            onRunJsonTest = {
                                jsonTestOutput = diagnosticsService.testJsonAndEdgeCases()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action: Run All Tests in Sequence
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val report = buildString {
                                appendLine("=== ZEVRA AI-DIAGNOSTIKRAPPORT ===")
                                appendLine("App Check: ${appCheckDiag?.diagnosis}")
                                appendLine("Standard Token: ${appCheckDiag?.standardTokenDetails}")
                                appendLine("Limited Use Token: ${appCheckDiag?.limitedUseTokenDetails}")
                                appendLine("Resurstest: ${resourceTestResult?.diagnosis}")
                                appendLine("Kameratest: ${cameraTestResult?.diagnosis}")
                            }
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Diagnostics", report))
                            Toast.makeText(context, "Diagnostikrapport kopierad till urklipp!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kopiera logg", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCheckingAppCheck = true
                                try {
                                    appCheckDiag = diagnosticsService.verifyAppCheck(context)
                                } finally {
                                    isCheckingAppCheck = false
                                }

                                isRunningResourceTest = true
                                try {
                                    resourceTestResult = diagnosticsService.runStandaloneResourceTest(
                                        context = context,
                                        resId = R.drawable.img_salmon_plate
                                    )
                                } finally {
                                    isRunningResourceTest = false
                                }

                                if (lastCameraBitmap != null) {
                                    isRunningCameraTest = true
                                    try {
                                        cameraTestResult = diagnosticsService.runCameraBitmapTest(lastCameraBitmap)
                                    } finally {
                                        isRunningCameraTest = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kör alla tester", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCheckTabContent(
    appCheckDiag: AppCheckDiagnostics?,
    isLoading: Boolean,
    customDebugToken: String,
    onCustomDebugTokenChange: (String) -> Unit,
    onSaveCustomToken: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        appCheckDiag == null -> MaterialTheme.colorScheme.surfaceVariant
                        appCheckDiag.standardTokenSuccess && appCheckDiag.limitedUseTokenSuccess -> EmeraldPrimary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (appCheckDiag?.standardTokenSuccess == true) EmeraldPrimary else MaterialTheme.colorScheme.error
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                isLoading -> Icons.Default.Refresh
                                appCheckDiag?.standardTokenSuccess == true -> Icons.Default.CheckCircle
                                else -> Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = if (appCheckDiag?.standardTokenSuccess == true) EmeraldPrimary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLoading) "Verifierar mot Firebase-servrar..." else "App Check-status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = appCheckDiag?.diagnosis ?: "Tryck på uppdateringsknappen nedan för att verifiera App Check mot Firebase.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    if (appCheckDiag?.httpStatusCode != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "HTTP-statuskod: ${appCheckDiag.httpStatusCode}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Detaljerad information:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Projekt: ${appCheckDiag?.projectId ?: "..."}", fontSize = 12.sp)
                    Text("• Byggläge: ${appCheckDiag?.buildVariant ?: "..."}", fontSize = 12.sp)
                    Text("• Debug-token sparad: ${if (appCheckDiag?.hasDebugSecretStored == true) "Ja (${appCheckDiag.debugSecretDisplay})" else "Nej"}", fontSize = 12.sp)
                    Text("• Standard token: ${appCheckDiag?.standardTokenDetails ?: "Ej testad"}", fontSize = 12.sp)
                    Text("• Limited-use token: ${appCheckDiag?.limitedUseTokenDetails ?: "Ej testad"}", fontSize = 12.sp)

                    if (appCheckDiag?.errorMessage != null) {
                        Text(
                            text = "• Fel: ${appCheckDiag.errorMessage}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Manuell hantering av debug-token:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Om du har en registrerad debug-token i Firebase Console kan du klistra in den här och spara:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customDebugToken,
                        onValueChange = onCustomDebugTokenChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Klistra in ny token (dold text)...", fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSaveCustomToken,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Spara och aktivera", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Uppdatera status", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceVisionTabContent(
    testResult: VisionTestResult?,
    isLoading: Boolean,
    onRunTest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_salmon_plate),
                        contentDescription = "Testbild Lax",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("img_salmon_plate.jpg", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Fristående resursbild (lax, potatis, sparris). Används för att testa Gemini Vision utan kamera.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = onRunTest,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                        } else {
                            Text("Kör test", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (testResult != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (testResult.isSuccess) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (testResult.isSuccess) EmeraldPrimary else MaterialTheme.colorScheme.error)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (testResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (testResult.isSuccess) EmeraldPrimary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (testResult.isSuccess) "AI-bildanalys lyckades!" else "AI-bildanalys misslyckades",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(testResult.diagnosis, fontSize = 13.sp)

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("• Svarstid: ${testResult.durationMs} ms", fontSize = 12.sp)
                        Text("• Bilddimensioner: ${testResult.imageWidth} x ${testResult.imageHeight} px (${testResult.imageConfig})", fontSize = 12.sp)
                        Text("• Rå bildstorlek: ${testResult.imageBytes / 1024} KB", fontSize = 12.sp)
                        Text("• Inline JPEG-data verifierad: ${if (testResult.inlineBytesVerified) "Ja (icke-tom)" else "Nej"}", fontSize = 12.sp)
                        Text("• Modell: ${testResult.modelName}", fontSize = 12.sp)

                        if (testResult.isSuccess) {
                            Text("• Identifierad rätt: ${testResult.parsedMealName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• Kalorier: ${testResult.parsedCalories} kcal", fontSize = 12.sp)
                            Text("• Antal ingredienser: ${testResult.parsedIngredientsCount}", fontSize = 12.sp)
                            if (!testResult.rawResponse.isNullOrBlank()) {
                                Text("• AI:ns bedömning: ${testResult.rawResponse}", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        } else {
                            if (testResult.httpStatusCode != null) {
                                Text("• HTTP-statuskod: ${testResult.httpStatusCode}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                            Text("• Felmeddelande: ${testResult.errorMessage}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraVisionTabContent(
    testResult: VisionTestResult?,
    lastCameraBitmap: Bitmap?,
    isLoading: Boolean,
    onRunTest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lastCameraBitmap != null) {
                        Image(
                            bitmap = lastCameraBitmap.asImageBitmap(),
                            contentDescription = "Senaste kamerabild",
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lastCameraBitmap != null) "Kamerabild tillgänglig" else "Ingen bild tagen ännu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (lastCameraBitmap != null) "${lastCameraBitmap.width}x${lastCameraBitmap.height} px (${lastCameraBitmap.config})" else "Gå till kameran och ta en bild, eller kör resursbildtestet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onRunTest,
                        enabled = !isLoading && lastCameraBitmap != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                        } else {
                            Text("Kör test", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (testResult != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (testResult.isSuccess) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (testResult.isSuccess) EmeraldPrimary else MaterialTheme.colorScheme.error)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (testResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (testResult.isSuccess) EmeraldPrimary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (testResult.isSuccess) "Kamerabilden analyserades!" else "Kamerabildsanalysen misslyckades",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(testResult.diagnosis, fontSize = 13.sp)

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("• Svarstid: ${testResult.durationMs} ms", fontSize = 12.sp)
                        Text("• Bildstorlek: ${testResult.imageWidth} x ${testResult.imageHeight} px (${testResult.imageBytes / 1024} KB)", fontSize = 12.sp)
                        Text("• Inline JPEG-data verifierad: ${if (testResult.inlineBytesVerified) "Ja (icke-tom)" else "Nej"}", fontSize = 12.sp)

                        if (testResult.isSuccess) {
                            Text("• Identifierad rätt: ${testResult.parsedMealName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("• Kalorier: ${testResult.parsedCalories} kcal", fontSize = 12.sp)
                            Text("• Ingredienser: ${testResult.parsedIngredientsCount} st", fontSize = 12.sp)
                        } else {
                            if (testResult.httpStatusCode != null) {
                                Text("• HTTP-statuskod: ${testResult.httpStatusCode}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                            Text("• Fel: ${testResult.errorMessage}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialTestsTabContent(
    nonFoodResult: VisionTestResult?,
    isLoadingNonFood: Boolean,
    onRunNonFoodTest: () -> Unit,
    jsonTestReport: String?,
    onRunJsonTest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Non-Food Image Test
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Test på icke-matbild", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Skickar ett icke-mat-mönster (geometriska linjer utan mat) till Gemini för att verifiera att modellen returnerar foods: [] och 'Ingen mat identifierades' utan att hitta på fejkade livsmedel.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onRunNonFoodTest,
                        enabled = !isLoadingNonFood,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoadingNonFood) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyserar icke-matbild...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kör test på icke-mat", fontSize = 12.sp)
                        }
                    }

                    if (nonFoodResult != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (nonFoodResult.parsedIngredientsCount == 0 || nonFoodResult.isSuccess) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (nonFoodResult.parsedIngredientsCount == 0) "✅ Icke-mat hanterades korrekt!" else "Resultat:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text("• Rätt: ${nonFoodResult.parsedMealName}", fontSize = 11.sp)
                                Text("• Identifierade livsmedel: ${nonFoodResult.parsedIngredientsCount ?: 0} st", fontSize = 11.sp)
                                Text("• Kalorier: ${nonFoodResult.parsedCalories ?: 0} kcal", fontSize = 11.sp)
                                Text("• Svarstid: ${nonFoodResult.durationMs} ms", fontSize = 11.sp)
                                Text(nonFoodResult.diagnosis, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Card 2: JSON Validation and Malformed JSON Resilience Test
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. JSON-validering & felhantering", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Verifierar robust parsing av: (A) Flerkomponents-måltid JSON, (B) Icke-mat JSON, och (C) Ogiltig/avbruten JSON utan app-krasch.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onRunJsonTest,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kör JSON-test", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    if (jsonTestReport != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = jsonTestReport,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
