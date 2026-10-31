package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MetarDto
import com.example.domain.AviationCalculations
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RunwayCrosswindCard(
    metar: MetarDto,
    runwayNumber: Int,
    onRunwayChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val windDirInt = metar.wdir?.toIntOrNull()
    val windSpeed = metar.wspd ?: 0.0
    val result = AviationCalculations.calculateCrosswind(
        windDirDeg = windDirInt,
        windSpeedKnots = windSpeed,
        runwayNumber = runwayNumber
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("runway_crosswind_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Runway Wind Components",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val formattedRunway = String.format("%02d", runwayNumber)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RWY $formattedRunway (${runwayNumber * 10}°)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Runway Selector controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        val next = if (runwayNumber <= 1) 36 else runwayNumber - 1
                        onRunwayChange(next)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .size(36.dp)
                        .testTag("runway_minus")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease Runway",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Slider(
                    value = runwayNumber.toFloat(),
                    onValueChange = { onRunwayChange(it.toInt().coerceIn(1, 36)) },
                    valueRange = 1f..36f,
                    steps = 34,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .testTag("runway_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(
                    onClick = {
                        val next = if (runwayNumber >= 36) 1 else runwayNumber + 1
                        onRunwayChange(next)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .size(36.dp)
                        .testTag("runway_plus")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase Runway",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Graphic visualization and stats side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Runway & Wind Compass graphic
                RunwayGraphic(
                    runwayHeading = runwayNumber * 10,
                    windDir = windDirInt,
                    modifier = Modifier.size(120.dp)
                )

                // Computed Values Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Crosswind
                    val crosswindColor = when {
                        result.crosswindKnots > 15.0 -> MaterialTheme.colorScheme.error
                        result.crosswindKnots > 10.0 -> Color(0xFFE65100)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "CROSSWIND",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val directionLabel = if (result.crosswindKnots > 0.0) {
                            if (result.isLeftCrosswind) "from Left" else "from Right"
                        } else "None"
                        Text(
                            text = "%.1f kt %s".format(result.crosswindKnots, directionLabel),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = crosswindColor
                        )
                    }

                    // Headwind / Tailwind
                    val isTailwind = result.headwindKnots < 0.0
                    val hwTwLabel = if (isTailwind) "TAILWIND" else "HEADWIND"
                    val hwTwColor = if (isTailwind) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = hwTwLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isTailwind) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.1f kt".format(kotlin.math.abs(result.headwindKnots)),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = hwTwColor
                        )
                    }
                }
            }

            // Advisory footnote
            if (result.crosswindKnots > 15.0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "High crosswind component exceeds typical maximum demonstrated crosswind for many light aircraft (e.g. C172 15 kt).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun RunwayGraphic(
    runwayHeading: Int,
    windDir: Int?,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = Color(0xFFE53935)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 4f

        // Outer Compass Ring
        drawCircle(
            color = surfaceColor,
            radius = radius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // North Marker
        drawLine(
            color = Color.Gray,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y - radius + 8.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        // Runway Strip (oriented at runwayHeading relative to North)
        rotate(runwayHeading.toFloat(), pivot = center) {
            val rwyWidth = 20.dp.toPx()
            val rwyLength = radius * 1.5f
            drawRoundRect(
                color = Color(0xFF37474F),
                topLeft = Offset(center.x - rwyWidth / 2f, center.y - rwyLength / 2f),
                size = androidx.compose.ui.geometry.Size(rwyWidth, rwyLength),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Runway Centerline Dash
            val dashH = 6.dp.toPx()
            val gap = 4.dp.toPx()
            var currY = center.y - rwyLength / 2f + 4.dp.toPx()
            while (currY < center.y + rwyLength / 2f - 8.dp.toPx()) {
                drawLine(
                    color = Color.White,
                    start = Offset(center.x, currY),
                    end = Offset(center.x, currY + dashH),
                    strokeWidth = 2.dp.toPx()
                )
                currY += dashH + gap
            }
        }

        // Wind Vector Arrow (pointing in direction wind travels)
        if (windDir != null) {
            rotate((windDir + 180).toFloat(), pivot = center) {
                val arrowLen = radius * 0.85f
                drawLine(
                    color = primaryColor,
                    start = Offset(center.x, center.y - arrowLen),
                    end = Offset(center.x, center.y + arrowLen / 3f),
                    strokeWidth = 3.5.dp.toPx()
                )
                // Arrow head at center + arrowLen / 3
                val headY = center.y + arrowLen / 3f
                val headPath = Path().apply {
                    moveTo(center.x, headY + 8.dp.toPx())
                    lineTo(center.x - 6.dp.toPx(), headY)
                    lineTo(center.x + 6.dp.toPx(), headY)
                    close()
                }
                drawPath(headPath, color = primaryColor)
            }
        }
    }
}
