package com.example.data.repository

import com.example.data.local.AirportDao
import com.example.data.local.AirportEntity
import com.example.data.model.MetarDto
import com.example.data.model.TafDto
import com.example.data.remote.AviationWeatherApi
import com.example.domain.AviationCalculations
import kotlinx.coroutines.flow.Flow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class WeatherRepository(
    private val api: AviationWeatherApi,
    private val airportDao: AirportDao
) {

    val recentAndFavoriteAirports: Flow<List<AirportEntity>> = airportDao.getAllAirports()
    val favoriteAirports: Flow<List<AirportEntity>> = airportDao.getFavoriteAirports()

    suspend fun fetchMetar(icao: String): MetarDto? {
        val cleanIcao = icao.trim().uppercase()
        val result = api.getMetar(ids = cleanIcao)
        val metar = result.firstOrNull { it.icaoId?.equals(cleanIcao, ignoreCase = true) == true }
            ?: result.firstOrNull()

        if (metar != null) {
            val existing = airportDao.getAirport(cleanIcao)
            val flightCat = metar.flightCategory.ifBlank { "VFR" }
            val (altimStr, _) = AviationCalculations.formatAltimeterDisplay(metar.rawOb, metar.altim)
            val windStr = if (metar.wspd != null) "${metar.wdir ?: "VRB"}@${metar.wspd.toInt()}kt" else "Calm"

            airportDao.upsert(
                AirportEntity(
                    icao = cleanIcao,
                    name = metar.name ?: existing?.name ?: cleanIcao,
                    isFavorite = existing?.isFavorite ?: false,
                    nickname = existing?.nickname ?: "",
                    lastSearchedTimestamp = System.currentTimeMillis(),
                    lastFlightCategory = flightCat,
                    lastTempC = metar.temp,
                    lastWindStr = windStr,
                    lastAltimStr = altimStr
                )
            )
        }
        return metar
    }

    suspend fun fetchMetarHistory(icao: String, hours: Int = 12): List<MetarDto> {
        val cleanIcao = icao.trim().uppercase()
        val result = api.getMetar(ids = cleanIcao, hours = hours)
        // Sort chronologically ascending (oldest to newest) for chart plotting, or return as list
        return result.filter { it.icaoId?.equals(cleanIcao, ignoreCase = true) == true || result.size <= hours * 2 }
            .sortedBy { it.obsTime ?: 0L }
    }

    suspend fun fetchFavoritesWeather(icaos: List<String>): List<MetarDto> {
        if (icaos.isEmpty()) return emptyList()
        val cleanList = icaos.map { it.trim().uppercase() }.distinct()
        val idsQuery = cleanList.joinToString(",")
        val results = api.getMetar(ids = idsQuery)

        // Update cached live summaries in DB
        results.forEach { metar ->
            val id = metar.icaoId?.uppercase() ?: return@forEach
            val (altimStr, _) = AviationCalculations.formatAltimeterDisplay(metar.rawOb, metar.altim)
            val windStr = if (metar.wspd != null) "${metar.wdir ?: "VRB"}@${metar.wspd.toInt()}kt" else "Calm"
            airportDao.updateLiveSummary(
                icao = id,
                cat = metar.flightCategory.ifBlank { "VFR" },
                temp = metar.temp,
                wind = windStr,
                altim = altimStr
            )
        }
        return results
    }

    suspend fun fetchTaf(icao: String): TafDto? {
        val cleanIcao = icao.trim().uppercase()
        val result = api.getTaf(ids = cleanIcao)
        return result.firstOrNull { it.icaoId?.equals(cleanIcao, ignoreCase = true) == true }
            ?: result.firstOrNull()
    }

    suspend fun findNearestAirport(lat: Double, lon: Double): MetarDto? {
        val delta = 1.2
        val minLat = (lat - delta)
        val maxLat = (lat + delta)
        val minLon = (lon - delta)
        val maxLon = (lon + delta)
        val bbox = "$minLat,$minLon,$maxLat,$maxLon"

        val stations = api.getMetar(bbox = bbox)
        if (stations.isEmpty()) return null

        return stations.minByOrNull { station ->
            val sLat = station.lat ?: return@minByOrNull Double.MAX_VALUE
            val sLon = station.lon ?: return@minByOrNull Double.MAX_VALUE
            haversineDistanceKm(lat, lon, sLat, sLon)
        }
    }

    suspend fun toggleFavorite(icao: String, currentStatus: Boolean) {
        val cleanIcao = icao.trim().uppercase()
        val existing = airportDao.getAirport(cleanIcao)
        if (existing != null) {
            airportDao.setFavorite(cleanIcao, !currentStatus)
        } else {
            airportDao.upsert(
                AirportEntity(
                    icao = cleanIcao,
                    name = cleanIcao,
                    isFavorite = !currentStatus,
                    lastSearchedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun setFavoriteNickname(icao: String, nickname: String) {
        val cleanIcao = icao.trim().uppercase()
        airportDao.updateNickname(cleanIcao, nickname.trim())
    }

    suspend fun addFavorite(icao: String, name: String = "", nickname: String = "") {
        val cleanIcao = icao.trim().uppercase()
        val existing = airportDao.getAirport(cleanIcao)
        airportDao.upsert(
            AirportEntity(
                icao = cleanIcao,
                name = if (name.isNotBlank()) name else existing?.name ?: cleanIcao,
                isFavorite = true,
                nickname = nickname.trim(),
                lastSearchedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFavorite(icao: String) {
        val cleanIcao = icao.trim().uppercase()
        airportDao.setFavorite(cleanIcao, false)
    }

    suspend fun isFavorite(icao: String): Boolean {
        return airportDao.getAirport(icao.trim().uppercase())?.isFavorite ?: false
    }

    suspend fun getAirport(icao: String): AirportEntity? {
        return airportDao.getAirport(icao.trim().uppercase())
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
