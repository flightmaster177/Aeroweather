package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CloudLayersCard
import com.example.ui.components.FavoriteNicknameDialog
import com.example.ui.components.FavoritesView
import com.example.ui.components.FlightCategoryGuideDialog
import com.example.ui.components.HeroWeatherCard
import com.example.ui.components.RawMetarCard
import com.example.ui.components.RunwayCrosswindCard
import com.example.ui.components.StatsGrid
import com.example.ui.components.TafForecastView
import com.example.ui.components.TrendsView
import com.example.ui.viewmodel.AeroTab
import com.example.ui.viewmodel.AeroWeatherViewModel

@Composable
fun AeroWeatherScreen(
    viewModel: AeroWeatherViewModel,
    onRequestGpsLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchInput by remember(uiState.currentIcao) { mutableStateOf(uiState.currentIcao) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top App Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AirplanemodeActive,
                                    contentDescription = "AeroWeather Logo",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "AeroWeather",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Live Aviation Weather (NOAA)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.toggleFavorite() },
                                modifier = Modifier.testTag("favorite_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (uiState.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.refresh() },
                                modifier = Modifier.testTag("refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Weather",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    SearchBarCard(
                        value = searchInput,
                        onValueChange = { searchInput = it.uppercase().take(4) },
                        onSearch = {
                            focusManager.clearFocus()
                            viewModel.loadStation(searchInput)
                        },
                        onGpsClick = onRequestGpsLocation,
                        isLocating = uiState.isLocating
                    )
                }

                // Quick Airport Chips
                item {
                    val defaultChips = listOf("KSJT", "KJFK", "KLAX", "KORD", "KSFO", "EGLL")
                    val airportsList = uiState.recentAirports.map { it.icao }.ifEmpty { defaultChips }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        airportsList.distinct().take(10).forEach { code ->
                            val isSelected = code.equals(uiState.currentIcao, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable {
                                        searchInput = code
                                        viewModel.loadStation(code)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("chip_$code"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = code,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Mode Tabs: Observation (METAR), Trends, Forecast (TAF), Runway Wind, Favorites
                item {
                    ScrollableTabRow(
                        selectedTabIndex = uiState.activeTab.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab.ordinal]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        divider = {}
                    ) {
                        AeroTab.values().forEach { tab ->
                            Tab(
                                selected = uiState.activeTab == tab,
                                onClick = { viewModel.setTab(tab) },
                                text = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                // Loading State (when primary station is loading)
                if (uiState.isLoading) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loading_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = if (uiState.isLocating) "Locating nearest airport via GPS..." else "Fetching Live NOAA Aviation Weather...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Error State
                if (!uiState.isLoading && uiState.errorMessage != null && uiState.activeTab != AeroTab.FAVORITES) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("error_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = uiState.errorMessage ?: "Unable to load aviation data.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.refresh() },
                                    modifier = Modifier.testTag("error_retry_button")
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                // Favorites Tab (Available even without active station loaded)
                if (uiState.activeTab == AeroTab.FAVORITES) {
                    item {
                        FavoritesView(
                            favorites = uiState.favoriteAirports,
                            unitCelsius = uiState.unitCelsius,
                            onSelectAirport = { code ->
                                searchInput = code
                                viewModel.loadStation(code)
                                viewModel.setTab(AeroTab.METAR)
                            },
                            onRemoveFavorite = { viewModel.removeFavorite(it) },
                            onEditNickname = { viewModel.setEditingFavoriteForNickname(it) },
                            onAddFavorite = { viewModel.addFavorite(it) }
                        )
                    }
                }

                // Content for station-dependent tabs (METAR, TRENDS, TAF, RUNWAY)
                val metar = uiState.currentMetar
                if (!uiState.isLoading && metar != null && uiState.activeTab != AeroTab.FAVORITES) {
                    when (uiState.activeTab) {
                        AeroTab.METAR -> {
                            item {
                                HeroWeatherCard(
                                    metar = metar,
                                    unitCelsius = uiState.unitCelsius,
                                    onUnitToggle = { viewModel.toggleUnit(it) },
                                    onFlightCategoryClick = { viewModel.setCategoryDialog(true) }
                                )
                            }
                            item {
                                StatsGrid(
                                    metar = metar,
                                    unitCelsius = uiState.unitCelsius
                                )
                            }
                            item {
                                CloudLayersCard(clouds = metar.clouds)
                            }
                            item {
                                RawMetarCard(rawOb = metar.rawOb)
                            }
                        }

                        AeroTab.TRENDS -> {
                            item {
                                TrendsView(
                                    history = uiState.metarHistory,
                                    selectedHours = uiState.trendHours,
                                    isLoading = uiState.isHistoryLoading,
                                    unitCelsius = uiState.unitCelsius,
                                    onHoursSelected = { viewModel.loadTrends(it) }
                                )
                            }
                        }

                        AeroTab.TAF -> {
                            item {
                                TafForecastView(taf = uiState.currentTaf)
                            }
                        }

                        AeroTab.RUNWAY -> {
                            item {
                                RunwayCrosswindCard(
                                    metar = metar,
                                    runwayNumber = uiState.selectedRunway,
                                    onRunwayChange = { viewModel.setRunway(it) }
                                )
                            }
                        }

                        AeroTab.FAVORITES -> Unit
                    }
                }
            }
        }

        // FAA Flight Category info dialog
        if (uiState.showCategoryDialog) {
            FlightCategoryGuideDialog(
                onDismiss = { viewModel.setCategoryDialog(false) }
            )
        }

        // Favorite Nickname dialog
        uiState.editingFavoriteForNickname?.let { favAirport ->
            FavoriteNicknameDialog(
                icao = favAirport.icao,
                initialNickname = favAirport.nickname,
                onConfirm = { nickname ->
                    viewModel.updateFavoriteNickname(favAirport.icao, nickname)
                    viewModel.setEditingFavoriteForNickname(null)
                },
                onDismiss = { viewModel.setEditingFavoriteForNickname(null) }
            )
        }
    }
}

@Composable
private fun SearchBarCard(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onGpsClick: () -> Unit,
    isLocating: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_bar_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                placeholder = {
                    Text(
                        "Search Airport ICAO (e.g. KSJT)",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onGpsClick,
                    enabled = !isLocating,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .size(42.dp)
                        .testTag("gps_button")
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Locate via GPS",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSearch,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .size(42.dp)
                        .testTag("search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Airport",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
