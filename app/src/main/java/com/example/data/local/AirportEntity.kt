package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "airports")
data class AirportEntity(
    @PrimaryKey
    val icao: String,
    val name: String,
    val isFavorite: Boolean = false,
    val nickname: String = "",
    val lastSearchedTimestamp: Long = System.currentTimeMillis(),
    val lastFlightCategory: String = "VFR",
    val lastTempC: Double? = null,
    val lastWindStr: String? = null,
    val lastAltimStr: String? = null
)
