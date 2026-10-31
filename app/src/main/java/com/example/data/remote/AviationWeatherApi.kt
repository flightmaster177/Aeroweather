package com.example.data.remote

import com.example.data.model.MetarDto
import com.example.data.model.StationInfoDto
import com.example.data.model.TafDto
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface AviationWeatherApi {

    @GET("metar")
    suspend fun getMetar(
        @Query("ids") ids: String? = null,
        @Query("bbox") bbox: String? = null,
        @Query("hours") hours: Int? = null,
        @Query("format") format: String = "json"
    ): List<MetarDto>

    @GET("taf")
    suspend fun getTaf(
        @Query("ids") ids: String? = null,
        @Query("format") format: String = "json"
    ): List<TafDto>

    @GET("stationinfo")
    suspend fun getStationInfo(
        @Query("ids") ids: String? = null,
        @Query("format") format: String = "json"
    ): List<StationInfoDto>

    companion object {
        private const val BASE_URL = "https://aviationweather.gov/api/data/"

        class LenientStringAdapter {
            @FromJson
            fun fromJson(reader: JsonReader): String? {
                return when (reader.peek()) {
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Unit>()
                        null
                    }
                    JsonReader.Token.NUMBER -> {
                        val num = reader.nextDouble()
                        if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
                    }
                    JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
                    else -> reader.nextString()
                }
            }

            @ToJson
            fun toJson(writer: JsonWriter, value: String?) {
                writer.value(value)
            }
        }

        class LenientDoubleAdapter {
            @FromJson
            fun fromJson(reader: JsonReader): Double? {
                return when (reader.peek()) {
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Unit>()
                        null
                    }
                    JsonReader.Token.STRING -> {
                        val str = reader.nextString().trim()
                        str.toDoubleOrNull()
                    }
                    JsonReader.Token.NUMBER -> reader.nextDouble()
                    else -> {
                        reader.skipValue()
                        null
                    }
                }
            }

            @ToJson
            fun toJson(writer: JsonWriter, value: Double?) {
                writer.value(value)
            }
        }

        fun create(): AviationWeatherApi {
            val userAgentInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "AeroWeather-Android/1.0 (flightmaster177@gmail.com)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(LenientStringAdapter())
                .add(LenientDoubleAdapter())
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AviationWeatherApi::class.java)
        }
    }
}
