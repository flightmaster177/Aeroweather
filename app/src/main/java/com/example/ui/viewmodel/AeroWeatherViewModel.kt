package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AeroWeatherDatabase
import com.example.data.local.AirportEntity
import com.example.data.model.MetarDto
import com.example.data.model.TafDto
import com.example.data.remote.AviationWeatherApi
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AeroTab(val title: String) {
    METAR("Observation"),
    TRENDS("Trends"),
    TAF("Forecast"),
    RUNWAY("Runway Wind"),
    FAVORITES("Favorites")
}

data class AeroWeatherUiState(
    val isLoading: Boolean = false,
    val isLocating: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentIcao: String = "KSJT",
    val currentMetar: MetarDto? = null,
    val currentTaf: TafDto? = null,
    val metarHistory: List<MetarDto> = emptyList(),
    val trendHours: Int = 12,
    val unitCelsius: Boolean = true,
    val activeTab: AeroTab = AeroTab.METAR,
    val selectedRunway: Int = 18,
    val isFavorite: Boolean = false,
    val recentAirports: List<AirportEntity> = emptyList(),
    val favoriteAirports: List<AirportEntity> = emptyList(),
    val showCategoryDialog: Boolean = false,
    val editingFavoriteForNickname: AirportEntity? = null
)

class AeroWeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeatherRepository

    private val _uiState = MutableStateFlow(AeroWeatherUiState())
    val uiState: StateFlow<AeroWeatherUiState> = _uiState.asStateFlow()

    init {
        val database = AeroWeatherDatabase.getDatabase(application)
        val api = AviationWeatherApi.create()
        repository = WeatherRepository(api, database.airportDao())

        // Observe recents
        viewModelScope.launch {
            repository.recentAndFavoriteAirports.collect { airports ->
                _uiState.update { it.copy(recentAirports = airports) }
            }
        }

        // Observe favorites and refresh their live conditions
        viewModelScope.launch {
            repository.favoriteAirports.collect { favs ->
                _uiState.update { it.copy(favoriteAirports = favs) }
                if (favs.isNotEmpty()) {
                    try {
                        repository.fetchFavoritesWeather(favs.map { it.icao })
                    } catch (_: Exception) {
                    }
                }
            }
        }

        // Initial station: KSJT (from original app)
        loadStation("KSJT")
    }

    fun loadStation(rawIcao: String) {
        val cleanIcao = rawIcao.trim().uppercase()
        if (cleanIcao.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentIcao = cleanIcao
                )
            }

            try {
                val metar = repository.fetchMetar(cleanIcao)
                val taf = repository.fetchTaf(cleanIcao)
                val isFav = repository.isFavorite(cleanIcao)

                if (metar != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            currentMetar = metar,
                            currentTaf = taf,
                            isFavorite = isFav
                        )
                    }
                    // Fetch trend data in background
                    loadTrends(_uiState.value.trendHours, silent = true)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No current METAR observation found for station \"$cleanIcao\". Check the ICAO code.",
                            currentMetar = null,
                            currentTaf = null,
                            metarHistory = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to fetch weather: ${e.localizedMessage ?: "Network error"}",
                        currentMetar = null,
                        currentTaf = null,
                        metarHistory = emptyList()
                    )
                }
            }
        }
    }

    fun loadTrends(hours: Int = _uiState.value.trendHours, silent: Boolean = false) {
        val icao = _uiState.value.currentIcao
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isHistoryLoading = true, trendHours = hours) }
            } else {
                _uiState.update { it.copy(trendHours = hours) }
            }

            try {
                val history = repository.fetchMetarHistory(icao, hours)
                _uiState.update {
                    it.copy(
                        isHistoryLoading = false,
                        metarHistory = history
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isHistoryLoading = false) }
            }
        }
    }

    fun locateNearestAirport(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLocating = true,
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val nearest = repository.findNearestAirport(lat, lon)
                if (nearest != null && !nearest.icaoId.isNullOrBlank()) {
                    val icao = nearest.icaoId
                    val taf = repository.fetchTaf(icao)
                    val isFav = repository.isFavorite(icao)

                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            isLoading = false,
                            currentIcao = icao,
                            currentMetar = nearest,
                            currentTaf = taf,
                            isFavorite = isFav,
                            errorMessage = null
                        )
                    }
                    loadTrends(_uiState.value.trendHours, silent = true)
                } else {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            isLoading = false,
                            errorMessage = "No METAR reporting airport found nearby your current GPS coordinates."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        isLoading = false,
                        errorMessage = "Failed to locate nearest airport: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun toggleUnit(isCelsius: Boolean) {
        _uiState.update { it.copy(unitCelsius = isCelsius) }
    }

    fun setTab(tab: AeroTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == AeroTab.TRENDS && _uiState.value.metarHistory.isEmpty()) {
            loadTrends(_uiState.value.trendHours)
        } else if (tab == AeroTab.FAVORITES) {
            val favs = _uiState.value.favoriteAirports
            if (favs.isNotEmpty()) {
                viewModelScope.launch {
                    try {
                        repository.fetchFavoritesWeather(favs.map { it.icao })
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun setRunway(runway: Int) {
        _uiState.update { it.copy(selectedRunway = runway.coerceIn(1, 36)) }
    }

    fun toggleFavorite() {
        val icao = _uiState.value.currentIcao
        val currentFav = _uiState.value.isFavorite
        viewModelScope.launch {
            repository.toggleFavorite(icao, currentFav)
            _uiState.update { it.copy(isFavorite = !currentFav) }
        }
    }

    fun addFavorite(icao: String, nickname: String = "") {
        viewModelScope.launch {
            repository.addFavorite(icao, nickname = nickname)
            if (icao.equals(_uiState.value.currentIcao, ignoreCase = true)) {
                _uiState.update { it.copy(isFavorite = true) }
            }
        }
    }

    fun removeFavorite(icao: String) {
        viewModelScope.launch {
            repository.removeFavorite(icao)
            if (icao.equals(_uiState.value.currentIcao, ignoreCase = true)) {
                _uiState.update { it.copy(isFavorite = false) }
            }
        }
    }

    fun updateFavoriteNickname(icao: String, nickname: String) {
        viewModelScope.launch {
            repository.setFavoriteNickname(icao, nickname)
        }
    }

    fun setEditingFavoriteForNickname(airport: AirportEntity?) {
        _uiState.update { it.copy(editingFavoriteForNickname = airport) }
    }

    fun refresh() {
        loadStation(_uiState.value.currentIcao)
    }

    fun setCategoryDialog(show: Boolean) {
        _uiState.update { it.copy(showCategoryDialog = show) }
    }
}
