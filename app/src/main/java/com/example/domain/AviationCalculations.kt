package com.example.domain

import com.example.data.model.CloudLayerDto
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

enum class FlightCategory(val label: String, val description: String) {
    VFR("VFR", "Visual Flight Rules: Ceiling > 3,000 ft and Visibility > 5 SM"),
    MVFR("MVFR", "Marginal VFR: Ceiling 1,000–3,000 ft and/or Visibility 3–5 SM"),
    IFR("IFR", "Instrument Flight Rules: Ceiling 500–999 ft and/or Visibility 1–<3 SM"),
    LIFR("LIFR", "Low IFR: Ceiling < 500 ft and/or Visibility < 1 SM")
}

data class DensityAltitudeResult(
    val densityAltFt: Int,
    val pressureAltFt: Int,
    val fieldElevFt: Int,
    val isaTempC: Double
)

data class CloudBaseResult(
    val estimatedBaseFtAgl: Int,
    val spreadC: Double,
    val spreadF: Double
)

data class CrosswindResult(
    val crosswindKnots: Double,
    val headwindKnots: Double, // positive is headwind, negative is tailwind
    val isLeftCrosswind: Boolean,
    val angleDiffDegrees: Int
)

data class MetarSegment(
    val title: String,
    val raw: String,
    val decoded: String
)

object AviationCalculations {

    fun cToF(c: Double): Double = (c * 9.0 / 5.0) + 32.0

    fun fToC(f: Double): Double = (f - 32.0) * 5.0 / 9.0

    fun extractAltimeterInHg(rawOb: String?, apiAltim: Double?): Double? {
        if (!rawOb.isNullOrBlank()) {
            val usMatch = Regex("""\bA(\d{4})\b""").find(rawOb)
            if (usMatch != null) {
                val digits = usMatch.groupValues[1].toDoubleOrNull()
                if (digits != null) return digits / 100.0
            }
            val intlMatch = Regex("""\bQ(\d{4})\b""").find(rawOb)
            if (intlMatch != null) {
                val hPa = intlMatch.groupValues[1].toDoubleOrNull()
                if (hPa != null) return hPa * 0.02953
            }
        }
        if (apiAltim != null && apiAltim > 0) {
            return if (apiAltim > 100.0) apiAltim * 0.02953 else apiAltim
        }
        return null
    }

    fun formatAltimeterDisplay(rawOb: String?, apiAltim: Double?): Pair<String, String> {
        val inHg = extractAltimeterInHg(rawOb, apiAltim) ?: return Pair("--", "--")
        val hPa = (inHg / 0.02953).roundToInt()
        val inHgStr = String.format("%.2f inHg", inHg)
        val hPaStr = "$hPa hPa"
        return Pair(inHgStr, hPaStr)
    }

    fun calculateDensityAltitude(
        tempC: Double?,
        altimInHg: Double?,
        elevMeters: Double?
    ): DensityAltitudeResult? {
        if (tempC == null) return null
        val altim = altimInHg ?: 29.92
        val fieldElevFt = ((elevMeters ?: 0.0) * 3.28084).roundToInt()

        // Pressure Alt = Field Elevation + (29.92 - Altimeter) * 1000
        val pressureAltFt = (fieldElevFt + (29.92 - altim) * 1000.0).roundToInt()

        // Standard ISA temp at station elevation = 15°C - 1.98°C per 1,000 ft
        val isaTempC = 15.0 - (1.98 * (fieldElevFt / 1000.0))

        // Density Altitude = Pressure Alt + 120 * (OAT - ISA Temp)
        val densityAltFt = (pressureAltFt + (120.0 * (tempC - isaTempC))).roundToInt()

        return DensityAltitudeResult(
            densityAltFt = densityAltFt,
            pressureAltFt = pressureAltFt,
            fieldElevFt = fieldElevFt,
            isaTempC = (isaTempC * 10.0).roundToInt() / 10.0
        )
    }

    fun calculateCloudBase(tempC: Double?, dewpC: Double?): CloudBaseResult? {
        if (tempC == null || dewpC == null) return null
        val spreadC = ((tempC - dewpC) * 10.0).roundToInt() / 10.0
        val spreadF = ((spreadC * 1.8) * 10.0).roundToInt() / 10.0

        val baseThousands = max(0.0, spreadC / 2.5)
        val estimatedBaseFt = (baseThousands * 1000.0).roundToInt()

        return CloudBaseResult(
            estimatedBaseFtAgl = estimatedBaseFt,
            spreadC = spreadC,
            spreadF = spreadF
        )
    }

    fun deriveFlightCategory(
        apiCategory: String?,
        visibStr: String?,
        clouds: List<CloudLayerDto>?
    ): FlightCategory {
        val normalizedApi = apiCategory?.trim()?.uppercase()
        when (normalizedApi) {
            "LIFR" -> return FlightCategory.LIFR
            "IFR" -> return FlightCategory.IFR
            "MVFR" -> return FlightCategory.MVFR
            "VFR" -> return FlightCategory.VFR
        }

        // Derive from ceiling and visibility
        var lowestCeiling: Int = 99999
        clouds?.forEach { c ->
            val cover = c.cover?.uppercase()
            if ((cover == "BKN" || cover == "OVC" || cover == "VV") && c.base != null) {
                if (c.base < lowestCeiling) {
                    lowestCeiling = c.base
                }
            }
        }

        val vis = parseVisibilitySm(visibStr) ?: 10.0

        return when {
            lowestCeiling < 500 || vis < 1.0 -> FlightCategory.LIFR
            lowestCeiling < 1000 || vis < 3.0 -> FlightCategory.IFR
            lowestCeiling <= 3000 || vis <= 5.0 -> FlightCategory.MVFR
            else -> FlightCategory.VFR
        }
    }

    fun parseVisibilitySm(visibStr: String?): Double? {
        if (visibStr.isNullOrBlank()) return null
        val cleaned = visibStr.replace("+", "").trim()
        if (cleaned.contains(" ")) {
            val parts = cleaned.split(" ")
            if (parts.size == 2) {
                val whole = parts[0].toDoubleOrNull() ?: 0.0
                val fractionParts = parts[1].split("/")
                if (fractionParts.size == 2) {
                    val num = fractionParts[0].toDoubleOrNull() ?: 0.0
                    val den = fractionParts[1].toDoubleOrNull() ?: 1.0
                    return whole + (num / den)
                }
            }
        }
        if (cleaned.contains("/")) {
            val parts = cleaned.split("/")
            if (parts.size == 2) {
                val num = parts[0].toDoubleOrNull() ?: 0.0
                val den = parts[1].toDoubleOrNull() ?: 1.0
                return num / den
            }
        }
        return cleaned.toDoubleOrNull()
    }

    fun calculateCrosswind(
        windDirDeg: Int?,
        windSpeedKnots: Double?,
        runwayNumber: Int // 1 to 36
    ): CrosswindResult {
        val speed = windSpeedKnots ?: 0.0
        val runwayHeadingDeg = (runwayNumber * 10) % 360
        if (windDirDeg == null || speed == 0.0) {
            return CrosswindResult(0.0, 0.0, false, 0)
        }

        var diff = (windDirDeg - runwayHeadingDeg) % 360
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        val radians = diff * (PI / 180.0)
        val crosswind = abs(speed * sin(radians))
        val headwind = speed * cos(radians)
        val isLeft = diff < 0

        return CrosswindResult(
            crosswindKnots = (crosswind * 10.0).roundToInt() / 10.0,
            headwindKnots = (headwind * 10.0).roundToInt() / 10.0,
            isLeftCrosswind = isLeft,
            angleDiffDegrees = abs(diff)
        )
    }

    fun getCloudCoverageName(cover: String?): String {
        return when (cover?.uppercase()) {
            "SKC", "CLR" -> "Clear Skies (0/8)"
            "FEW" -> "Few Clouds (1/8–2/8)"
            "SCT" -> "Scattered (3/8–4/8)"
            "BKN" -> "Broken Ceiling (5/8–7/8)"
            "OVC" -> "Overcast Ceiling (8/8)"
            "VV", "OVX" -> "Vertical Visibility / Obscured"
            else -> cover ?: "Unknown"
        }
    }

    fun decodeMetar(rawOb: String?): List<MetarSegment> {
        if (rawOb.isNullOrBlank()) return emptyList()
        val segments = mutableListOf<MetarSegment>()
        val tokens = rawOb.trim().split(Regex("""\s+"""))

        for (token in tokens) {
            when {
                token == "METAR" || token == "SPECI" -> {
                    segments.add(MetarSegment("Report Type", token, if (token == "SPECI") "Special weather report" else "Routine aerodrome observation"))
                }
                token.matches(Regex("""[A-Z0-9]{4}""")) && segments.none { it.title == "Station" } -> {
                    segments.add(MetarSegment("Station", token, "ICAO identifier $token"))
                }
                token.matches(Regex("""\d{6}Z""")) -> {
                    val day = token.substring(0, 2)
                    val hour = token.substring(2, 4)
                    val min = token.substring(4, 6)
                    segments.add(MetarSegment("Date/Time", token, "Day $day of month at $hour:$min UTC"))
                }
                token == "AUTO" -> {
                    segments.add(MetarSegment("Modifier", token, "Fully automated observation without human intervention"))
                }
                token == "COR" -> {
                    segments.add(MetarSegment("Modifier", token, "Corrected report"))
                }
                token.matches(Regex("""(\d{3}|VRB)\d{2,3}(G\d{2,3})?KT""")) -> {
                    val m = Regex("""(\d{3}|VRB)(\d{2,3})(?:G(\d{2,3}))?KT""").find(token)
                    if (m != null) {
                        val dir = if (m.groupValues[1] == "VRB") "Variable direction" else "${m.groupValues[1]}°"
                        val spd = "${m.groupValues[2]} knots"
                        val gst = if (m.groupValues[3].isNotBlank()) ", gusts to ${m.groupValues[3]} knots" else ""
                        segments.add(MetarSegment("Wind", token, "From $dir at $spd$gst"))
                    }
                }
                token.matches(Regex("""\d+SM|\d+/\d+SM|\d+\s+\d+/\d+SM""")) -> {
                    segments.add(MetarSegment("Visibility", token, "Surface visibility $token"))
                }
                token.matches(Regex("""(FEW|SCT|BKN|OVC|VV)\d{3}(CB|TCU)?""")) -> {
                    val cov = token.substring(0, 3)
                    val base = token.substring(3, 6).toIntOrNull()?.let { it * 100 }
                    val type = if (token.length > 6) " (${token.substring(6)})" else ""
                    val covName = getCloudCoverageName(cov)
                    segments.add(MetarSegment("Sky Condition", token, "$covName at ${base ?: 0} ft AGL$type"))
                }
                token == "CLR" || token == "SKC" -> {
                    segments.add(MetarSegment("Sky Condition", token, "Clear skies below 12,000 ft"))
                }
                token.matches(Regex("""M?\d{2}/M?\d{2}""")) -> {
                    val parts = token.split("/")
                    val t = parts[0].replace("M", "-")
                    val d = parts[1].replace("M", "-")
                    segments.add(MetarSegment("Temp / Dewpoint", token, "Temperature ${t}°C, Dewpoint ${d}°C"))
                }
                token.matches(Regex("""A\d{4}""")) -> {
                    val alt = token.substring(1).toDoubleOrNull()?.let { it / 100.0 }
                    segments.add(MetarSegment("Altimeter", token, "${String.format("%.2f", alt ?: 29.92)} inHg"))
                }
                token.matches(Regex("""Q\d{4}""")) -> {
                    val qnh = token.substring(1)
                    segments.add(MetarSegment("Altimeter", token, "$qnh hPa"))
                }
                token == "RMK" -> {
                    val rmkIdx = rawOb.indexOf("RMK")
                    if (rmkIdx >= 0) {
                        val rmkText = rawOb.substring(rmkIdx + 3).trim()
                        segments.add(MetarSegment("Remarks", "RMK $rmkText", "Automated system sensors and pressure remarks"))
                        break
                    }
                }
            }
        }
        return segments
    }
}
