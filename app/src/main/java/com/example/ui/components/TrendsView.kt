package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MetarDto
import com.example.domain.AviationCalculations
import com.example.domain.FlightCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Composable
fun TrendsView(
    history: List<MetarDto>,
    selectedHours: Int,
    isLoading: Boolean,
    unitCelsius: Boolean,
    onHoursSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var isHistoryLogExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Range Filter Chips (6h, 12h, 24h)
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
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Aviation Weather Trends",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(6, 12, 24).forEach { hrs ->
                    FilterChip(
                        selected = selectedHours == hrs,
                        onClick = { onHoursSelected(hrs) },
                        label = { Text("${hrs}h", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("trend_hours_$hrs")
                    )
                }
            }
        }

        if (isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trends_loading_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Loading ${selectedHours}-hour NOAA trend data...",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        if (history.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No historical METAR reports available for the selected ${selectedHours}h window.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        // 1. Pressure Tendency Card
        PressureTendencyCard(history = history)

        // 2. Flight Category Timeline
        FlightCategoryTimelineCard(history = history, isDark = isDark)

        // 3. Temperature & Dew Point Convergence Graph
        TempDewpointChartCard(history = history, unitCelsius = unitCelsius)

        // 4. Wind Speed & Gusts Chart
        WindTrendChartCard(history = history)

        // 5. Expandable Full Historical Observations Log
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_log_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isHistoryLogExpanded = !isHistoryLogExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Historical METAR Logs (${history.size} Reports)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (isHistoryLogExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = isHistoryLogExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        // Display newest first in log
                        history.reversed().forEach { obs ->
                            val timeStr = obs.obsTime?.let {
                                val sdf = SimpleDateFormat("HH:mm 'Z'", Locale.US).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }
                                sdf.format(Date(it * 1000L))
                            } ?: "--"

                            val cat = AviationCalculations.deriveFlightCategory(obs.flightCategory, obs.visib, obs.clouds)
                            val (badgeBg, badgeText) = getFlightCategoryColors(cat, isDark)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = cat.label,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = badgeText
                                        )
                                    }
                                }

                                Text(
                                    text = obs.rawOb ?: "No raw observation",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PressureTendencyCard(history: List<MetarDto>) {
    val validAltimeters = history.mapNotNull {
        AviationCalculations.extractAltimeterInHg(it.rawOb, it.altim)
    }

    if (validAltimeters.size < 2) return

    val first = validAltimeters.first()
    val last = validAltimeters.last()
    val deltaInHg = last - first
    val deltaHpa = (deltaInHg / 0.02953)

    val (tendencyText, tendencyColor, advice) = when {
        deltaHpa < -2.0 -> Triple("Falling Rapidly (Front / Storm Risk)", MaterialTheme.colorScheme.error, "Rapidly dropping pressure suggests an incoming cold front, storm activity, or worsening ceiling.")
        deltaHpa < -0.8 -> Triple("Falling Barometer", Color(0xFFE65100), "Deteriorating weather trend. Monitor altimeter settings closely.")
        deltaHpa > 2.0 -> Triple("Rising Rapidly", Color(0xFF2E7D32), "Rapid clearing or post-frontal high pressure building.")
        deltaHpa > 0.8 -> Triple("Rising Barometer", Color(0xFF2E7D32), "Improving weather trend with stabilizing pressure.")
        else -> Triple("Steady Pressure", MaterialTheme.colorScheme.primary, "Stable atmospheric pressure system.")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pressure_tendency_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Barometric Pressure Tendency",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${if (deltaHpa >= 0) "+" else ""}%.1f hPa (%.2f inHg)".format(deltaHpa, deltaInHg),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = tendencyColor
                )
            }

            Text(
                text = tendencyText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = tendencyColor
            )

            Text(
                text = advice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FlightCategoryTimelineCard(history: List<MetarDto>, isDark: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flight_category_timeline_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Flight Category Timeline",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Timeline bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                history.forEach { obs ->
                    val cat = AviationCalculations.deriveFlightCategory(obs.flightCategory, obs.visib, obs.clouds)
                    val (badgeBg, _) = getFlightCategoryColors(cat, isDark)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(badgeBg)
                    )
                }
            }

            // Time markers (earliest -> latest)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val earliest = history.firstOrNull()?.obsTime?.let {
                    SimpleDateFormat("HH:mm'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(it * 1000L))
                } ?: ""
                val latest = history.lastOrNull()?.obsTime?.let {
                    SimpleDateFormat("HH:mm'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(it * 1000L))
                } ?: ""

                Text(text = "Past: $earliest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Latest: $latest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TempDewpointChartCard(history: List<MetarDto>, unitCelsius: Boolean) {
    val tempPoints = history.mapNotNull { it.temp }
    val dewpPoints = history.mapNotNull { it.dewp }

    if (tempPoints.isEmpty() || dewpPoints.isEmpty()) return

    val latestTemp = tempPoints.last()
    val latestDewp = dewpPoints.last()
    val latestSpread = latestTemp - latestDewp
    val isFogRisk = latestSpread <= 2.5

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("temp_dewpoint_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Temp vs. Dew Point Convergence",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem(color = Color(0xFFE65100), label = "Temp")
                    LegendItem(color = Color(0xFF00838F), label = "Dewpoint")
                }
            }

            // Canvas Line Chart
            DualLineChart(
                line1 = tempPoints,
                line2 = dewpPoints,
                color1 = Color(0xFFE65100),
                color2 = Color(0xFF00838F),
                unitCelsius = unitCelsius,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            // Spread warning indicator if converging
            if (isFogRisk) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Temperature & Dewpoint converging (Spread: %.1f°C). High probability of low clouds, radiation fog, or ground mist.".format(latestSpread),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBF360C)
                    )
                }
            }
        }
    }
}

@Composable
private fun WindTrendChartCard(history: List<MetarDto>) {
    val speeds = history.map { it.wspd ?: 0.0 }
    val gusts = history.map { it.wgst ?: 0.0 }

    if (speeds.all { it == 0.0 } && gusts.all { it == 0.0 }) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wind_trend_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Wind Speed & Gusts (kt)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, label = "Sustained")
                    LegendItem(color = MaterialTheme.colorScheme.error, label = "Gusts")
                }
            }

            DualLineChart(
                line1 = speeds,
                line2 = gusts,
                color1 = MaterialTheme.colorScheme.primary,
                color2 = MaterialTheme.colorScheme.error,
                unitCelsius = false, // unit not applicable, kt
                unitSuffix = " kt",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DualLineChart(
    line1: List<Double>,
    line2: List<Double>,
    color1: Color,
    color2: Color,
    unitCelsius: Boolean,
    unitSuffix: String? = null,
    modifier: Modifier = Modifier
) {
    val allValues = line1 + line2
    val minVal = (allValues.minOrNull() ?: 0.0).toFloat()
    val maxVal = (allValues.maxOrNull() ?: 30.0).toFloat().coerceAtLeast(minVal + 5f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padY = 20f
        val padX = 15f
        val usableW = w - 2 * padX
        val usableH = h - 2 * padY

        val range = (maxVal - minVal).coerceAtLeast(1f)

        fun getPoint(index: Int, total: Int, value: Double): Offset {
            val x = padX + (index.toFloat() / (total - 1).coerceAtLeast(1)) * usableW
            val y = padY + usableH - ((value.toFloat() - minVal) / range) * usableH
            return Offset(x, y)
        }

        // Draw horizontal grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val gy = padY + (usableH / gridLines) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.35f),
                start = Offset(padX, gy),
                end = Offset(w - padX, gy),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw line 2 (e.g. Dew point or gusts)
        if (line2.isNotEmpty()) {
            val path2 = Path()
            line2.forEachIndexed { idx, v ->
                val p = getPoint(idx, line2.size, v)
                if (idx == 0) path2.moveTo(p.x, p.y) else path2.lineTo(p.x, p.y)
            }
            drawPath(path2, color = color2, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            line2.forEachIndexed { idx, v ->
                val p = getPoint(idx, line2.size, v)
                drawCircle(color = color2, radius = 3.5.dp.toPx(), center = p)
            }
        }

        // Draw line 1 (e.g. Temp or Sustained wind)
        if (line1.isNotEmpty()) {
            val path1 = Path()
            line1.forEachIndexed { idx, v ->
                val p = getPoint(idx, line1.size, v)
                if (idx == 0) path1.moveTo(p.x, p.y) else path1.lineTo(p.x, p.y)
            }
            drawPath(path1, color = color1, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            line1.forEachIndexed { idx, v ->
                val p = getPoint(idx, line1.size, v)
                drawCircle(color = color1, radius = 4.dp.toPx(), center = p)
            }
        }
    }
}
