package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MetarDto
import com.example.domain.AviationCalculations
import com.example.domain.FlightCategory
import com.example.ui.theme.IfrRed
import com.example.ui.theme.IfrRedBg
import com.example.ui.theme.IfrRedBgDark
import com.example.ui.theme.IfrRedDark
import com.example.ui.theme.LifrPurple
import com.example.ui.theme.LifrPurpleBg
import com.example.ui.theme.LifrPurpleBgDark
import com.example.ui.theme.LifrPurpleDark
import com.example.ui.theme.MvfrBlue
import com.example.ui.theme.MvfrBlueBg
import com.example.ui.theme.MvfrBlueBgDark
import com.example.ui.theme.MvfrBlueDark
import com.example.ui.theme.VfrGreen
import com.example.ui.theme.VfrGreenBg
import com.example.ui.theme.VfrGreenBgDark
import com.example.ui.theme.VfrGreenDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Composable
fun HeroWeatherCard(
    metar: MetarDto,
    unitCelsius: Boolean,
    onUnitToggle: (Boolean) -> Unit,
    onFlightCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val flightCat = AviationCalculations.deriveFlightCategory(
        metar.flightCategory,
        metar.visib,
        metar.clouds
    )

    val (badgeBg, badgeText) = getFlightCategoryColors(flightCat, isDark)

    val tempC = metar.temp
    val dewpC = metar.dewp
    val altimInHg = AviationCalculations.extractAltimeterInHg(metar.rawOb, metar.altim)
    val daResult = AviationCalculations.calculateDensityAltitude(tempC, altimInHg, metar.elev)
    val cbResult = AviationCalculations.calculateCloudBase(tempC, dewpC)

    val obsTimeFormatted = metar.obsTime?.let {
        val sdf = SimpleDateFormat("HH:mm 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        "Observed at ${sdf.format(Date(it * 1000L))}"
    } ?: "Observation time unavailable"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_weather_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: ICAO, Name, Observation Time, and Flight Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = metar.icaoId ?: "----",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!metar.name.isNullOrBlank()) {
                        Text(
                            text = metar.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = obsTimeFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Flight Category Badge (Clickable for FAA criteria modal)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(badgeBg)
                        .clickable { onFlightCategoryClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("flight_category_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = flightCat.label,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = badgeText
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Flight Category Info",
                            tint = badgeText.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Big Temperature Display with °C / °F switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayTemp = if (tempC != null) {
                    if (unitCelsius) "${tempC.roundToInt()}°" else "${AviationCalculations.cToF(tempC).roundToInt()}°"
                } else "--"

                Text(
                    text = displayTemp,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Unit Toggle Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UnitToggleButton(
                        label = "°C",
                        isSelected = unitCelsius,
                        onClick = { onUnitToggle(true) }
                    )
                    UnitToggleButton(
                        label = "°F",
                        isSelected = !unitCelsius,
                        onClick = { onUnitToggle(false) }
                    )
                }
            }

            // Density Altitude Card
            DensityAltitudeCard(daResult = daResult)

            // Cloud Base Estimation Card
            CloudBaseCard(cbResult = cbResult, unitCelsius = unitCelsius)
        }
    }
}

@Composable
private fun UnitToggleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "unit_bg"
    )
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("unit_toggle_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = textColor
        )
    }
}

@Composable
private fun DensityAltitudeCard(daResult: com.example.domain.DensityAltitudeResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DENSITY ALTITUDE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val daText = if (daResult != null) "%,d ft MSL".format(daResult.densityAltFt) else "N/A"
                Text(
                    text = daText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subText = if (daResult != null) {
                    "PA: %,d ft | Field Elev: %,d ft | ISA: %.1f°C".format(
                        daResult.pressureAltFt,
                        daResult.fieldElevFt,
                        daResult.isaTempC
                    )
                } else "PA + 120 × (OAT - ISA Temp)"
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.Thermostat,
                contentDescription = "Density Altitude",
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun CloudBaseCard(cbResult: com.example.domain.CloudBaseResult?, unitCelsius: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EST. CLOUD BASE HEIGHT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val cbText = if (cbResult != null) "%,d ft AGL".format(cbResult.estimatedBaseFtAgl) else "N/A"
                Text(
                    text = cbText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                val formulaText = if (cbResult != null) {
                    if (unitCelsius) {
                        "Spread: %.1f°C | (Temp - Dewp) / 2.5 × 1,000 ft".format(cbResult.spreadC)
                    } else {
                        "Spread: %.1f°F | (Temp - Dewp) / 4.5 × 1,000 ft".format(cbResult.spreadF)
                    }
                } else "(Temp - Dewpoint) / rate"
                Text(
                    text = formulaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = "Cloud Base",
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

fun getFlightCategoryColors(flightCat: FlightCategory, isDark: Boolean): Pair<Color, Color> {
    return when (flightCat) {
        FlightCategory.VFR -> if (isDark) Pair(VfrGreenBgDark, VfrGreenDark) else Pair(VfrGreenBg, VfrGreen)
        FlightCategory.MVFR -> if (isDark) Pair(MvfrBlueBgDark, MvfrBlueDark) else Pair(MvfrBlueBg, MvfrBlue)
        FlightCategory.IFR -> if (isDark) Pair(IfrRedBgDark, IfrRedDark) else Pair(IfrRedBg, IfrRed)
        FlightCategory.LIFR -> if (isDark) Pair(LifrPurpleBgDark, LifrPurpleDark) else Pair(LifrPurpleBg, LifrPurple)
    }
}
