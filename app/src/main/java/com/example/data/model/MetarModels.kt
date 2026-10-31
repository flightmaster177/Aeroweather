package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MetarDto(
    @Json(name = "icaoId") val icaoId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "obsTime") val obsTime: Long? = null,
    @Json(name = "reportTime") val reportTime: String? = null,
    @Json(name = "receiptTime") val receiptTime: String? = null,
    @Json(name = "temp") val temp: Double? = null,
    @Json(name = "dewp") val dewp: Double? = null,
    @Json(name = "wdir") val wdir: String? = null,
    @Json(name = "wspd") val wspd: Double? = null,
    @Json(name = "wgst") val wgst: Double? = null,
    @Json(name = "visib") val visib: String? = null,
    @Json(name = "altim") val altim: Double? = null,
    @Json(name = "slp") val slp: Double? = null,
    @Json(name = "rawOb") val rawOb: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lon") val lon: Double? = null,
    @Json(name = "elev") val elev: Double? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "fltCat") val fltCat: String? = null,
    @Json(name = "fltcat") val fltcatAlt: String? = null,
    @Json(name = "clouds") val clouds: List<CloudLayerDto>? = emptyList(),
    @Json(name = "precip") val precip: Double? = null
) {
    val flightCategory: String
        get() = fltCat ?: fltcatAlt ?: ""
}

@JsonClass(generateAdapter = true)
data class CloudLayerDto(
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "base") val base: Int? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class TafDto(
    @Json(name = "icaoId") val icaoId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "rawTAF") val rawTAF: String? = null,
    @Json(name = "issueTime") val issueTime: String? = null,
    @Json(name = "bulletinTime") val bulletinTime: String? = null,
    @Json(name = "validTimeFrom") val validTimeFrom: Long? = null,
    @Json(name = "validTimeTo") val validTimeTo: Long? = null,
    @Json(name = "remarks") val remarks: String? = null,
    @Json(name = "fcsts") val fcsts: List<TafForecastDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TafForecastDto(
    @Json(name = "timeFrom") val timeFrom: Long? = null,
    @Json(name = "timeTo") val timeTo: Long? = null,
    @Json(name = "fcstChange") val fcstChange: String? = null,
    @Json(name = "probability") val probability: Int? = null,
    @Json(name = "wdir") val wdir: String? = null,
    @Json(name = "wspd") val wspd: Double? = null,
    @Json(name = "wgst") val wgst: Double? = null,
    @Json(name = "visib") val visib: String? = null,
    @Json(name = "wxString") val wxString: String? = null,
    @Json(name = "clouds") val clouds: List<CloudLayerDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class StationInfoDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "icaoId") val icaoId: String? = null,
    @Json(name = "iataId") val iataId: String? = null,
    @Json(name = "faaId") val faaId: String? = null,
    @Json(name = "site") val site: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lon") val lon: Double? = null,
    @Json(name = "elev") val elev: Double? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "country") val country: String? = null
)
