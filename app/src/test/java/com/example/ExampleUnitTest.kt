package com.example

import com.example.data.local.AirportEntity
import com.example.domain.AviationCalculations
import com.example.domain.FlightCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDensityAltitudeCalculation() {
        // At sea level (0m / 0 ft), standard altimeter 29.92, temp 15°C -> DA should be ~0 ft
        val daStandard = AviationCalculations.calculateDensityAltitude(
            tempC = 15.0,
            altimInHg = 29.92,
            elevMeters = 0.0
        )
        assertNotNull(daStandard)
        assertEquals(0, daStandard!!.densityAltFt)
        assertEquals(0, daStandard.pressureAltFt)

        // Hot day at 1,000 ft elevation (304.8m), 35°C, altimeter 29.82
        val daHot = AviationCalculations.calculateDensityAltitude(
            tempC = 35.0,
            altimInHg = 29.82,
            elevMeters = 304.8
        )
        assertNotNull(daHot)
        assertTrue(daHot!!.densityAltFt > 3000)
    }

    @Test
    fun testCloudBaseCalculation() {
        // Temp 20°C, Dewpoint 15°C -> spread 5°C. Base = 5 / 2.5 * 1000 = 2,000 ft AGL
        val cb = AviationCalculations.calculateCloudBase(tempC = 20.0, dewpC = 15.0)
        assertNotNull(cb)
        assertEquals(2000, cb!!.estimatedBaseFtAgl)
        assertEquals(5.0, cb.spreadC, 0.01)
    }

    @Test
    fun testFlightCategoryDerivation() {
        // Ceiling 400 ft -> LIFR
        val lifr = AviationCalculations.deriveFlightCategory(
            apiCategory = null,
            visibStr = "5",
            clouds = listOf(com.example.data.model.CloudLayerDto(cover = "OVC", base = 400))
        )
        assertEquals(FlightCategory.LIFR, lifr)

        // Ceiling 2500 ft -> MVFR
        val mvfr = AviationCalculations.deriveFlightCategory(
            apiCategory = null,
            visibStr = "10",
            clouds = listOf(com.example.data.model.CloudLayerDto(cover = "BKN", base = 2500))
        )
        assertEquals(FlightCategory.MVFR, mvfr)

        // Clear sky, 10SM -> VFR
        val vfr = AviationCalculations.deriveFlightCategory(
            apiCategory = null,
            visibStr = "10+",
            clouds = emptyList()
        )
        assertEquals(FlightCategory.VFR, vfr)
    }

    @Test
    fun testCrosswindCalculation() {
        // Runway 18 (180°), Wind 090° at 10 kt -> direct 90° crosswind of 10 kt from Left
        val rwy18 = AviationCalculations.calculateCrosswind(
            windDirDeg = 90,
            windSpeedKnots = 10.0,
            runwayNumber = 18
        )
        assertEquals(10.0, rwy18.crosswindKnots, 0.5)
        assertEquals(0.0, rwy18.headwindKnots, 0.5)
        assertTrue(rwy18.isLeftCrosswind)

        // Runway 09 (090°), Wind 090° at 15 kt -> 0 crosswind, 15 kt headwind
        val rwy09 = AviationCalculations.calculateCrosswind(
            windDirDeg = 90,
            windSpeedKnots = 15.0,
            runwayNumber = 9
        )
        assertEquals(0.0, rwy09.crosswindKnots, 0.5)
        assertEquals(15.0, rwy09.headwindKnots, 0.5)
    }

    @Test
    fun testAltimeterExtraction() {
        val us = AviationCalculations.extractAltimeterInHg("METAR KSJT 310251Z 07007KT 7SM OVC003 14/14 A3007", null)
        assertEquals(30.07, us ?: 0.0, 0.001)

        val intl = AviationCalculations.extractAltimeterInHg("METAR EGLL 310250Z 24005KT 9999 FEW040 10/08 Q1013", null)
        assertEquals(29.91, intl ?: 0.0, 0.05)
    }

    @Test
    fun testMetarDecoder() {
        val raw = "METAR KSJT 310251Z AUTO 07007KT 7SM OVC003 14/14 A3007 RMK AO2"
        val segments = AviationCalculations.decodeMetar(raw)
        assertTrue(segments.any { it.title == "Station" && it.raw == "KSJT" })
        assertTrue(segments.any { it.title == "Wind" })
        assertTrue(segments.any { it.title == "Sky Condition" })
        assertTrue(segments.any { it.title == "Altimeter" })
    }

    @Test
    fun testAirportEntityWithNickname() {
        val airport = AirportEntity(
            icao = "KSJT",
            name = "San Angelo/Mathis Fld",
            isFavorite = true,
            nickname = "Home Base",
            lastFlightCategory = "LIFR",
            lastTempC = 14.4
        )
        assertEquals("KSJT", airport.icao)
        assertTrue(airport.isFavorite)
        assertEquals("Home Base", airport.nickname)
        assertEquals(14.4, airport.lastTempC ?: 0.0, 0.01)
    }
}
