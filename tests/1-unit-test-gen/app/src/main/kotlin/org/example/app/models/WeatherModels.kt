package org.example.app.models

data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class RawWeatherData(
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val weatherCode: Int,
    val city: String
)

data class WeatherData(
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val description: String,
    val city: String
)

data class TemperatureAnalysis(
    val average: Double,
    val min: Double,
    val max: Double,
    val trend: Double,
    val recommendations: List<String>
)