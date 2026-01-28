package org.example.app.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter

import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.app.models.RawWeatherData
import org.example.app.models.City

interface WeatherRepository {
    suspend fun fetchWeatherData(city: City): RawWeatherData
    fun getSupportedCities(): List<City>
    suspend fun close()
}

class WeatherRepositoryImpl(
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    },
    private val baseUrl: String = "https://api.open-meteo.com/v1/forecast"
) : WeatherRepository {
    
    private val cities = listOf(
        City("London", 51.5074, -0.1278),
        City("New York", 40.7128, -74.0060),
        City("Tokyo", 35.6762, 139.6503),
        City("Paris", 48.8566, 2.3522),
        City("Sydney", -33.8688, 151.2093)
    )
    
    override suspend fun fetchWeatherData(city: City): RawWeatherData {
        val response: OpenMeteoResponse = client.get(baseUrl) {
            parameter("latitude", city.latitude)
            parameter("longitude", city.longitude)
            parameter("current_weather", "true")
            parameter("hourly", "relativehumidity_2m,surface_pressure")
            parameter("timezone", "auto")
        }.body()
        
        val currentHour = getCurrentHourIndex()
        
        return RawWeatherData(
            temperature = response.current_weather.temperature,
            humidity = response.hourly.relativehumidity_2m[currentHour],
            pressure = response.hourly.surface_pressure[currentHour],
            weatherCode = response.current_weather.weathercode,
            city = city.name
        )
    }
    
    override fun getSupportedCities(): List<City> = cities
    
    private fun getCurrentHourIndex(): Int {
        val currentHour = java.time.LocalDateTime.now().hour
        return currentHour
    }
    

    
    override suspend fun close() {
        client.close()
    }
}

@Serializable
data class OpenMeteoResponse(
    val current_weather: CurrentWeather,
    val hourly: HourlyData
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val time: String
)

@Serializable
data class HourlyData(
    val time: List<String>,
    val relativehumidity_2m: List<Double>,
    val surface_pressure: List<Double>
)