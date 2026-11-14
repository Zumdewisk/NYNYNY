package com.example.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CalorieOver
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroCarbs
import com.example.ui.theme.MacroFat
import com.example.ui.theme.MacroProtein
import com.example.ui.theme.MintAccent
import kotlin.math.roundToInt

@Composable
fun CalorieCircularProgress(
    consumedCalories: Int,
    targetCalories: Int,
    modifier: Modifier = Modifier
) {
    val safeTarget = targetCalories.coerceAtLeast(1)
    val rawPercent = ((consumedCalories.toFloat() / safeTarget.toFloat()) * 100f).roundToInt()
    val progress = (consumedCalories.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)

    val isOverTarget = consumedCalories > targetCalories
    val deltaCalories = if (isOverTarget) {
        consumedCalories - targetCalories
    } else {
        (targetCalories - consumedCalories).coerceAtLeast(0)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 850),
        label = "calorie_progress"
    )

    Box(
        modifier = modifier.size(196.dp),
        contentAlignment = Alignment.Center
    ) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val arcBrush = if (isOverTarget) {
            Brush.sweepGradient(listOf(CalorieOver, Color(0xFFF97316), CalorieOver))
        } else {
            Brush.sweepGradient(listOf(EmeraldDark, EmeraldPrimary, EmeraldLight, MintAccent, EmeraldDark))
        }

        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 15.dp.toPx()

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated Foreground Arc
            if (animatedProgress > 0f) {
                drawArc(
                    brush = arcBrush,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isOverTarget) CalorieOver.copy(alpha = 0.14f) else EmeraldPrimary.copy(alpha = 0.14f)
            ) {
                Text(
                    text = if (isOverTarget) "ÖVER MÅL" else "KVAR IDAG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = if (isOverTarget) CalorieOver else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$deltaCalories",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.ExtraBold,
                color = if (isOverTarget) CalorieOver else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "kcal • $rawPercent% av mål",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MacroProgressBar(
    name: String,
    consumed: Double,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val roundedConsumed = consumed.roundToInt()
    val safeTarget = target.coerceAtLeast(1)
    val progress = (consumed.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    val remaining = (target - roundedConsumed).coerceAtLeast(0)
    val over = (roundedConsumed - target).coerceAtLeast(0)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 650),
        label = "macro_$name"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top label row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Consumed / Target cleanly stacked so numbers never wrap awkwardly
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$roundedConsumed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "/ ${target}g",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = color,
                trackColor = color.copy(alpha = 0.16f),
                strokeCap = StrokeCap.Round
            )

            Text(
                text = when {
                    over > 0 -> "+${over}g över"
                    remaining == 0 && roundedConsumed > 0 -> "Mål nått"
                    else -> "${remaining}g kvar"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (over > 0) CalorieOver else color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MacroSummaryRow(
    consumedProtein: Double,
    targetProtein: Int,
    consumedCarbs: Double,
    targetCarbs: Int,
    consumedFat: Double,
    targetFat: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MacroProgressBar(
            name = "Protein",
            consumed = consumedProtein,
            target = targetProtein,
            color = MacroProtein,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        MacroProgressBar(
            name = "Kolhydrater",
            consumed = consumedCarbs,
            target = targetCarbs,
            color = MacroCarbs,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        MacroProgressBar(
            name = "Fett",
            consumed = consumedFat,
            target = targetFat,
            color = MacroFat,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}
