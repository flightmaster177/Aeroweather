package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MetarDto
import com.example.domain.AviationCalculations
import kotlin.math.roundToInt

@Composable
fun StatsGrid(
    metar: MetarDto,
    unitCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wind Card with Directional Arrow
            val windSpeed = metar.wspd?.roundToInt()
            val windDir = metar.wdir
            val windGust = metar.wgst?.roundToInt()

            val windValue = if (windSpeed != null) "$windSpeed kt" else "Calm"
            val windSub = buildString {
                if (!windDir.isNullOrBlank()) {
                    append(if (windDir == "VRB") "Variable" else "$windDir°")
                } else {
                    append("Calm")
                }
                if (windSpeed != null && windSpeed > 0) {
                    append(" at $windSpeed kt")
                }
                if (windGust != null && windGust > 0) {
                    append(" (Gusts $windGust kt)")
                }
            }
            val windHeadingFloat = windDir?.toFloatOrNull() ?: 0f

            StatCard(
                title = "Wind",
                icon = Icons.Default.Air,
                value = windValue,
                subtitle = windSub,
                modifier = Modifier.weight(1f).testTag("stat_wind"),
                trailingContent = {
                    if (windDir != null && windDir != "VRB" && windSpeed != null && windSpeed > 0) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Wind Arrow",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(windHeadingFloat + 180f) // Arrow points in the direction wind is blowing towards
                            )
                        }
                    }
                }
            )

            // Visibility Card
            val visText = if (!metar.visib.isNullOrBlank()) "${metar.visib} SM" else "--"
            StatCard(
                title = "Visibility",
                icon = Icons.Default.Visibility,
                value = visText,
                subtitle = "Surface Visibility",
                modifier = Modifier.weight(1f).testTag("stat_visib")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dewpoint & Spread Card
            val tempC = metar.temp
            val dewC = metar.dewp
            val dewValue = if (dewC != null) {
                if (unitCelsius) "${dewC.roundToInt()}°C" else "${AviationCalculations.cToF(dewC).roundToInt()}°F"
            } else "--"

            val spreadSub = if (tempC != null && dewC != null) {
                val spreadC = ((tempC - dewC) * 10.0).roundToInt() / 10.0
                if (unitCelsius) "Spread: ${spreadC}°C" else "Spread: ${((spreadC * 1.8) * 10.0).roundToInt() / 10.0}°F"
            } else "Spread: --"

            StatCard(
                title = "Dew Point",
                icon = Icons.Default.WaterDrop,
                value = dewValue,
                subtitle = spreadSub,
                modifier = Modifier.weight(1f).testTag("stat_dewp")
            )

            // Altimeter Card
            val (inHgStr, hPaStr) = AviationCalculations.formatAltimeterDisplay(metar.rawOb, metar.altim)
            StatCard(
                title = "Altimeter",
                icon = Icons.Default.Speed,
                value = inHgStr,
                subtitle = hPaStr,
                modifier = Modifier.weight(1f).testTag("stat_altim")
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    icon: ImageVector,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                trailingContent?.invoke()
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
