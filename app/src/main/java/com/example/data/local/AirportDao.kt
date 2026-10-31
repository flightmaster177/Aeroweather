package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AirportDao {

    @Query("SELECT * FROM airports ORDER BY isFavorite DESC, lastSearchedTimestamp DESC LIMIT 20")
    fun getAllAirports(): Flow<List<AirportEntity>>

    @Query("SELECT * FROM airports WHERE isFavorite = 1 ORDER BY lastSearchedTimestamp DESC")
    fun getFavoriteAirports(): Flow<List<AirportEntity>>

    @Query("SELECT * FROM airports WHERE icao = :icao LIMIT 1")
    suspend fun getAirport(icao: String): AirportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(airport: AirportEntity)

    @Query("UPDATE airports SET isFavorite = :isFavorite WHERE icao = :icao")
    suspend fun setFavorite(icao: String, isFavorite: Boolean)

    @Query("UPDATE airports SET nickname = :nickname WHERE icao = :icao")
    suspend fun updateNickname(icao: String, nickname: String)

    @Query("UPDATE airports SET lastFlightCategory = :cat, lastTempC = :temp, lastWindStr = :wind, lastAltimStr = :altim WHERE icao = :icao")
    suspend fun updateLiveSummary(icao: String, cat: String, temp: Double?, wind: String?, altim: String?)

    @Query("DELETE FROM airports WHERE icao = :icao")
    suspend fun delete(icao: String)
}
