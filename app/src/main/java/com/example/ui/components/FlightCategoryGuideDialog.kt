package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.FlightCategory

@Composable
fun FlightCategoryGuideDialog(
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "FAA Flight Categories",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Aviation flight categories are governed by the lowest ceiling layer (BKN, OVC, or VV) and surface visibility:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlightCategoryRow(
                    category = FlightCategory.VFR,
                    ceiling = "Ceiling > 3,000 ft AGL",
                    visibility = "Visibility > 5 SM",
                    isDark = isDark
                )

                FlightCategoryRow(
                    category = FlightCategory.MVFR,
                    ceiling = "Ceiling 1,000 – 3,000 ft AGL",
                    visibility = "Visibility 3 – 5 SM",
                    isDark = isDark
                )

                FlightCategoryRow(
                    category = FlightCategory.IFR,
                    ceiling = "Ceiling 500 – 999 ft AGL",
                    visibility = "Visibility 1 – < 3 SM",
                    isDark = isDark
                )

                FlightCategoryRow(
                    category = FlightCategory.LIFR,
                    ceiling = "Ceiling < 500 ft AGL",
                    visibility = "Visibility < 1 SM",
                    isDark = isDark
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun FlightCategoryRow(
    category: FlightCategory,
    ceiling: String,
    visibility: String,
    isDark: Boolean
) {
    val (badgeBg, badgeText) = getFlightCategoryColors(category, isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = category.label,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = badgeText
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ceiling,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = visibility,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
